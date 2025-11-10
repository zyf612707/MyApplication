package com.example.myapplication

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.myapplication.di.Injector
import com.example.myapplication.ui.viewmodel.SingleEvent
import com.example.myapplication.ui.viewmodel.UiState
import com.example.myapplication.ui.viewmodel.UploadViewModel
import androidx.compose.foundation.clickable

// 卡片数据类保持不变
data class FlashCard(
    val title: String,
    val content: String,
    var isFront: Boolean = true
)

class MainActivity : AppCompatActivity() {

    private val uploadViewModel: UploadViewModel by viewModels { Injector.provideViewModelFactory(this) }

    private val openDocumentLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { uploadViewModel.onFileSelected(it, contentResolver) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 直接显示首页，不再在onCreate中设置Compose内容
        showHomePage()
    }

    private fun openPdf(uri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "No PDF viewer found", Toast.LENGTH_SHORT).show()
        }
    }

    // 首页（有三个按钮）
    fun showHomePage() {
        setContentView(R.layout.activity_main)

        val btnUpload: Button = findViewById(R.id.btn_upload)
        val btnCard: Button = findViewById(R.id.btn_card)
        val btnQuiz: Button = findViewById(R.id.btn_quiz)

        btnUpload.setOnClickListener { showUploadPage() }
        btnCard.setOnClickListener { showCardPage() }
        btnQuiz.setOnClickListener { showQuizPage() }

        Log.d("MainActivity", "首页显示完成")
    }

    // 上传资料页 - 修改为显示Compose内容
    private fun showUploadPage() {
        Log.d("MainActivity", "显示上传页面（Compose）")

        setContent {
            UploadComposeScreen(
                onBackToHome = { showHomePage() },
                openDocumentLauncher = openDocumentLauncher,
                uploadViewModel = uploadViewModel,
                contentResolver = contentResolver,
                openPdf = { uri -> openPdf(uri) }
            )
        }
    }

    // 卡片页
    private fun showCardPage() {
        Log.d("MainActivity", "显示卡片页面（Fragment）")

        try {
            val cardFragment = CardFragment()

            supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, cardFragment)
                .addToBackStack("card")
                .commit()

            Log.d("MainActivity", "卡片Fragment事务完成")
        } catch (e: Exception) {
            Log.e("MainActivity", "显示卡片Fragment错误", e)
            Toast.makeText(this, "打开卡片页面失败", Toast.LENGTH_SHORT).show()
        }
    }

    // AI 抽查页
    private fun showQuizPage() {
        Log.d("MainActivity", "显示问答页面（Fragment）")

        supportFragmentManager.beginTransaction()
            .replace(android.R.id.content, QuizFragment())
            .addToBackStack("quiz")
            .commit()
    }
}

// 新的Compose上传页面组件
// 在MainActivity.kt中更新UploadComposeScreen函数
@Composable
fun UploadComposeScreen(
    onBackToHome: () -> Unit,
    openDocumentLauncher: androidx.activity.result.ActivityResultLauncher<Array<String>>,
    uploadViewModel: UploadViewModel,
    contentResolver: android.content.ContentResolver,
    openPdf: (Uri) -> Unit
) {
    val uiState by uploadViewModel.uiState.collectAsState()
    val context = LocalContext.current

    // 处理ViewModel的事件
    LaunchedEffect(Unit) {
        uploadViewModel.singleEvent.collect { event ->
            when (event) {
                is SingleEvent.ViewPdf -> openPdf(event.uri)
                is SingleEvent.UploadSuccess -> Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                is SingleEvent.UploadError -> Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
                is SingleEvent.AIProcessSuccess -> {
                    Toast.makeText(context, "成功生成${event.cardCount}张知识卡片", Toast.LENGTH_LONG).show()
                }
                is SingleEvent.AIProcessError -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
                }
                is SingleEvent.AIProcessProgress -> {
                    // 添加AIProcessProgress分支
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // 根据状态显示不同的界面
    when (val state = uiState) {
        is UiState.Idle -> {
            IdleUploadScreen(
                onSelectFileClick = {
                    openDocumentLauncher.launch(arrayOf("text/plain", "application/pdf"))
                },
                onBackToHome = onBackToHome
            )
        }
        is UiState.FileSelected -> {
            FileSelectedScreen(
                fileName = state.fileName,
                onViewContent = { uploadViewModel.viewSelectedFile(contentResolver) },
                onUpload = {
                    // 修改为使用AI处理
                    uploadViewModel.uploadAndProcessFile(contentResolver, processWithAI = true)
                },
                onBackToHome = onBackToHome
            )
        }
        is UiState.ViewingTextContent -> {
            TextContentScreen(
                content = state.content,
                onBack = { uploadViewModel.goBackToFileSelected() },
                onBackToHome = onBackToHome
            )
        }
        is UiState.Processing -> {
            // 处理中状态
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("AI正在处理内容...")
            }
        }
        is UiState.Processed -> {
            // 处理完成状态
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("✅ 处理完成")
                Text("生成 ${state.cardCount} 张知识卡片")
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onBackToHome) {
                    Text("返回首页")
                }
            }
        }
    }
}

// 初始状态屏幕
@Composable
fun IdleUploadScreen(onSelectFileClick: () -> Unit, onBackToHome: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 返回首页按钮
        Button(
            onClick = onBackToHome,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Text("返回首页")
        }

        // 主要内容
        Column(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "文件上传",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 32.dp)
            )
            Button(onClick = onSelectFileClick) {
                Text("选择文件")
            }
        }
    }
}

// 文件已选择屏幕
@Composable
fun FileSelectedScreen(
    fileName: String,
    onViewContent: () -> Unit,
    onUpload: () -> Unit,
    onBackToHome: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 返回首页按钮
        Button(
            onClick = onBackToHome,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Text("返回首页")
        }

        // 文件信息
        Column(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("已选择文件:")
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = fileName,
                modifier = Modifier.clickable(onClick = onViewContent),
                textDecoration = TextDecoration.Underline
            )
            Spacer(modifier = Modifier.height(32.dp))

            // 操作按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(onClick = onViewContent) {
                    Text("查看内容")
                }
                Button(onClick = onUpload) {
                    Text("上传文件")
                }
            }
        }
    }
}

// 增强的文本内容查看屏幕（替换原来的TextContentScreen）
@Composable
fun EnhancedTextContentScreen(
    content: String,
    fileName: String,
    onBack: () -> Unit,
    onProcessWithAI: (String, String) -> Unit,
    onBackToHome: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 顶部按钮栏
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(onClick = onBackToHome) {
                Text("返回首页")
            }
            Button(onClick = onBack) {
                Text("返回文件选择")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // AI处理按钮
        Button(
            onClick = { onProcessWithAI(content, fileName) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("使用AI处理内容")
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 文本内容
        Text(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .weight(1f),
            text = content
        )
    }
}

// 处理中屏幕
@Composable
fun ProcessingScreen(
    fileName: String,
    onBackToHome: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 返回首页按钮
        Button(
            onClick = onBackToHome,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Text("返回首页")
        }

        // 处理中内容
        Column(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(64.dp),
                strokeWidth = 4.dp
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "AI正在处理文件",
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = fileName,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "正在提取知识点并生成学习卡片...",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

// 结果屏幕
@Composable
fun ResultScreen(
    fileName: String,
    cardCount: Int,
    onViewCards: () -> Unit,
    onProcessAnother: () -> Unit,
    onBackToHome: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 返回首页按钮
        Button(
            onClick = onBackToHome,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Text("返回首页")
        }

        // 结果内容
        Column(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "✅ 处理完成",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "文件: $fileName",
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "生成知识卡片: $cardCount 张",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 操作按钮
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onViewCards,
                    modifier = Modifier.width(200.dp)
                ) {
                    Text("查看学习卡片")
                }

                Button(
                    onClick = onProcessAnother,
                    modifier = Modifier.width(200.dp)
                ) {
                    Text("处理另一个文件")
                }
            }
        }
    }
}

// 原有的Composable函数保持不变（作为备用）
@Composable
fun TextContentScreen(
    content: String,
    onBack: () -> Unit,
    onBackToHome: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 顶部按钮栏
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(onClick = onBackToHome) {
                Text("返回首页")
            }
            Button(onClick = onBack) {
                Text("返回文件选择")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 文本内容
        Text(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .weight(1f),
            text = content
        )
    }
}