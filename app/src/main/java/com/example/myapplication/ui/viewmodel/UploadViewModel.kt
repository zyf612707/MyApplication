package com.example.upload10.ui.viewmodel

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.upload10.data.AppDatabase
import com.example.upload10.data.remote.UploadService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.BufferedReader
import java.io.InputStreamReader

sealed class UiState {
    object Idle : UiState()
    data class FileSelected(val uri: Uri, val fileName: String) : UiState()
    data class ViewingTextContent(val content: String, val previousState: FileSelected) : UiState()
}

class UploadViewModel(
    private val uploadService: UploadService,
    private val appDatabase: AppDatabase
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState = _uiState.asStateFlow()

    private val _singleEvent = MutableSharedFlow<SingleEvent>()
    val singleEvent = _singleEvent.asSharedFlow()

    fun onFileSelected(uri: Uri, contentResolver: ContentResolver) {
        val fileName = getFileName(uri, contentResolver) ?: "Unknown File"
        _uiState.value = UiState.FileSelected(uri, fileName)
    }

    fun viewSelectedFile(contentResolver: ContentResolver) {
        val currentState = _uiState.value
        if (currentState !is UiState.FileSelected) return

        viewModelScope.launch {
            val mimeType = contentResolver.getType(currentState.uri)

            // Fallback to check file extension if MIME type is null
            val effectiveMimeType = mimeType ?: when {
                currentState.fileName.endsWith(".txt", ignoreCase = true) -> "text/plain"
                currentState.fileName.endsWith(".pdf", ignoreCase = true) -> "application/pdf"
                else -> null
            }

            when (effectiveMimeType) {
                "text/plain" -> {
                    val content = readTextFromUri(currentState.uri, contentResolver)
                    _uiState.value = UiState.ViewingTextContent(content, currentState)
                }
                "application/pdf" -> {
                    _singleEvent.emit(SingleEvent.ViewPdf(currentState.uri))
                }
                else -> {
                    _singleEvent.emit(SingleEvent.UploadError("无法识别的文件类型，无法预览。"))
                }
            }
        }
    }

    fun goBackToFileSelected() {
        val currentState = _uiState.value
        if (currentState is UiState.ViewingTextContent) {
            _uiState.value = currentState.previousState
        }
    }

    fun uploadFileToServer(contentResolver: ContentResolver) {
        val currentState = _uiState.value
        if (currentState !is UiState.FileSelected) return

        viewModelScope.launch {
            try {
                val filePart = withContext(Dispatchers.IO) {
                    contentResolver.openInputStream(currentState.uri)?.use { inputStream ->
                        val requestBody = inputStream.readBytes().toRequestBody("file/*".toMediaTypeOrNull())
                        MultipartBody.Part.createFormData("file", "upload_file", requestBody)
                    }
                }

                if (filePart != null) {
                    val knowledgeCards = uploadService.uploadFile(filePart)
                    appDatabase.knowledgeCardDao().insertAll(knowledgeCards)
                    _singleEvent.emit(SingleEvent.UploadSuccess("File uploaded successfully!"))
                    _uiState.value = UiState.Idle // Go back to idle after successful upload
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _singleEvent.emit(SingleEvent.UploadError("Upload failed: ${e.message}"))
            }
        }
    }

    private fun getFileName(uri: Uri, contentResolver: ContentResolver): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor: Cursor? = contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val displayNameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (displayNameIndex != -1) {
                      result = it.getString(displayNameIndex)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != -1) {
                result = result?.substring(cut!! + 1)
            }
        }
        return result
    }

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

sealed class SingleEvent {
    data class ViewPdf(val uri: Uri) : SingleEvent()
    data class UploadSuccess(val message: String) : SingleEvent()
    data class UploadError(val message: String) : SingleEvent()
}