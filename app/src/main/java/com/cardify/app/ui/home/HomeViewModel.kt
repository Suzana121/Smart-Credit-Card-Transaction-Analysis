package com.cardify.app.ui.home

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cardify.app.data.api.RetrofitClient
import com.cardify.app.data.model.Transaction
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

    // רשימת הקניות
    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()

    // סטטוסים כלליים
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // --- חדש: סטטוסים להעלאת קובץ ---
    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

    private val _uploadMessage = MutableStateFlow<String?>(null)
    val uploadMessage: StateFlow<String?> = _uploadMessage.asStateFlow()

    init {
        fetchTransactions()
    }

    // משיכת נתונים (כמו קודם)
    fun fetchTransactions() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val response = RetrofitClient.apiService.getTransactions()
                if (response.isSuccessful && response.body() != null) {
                    _transactions.value = response.body()!!
                } else {
                    // אם הרשימה ריקה או שיש שגיאה, זה לא נורא בשלב הזה
                    Log.d("HomeViewModel", "Response code: ${response.code()}")
                }
            } catch (e: Exception) {
                _errorMessage.value = "Network Error: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // --- הפונקציה החדשה: העלאת קובץ ---
    fun uploadFile(uri: Uri, context: Context) {
        viewModelScope.launch {
            _isUploading.value = true
            _uploadMessage.value = null

            try {
                // 1. המרת ה-Uri לקובץ פיזי זמני
                val file = getFileFromUri(context, uri)

                if (file != null) {
                    // 2. הכנת הקובץ לשליחה
                    val requestFile = file.asRequestBody("text/csv".toMediaTypeOrNull())
                    val body = MultipartBody.Part.createFormData("file", file.name, requestFile)

                    // 3. שליחה לשרת
                    val response = RetrofitClient.apiService.uploadFile(body)

                    if (response.isSuccessful) {
                        _uploadMessage.value = "Success! File uploaded."
                        // 4. אם הצלחנו - נרענן את הרשימה כדי לראות את הקניות החדשות
                        fetchTransactions()
                    } else {
                        _uploadMessage.value = "Upload failed: ${response.code()}"
                    }
                } else {
                    _uploadMessage.value = "Error: Could not read file"
                }
            } catch (e: Exception) {
                _uploadMessage.value = "Error: ${e.localizedMessage}"
                Log.e("Upload", "Error", e)
            } finally {
                _isUploading.value = false
            }
        }
    }

    // פונקציית עזר להמרת Uri לקובץ (בלי זה אי אפשר לשלוח לשרת)
    private fun getFileFromUri(context: Context, uri: Uri): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val file = File(context.cacheDir, "temp_upload.csv")
            val outputStream = FileOutputStream(file)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    fun updateTransactionStatus(transactionId: String, newStatus: String) {
        viewModelScope.launch {
            try {
                val statusUpdate = mapOf("status" to newStatus)
                val response = RetrofitClient.apiService.updateTransactionStatus(transactionId, statusUpdate)

                if (response.isSuccessful) {
                    // מרעננים את הרשימה כדי שהשינוי יופיע מול המשתמש
                    fetchTransactions()
                    Log.d("HomeViewModel", "Status updated successfully")
                } else {
                    Log.e("HomeViewModel", "Failed to update status: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error updating status", e)
            }
        }
    }
}
