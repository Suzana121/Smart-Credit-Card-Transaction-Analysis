package com.cardify.app.ui.chat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cardify.app.data.api.RetrofitClient
import com.cardify.app.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
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

    private val _pendingTransaction = MutableStateFlow<ChatTransaction?>(null)
    val pendingTransaction: StateFlow<ChatTransaction?> = _pendingTransaction.asStateFlow()

    private val _replyTo = MutableStateFlow<ChatMessage?>(null)
    val replyTo: StateFlow<ChatMessage?> = _replyTo.asStateFlow()

    private var currentChatId: String = ""

    // ─── טעינה ──────────────────────────────────────────────────

    fun loadChats() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = RetrofitClient.apiService.getChats()
                if (response.isSuccessful) _chats.value = response.body() ?: emptyList()
            } catch (e: Exception) {
                Log.e("ChatVM", "loadChats error", e)
            } finally { _isLoading.value = false }
        }
    }

    fun loadMessages(chatId: String) {
        currentChatId = chatId
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = RetrofitClient.apiService.getMessages(chatId)
                if (response.isSuccessful) _messages.value = response.body() ?: emptyList()
            } catch (e: Exception) {
                Log.e("ChatVM", "loadMessages error", e)
            } finally { _isLoading.value = false }
        }
    }

    fun sendMessage(
        chatId:       String,
        text:         String,
        transaction:  ChatTransaction? = null,
        nicknames:    Map<String, String> = emptyMap(),
        senderPhones: Map<String, String> = emptyMap()
    ) {
        viewModelScope.launch {
            try {
                val reply    = _replyTo.value
                val snapshot = reply?.let {
                    val phone       = senderPhones[it.senderId] ?: ""
                    val displayName = if (phone.isNotBlank())
                        nicknames[phone]?.takeIf { n -> n.isNotBlank() } ?: it.senderName
                    else it.senderName
                    ReplySnapshot(
                        senderName   = displayName,
                        text         = it.text,
                        businessName = it.transaction?.businessName ?: "",
                        isAudio      = it.audioUrl != null
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
            } catch (e: Exception) { Log.e("ChatVM", "sendMessage error", e) }
        }
    }

    // ─── מחיקה ──────────────────────────────────────────────────

    fun deleteMessage(chatId: String, messageId: String, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.deleteMessage(chatId, messageId)
                if (response.isSuccessful) loadMessages(chatId)
                else onError(response.errorBody()?.string() ?: "Error")
            } catch (e: Exception) {
                Log.e("ChatVM", "deleteMessage error", e)
                onError(e.message ?: "Error")
            }
        }
    }

    // ─── תגובת אימוג'י ──────────────────────────────────────────

    fun reactToMessage(chatId: String, messageId: String, emoji: String,
                       currentUserId: String) {
        viewModelScope.launch {
            try {
                val currentMsg    = _messages.value.find { it.id == messageId }
                val existingEmoji = currentMsg?.reactions?.get(currentUserId)
                val finalEmoji    = if (existingEmoji == emoji) "" else emoji

                _messages.value = _messages.value.map { msg ->
                    if (msg.id != messageId) msg
                    else {
                        val updated = msg.reactions.toMutableMap()
                        if (finalEmoji.isEmpty()) updated.remove(currentUserId)
                        else updated[currentUserId] = finalEmoji
                        msg.copy(reactions = updated)
                    }
                }

                val response = RetrofitClient.apiService.reactToMessage(
                    chatId, messageId, ReactRequest(emoji = finalEmoji)
                )
                if (!response.isSuccessful) loadMessages(chatId)
            } catch (e: Exception) {
                Log.e("ChatVM", "react error", e)
                loadMessages(chatId)
            }
        }
    }

    // ─── העברת הודעה ────────────────────────────────────────────

    fun forwardMessage(
        targetChatId: String,
        message: ChatMessage,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val body = mutableMapOf<String, Any>("forwarded" to true)
                if (message.text.isNotBlank()) body["text"] = message.text
                if (message.transaction != null) body["transaction"] = message.transaction

                val response = RetrofitClient.apiService.forwardMessage(targetChatId, body)
                if (response.isSuccessful) { loadChats(); onSuccess() }
                else onError(response.errorBody()?.string() ?: "Error")
            } catch (e: Exception) {
                Log.e("ChatVM", "forwardMessage error", e)
                onError(e.message ?: "Error")
            }
        }
    }

    // ─── עדכון שם קבוצה ─────────────────────────────────────────

    fun updateGroupName(chatId: String, newName: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.updateGroupName(
                    chatId, mapOf("groupName" to newName)
                )
                if (response.isSuccessful) {
                    // עדכון אופטימיסטי מקומי של רשימת הצ'אטים
                    _chats.value = _chats.value.map { chat ->
                        if (chat.id == chatId) chat.copy(groupName = newName) else chat
                    }
                    onSuccess()
                }
            } catch (e: Exception) {
                Log.e("ChatVM", "updateGroupName error", e)
            }
        }
    }

    // ─── חברים ──────────────────────────────────────────────────

    fun loadFriends() {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.getFriends()
                if (response.isSuccessful) _friends.value = response.body() ?: emptyList()
            } catch (e: Exception) { Log.e("ChatVM", "loadFriends error", e) }
        }
    }

    fun createChat(participantPhones: List<String>, groupName: String,
                   onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val userIds = mutableListOf<String>()
                for (phone in participantPhones) {
                    val res = RetrofitClient.apiService.searchUserByPhone(phone)
                    if (res.isSuccessful) res.body()?.id?.let { userIds.add(it) }
                }
                if (userIds.isEmpty()) return@launch
                val response = RetrofitClient.apiService.createChat(
                    CreateChatRequest(userIds, groupName))
                if (response.isSuccessful) {
                    val chatId = response.body()?.get("id") as? String ?: ""
                    if (chatId.isNotBlank()) { loadChats(); onSuccess(chatId) }
                }
            } catch (e: Exception) { Log.e("ChatVM", "createChat error", e) }
        }
    }

    fun fetchUnreadCount() {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.getUnreadCount()
                if (response.isSuccessful) _unreadCount.value = response.body()?.unread ?: 0
            } catch (e: Exception) { Log.e("ChatVM", "unread error", e) }
        }
    }

    fun setPendingTransaction(txn: ChatTransaction) { _pendingTransaction.value = txn }
    fun clearPendingTransaction()                    { _pendingTransaction.value = null }
    fun setReplyTo(message: ChatMessage)             { _replyTo.value = message }
    fun clearReplyTo()                               { _replyTo.value = null }

    override fun onCleared() { super.onCleared() }
}