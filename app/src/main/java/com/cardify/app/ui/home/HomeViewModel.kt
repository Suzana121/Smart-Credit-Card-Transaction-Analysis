package com.cardify.app.ui.home

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cardify.app.data.api.RetrofitClient
import com.cardify.app.data.model.Transaction
import com.cardify.app.data.model.UploadedFile
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

class HomeViewModel : ViewModel() {

    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _uploadMessage = MutableStateFlow<String?>(null)
    val uploadMessage: StateFlow<String?> = _uploadMessage.asStateFlow()

    private val _manualOverrides = MutableStateFlow<Map<String, String>>(emptyMap())
    val manualOverrides: StateFlow<Map<String, String>> = _manualOverrides.asStateFlow()

    private val _uploads = MutableStateFlow<List<UploadedFile>>(emptyList())

    init {
        fetchUploads()
    }

    private fun fetchUploads() {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.getUploads()
                if (response.isSuccessful) {
                    _uploads.value = response.body() ?: emptyList()
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Uploads fetch error", e)
            }
        }
    }

    fun fetchTransactions(limit: Int = 5) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = RetrofitClient.apiService.getTransactions(limit = limit)
                if (response.isSuccessful) {
                    _transactions.value = response.body()?.transactions ?: emptyList()
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Fetch error", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
    private val _isSuccess = MutableStateFlow(false)
    val isSuccess: StateFlow<Boolean> = _isSuccess.asStateFlow()
    fun uploadFile(uri: Uri, context: Context) {
        viewModelScope.launch {
            _isUploading.value = true
            _isSuccess.value = false
            _uploadMessage.value = "Uploading..."

            try {
                val originalName = getOriginalFileName(context, uri) ?: "upload.xlsx"

                // 1. בדיקת סיומת קובץ בצד הלקוח (לפני השליחה)
                val lowerName = originalName.lowercase()
                if (!(lowerName.endsWith(".csv") || lowerName.endsWith(".xlsx") || lowerName.endsWith(".xls"))) {
                    _isUploading.value = false
                    _uploadMessage.value = "Invalid file type. Please select Excel or CSV only."
                    return@launch
                }

                // בדיקת כפילות לפי שם
                if (_uploads.value.any { it.fileName == originalName }) {
                    _isUploading.value = false
                    val msg = "This file already exists"
                    _uploadMessage.value = msg
                    // הוספת השורה הזו תציג את ההודעה למשתמש
                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
                    return@launch
                }

                val file = getFileFromUri(context, uri, originalName)
                if (file != null) {
                    val mimeType = when {
                        originalName.endsWith(".csv") -> "text/csv"
                        originalName.endsWith(".xls") -> "application/vnd.ms-excel"
                        else -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                    }
                    val requestFile = file.asRequestBody(mimeType.toMediaTypeOrNull())
                    val body = MultipartBody.Part.createFormData("file", originalName, requestFile)

                    val response = RetrofitClient.apiService.uploadFile(body)

                    if (response.isSuccessful) {
                        _isUploading.value = false
                        _isProcessing.value = true
                        delay(3000)
                        _isSuccess.value = true

                        val warnings = response.body()?.warnings
                        _uploadMessage.value = if (!warnings.isNullOrEmpty()) {
                            "Success! Note: ${warnings.first()}"
                        } else {
                            "Success! Data processed."
                        }

                        fetchTransactions()
                        delay(2500)
                        _isProcessing.value = false
                        _isSuccess.value = false
                    } else {
                        _isUploading.value = false
                        val errorBody = response.errorBody()?.string()
                        val errorMessage = try {
                            val json = org.json.JSONObject(errorBody ?: "{}")
                            json.optString("message", "Upload failed: ${response.code()}")
                        } catch (e: Exception) {
                            "Upload failed: ${response.code()}"
                        }
                        _uploadMessage.value = errorMessage
                        android.widget.Toast.makeText(context, errorMessage, android.widget.Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Upload error", e)
                _isUploading.value = false
                _isProcessing.value = false
                _isSuccess.value = false
                _uploadMessage.value = "Error: ${e.localizedMessage}"
            }
        }
    }
    private fun getOriginalFileName(context: Context, uri: Uri): String? {
        return try {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (it.moveToFirst() && nameIndex >= 0) it.getString(nameIndex) else null
            }
        } catch (e: Exception) { null }
    }

    private fun getFileFromUri(context: Context, uri: Uri, fileName: String): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val file = File(context.cacheDir, fileName)
            val outputStream = FileOutputStream(file)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()
            file
        } catch (e: Exception) { null }
    }

    fun updateTransactionStatus(transactionId: String, newStatus: String) {
        val previousList = _transactions.value
        _transactions.value = previousList.map { t ->
            if (t.id == transactionId) t.copy(status = newStatus) else t
        }
        _manualOverrides.value = _manualOverrides.value + (transactionId to newStatus)

        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.updateTransactionStatus(
                    transactionId, mapOf("status" to newStatus)
                )
                if (!response.isSuccessful) {
                    _transactions.value = previousList
                    _manualOverrides.value = _manualOverrides.value - transactionId
                }
            } catch (e: Exception) {
                _transactions.value = previousList
                _manualOverrides.value = _manualOverrides.value - transactionId
            }
        }
    }

    fun updateProfile(onSuccess: () -> Unit) {
        if (_manualOverrides.value.isEmpty()) return
        viewModelScope.launch {
            try {
                val body = mapOf<String, Any>("overrides" to _manualOverrides.value)
                val response = RetrofitClient.apiService.updateProfile(body)
                if (response.isSuccessful) {
                    _manualOverrides.value = emptyMap()
                    onSuccess()
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Profile update error", e)
            }
        }
    }
}