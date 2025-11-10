package com.example.myapplication.ui.viewmodel

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.AppDatabase
import com.example.myapplication.data.KnowledgeCard
import com.example.myapplication.data.remote.UploadService
import com.example.myapplication.ui.viewmodel.AIRequest
import com.example.myapplication.ui.viewmodel.AIResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import java.io.BufferedReader
import java.io.InputStreamReader

// UiState 密封类保持不变
sealed class UiState {
    object Idle : UiState()
    data class FileSelected(val uri: Uri, val fileName: String) : UiState()
    data class ViewingTextContent(val content: String, val previousState: FileSelected) : UiState()
    object Processing : UiState()
    data class Processed(val fileName: String, val cardCount: Int) : UiState()
}

// SingleEvent 密封类保持不变
sealed class SingleEvent {
    data class ViewPdf(val uri: Uri) : SingleEvent()
    data class UploadSuccess(val message: String) : SingleEvent()
    data class UploadError(val message: String) : SingleEvent()
    data class AIProcessError(val message: String) : SingleEvent()
    data class AIProcessProgress(val message: String) : SingleEvent()
    data class AIProcessSuccess(
        val fileName: String,
        val cardCount: Int,
        val qualityScore: Double,
        val fromCache: Boolean
    ) : SingleEvent()
}

class UploadViewModel(
    private val uploadService: UploadService,
    private val appDatabase: AppDatabase
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState = _uiState.asStateFlow()

    private val _singleEvent = MutableSharedFlow<SingleEvent>()
    val singleEvent = _singleEvent.asSharedFlow()

    // 文件选择方法
    fun onFileSelected(uri: Uri, contentResolver: ContentResolver) {
        val fileName = getFileName(uri, contentResolver) ?: "Unknown File"
        _uiState.value = UiState.FileSelected(uri, fileName)
    }

    // 查看文件内容方法
    fun viewSelectedFile(contentResolver: ContentResolver) {
        val currentState = _uiState.value
        if (currentState !is UiState.FileSelected) return

        viewModelScope.launch {
            try {
                val content = readTextFromUri(currentState.uri, contentResolver)
                _uiState.value = UiState.ViewingTextContent(content, currentState)
            } catch (e: Exception) {
                _singleEvent.emit(SingleEvent.UploadError("无法读取文件内容: ${e.message}"))
            }
        }
    }

    // 返回文件选择状态方法
    fun goBackToFileSelected() {
        val currentState = _uiState.value
        if (currentState is UiState.ViewingTextContent) {
            _uiState.value = currentState.previousState
        }
    }

    // 修改后的 processFileWithAI 方法 - 现在调用真实API
    fun processFileWithAI(content: String, fileName: String) {
        viewModelScope.launch {
            try {
                _uiState.value = UiState.Processing
                _singleEvent.emit(SingleEvent.AIProcessProgress("开始调用AI处理内容..."))

                Log.d("UploadViewModel", "开始调用DeepSeek API处理文件: $fileName")

                // 1. 构建AI请求
                val aiRequest = buildAIRequest(content, fileName)

                _singleEvent.emit(SingleEvent.AIProcessProgress("正在调用DeepSeek API..."))

                // 2. 调用真实的DeepSeek API
                val aiResponse = callDeepSeekAPI(aiRequest)

                if (aiResponse != null) {
                    _singleEvent.emit(SingleEvent.AIProcessProgress("AI处理完成，正在解析结果..."))

                    // 3. 解析AI响应并转换为知识卡片
                    val knowledgeCards = parseAIResponseToCards(aiResponse, fileName)
                    Log.d("UploadViewModel", "成功生成${knowledgeCards.size}张知识卡片")

                    // 4. 存入数据库
                    appDatabase.knowledgeCardDao().insertAll(knowledgeCards)
                    Log.d("UploadViewModel", "数据已存入数据库")

                    val cardCount = knowledgeCards.size
                    _uiState.value = UiState.Processed(fileName, cardCount)
                    _singleEvent.emit(SingleEvent.AIProcessSuccess(
                        fileName,
                        cardCount,
                        calculateQualityScore(knowledgeCards),
                        false
                    ))
                } else {
                    throw Exception("API调用返回空响应")
                }

            } catch (e: Exception) {
                Log.e("UploadViewModel", "DeepSeek API处理失败", e)
                _singleEvent.emit(SingleEvent.AIProcessError("AI处理失败: ${e.message}"))
                _uiState.value = UiState.Idle
            }
        }
    }

    // 构建AI请求
    private fun buildAIRequest(content: String, fileName: String): AIRequest {
        return AIRequest(
            model = "deepseek-chat", // 使用deepseek-chat模型
            messages = listOf(
                AIMessage("system", "你是一个专业的学习助手，擅长从文本内容中提取结构化知识。请将用户提供的学习内容整理成知识卡片格式。"),
                AIMessage("user", """
                    请将以下文件内容结构化提取为知识卡片：
                    
                    文件名称: $fileName
                    内容要求:
                    1. 识别主要知识点和概念
                    2. 为每个知识点提取核心关键词（正面显示）
                    3. 提供详细的解释或定义（背面显示）
                    4. 按逻辑顺序编号
                    5. 如果有章节结构，请标注章节信息
                    
                    文件内容:
                    ${content.take(4000)} // 限制内容长度，避免超过token限制
                    
                    请以规范的JSON格式回复，包含以下字段：
                    - file_tag: 文件主题标签
                    - sections: 章节列表，每个章节包含section_title和items
                    - items: 知识点列表，每个知识点包含item_id, keyword, explanation
                """.trimIndent())
            ),
            temperature = 0.7,
            max_tokens = 2000
        )
    }

    // 调用DeepSeek API
    private suspend fun callDeepSeekAPI(aiRequest: AIRequest): AIResponse? {
        return try {
            _singleEvent.emit(SingleEvent.AIProcessProgress("正在与DeepSeek API通信..."))

            val response = uploadService.processContentWithAI(aiRequest)

            if (response.isSuccessful) {
                response.body()
            } else {
                val errorBody = response.errorBody()?.string() ?: "未知错误"
                Log.e("UploadViewModel", "API调用失败: ${response.code()} - $errorBody")
                throw Exception("API请求失败: ${response.code()} - $errorBody")
            }
        } catch (e: Exception) {
            Log.e("UploadViewModel", "调用DeepSeek API异常", e)
            throw Exception("网络请求失败: ${e.message}")
        }
    }

    // 解析AI响应为知识卡片
    private fun parseAIResponseToCards(aiResponse: AIResponse, fileName: String): List<KnowledgeCard> {
        val cards = mutableListOf<KnowledgeCard>()

        try {
            // 这里解析AI返回的JSON结构
            // 假设AI返回的数据结构符合我们的预期
            val fileTag = aiResponse.fileTag ?: "默认标签"

            aiResponse.sections?.forEachIndexed { sectionIndex, section ->
                val sectionTitle = section.sectionTitle ?: "第${sectionIndex + 1}部分"

                section.items?.forEachIndexed { itemIndex, item ->
                    val card = KnowledgeCard(
                        title = item.keyword ?: "知识点",
                        content = item.explanation ?: "",
                        fileTag = fileTag,
                        section = sectionTitle,
                        itemId = item.itemId ?: "${sectionIndex + 1}-${itemIndex + 1}",
                        keyword = item.keyword ?: "",
                        explanation = item.explanation ?: "",
                        sourceFile = fileName
                    )
                    cards.add(card)
                }
            }

            // 如果AI没有返回有效数据，使用备用方案
            if (cards.isEmpty()) {
                cards.addAll(fallbackProcessing(aiResponse.choices?.firstOrNull()?.message?.content ?: "", fileName))
            }

        } catch (e: Exception) {
            Log.e("UploadViewModel", "解析AI响应失败", e)
            // 解析失败时使用备用处理方案
            cards.addAll(fallbackProcessing(aiResponse.choices?.firstOrNull()?.message?.content ?: "", fileName))
        }

        return cards
    }

    // 备用处理方案（当AI返回的数据不符合预期时）
    private fun fallbackProcessing(content: String, fileName: String): List<KnowledgeCard> {
        val cards = mutableListOf<KnowledgeCard>()

        // 简单的文本分割处理作为备用方案
        val lines = content.split("\n", ".", "。").filter { it.isNotBlank() && it.length > 10 }

        lines.forEachIndexed { index, line ->
            if (line.length > 5) {
                val card = KnowledgeCard(
                    title = "知识点 ${index + 1}",
                    content = line.trim(),
                    fileTag = "AI处理",
                    section = "第${(index / 5) + 1}部分",
                    itemId = (index + 1).toString(),
                    keyword = extractKeyword(line),
                    explanation = line,
                    sourceFile = fileName
                )
                cards.add(card)
            }
        }

        if (cards.isEmpty()) {
            cards.add(createDefaultCard(fileName))
        }

        return cards
    }

    // 从文本中提取关键词
    private fun extractKeyword(text: String): String {
        // 简单的关键词提取逻辑
        val words = text.split(" ", "、", "，").filter { it.length in 2..6 }
        return words.firstOrNull() ?: text.take(15) + (if (text.length > 15) "..." else "")
    }

    // 创建默认卡片
    private fun createDefaultCard(fileName: String): KnowledgeCard {
        return KnowledgeCard(
            title = "默认知识点",
            content = "这是一个由AI生成的知识点",
            fileTag = "AI处理",
            section = "第一部分",
            itemId = "1",
            keyword = "示例关键词",
            explanation = "这是AI对内容的理解和总结",
            sourceFile = fileName
        )
    }

    // 计算内容质量分数
    private fun calculateQualityScore(cards: List<KnowledgeCard>): Double {
        if (cards.isEmpty()) return 0.0

        var totalScore = 0.0
        cards.forEach { card ->
            var cardScore = 0.0

            // 关键词质量 (0-40分)
            cardScore += when {
                card.keyword.length in 2..5 -> 40.0
                card.keyword.length in 6..10 -> 30.0
                else -> 20.0
            }

            // 解释质量 (0-40分)
            cardScore += when {
                card.explanation.length in 50..200 -> 40.0
                card.explanation.length in 30..49 -> 30.0
                card.explanation.length in 201..300 -> 30.0
                else -> 20.0
            }

            // 内容完整性 (0-20分)
            cardScore += if (card.keyword.isNotBlank() && card.explanation.isNotBlank()) 20.0 else 10.0

            totalScore += cardScore / 100.0
        }

        return totalScore / cards.size
    }

    // 修改现有的上传方法
    fun uploadAndProcessFile(contentResolver: ContentResolver, processWithAI: Boolean = true) {
        val currentState = _uiState.value
        if (currentState !is UiState.FileSelected) return

        viewModelScope.launch {
            try {
                _uiState.value = UiState.Processing

                if (processWithAI) {
                    // 读取文件内容进行AI处理
                    val content = readTextFromUri(currentState.uri, contentResolver)
                    processFileWithAI(content, currentState.fileName)
                } else {
                    // 原有的上传逻辑
                    val filePart = withContext(Dispatchers.IO) {
                        contentResolver.openInputStream(currentState.uri)?.use { inputStream ->
                            val requestBody = inputStream.readBytes().toRequestBody("file/*".toMediaTypeOrNull())
                            MultipartBody.Part.createFormData("file", "upload_file", requestBody)
                        }
                    }

                    if (filePart != null) {
                        val knowledgeCards = uploadService.uploadFile(filePart)
                        appDatabase.knowledgeCardDao().insertAll(knowledgeCards)
                        _singleEvent.emit(SingleEvent.UploadSuccess("文件上传成功！"))
                        _uiState.value = UiState.Idle
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _singleEvent.emit(SingleEvent.UploadError("处理失败: ${e.message}"))
                _uiState.value = UiState.Idle
            }
        }
    }

    // 辅助方法：获取文件名
    private fun getFileName(uri: Uri, contentResolver: ContentResolver): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val displayNameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (displayNameIndex != -1) {
                        result = it.getString(displayNameIndex)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != null && cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result
    }

    // 辅助方法：从URI读取文本
    private suspend fun readTextFromUri(uri: Uri, contentResolver: ContentResolver): String {
        return withContext(Dispatchers.IO) {
            val stringBuilder = StringBuilder()
            contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    var line: String? = reader.readLine()
                    while (line != null) {
                        stringBuilder.append(line).append('\n')
                        line = reader.readLine()
                    }
                }
            }
            stringBuilder.toString()
        }
    }
}

// 新增的AI相关数据模型
data class AIRequest(
    val model: String,
    val messages: List<AIMessage>,
    val temperature: Double = 0.7,
    val max_tokens: Int = 2000
)

data class AIMessage(
    val role: String,
    val content: String
)

data class AIResponse(
    val choices: List<AIChoice>? = null,
    val fileTag: String? = null,
    val sections: List<AISection>? = null
)

data class AIChoice(
    val message: AIMessageContent
)

data class AIMessageContent(
    val content: String
)

data class AISection(
    val sectionTitle: String? = null,
    val items: List<AIItem>? = null
)

data class AIItem(
    val itemId: String? = null,
    val keyword: String? = null,
    val explanation: String? = null
)