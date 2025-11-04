package com.example.myapplication

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showHomePage()
    }

    // 首页（有三个按钮）
    private fun showHomePage() {
        setContentView(R.layout.activity_main)

        val btnUpload: Button = findViewById(R.id.btn_upload)
        val btnCard: Button = findViewById(R.id.btn_card)
        val btnQuiz: Button = findViewById(R.id.btn_quiz)

        btnUpload.setOnClickListener { showUploadPage() }
        btnCard.setOnClickListener { showCardPage() }
        btnQuiz.setOnClickListener { showQuizPage() }
    }

    // 上传资料页
    private fun showUploadPage() {
        setContentView(R.layout.fragment_upload)
        val btnBack: Button = findViewById(R.id.btn_back_home_upload)
        btnBack.setOnClickListener { showHomePage() }
    }

    // 卡片页
    private fun showCardPage() {
        setContentView(R.layout.fragment_card)
        val btnBack: Button = findViewById(R.id.btn_back_home_card)
        btnBack.setOnClickListener { showHomePage() }
    }

    // AI 抽查页hal
    private fun showQuizPage() {
        supportFragmentManager.beginTransaction()
            .replace(android.R.id.content, QuizFragment())
            .addToBackStack("quiz")
            .commit()
    }
}
