package com.cardify.app.ui.chat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cardify.app.data.UserSession
import com.cardify.app.data.api.RetrofitClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChatMessage(
    val id: String = "",
    val shareId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val timestamp: String = ""
) {
    val isMyMessage: Boolean get() = senderId == UserSession.id
}

sealed class ChatUiState {
    object Loading : ChatUiState()
    data class Success(val messages: List<ChatMessage>) : ChatUiState()
    data class Error(val message: String) : ChatUiState()
}

class ChatViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<ChatUiState>(ChatUiState.Loading)
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    private var currentShareId: String = ""

    fun loadMessages(shareId: String) {
        currentShareId = shareId
        viewModelScope.launch {
            _uiState.value = ChatUiState.Loading
            fetchMessages()
            // polling כל 5 שניות לעדכון הודעות
            while (true) {
                delay(5000)
                fetchMessages()
            }
        }
    }

    private suspend fun fetchMessages() {
        try {
            val response = RetrofitClient.apiService.getMessages(currentShareId)
            if (response.isSuccessful) {
                val messages = parseMessages(response.body())
                _uiState.value = ChatUiState.Success(messages)
            } else {
                if (_uiState.value is ChatUiState.Loading) {
                    _uiState.value = ChatUiState.Error("Failed to load messages")
                }
            }
        } catch (e: Exception) {
            Log.e("ChatViewModel", "Error fetching messages", e)
            if (_uiState.value is ChatUiState.Loading) {
                _uiState.value = ChatUiState.Error("Network error")
            }
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank() || currentShareId.isEmpty()) return
        viewModelScope.launch {
            _isSending.value = true
            try {
                val response = RetrofitClient.apiService.sendMessage(
                    mapOf("shareId" to currentShareId, "text" to text)
                )
                if (response.isSuccessful) {
                    fetchMessages() // רענון מיידי אחרי שליחה
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error sending message", e)
            } finally {
                _isSending.value = false
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseMessages(body: Any?): List<ChatMessage> {
        return (body as? List<Map<String, Any>>)?.map { m ->
            ChatMessage(
                id = m["id"]?.toString() ?: "",
                shareId = m["shareId"]?.toString() ?: "",
                senderId = m["senderId"]?.toString() ?: "",
                senderName = m["senderName"]?.toString() ?: "",
                text = m["text"]?.toString() ?: "",
                timestamp = m["timestamp"]?.toString() ?: ""
            )
        } ?: emptyList()
    }
}