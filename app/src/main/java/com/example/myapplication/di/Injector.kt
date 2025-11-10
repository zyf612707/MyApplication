package com.example.myapplication.di

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import com.example.myapplication.data.AppDatabase
import com.example.myapplication.data.remote.UploadService
import com.example.myapplication.ui.viewmodel.UploadViewModel
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object Injector {

    private fun provideUploadService(): UploadService {
        // 创建日志拦截器 - 修复：添加正确的导入
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        // 创建认证拦截器 - 修复：移除参数名
        val authInterceptor = Interceptor { chain ->
            val originalRequest = chain.request()
            val newRequest = originalRequest.newBuilder()
                .addHeader("Authorization", "Bearer sk-71f11734e5394e3c886cd23d3f95b7eb")
                .addHeader("Content-Type", "application/json")
                .build()
            chain.proceed(newRequest)
        }

        // 创建OkHttpClient - 修复：添加正确的导入
        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl("https://api.deepseek.com/v1/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(UploadService::class.java)
    }

    private fun provideDatabase(context: Context): AppDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java, "knowledge-db"
        ).build()
    }

    fun provideViewModelFactory(context: Context): ViewModelProvider.Factory {
        return object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(UploadViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return UploadViewModel(provideUploadService(), provideDatabase(context)) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}