package com.cardify.app.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cardify.app.data.UserSession
import com.cardify.app.data.model.Chat
import com.cardify.app.data.model.ChatTransaction
import com.cardify.app.ui.components.AppScaffold
import com.cardify.app.ui.home.CardifyColors

@Composable
fun ChatsScreen(
    onNavigate: (String) -> Unit,
    onOpenChat: (Chat) -> Unit,           // ← מקבל Chat במקום String
    pendingTransaction: ChatTransaction? = null,
    viewModel: ChatViewModel = viewModel()
) {
    val chats       by viewModel.chats.collectAsState()
    val isLoading   by viewModel.isLoading.collectAsState()
    var showNewChat by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.loadChats() }

    LaunchedEffect(pendingTransaction) {
        pendingTransaction?.let { viewModel.setPendingTransaction(it) }
    }

    if (showNewChat) {
        NewChatDialog(
            viewModel     = viewModel,
            onDismiss     = { showNewChat = false },
            onChatCreated = { chatId ->
                showNewChat = false
                // לאחר יצירת צ'אט חדש, מחפשים אותו ברשימה ומנווטים אליו
                val newChat = chats.find { it.id == chatId }
                newChat?.let { onOpenChat(it) }
            }
        )
    }

    AppScaffold(currentRoute = "wallet", onNavigate = onNavigate) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(padding)
        ) {
            Column(Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Chats", fontSize = 22.sp, fontWeight = FontWeight.Bold,
                            color = CardifyColors.DarkGreen)
                        if (pendingTransaction != null) {
                            Text(
                                "Select a chat to share: ${pendingTransaction.businessName}",
                                fontSize = 11.sp,
                                color = CardifyColors.IrregularRed,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    IconButton(onClick = { showNewChat = true }) {
                        Icon(Icons.Default.Add, null, tint = CardifyColors.DarkGreen)
                    }
                }

                HorizontalDivider(color = Color(0xFFEEEEEE))

                if (isLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = CardifyColors.DarkGreen)
                    }
                } else if (chats.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("No chats yet", color = Color.Gray, fontSize = 16.sp)
                            Spacer(Modifier.height(8.dp))
                            Text("Tap + to start a conversation",
                                color = Color.LightGray, fontSize = 13.sp)
                        }
                    }
                } else {
                    LazyColumn(Modifier.fillMaxSize()) {
                        items(chats, key = { it.id }) { chat ->
                            ChatItem(
                                chat          = chat,
                                currentUserId = UserSession.userId ?: "",
                                hasPending    = pendingTransaction != null,
                                onClick       = { onOpenChat(chat) }   // ← מעביר Chat
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 72.dp),
                                color = Color(0xFFEEEEEE)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatItem(
    chat: Chat,
    currentUserId: String,
    hasPending: Boolean = false,
    onClick: () -> Unit
) {
    val displayName = if (chat.isGroup) {
        chat.groupName.ifBlank { "Group" }
    } else {
        val otherId = chat.participants.firstOrNull { it != currentUserId } ?: ""
        chat.participantNames[otherId] ?: "Unknown"
    }

    val initials = displayName.take(1).uppercase()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(
                if (hasPending) CardifyColors.DarkGreen.copy(alpha = 0.05f)
                else Color.White
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(CardifyColors.DarkGreen.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(initials, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                color = CardifyColors.DarkGreen)
        }

        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(displayName, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                if (hasPending) {
                    Text("Tap to share →", fontSize = 11.sp,
                        color = CardifyColors.DarkGreen, fontWeight = FontWeight.SemiBold)
                } else {
                    Text(formatChatTime(chat.lastMessageAt), fontSize = 11.sp, color = Color.Gray)
                }
            }
            Spacer(Modifier.height(2.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    chat.lastMessage.ifBlank { "No messages yet" },
                    fontSize = 13.sp, color = Color.Gray,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (chat.unreadCount > 0 && !hasPending) {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(CardifyColors.DarkGreen),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            chat.unreadCount.toString(),
                            fontSize = 11.sp, color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

fun formatChatTime(timestamp: String): String {
    if (timestamp.isBlank()) return ""
    return try {
        // Firestore יכול להחזיר כמה פורמטים — מנסים אחד אחד
        val instant = when {
            // פורמט עם offset: 2024-01-15T10:30:00+00:00
            timestamp.contains('T') && (timestamp.contains('+') || timestamp.endsWith('Z')) ->
                java.time.OffsetDateTime.parse(timestamp).toInstant()
            // פורמט עם T אבל בלי offset: 2024-01-15T10:30:00
            timestamp.contains('T') ->
                java.time.LocalDateTime.parse(timestamp)
                    .toInstant(java.time.ZoneOffset.UTC)
            // פורמט עם רווח: 2024-01-15 10:30:00
            timestamp.contains(' ') ->
                java.time.LocalDateTime.parse(
                    timestamp.replace(' ', 'T')
                ).toInstant(java.time.ZoneOffset.UTC)
            else -> return ""
        }

        // המרה ל-timezone של המכשיר
        val zoneId    = java.time.ZoneId.systemDefault()
        val localDt   = instant.atZone(zoneId)
        val today     = java.time.LocalDate.now(zoneId)

        when (localDt.toLocalDate()) {
            today                  -> "%02d:%02d".format(localDt.hour, localDt.minute)
            today.minusDays(1)     -> "Yesterday"
            else                   -> "%02d/%02d".format(localDt.dayOfMonth, localDt.monthValue)
        }
    } catch (e: Exception) {
        android.util.Log.e("formatChatTime", "Failed to parse: $timestamp", e)
        ""
    }
}

@Composable
fun NewChatDialog(
    viewModel: ChatViewModel,
    onDismiss: () -> Unit,
    onChatCreated: (String) -> Unit
) {
    val friends   by viewModel.friends.collectAsState()
    val selected  = remember { mutableStateListOf<String>() }
    var groupName by remember { mutableStateOf("") }
    val isGroup   = selected.size > 1

    LaunchedEffect(Unit) { viewModel.loadFriends() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Chat", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                if (isGroup) {
                    OutlinedTextField(
                        value = groupName,
                        onValueChange = { groupName = it },
                        label = { Text("Group name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                }
                Text("Select friends:", fontSize = 13.sp, color = Color.Gray)
                Spacer(Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    items(friends.filter { it.status == "approved" }) { friend ->
                        val friendId = friend.phone
                        val isChecked = friendId in selected
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isChecked) selected.remove(friendId)
                                    else selected.add(friendId)
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = {
                                    if (it) selected.add(friendId) else selected.remove(friendId)
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = CardifyColors.DarkGreen)
                            )
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.Default.Person, null,
                                tint = CardifyColors.DarkGreen,
                                modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(friend.name, fontSize = 14.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selected.isNotEmpty()) {
                        viewModel.createChat(
                            participantPhones = selected.toList(),
                            groupName = if (isGroup) groupName else "",
                            onSuccess = { chatId -> onChatCreated(chatId) }
                        )
                    }
                },
                enabled = selected.isNotEmpty(),
                colors  = ButtonDefaults.buttonColors(containerColor = CardifyColors.DarkGreen)
            ) { Text("Start Chat") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        shape = RoundedCornerShape(16.dp)
    )
}