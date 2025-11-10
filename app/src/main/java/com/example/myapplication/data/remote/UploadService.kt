package com.example.myapplication.data.remote

import okhttp3.MultipartBody
import retrofit2.http.*
import retrofit2.http.POST
import com.example.myapplication.data.KnowledgeCard
import com.example.myapplication.data.model.AIProcessRequest
import com.example.myapplication.data.model.AIProcessResponse
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.http.*
import com.example.myapplication.ui.viewmodel.AIRequest
import com.example.myapplication.ui.viewmodel.AIResponse

interface UploadService {
    @Multipart
    @POST("upload")
    suspend fun uploadFile(
        @Part file: MultipartBody.Part
    ): List<KnowledgeCard>

    // 修复：添加正确的返回类型和注解
    @POST("chat/completions")
    suspend fun processContentWithAI(@Body request: AIRequest): Response<AIResponse>

    // 新增AI处理接口
    @POST("ai/process")
    @Headers("Content-Type: application/json")
    suspend fun processContentWithAI(
        @Body request: AIProcessRequest
    ): AIProcessResponse

    interface UploadService {
        @Multipart
        @POST("upload")
        suspend fun uploadFile(@Part file: MultipartBody.Part): List<KnowledgeCard>

        // 新增DeepSeek API接口
        @Headers("Content-Type: application/json")
        @POST("chat/completions")
        suspend fun processContentWithAI(@Body request: AIRequest): Response<AIResponse>
    }
}

private fun provideUploadService(): UploadService {
    return Retrofit.Builder()
        .baseUrl("https://api.deepseek.com/v1/") // 正确的DeepSeek API地址
        .addConverterFactory(GsonConverterFactory.create())
        .client(
            OkHttpClient.Builder()
                .addInterceptor { chain ->
                    val request = chain.request().newBuilder()
                        .addHeader("Authorization", "Bearer sk-71f11734e5394e3c886cd23d3f95b7eb") // 在这里添加API密钥
                        .addHeader("Content-Type", "application/json")
                        .build()
                    chain.proceed(request)
                }
                .build()
        )
        .build()
        .create(UploadService::class.java)
}