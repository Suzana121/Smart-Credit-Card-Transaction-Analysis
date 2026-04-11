package com.cardify.app.ui.home

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cardify.app.data.api.RetrofitClient
import com.cardify.app.data.api.toUserMessage
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

/**
 * ViewModel for the home screen.
 *
 * Manages the user's transaction list, friend list, file upload flow, and
 * optimistic transaction-status updates. All state is exposed as [StateFlow].
 */
class HomeViewModel : ViewModel() {

    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())

    /** The full list of transactions fetched from the server. */
    val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()

    private val _isLoading = MutableStateFlow(false)

    /** `true` while the initial transaction list is being loaded. */
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isUploading = MutableStateFlow(false)

    /** `true` while a file upload request is in flight. */
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

    private val _uploadMessage = MutableStateFlow<String?>(null)

    /** User-facing message describing the result of the most recent upload attempt. */
    val uploadMessage: StateFlow<String?> = _uploadMessage.asStateFlow()

    private val _friends = MutableStateFlow<List<Friend>>(emptyList())

    /** The authenticated user's friend list, used to populate the share bottom sheet. */
    val friends: StateFlow<List<Friend>> = _friends.asStateFlow()

    private val _isSendingShare = MutableStateFlow(false)

    /** `true` while a share-transaction request is in flight. */
    val isSendingShare: StateFlow<Boolean> = _isSendingShare.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)

    /**
     * One-shot user-facing error message. The UI should display this and then call
     * [clearError] to reset it to `null` so the same message is not shown twice.
     */
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    /**
     * Clears the current [errorMessage] after it has been displayed.
     */
    fun clearError() { _errorMessage.value = null }

    /**
     * Loads the transaction list from the server and updates [transactions].
     * Sets [isLoading] to `true` while the request is in flight.
     */
    fun fetchTransactions() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = RetrofitClient.apiService.getTransactions()
                if (response.isSuccessful) {
                    _transactions.value = response.body() ?: emptyList()
                } else {
                    Log.e("HomeViewModel", "Fetch transactions HTTP ${response.code()}")
                    _errorMessage.value = when (response.code()) {
                        401  -> "Session expired. Please log in again."
                        403  -> "Access denied."
                        404  -> "No transactions found."
                        500, 502, 503 -> "Server error (${response.code()}). Please try again later."
                        else -> "Failed to load transactions (${response.code()})."
                    }
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Fetch error", e)
                _errorMessage.value = e.toUserMessage()
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Loads the authenticated user's friend list and updates [friends].
     */
    fun loadFriends() {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.getFriends()
                if (!response.isSuccessful) {
                    Log.e("HomeViewModel", "Load friends HTTP ${response.code()}")
                    // Friends are non-critical; suppress error UI for non-auth failures
                    if (response.code() == 401) {
                        _errorMessage.value = "Session expired. Please log in again."
                    }
                } else {
                    _friends.value = response.body() ?: emptyList()
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error loading friends", e)
                // Only surface network errors; friends list is non-critical
                if (e is java.net.UnknownHostException || e is java.net.ConnectException) {
                    _errorMessage.value = e.toUserMessage()
                }
            }
        }
    }

    /**
     * Shares a transaction with a friend.
     *
     * @param friendPhone Phone number of the recipient friend.
     * @param transactionId ID of the transaction to share.
     * @param onSuccess Callback invoked on the main thread if the share request succeeds.
     */
    fun sendShare(friendPhone: String, transactionId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isSendingShare.value = true
            try {
                val response = RetrofitClient.apiService.postShare(
                    ShareRequest(friendPhone, transactionId)
                )
                if (response.isSuccessful) {
                    onSuccess()
                } else {
                    Log.e("HomeViewModel", "Share failed HTTP ${response.code()}")
                    _errorMessage.value = "Failed to share transaction. Please try again."
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error sending share", e)
                _errorMessage.value = e.toUserMessage()
            } finally {
                _isSendingShare.value = false
            }
        }
    }

    /**
     * Uploads a CSV or Excel file to the server for transaction analysis.
     *
     * On success, waits 2 seconds and then refreshes the transaction list so
     * newly imported transactions appear immediately.
     *
     * @param uri Content URI of the file chosen by the user.
     * @param context Context used to resolve the URI via [android.content.ContentResolver].
     */
    fun uploadFile(uri: Uri, context: Context) {
        viewModelScope.launch {
            _isUploading.value = true
            _uploadMessage.value = "Uploading..."
            try {
                val file = getFileFromUri(context, uri)
                if (file == null) {
                    _uploadMessage.value = "Could not read the selected file. Please try again."
                    return@launch
                }
                val requestFile = file.asRequestBody(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                        .toMediaTypeOrNull()
                )
                val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
                val response = RetrofitClient.apiService.uploadFile(body)
                when {
                    response.isSuccessful -> {
                        _uploadMessage.value = "Success! Transactions processed."
                        delay(2000)
                        fetchTransactions()
                    }
                    response.code() == 400 ->
                        _uploadMessage.value = "Invalid file format. Please upload a CSV or Excel file."
                    response.code() == 413 ->
                        _uploadMessage.value = "File is too large. Maximum size is 10 MB."
                    response.code() in 500..599 ->
                        _uploadMessage.value = "Server error (${response.code()}). Please try again later."
                    else ->
                        _uploadMessage.value = "Upload failed (${response.code()}). Please try again."
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Upload error", e)
                _uploadMessage.value = e.toUserMessage()
            } finally {
                _isUploading.value = false
            }
        }
    }

    /**
     * Optimistically updates a transaction's status in the local list and persists the change
     * to the server. If the server call fails, the previous list is restored.
     *
     * @param transactionId ID of the transaction to update.
     * @param newStatus New status string: `"REGULAR"` or `"IRREGULAR"`.
     */
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

    /**
     * Copies the file at the given [uri] into the app's cache directory so it can be
     * read as a regular [File] for multipart upload.
     *
     * @param context Context used to open the content URI.
     * @param uri The content URI of the file to copy.
     * @return A temporary [File] in the cache directory, or `null` on failure.
     */
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
