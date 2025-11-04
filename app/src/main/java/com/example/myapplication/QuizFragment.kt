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
import java.util.*
import androidx.lifecycle.lifecycleScope

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

        // 直接初始化，不使用协程（参考ChatFragment的成功做法）
        initializeViews(view)
        setupRecyclerView()
        setupClickListeners()
        startQuizSession()
    }

    private fun initializeViews(view: View) {
        Log.d("QuizFragment", "initializeViews: 开始初始化视图")

        try {
            // 使用更安全的findViewById方式
            recyclerMessages = view.findViewById(R.id.recycler_messages)
            editTextAnswer = view.findViewById(R.id.edit_text_answer)
            fabSend = view.findViewById(R.id.fab_send)
            loadingAnimation = view.findViewById(R.id.loading_animation)
            thinkingAnimation = view.findViewById(R.id.thinking_animation)
            btnBackHomeQuiz = view.findViewById(R.id.btn_back_home_quiz)

            // 详细的视图检查
            Log.d("QuizFragment", "=== 视图初始化检查 ===")
            Log.d("QuizFragment", "recyclerMessages: ${recyclerMessages != null}")
            Log.d("QuizFragment", "editTextAnswer: ${editTextAnswer != null}")
            Log.d("QuizFragment", "fabSend: ${fabSend != null} (ID: ${fabSend.id})")
            Log.d("QuizFragment", "loadingAnimation: ${loadingAnimation != null}")
            Log.d("QuizFragment", "thinkingAnimation: ${thinkingAnimation != null}")
            Log.d("QuizFragment", "btnBackHomeQuiz: ${btnBackHomeQuiz != null}")

            // 特别检查fabSend的详细状态
            if (fabSend != null) {
                Log.d("QuizFragment", "fabSend详细状态:")
                Log.d("QuizFragment", " - visibility: ${fabSend.visibility}")
                Log.d("QuizFragment", " - enabled: ${fabSend.isEnabled}")
                Log.d("QuizFragment", " - clickable: ${fabSend.isClickable}")
                Log.d("QuizFragment", " - focusable: ${fabSend.isFocusable}")
            } else {
                Log.e("QuizFragment", "❌ fabSend为null！")
            }

        } catch (e: Exception) {
            Log.e("QuizFragment", "❌ 初始化视图时出错", e)
            // 即使出错也不崩溃，显示错误信息
            showToast("界面初始化失败，请重启应用")
        }
    }

    private fun setupRecyclerView() {
        Log.d("QuizFragment", "setupRecyclerView: 设置RecyclerView")
        messageAdapter = MessageAdapter(messageList)
        recyclerMessages.apply {
            layoutManager = LinearLayoutManager(requireContext()).apply {
                stackFromEnd = true // 从底部开始显示
            }
            adapter = messageAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupClickListeners() {
        Log.d("QuizFragment", "setupClickListeners: 设置点击监听器")

        // 🔥 关键修复：使用ChatFragment的成功模式
        // 1. 先测试最简单的点击事件
        fabSend.setOnClickListener {
            Log.d("QuizFragment", "🎯 发送按钮被点击 - 简单测试")
            Toast.makeText(requireContext(), "按钮被点击了！", Toast.LENGTH_SHORT).show()

            // 2. 然后处理实际逻辑
            handleSendMessage()
        }

        // 返回按钮
        btnBackHomeQuiz.setOnClickListener {
            Log.d("QuizFragment", "返回按钮被点击")
            requireActivity().onBackPressed()
        }

        // 输入框回车键发送
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

            // 获取AI回复
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

    private fun getAIResponse(userInput: String) {
        Log.d("QuizFragment", "getAIResponse: 获取AI回复")

        // 显示思考动画
        showThinkingAnimation(true)

        // 使用协程处理异步操作
        lifecycleScope.launch {
            try {
                // 模拟网络延迟
                delay(1000)

                // 使用安全的模拟回复
                val aiResponse = generateAIResponse(userInput)

                // 在主线程更新UI
                withContext(Dispatchers.Main) {
                    showThinkingAnimation(false)
                    addAiMessage(aiResponse)
                    Log.d("QuizFragment", "✅ AI回复处理完成")
                }

            } catch (e: Exception) {
                Log.e("QuizFragment", "❌ AI回复处理失败", e)
                withContext(Dispatchers.Main) {
                    showThinkingAnimation(false)
                    addAiMessage("抱歉，出现了一些错误。请重试。")
                }
            }
        }
    }

    private fun generateAIResponse(userInput: String): String {
        return when {
            userInput.length < 3 -> "您的回答有点简短，可以再详细说明一下吗？🤔"
            userInput.contains("不知道") || userInput.contains("不清楚") ->
                "没关系！让我们一起来学习！正确答案是：这是一个需要掌握的重要知识点。"
            userInput.length > 100 -> "👍 很详细的回答！您的理解很深入。补充一点：这个知识点在实际应用中很重要。"
            else -> "✅ 很好的回答！您的理解基本正确。✨"
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
            "请说明相对论的基本原理"
        )
        return questions.random()
    }

    private fun showLoading(show: Boolean) {
        loadingAnimation.visibility = if (show) View.VISIBLE else View.GONE
        Log.d("QuizFragment", "加载动画: ${if (show) "显示" else "隐藏"}")
    }

    private fun showThinkingAnimation(show: Boolean) {
        thinkingAnimation.visibility = if (show) View.VISIBLE else View.GONE
        Log.d("QuizFragment", "思考动画: ${if (show) "显示" else "隐藏"}")
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d("QuizFragment", "onDestroyView: 视图销毁")
    }
}

// 数据类
data class ChatMessage(val message: String, val isUser: Boolean)

// 适配器
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