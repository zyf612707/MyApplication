package com.example.myapplication

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment

class CardFragment : Fragment() {

    // 卡片数据
    private val cardList = listOf(
        CardData("人工智能基础", "人工智能是一门研究如何让机器模拟人类智能的科学。"),
        CardData("机器学习", "机器学习是人工智能的一个分支，让计算机通过数据自动学习改进。"),
        CardData("深度学习", "深度学习是基于神经网络的一种机器学习方法。"),
        CardData("自然语言处理", "自然语言处理是让计算机理解和生成人类语言的技术。"),
        CardData("计算机视觉", "计算机视觉是让计算机能够'看'和理解图像和视频的技术。")
    )

    private var currentCardIndex = 0
    private var startX = 0f
    private var isClick = true // 标记是否为点击事件

    // 视图元素
    private lateinit var flashcard: CardView
    private lateinit var frontText: TextView
    private lateinit var backText: TextView
    private lateinit var btnBack: Button
    private lateinit var cardIndicator: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d("CardFragment", "onCreateView called")
        return inflater.inflate(R.layout.fragment_card, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("CardFragment", "onViewCreated called")

        // 初始化视图
        initViews(view)

        // 设置事件监听器
        setupEventListeners()

        // 显示第一张卡片
        updateCard()

        Log.d("CardFragment", "Fragment setup complete, total cards: ${cardList.size}")
    }

    private fun initViews(view: View) {
        flashcard = view.findViewById(R.id.flashcard)
        frontText = view.findViewById(R.id.frontText)
        backText = view.findViewById(R.id.backText)
        btnBack = view.findViewById(R.id.btn_back_home_card)
        cardIndicator = view.findViewById(R.id.cardIndicator)

        Log.d("CardFragment", "All views initialized")
    }

    private fun setupEventListeners() {
        // 1. 滑动和点击事件统一处理
        flashcard.setOnTouchListener { _, event ->
            handleTouchEvent(event)
        }

        // 2. 返回按钮
        btnBack.setOnClickListener {
            Log.d("CardFragment", "Back button clicked")
            activity?.supportFragmentManager?.popBackStack()
        }
    }

    private fun updateCard() {
        val currentCard = cardList[currentCardIndex]
        frontText.text = currentCard.title
        backText.text = currentCard.content

        // 更新指示器
        cardIndicator.text = "${currentCardIndex + 1}/${cardList.size}"

        // 重置为正面显示
        frontText.visibility = View.VISIBLE
        backText.visibility = View.GONE

        Log.d("CardFragment", "Card updated: ${currentCardIndex + 1}/${cardList.size} - ${currentCard.title}")
    }

    private fun flipCard() {
        if (frontText.visibility == View.VISIBLE) {
            // 翻到背面
            frontText.visibility = View.GONE
            backText.visibility = View.VISIBLE
            Log.d("CardFragment", "Flipped to back")
        } else {
            // 翻回正面
            frontText.visibility = View.VISIBLE
            backText.visibility = View.GONE
            Log.d("CardFragment", "Flipped to front")
        }
    }

    private fun handleTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // 记录触摸起始位置
                startX = event.x
                isClick = true // 初始假设是点击
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                // 如果有明显移动，则不是点击
                if (Math.abs(event.x - startX) > 10) {
                    isClick = false
                }
            }
            MotionEvent.ACTION_UP -> {
                val endX = event.x
                val deltaX = endX - startX

                Log.d("CardFragment", "Touch event - deltaX: $deltaX, isClick: $isClick")

                // 如果是点击事件
                if (isClick && Math.abs(deltaX) < 10) {
                    Log.d("CardFragment", "Card clicked - flipping")
                    flipCard()
                    return true
                }
                // 检测左滑（deltaX为负值）
                else if (deltaX < -100) {
                    // 左滑 - 切换到下一张卡片
                    currentCardIndex = (currentCardIndex + 1) % cardList.size
                    updateCard()
                    Log.d("CardFragment", "Swiped left - Next card: ${currentCardIndex + 1}")
                    return true
                }
                // 检测右滑（deltaX为正值）
                else if (deltaX > 100) {
                    // 右滑 - 切换到上一张卡片
                    currentCardIndex = if (currentCardIndex - 1 < 0) {
                        cardList.size - 1
                    } else {
                        currentCardIndex - 1
                    }
                    updateCard()
                    Log.d("CardFragment", "Swiped right - Previous card: ${currentCardIndex + 1}")
                    return true
                }
            }
        }
        return false
    }

    // 卡片数据类
    data class CardData(val title: String, val content: String)
}