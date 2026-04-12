package com.cardify.app.ui.chat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cardify.app.data.UserSession
import com.cardify.app.data.api.RetrofitClient
import com.cardify.app.data.api.toUserMessage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * A single chat message within a share conversation.
 *
 * @property id Unique message identifier.
 * @property shareId ID of the share this message belongs to.
 * @property senderId User ID of the message author.
 * @property senderName Display name of the message author.
 * @property text Message body text.
 * @property timestamp ISO-style timestamp string from the server.
 */
data class ChatMessage(
    val id: String = "",
    val shareId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val timestamp: String = ""
) {
    /** `true` if this message was sent by the currently authenticated user. */
    val isMyMessage: Boolean get() = senderId == UserSession.id
}

/**
 * Sealed class representing all possible states of the chat screen.
 */
sealed class ChatUiState {
    /** Messages are being loaded for the first time. */
    object Loading : ChatUiState()

    /**
     * Messages have been loaded successfully.
     *
     * @property messages Ordered list of [ChatMessage] objects for the conversation.
     */
    data class Success(val messages: List<ChatMessage>) : ChatUiState()

    /**
     * The initial load failed.
     *
     * @property message Human-readable error description.
     */
    data class Error(val message: String) : ChatUiState()
}

/**
 * ViewModel for the chat screen.
 *
 * Loads the message thread for a share conversation and polls for new messages every
 * 5 seconds. Also handles sending new messages and immediately refreshing the thread.
 */
class ChatViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<ChatUiState>(ChatUiState.Loading)

    /** Observable UI state consumed by [ChatScreen]. */
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _isSending = MutableStateFlow(false)

    /** `true` while a send-message request is in flight. */
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    private val _sendError = MutableStateFlow<String?>(null)

    /**
     * One-shot error message set when a message fails to send. The UI should display this
     * and then call [clearSendError] to reset it.
     */
    val sendError: StateFlow<String?> = _sendError.asStateFlow()

    /** Clears [sendError] after it has been shown to the user. */
    fun clearSendError() { _sendError.value = null }

    /** The share ID whose messages are currently being loaded and polled. */
    private var currentShareId: String = ""

    /**
     * Starts loading and polling messages for the given share conversation.
     * Polls the server every 5 seconds until the ViewModel is cleared.
     *
     * @param shareId ID of the share whose messages to display.
     */
    fun loadMessages(shareId: String) {
        currentShareId = shareId
        viewModelScope.launch {
            _uiState.value = ChatUiState.Loading
            fetchMessages()
            while (true) {
                delay(5000)
                fetchMessages()
            }
        }
    }

    /**
     * Performs a single fetch of the current conversation's messages.
     * Only transitions to [ChatUiState.Error] if the UI state is still [ChatUiState.Loading]
     * (i.e. the first load failed), so polling failures are silent.
     */
    private suspend fun fetchMessages() {
        try {
            val response = RetrofitClient.apiService.getMessages(currentShareId)
            if (response.isSuccessful) {
                val messages = parseMessages(response.body())
                _uiState.value = ChatUiState.Success(messages)
            } else {
                Log.e("ChatViewModel", "Fetch messages HTTP ${response.code()}")
                if (_uiState.value is ChatUiState.Loading) {
                    _uiState.value = ChatUiState.Error(
                        when (response.code()) {
                            401  -> "Session expired. Please log in again."
                            403  -> "Access denied to this conversation."
                            404  -> "Conversation not found."
                            500, 502, 503 -> "Server error. Please try again later."
                            else -> "Failed to load messages (${response.code()})."
                        }
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("ChatViewModel", "Error fetching messages", e)
            if (_uiState.value is ChatUiState.Loading) {
                _uiState.value = ChatUiState.Error(e.toUserMessage())
            }
        }
    }

    /**
     * Sends a message to the current conversation and immediately refreshes the thread.
     * No-ops if [text] is blank or no share is currently loaded.
     *
     * @param text The message body to send.
     */
    fun sendMessage(text: String) {
        if (text.isBlank() || currentShareId.isEmpty()) return
        viewModelScope.launch {
            _isSending.value = true
            try {
                val response = RetrofitClient.apiService.sendMessage(
                    mapOf("shareId" to currentShareId, "text" to text)
                )
                if (response.isSuccessful) {
                    fetchMessages()
                } else {
                    Log.e("ChatViewModel", "Send message HTTP ${response.code()}")
                    _sendError.value = "Message not sent. Please try again."
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error sending message", e)
                _sendError.value = e.toUserMessage()
            } finally {
                _isSending.value = false
            }
        }
    }

    /**
     * Parses the loosely-typed server response body into a list of [ChatMessage] objects.
     *
     * @param body The raw response body from the messages endpoint.
     * @return A list of parsed [ChatMessage] objects, or empty if the body is null or malformed.
     */
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
