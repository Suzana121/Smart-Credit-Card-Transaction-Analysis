package com.cardify.app.ui.chat

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cardify.app.ui.home.CardifyColors
import kotlinx.coroutines.launch

/**
 * Chat screen for a conversation linked to a specific share.
 *
 * Loads messages via [ChatViewModel.loadMessages] and auto-scrolls to the latest entry
 * whenever [ChatUiState] transitions to [ChatUiState.Success]. The top bar shows
 * [friendName] and a back button. The bottom bar provides a multi-line text input and a
 * send button that is disabled while [ChatViewModel.isSending] is `true`.
 *
 * @param shareId The ID of the share whose message thread to display.
 * @param friendName The display name of the other participant, shown in the top bar.
 * @param onBack Called when the back button is tapped.
 * @param viewModel The [ChatViewModel] managing message loading, polling, and sending.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    shareId: String,
    friendName: String,
    onBack: () -> Unit,
    viewModel: ChatViewModel = viewModel()
) {
    var messageText by remember { mutableStateOf("") }
    val uiState by viewModel.uiState.collectAsState()
    val isSending by viewModel.isSending.collectAsState()
    val sendError by viewModel.sendError.collectAsState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(shareId) {
        viewModel.loadMessages(shareId)
    }

    LaunchedEffect(sendError) {
        sendError?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearSendError()
        }
    }

    // גלילה אוטומטית לתחתית כשמגיעות הודעות חדשות
    LaunchedEffect(uiState) {
        if (uiState is ChatUiState.Success) {
            val messages = (uiState as ChatUiState.Success).messages
            if (messages.isNotEmpty()) {
                coroutineScope.launch {
                    listState.animateScrollToItem(messages.size - 1)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            friendName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            "Chat about shared transaction",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CardifyColors.DarkGreen
                )
            )
        },
        bottomBar = {
            // תיבת הקלדה
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    placeholder = { Text("Type a message...", fontSize = 14.sp) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CardifyColors.DarkGreen,
                        unfocusedBorderColor = Color(0xFFDDDDDD)
                    ),
                    singleLine = false,
                    maxLines = 3
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (messageText.isNotBlank()) {
                            viewModel.sendMessage(messageText.trim())
                            messageText = ""
                        }
                    },
                    enabled = !isSending && messageText.isNotBlank(),
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            if (messageText.isNotBlank()) CardifyColors.DarkGreen
                            else Color(0xFFDDDDDD)
                        )
                ) {
                    if (isSending) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Send, null, tint = Color.White)
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F5F5))
                .padding(padding)
        ) {
            when (val state = uiState) {
                is ChatUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = CardifyColors.DarkGreen
                    )
                }
                is ChatUiState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(state.message, color = Color.Gray)
                    }
                }
                is ChatUiState.Success -> {
                    if (state.messages.isEmpty()) {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("No messages yet", color = Color.Gray, fontSize = 14.sp)
                            Text(
                                "Start the conversation!",
                                color = Color.LightGray,
                                fontSize = 12.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            items(state.messages, key = { it.id }) { message ->
                                ChatBubble(message = message)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Single message bubble. Outgoing messages are right-aligned with a teal background;
 * incoming messages are left-aligned with a white background and include the sender's name.
 *
 * @param message The [ChatMessage] to render.
 */
@Composable
fun ChatBubble(message: ChatMessage) {
    val isMe = message.isMyMessage

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
    ) {
        Column(
            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            // שם השולח (רק להודעות של הצד השני)
            if (!isMe) {
                Text(
                    message.senderName,
                    fontSize = 11.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                )
            }

            // בועת הודעה
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isMe) 16.dp else 4.dp,
                            bottomEnd = if (isMe) 4.dp else 16.dp
                        )
                    )
                    .background(
                        if (isMe) CardifyColors.DarkGreen else Color.White
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = message.text,
                    color = if (isMe) Color.White else Color.Black,
                    fontSize = 14.sp
                )
            }

            // זמן
            Text(
                text = formatTimestamp(message.timestamp),
                fontSize = 10.sp,
                color = Color.Gray,
                modifier = Modifier.padding(
                    start = if (isMe) 0.dp else 4.dp,
                    end = if (isMe) 4.dp else 0.dp,
                    top = 2.dp
                )
            )
        }
    }
}

/**
 * Extracts the HH:mm time portion from a full ISO-style timestamp string.
 *
 * @param timestamp An ISO-style timestamp (e.g. `"2025-11-10T14:35:00"`).
 * @return The time string `"HH:mm"`, or [timestamp] unchanged if it is too short or parsing fails.
 */
private fun formatTimestamp(timestamp: String): String {
    return try {
        // מציג רק את השעה אם זה היום
        if (timestamp.length >= 16) timestamp.substring(11, 16) else timestamp
    } catch (e: Exception) {
        timestamp
    }
}