package com.example.myapplication

import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import com.example.upload10.di.Injector
import com.example.upload10.ui.viewmodel.SingleEvent
import com.example.upload10.ui.viewmodel.UiState
import com.example.upload10.ui.viewmodel.UploadViewModel

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
                onUpload = { uploadViewModel.uploadFileToServer(contentResolver) },
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

// 文本内容查看屏幕
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

// 原有的Composable函数保持不变（作为备用）
@Composable
fun InitialScreen(onSelectFileClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = onSelectFileClick) {
            Text("选择文件")
        }
    }
}

@Composable
fun FileSelectedScreenOld(fileName: String, onViewContent: () -> Unit, onUpload: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
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
        Button(onClick = onUpload) {
            Text("上传文件")
        }
    }
}

@Composable
fun TextContentScreenOld(content: String, onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        Button(onClick = onBack) {
            Text("返回")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            text = content
        )
    }
}