package com.cardify.app.ui.shared_info

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cardify.app.data.api.RetrofitClient
import com.cardify.app.data.model.ShareRequest
import com.cardify.app.data.model.Friend
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for the "share with friends" screen.
 *
 * Loads the authenticated user's friend list and handles the API call to share a specific
 * transaction with a chosen friend.
 */
class ShareWithFriendsViewModel : ViewModel() {

    private val _friends = MutableStateFlow<List<Friend>>(emptyList())

    /** The current user's friend list, used to populate the friend picker. */
    val friends = _friends.asStateFlow()

    private val _isSending = MutableStateFlow(false)

    /** `true` while a share request is in flight. */
    val isSending = _isSending.asStateFlow()

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
                }
            } catch (e: Exception) { Log.e("ShareVM", "Error", e) }
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
                if (res.isSuccessful) onSuccess()
            } catch (e: Exception) { Log.e("ShareVM", "Error sending", e) }
            finally { _isSending.value = false }
        }
    }
}
