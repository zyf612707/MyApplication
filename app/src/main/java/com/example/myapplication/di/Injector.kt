package com.example.upload10.di

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import com.example.upload10.data.AppDatabase
import com.example.upload10.data.remote.UploadService
import com.example.upload10.ui.viewmodel.UploadViewModel
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object Injector {

    private fun provideUploadService(): UploadService {
        return Retrofit.Builder()
            .baseUrl("https://your.api.url/") // Replace with your API base URL
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