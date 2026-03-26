package com.cardify.app.ui.shared_info

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cardify.app.data.api.RetrofitClient
import com.cardify.app.data.model.ShareRequest
import com.cardify.app.data.model.Friend
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ShareWithFriendsViewModel : ViewModel() {
    private val _friends = MutableStateFlow<List<Friend>>(emptyList())
    val friends = _friends.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending = _isSending.asStateFlow()

    init { loadFriends() }

    fun loadFriends() {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.getFriends()
                if (response.isSuccessful) {
                    // חשוב: מציגים רק חברים מאושרים
                    _friends.value = response.body() ?: emptyList()
                }
            } catch (e: Exception) { Log.e("ShareVM", "Error", e) }
        }
    }

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