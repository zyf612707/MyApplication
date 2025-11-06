package com.example.upload10.data.remote

import com.example.upload10.data.KnowledgeCard
import okhttp3.MultipartBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface UploadService {
    @Multipart
    @POST("upload")
    suspend fun uploadFile(
        @Part file: MultipartBody.Part
    ): List<KnowledgeCard>
}