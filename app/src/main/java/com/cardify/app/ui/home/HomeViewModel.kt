package com.cardify.app.ui.home

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cardify.app.data.api.RetrofitClient
import com.cardify.app.data.model.Friend
import com.cardify.app.data.model.ShareRequest
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
import android.content.Context

class HomeViewModel : ViewModel() {

    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

    private val _uploadMessage = MutableStateFlow<String?>(null)
    val uploadMessage: StateFlow<String?> = _uploadMessage.asStateFlow()

    private val _friends = MutableStateFlow<List<Friend>>(emptyList())
    val friends: StateFlow<List<Friend>> = _friends.asStateFlow()

    private val _isSendingShare = MutableStateFlow(false)
    val isSendingShare: StateFlow<Boolean> = _isSendingShare.asStateFlow()

    fun fetchTransactions() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = RetrofitClient.apiService.getTransactions()
                if (response.isSuccessful) {
                    _transactions.value = response.body() ?: emptyList()
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Fetch error", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadFriends() {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.getFriends()
                if (response.isSuccessful) {
                    _friends.value = response.body() ?: emptyList()
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error loading friends", e)
            }
        }
    }

    fun sendShare(friendPhone: String, transactionId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isSendingShare.value = true
            try {
                val response = RetrofitClient.apiService.postShare(
                    ShareRequest(friendPhone, transactionId)
                )
                if (response.isSuccessful) onSuccess()
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error sending share", e)
            } finally {
                _isSendingShare.value = false
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
                    Log.e("HomeViewModel", "Update status failed: HTTP ${response.code()}")
                }
            } catch (e: Exception) {
                _transactions.value = previousList
                Log.e("HomeViewModel", "Update status error", e)
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