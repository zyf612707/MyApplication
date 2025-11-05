package com.example.myapplication

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import androidx.lifecycle.lifecycleScope
import org.json.JSONArray

class QuizFragment : Fragment() {

    // 视图变量
    private lateinit var recyclerMessages: RecyclerView
    private lateinit var editTextAnswer: TextInputEditText
    private lateinit var fabSend: FloatingActionButton
    private lateinit var loadingAnimation: ProgressBar
    private lateinit var thinkingAnimation: ProgressBar
    private lateinit var btnBackHomeQuiz: ImageButton

    // 数据变量
    private val messageList = mutableListOf<ChatMessage>()
    private lateinit var messageAdapter: MessageAdapter

    // 🔥 DeepSeek API配置
    private val deepSeekApiKey = "sk-71f11734e5394e3c886cd23d3f95b7eb" // 替换为您的API密钥
    private val deepSeekApiUrl = "https://api.deepseek.com/v1/chat/completions"

    // 🔥 配置OkHttpClient（参考搜索结果的最佳实践）
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS) // 连接超时
        .readTimeout(60, TimeUnit.SECONDS)    // 读取超时（AI响应可能较慢）
        .writeTimeout(30, TimeUnit.SECONDS)   // 写入超时
        .retryOnConnectionFailure(true)      // 连接失败时重试
        .build()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d("QuizFragment", "onCreateView: 创建视图")
        return inflater.inflate(R.layout.fragment_quiz, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("QuizFragment", "onViewCreated: 视图创建完成")

        initializeViews(view)
        setupRecyclerView()
        setupClickListeners()
        startQuizSession()
    }

    private fun initializeViews(view: View) {
        Log.d("QuizFragment", "initializeViews: 开始初始化视图")

        try {
            recyclerMessages = view.findViewById(R.id.recycler_messages)
            editTextAnswer = view.findViewById(R.id.edit_text_answer)
            fabSend = view.findViewById(R.id.fab_send)
            loadingAnimation = view.findViewById(R.id.loading_animation)
            thinkingAnimation = view.findViewById(R.id.thinking_animation)
            btnBackHomeQuiz = view.findViewById(R.id.btn_back_home_quiz)

            Log.d("QuizFragment", "✅ 所有视图初始化完成")

        } catch (e: Exception) {
            Log.e("QuizFragment", "❌ 初始化视图时出错", e)
            showToast("界面初始化失败，请重启应用")
        }
    }

    private fun setupRecyclerView() {
        Log.d("QuizFragment", "setupRecyclerView: 设置RecyclerView")
        messageAdapter = MessageAdapter(messageList)
        recyclerMessages.apply {
            layoutManager = LinearLayoutManager(requireContext()).apply {
                stackFromEnd = true
            }
            adapter = messageAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupClickListeners() {
        Log.d("QuizFragment", "setupClickListeners: 设置点击监听器")

        // 发送按钮点击事件
        fabSend.setOnClickListener {
            Log.d("QuizFragment", "🎯 发送按钮被点击")
            handleSendMessage()
        }

        // 返回按钮
        btnBackHomeQuiz.setOnClickListener {
            Log.d("QuizFragment", "返回按钮被点击")
            requireActivity().onBackPressed()
        }

        // 回车键发送
        editTextAnswer.setOnKeyListener { _, keyCode, event ->
            if (keyCode == android.view.KeyEvent.KEYCODE_ENTER &&
                event.action == android.view.KeyEvent.ACTION_DOWN) {
                Log.d("QuizFragment", "回车键发送")
                fabSend.performClick()
                true
            } else {
                false
            }
        }

        Log.d("QuizFragment", "✅ 点击监听器设置完成")
    }

    private fun handleSendMessage() {
        Log.d("QuizFragment", "handleSendMessage: 处理发送消息")

        val userInput = editTextAnswer.text?.toString()?.trim() ?: ""
        Log.d("QuizFragment", "用户输入: '$userInput'")

        if (userInput.isNotEmpty()) {
            // 立即清空输入框
            editTextAnswer.setText("")

            // 添加用户消息到界面
            addUserMessage(userInput)

            // 🔥 调用真实的DeepSeek API
            getAIResponse(userInput)
        } else {
            Log.d("QuizFragment", "用户输入为空")
            showToast("请输入内容")
        }
    }

    private fun addUserMessage(message: String) {
        Log.d("QuizFragment", "添加用户消息: ${message.take(50)}...")

        val userMessage = ChatMessage(message, true)
        messageList.add(userMessage)
        messageAdapter.notifyItemInserted(messageList.size - 1)
        scrollToBottom()
    }

    private fun addAiMessage(message: String) {
        Log.d("QuizFragment", "添加AI消息: ${message.take(50)}...")

        val aiMessage = ChatMessage(message, false)
        messageList.add(aiMessage)
        messageAdapter.notifyItemInserted(messageList.size - 1)
        scrollToBottom()
    }

    private fun scrollToBottom() {
        recyclerMessages.post {
            if (messageList.isNotEmpty()) {
                recyclerMessages.smoothScrollToPosition(messageList.size - 1)
            }
        }
    }

    // 🔥 关键修改：接入真实的DeepSeek API
    private fun getAIResponse(userInput: String) {
        Log.d("QuizFragment", "getAIResponse: 调用真实DeepSeek API")

        // 显示思考动画
        showThinkingAnimation(true)

        // 使用协程处理异步API调用
        lifecycleScope.launch {
            try {
                Log.d("QuizFragment", "开始API调用流程")

                // 构建完整的对话上下文
                val conversationContext = buildConversationContext(userInput)
                Log.d("QuizFragment", "对话上下文构建完成")

                // 调用DeepSeek API
                val aiResponse = callDeepSeekAPI(conversationContext)
                Log.d("QuizFragment", "API调用成功，响应长度: ${aiResponse.length}")

                // 在主线程更新UI
                withContext(Dispatchers.Main) {
                    showThinkingAnimation(false)
                    addAiMessage(aiResponse)
                    Log.d("QuizFragment", "✅ AI回复显示完成")
                }

            } catch (e: Exception) {
                Log.e("QuizFragment", "❌ API调用失败", e)

                // 错误处理：显示友好的错误信息
                withContext(Dispatchers.Main) {
                    showThinkingAnimation(false)
                    val errorMessage = when {
                        e is IOException -> "网络连接失败，请检查网络设置"
                        e.message?.contains("401") == true -> "API密钥无效，请检查配置"
                        e.message?.contains("429") == true -> "请求过于频繁，请稍后重试"
                        else -> "服务暂时不可用：${e.message?.take(50)}..."
                    }
                    addAiMessage("❌ $errorMessage\n\n💡 提示：已切换至模拟回复模式")

                    // 降级到模拟回复
                    showSimulatedResponse(userInput)
                }
            }
        }
    }

    // 🔥 构建多轮对话上下文（保持对话连贯性）
    private fun buildConversationContext(userInput: String): List<Map<String, String>> {
        val messages = mutableListOf<Map<String, String>>()

        // 系统提示词（定义AI角色）
        messages.add(mapOf(
            "role" to "system",
            "content" to """你是一个智能学习助手，专门帮助学生通过问答方式巩固知识。请遵循以下规则：
            1. 根据学生的回答给予针对性的反馈和指导
            2. 如果回答正确，给予肯定并可以适当扩展相关知识
            3. 如果回答不完整，指出缺失的部分并给出提示
            4. 如果回答错误，不要直接给出答案，先引导思考
            5. 保持友好鼓励的语气，使用中文回复
            6. 回复长度控制在100-300字之间"""
        ))

        // 添加历史对话（最近3轮，避免上下文过长）
        val recentMessages = messageList.takeLast(6) // 最近3轮对话（每轮2条消息）
        recentMessages.forEach { chatMessage ->
            messages.add(mapOf(
                "role" to if (chatMessage.isUser) "user" else "assistant",
                "content" to chatMessage.message
            ))
        }

        // 添加当前用户输入
        messages.add(mapOf(
            "role" to "user",
            "content" to userInput
        ))

        Log.d("QuizFragment", "构建了${messages.size}条消息的上下文")
        return messages
    }

    // 🔥 真实的DeepSeek API调用（参考搜索结果的实现）
    private suspend fun callDeepSeekAPI(messages: List<Map<String, String>>): String {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("QuizFragment", "开始构建API请求")

                // 构建请求体（符合DeepSeek API格式）
                val requestBody = JSONObject().apply {
                    put("model", "deepseek-chat") // 使用deepseek-chat模型
                    put("messages", JSONArray(messages))
                    put("temperature", 0.7)      // 控制创造性
                    put("max_tokens", 1000)      // 最大响应长度
                    put("stream", false)         // 非流式响应
                }.toString()

                Log.d("QuizFragment", "请求体构建完成")

                // 构建HTTP请求
                val request = Request.Builder()
                    .url(deepSeekApiUrl)
                    .post(requestBody.toRequestBody("application/json".toMediaType()))
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Authorization", "Bearer $deepSeekApiKey")
                    .addHeader("Accept", "application/json")
                    .build()

                Log.d("QuizFragment", "发送API请求...")

                // 执行网络请求
                val response = client.newCall(request).execute()
                Log.d("QuizFragment", "收到API响应，状态码: ${response.code}")

                if (!response.isSuccessful) {
                    throw IOException("API请求失败: ${response.code} - ${response.message}")
                }

                // 解析响应
                val responseBody = response.body?.string()
                if (responseBody.isNullOrEmpty()) {
                    throw IOException("API返回空响应")
                }

                Log.d("QuizFragment", "响应体: ${responseBody.take(200)}...")

                // 解析JSON响应
                val jsonResponse = JSONObject(responseBody)
                val choices = jsonResponse.getJSONArray("choices")
                if (choices.length() == 0) {
                    throw IOException("API返回无效的choices数组")
                }

                val message = choices.getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
                    .trim()

                Log.d("QuizFragment", "成功解析AI回复")
                return@withContext message

            } catch (e: Exception) {
                Log.e("QuizFragment", "API调用异常", e)
                throw e // 重新抛出异常以便上层处理
            }
        }
    }

    // 🔥 降级方案：模拟回复（当API调用失败时使用）
    private fun showSimulatedResponse(userInput: String) {
        lifecycleScope.launch {
            delay(1000) // 模拟延迟

            val simulatedResponse = when {
                userInput.length < 3 -> "您的回答有点简短，可以再详细说明一下吗？🤔"
                userInput.contains("不知道") || userInput.contains("不清楚") ->
                    "没关系！让我们一起来学习！正确答案应该是：这是一个需要掌握的重要知识点。"
                userInput.length > 100 -> "👍 很详细的回答！您的理解很深入。补充一点：这个知识点在实际应用中很重要。"
                else -> "✅ 很好的回答！您的理解基本正确。✨"
            }

            withContext(Dispatchers.Main) {
                addAiMessage("💡 模拟回复（API不可用时）：\n$simulatedResponse")
            }
        }
    }

    private fun startQuizSession() {
        Log.d("QuizFragment", "startQuizSession: 开始问答会话")
        showLoading(true)

        lifecycleScope.launch {
            delay(800) // 模拟加载延迟

            withContext(Dispatchers.Main) {
                showLoading(false)
                val question = getRandomQuestion()
                addAiMessage(question)
                Log.d("QuizFragment", "✅ 问答会话开始")
            }
        }
    }

    private fun getRandomQuestion(): String {
        val questions = listOf(
            "请简述光合作用的主要过程？",
            "什么是牛顿第一定律？",
            "解释一下细胞分裂的不同阶段",
            "请说明相对论的基本原理",
            "请描述一下生态系统中的食物链概念？"
        )
        return questions.random()
    }

    private fun showLoading(show: Boolean) {
        loadingAnimation.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showThinkingAnimation(show: Boolean) {
        thinkingAnimation.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d("QuizFragment", "onDestroyView: 视图销毁")
    }
}

// 数据类保持不变
data class ChatMessage(val message: String, val isUser: Boolean)

// 适配器保持不变
class MessageAdapter(private val messages: List<ChatMessage>) :
    RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val layoutAiMessage: LinearLayout = itemView.findViewById(R.id.layout_ai_message)
        val layoutUserMessage: LinearLayout = itemView.findViewById(R.id.layout_user_message)
        val textAiMessage: TextView = itemView.findViewById(R.id.text_ai_message)
        val textUserMessage: TextView = itemView.findViewById(R.id.text_user_message)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messages[position]

        if (message.isUser) {
            holder.layoutUserMessage.visibility = View.VISIBLE
            holder.layoutAiMessage.visibility = View.GONE
            holder.textUserMessage.text = message.message
        } else {
            holder.layoutUserMessage.visibility = View.GONE
            holder.layoutAiMessage.visibility = View.VISIBLE
            holder.textAiMessage.text = message.message
        }
    }

    override fun getItemCount() = messages.size
}