package com.cardify.app.ui.shared_info

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cardify.app.data.api.RetrofitClient
import com.cardify.app.data.model.ShareRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ShareWithFriendsViewModel : ViewModel() {

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    fun sendShare(
        sharedWith: String,
        transactionId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isSending.value = true
            try {
                val response = RetrofitClient.apiService.postShare(
                    ShareRequest(sharedWith = sharedWith, transactionId = transactionId)
                )
                if (response.isSuccessful) {
                    Log.d("ShareVM", "Share saved: id=${response.body()?.get("id")}")
                    onSuccess()
                } else {
                    val error = response.errorBody()?.string() ?: "Unknown error"
                    Log.e("ShareVM", "Share failed: HTTP ${response.code()} | $error")
                    onError(error)
                }
            } catch (e: Exception) {
                Log.e("ShareVM", "Share error", e)
                onError(e.localizedMessage ?: "Network error")
            } finally {
                _isSending.value = false
            }
        }
    }
}
