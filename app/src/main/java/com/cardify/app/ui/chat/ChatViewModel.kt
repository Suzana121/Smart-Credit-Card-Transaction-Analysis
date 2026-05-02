package com.cardify.app.ui.chat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cardify.app.data.api.RetrofitClient
import com.cardify.app.data.model.*import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {

    private val _chats       = MutableStateFlow<List<Chat>>(emptyList())
    val chats: StateFlow<List<Chat>> = _chats.asStateFlow()

    private val _messages    = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading   = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    private val _friends     = MutableStateFlow<List<Friend>>(emptyList())
    val friends: StateFlow<List<Friend>> = _friends.asStateFlow()

    // עסקה שממתינה לשליחה בצ'ט
    private val _pendingTransaction = MutableStateFlow<ChatTransaction?>(null)
    val pendingTransaction: StateFlow<ChatTransaction?> = _pendingTransaction.asStateFlow()

    // הודעה שממתינה לתגובה
    private val _replyTo = MutableStateFlow<ChatMessage?>(null)
    val replyTo: StateFlow<ChatMessage?> = _replyTo.asStateFlow()

    fun loadChats() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = RetrofitClient.apiService.getChats()
                if (response.isSuccessful) {
                    _chats.value = response.body() ?: emptyList()
                }
            } catch (e: Exception) {
                Log.e("ChatVM", "loadChats error", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadMessages(chatId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = RetrofitClient.apiService.getMessages(chatId)
                if (response.isSuccessful) {
                    _messages.value = response.body() ?: emptyList()
                }
            } catch (e: Exception) {
                Log.e("ChatVM", "loadMessages error", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun sendMessage(chatId: String, text: String, transaction: ChatTransaction? = null) {
        viewModelScope.launch {
            try {
                val reply = _replyTo.value
                val snapshot = reply?.let {
                    ReplySnapshot(
                        senderName   = it.senderName,
                        text         = it.text,
                        businessName = it.transaction?.businessName ?: ""
                    )
                }
                val request = SendMessageRequest(
                    text           = text,
                    transaction    = transaction,
                    replyToId      = reply?.id,
                    replyToMessage = snapshot
                )
                val response = RetrofitClient.apiService.sendMessage(chatId, request)
                if (response.isSuccessful) {
                    _pendingTransaction.value = null
                    _replyTo.value = null
                    loadMessages(chatId)
                }
            } catch (e: Exception) {
                Log.e("ChatVM", "sendMessage error", e)
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
                Log.e("ChatVM", "loadFriends error", e)
            }
        }
    }

    fun createChat(
        participantPhones: List<String>,
        groupName: String,
        onSuccess: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val userIds = mutableListOf<String>()
                for (phone in participantPhones) {
                    val res = RetrofitClient.apiService.searchUserByPhone(phone)
                    if (res.isSuccessful) {
                        res.body()?.id?.let { userIds.add(it) }
                    }
                }

                if (userIds.isEmpty()) return@launch

                val request  = CreateChatRequest(
                    participantIds = userIds,
                    groupName      = groupName
                )
                val response = RetrofitClient.apiService.createChat(request)
                if (response.isSuccessful) {
                    val chatId = response.body()?.get("id") as? String ?: ""
                    if (chatId.isNotBlank()) {
                        loadChats()
                        onSuccess(chatId)
                    }
                }
            } catch (e: Exception) {
                Log.e("ChatVM", "createChat error", e)
            }
        }
    }

    fun fetchUnreadCount() {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.getUnreadCount()
                if (response.isSuccessful) {
                    _unreadCount.value = response.body()?.unread ?: 0
                }
            } catch (e: Exception) {
                Log.e("ChatVM", "unread error", e)
            }
        }
    }

    fun setPendingTransaction(txn: ChatTransaction) {
        _pendingTransaction.value = txn
    }

    fun clearPendingTransaction() {
        _pendingTransaction.value = null
    }

    fun setReplyTo(message: ChatMessage) {
        _replyTo.value = message
    }

    fun clearReplyTo() {
        _replyTo.value = null
    }
}