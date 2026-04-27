package com.cardify.app.ui.home

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cardify.app.data.api.RetrofitClient
import com.cardify.app.data.model.Transaction
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

    private val _uploadMessage = MutableStateFlow<String?>(null)
    val uploadMessage: StateFlow<String?> = _uploadMessage.asStateFlow()

    fun fetchTransactions() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = RetrofitClient.apiService.getTransactions(limit = 5)
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

    fun uploadFile(uri: Uri, context: Context) {
        viewModelScope.launch {
            _isUploading.value = true
            _uploadMessage.value = "Uploading..."
            try {
                val file = getFileFromUri(context, uri)
                if (file != null) {
                    val requestFile = file.asRequestBody(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                            .toMediaTypeOrNull()
                    )
                    val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
                    val response = RetrofitClient.apiService.uploadFile(body)
                    if (response.isSuccessful) {
                        _uploadMessage.value = "Success! Data processed."
                        delay(2000)
                        fetchTransactions()
                    } else {
                        _uploadMessage.value = "Failed: ${response.code()}"
                    }
                }
            } catch (e: Exception) {
                _uploadMessage.value = "Error: ${e.localizedMessage}"
            } finally {
                _isUploading.value = false
            }
        }
    }

    fun updateTransactionStatus(transactionId: String, newStatus: String) {
        val previousList = _transactions.value
        _transactions.value = previousList.map { t ->
            if (t.id == transactionId) t.copy(status = newStatus) else t
        }
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.updateTransactionStatus(
                    transactionId, mapOf("status" to newStatus)
                )
                if (!response.isSuccessful) {
                    _transactions.value = previousList
                    Log.e("HomeViewModel", "Update failed: ${response.code()}")
                }
            } catch (e: Exception) {
                _transactions.value = previousList
                Log.e("HomeViewModel", "Update error", e)
            }
        }
    }

    // --- עדכון פרופיל לאחר תיקונים ידניים ---
    fun updateProfile(overrides: Map<String, String>, onSuccess: () -> Unit) {
        if (overrides.isEmpty()) return
        viewModelScope.launch {
            try {
                val body = mapOf<String, Any>("overrides" to overrides)
                val response = RetrofitClient.apiService.updateProfile(body)
                if (response.isSuccessful) {
                    Log.d("HomeViewModel", "Profile updated with ${overrides.size} override(s)")
                    onSuccess()
                } else {
                    Log.e("HomeViewModel", "Profile update failed: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Profile update error", e)
            }
        }
    }

    private fun getFileFromUri(context: Context, uri: Uri): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val file = File(context.cacheDir, "temp_upload.xlsx")
            val outputStream = FileOutputStream(file)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()
            file
        } catch (e: Exception) { null }
    }
}