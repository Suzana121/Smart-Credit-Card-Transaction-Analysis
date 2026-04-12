package com.cardify.app.ui.shared_info

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cardify.app.data.api.RetrofitClient
import com.cardify.app.data.api.toUserMessage
import com.cardify.app.data.model.ShareRequest
import com.cardify.app.data.model.Friend
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for the "share with friends" screen.
 *
 * Loads the authenticated user's friend list and handles the API call to share a specific
 * transaction with a chosen friend. Exposes [errorMessage] for network and server errors
 * so the screen can show a Toast without crashing.
 */
class ShareWithFriendsViewModel : ViewModel() {

    private val _friends = MutableStateFlow<List<Friend>>(emptyList())

    /** The current user's friend list, used to populate the friend picker. */
    val friends = _friends.asStateFlow()

    private val _isSending = MutableStateFlow(false)

    /** `true` while a share request is in flight. */
    val isSending = _isSending.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)

    /**
     * One-shot user-facing error message. Call [clearError] after displaying to prevent
     * the same message from being shown again.
     */
    val errorMessage = _errorMessage.asStateFlow()

    /** Clears the current [errorMessage] after it has been shown to the user. */
    fun clearError() { _errorMessage.value = null }

    init { loadFriends() }

    /**
     * Fetches the authenticated user's approved friend list from the server.
     */
    fun loadFriends() {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.getFriends()
                if (response.isSuccessful) {
                    _friends.value = response.body() ?: emptyList()
                } else {
                    Log.e("ShareVM", "Load friends HTTP ${response.code()}")
                    if (response.code() == 401) {
                        _errorMessage.value = "Session expired. Please log in again."
                    }
                }
            } catch (e: Exception) {
                Log.e("ShareVM", "Error loading friends", e)
                _errorMessage.value = e.toUserMessage()
            }
        }
    }

    /**
     * Shares a transaction with a specific friend.
     *
     * @param friendPhone Phone number of the friend to share with.
     * @param txnId ID of the transaction to share.
     * @param onSuccess Callback invoked on the main thread if the share succeeds.
     */
    fun sendShare(friendPhone: String, txnId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isSending.value = true
            try {
                val res = RetrofitClient.apiService.postShare(ShareRequest(friendPhone, txnId))
                if (res.isSuccessful) {
                    onSuccess()
                } else {
                    Log.e("ShareVM", "Send share HTTP ${res.code()}")
                    _errorMessage.value = "Failed to share transaction. Please try again."
                }
            } catch (e: Exception) {
                Log.e("ShareVM", "Error sending share", e)
                _errorMessage.value = e.toUserMessage()
            } finally {
                _isSending.value = false
            }
        }
    }
}
