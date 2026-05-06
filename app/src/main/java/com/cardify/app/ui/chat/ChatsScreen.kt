package com.cardify.app.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cardify.app.data.UserSession
import com.cardify.app.data.model.Chat
import com.cardify.app.data.model.ChatTransaction
import com.cardify.app.data.model.Friend
import com.cardify.app.ui.components.AppScaffold
import com.cardify.app.ui.home.CardifyColors

@Composable
fun ChatsScreen(
    onNavigate: (String) -> Unit,
    onOpenChat: (Chat) -> Unit,
    pendingTransaction: ChatTransaction? = null,
    viewModel: ChatViewModel = viewModel()
) {
    val chats       by viewModel.chats.collectAsState()
    val friends     by viewModel.friends.collectAsState()
    val isLoading   by viewModel.isLoading.collectAsState()
    var showNewChat by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val currentUserId   = UserSession.userId ?: ""
    val approvedFriends = remember(friends) { friends.filter { it.status == "approved" } }

    val filteredChats = remember(chats, searchQuery) {
        if (searchQuery.isBlank()) chats
        else chats.filter { chat ->
            val displayName = chatDisplayName(chat, currentUserId)
            displayName.contains(searchQuery, ignoreCase = true) ||
                    chat.lastMessage.contains(searchQuery, ignoreCase = true)
        }
    }

    val matchingFriends = remember(approvedFriends, searchQuery, chats) {
        if (searchQuery.isBlank()) emptyList()
        else {
            approvedFriends.filter { friend ->
                friend.name.contains(searchQuery, ignoreCase = true) &&
                        chats.none { chat ->
                            !chat.isGroup &&
                                    chat.participants.any { it != currentUserId &&
                                            chat.participantNames[it] == friend.name }
                        }
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadChats()
        viewModel.loadFriends()
    }
    LaunchedEffect(pendingTransaction) {
        pendingTransaction?.let { viewModel.setPendingTransaction(it) }
    }

    if (showNewChat) {
        NewChatDialog(
            viewModel     = viewModel,
            onDismiss     = { showNewChat = false },
            onChatCreated = { chatId ->
                showNewChat = false
                viewModel.loadChats()
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

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    placeholder = { Text("Search chats or friends...", fontSize = 14.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, null,
                            tint = Color.Gray, modifier = Modifier.size(20.dp))
                    },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, null,
                                    tint = Color.Gray, modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    shape = RoundedCornerShape(24.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = CardifyColors.DarkGreen,
                        unfocusedBorderColor = Color(0xFFDDDDDD)
                    )
                )

                if (isLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = CardifyColors.DarkGreen)
                    }
                } else {
                    LazyColumn(Modifier.fillMaxSize()) {
                        if (filteredChats.isNotEmpty()) {
                            items(filteredChats, key = { it.id }) { chat ->
                                ChatItem(
                                    chat          = chat,
                                    currentUserId = currentUserId,
                                    hasPending    = pendingTransaction != null,
                                    onClick       = { onOpenChat(chat) }
                                )
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 72.dp),
                                    color = Color(0xFFEEEEEE)
                                )
                            }
                        }

                        if (matchingFriends.isNotEmpty()) {
                            item {
                                Text(
                                    "Start a chat with",
                                    fontSize = 12.sp, color = Color.Gray,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(
                                        start = 16.dp, top = 12.dp, bottom = 4.dp)
                                )
                            }
                            items(matchingFriends, key = { "friend_${it.phone}" }) { friend ->
                                FriendChatItem(
                                    friend  = friend,
                                    onClick = {
                                        viewModel.createChat(
                                            participantPhones = listOf(friend.phone),
                                            groupName         = "",
                                            onSuccess         = { chatId ->
                                                viewModel.loadChats()
                                                val newChat = chats.find { it.id == chatId }
                                                if (newChat != null) {
                                                    onOpenChat(newChat)
                                                } else {
                                                    onOpenChat(Chat(
                                                        id               = chatId,
                                                        participants     = listOf(currentUserId, friend.phone),
                                                        participantNames = mapOf(),
                                                        isGroup          = false,
                                                        groupName        = ""
                                                    ))
                                                }
                                                searchQuery = ""
                                            }
                                        )
                                    }
                                )
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 72.dp),
                                    color = Color(0xFFEEEEEE)
                                )
                            }
                        }

                        if (filteredChats.isEmpty() && matchingFriends.isEmpty()) {
                            item {
                                Box(
                                    Modifier.fillMaxWidth().padding(vertical = 48.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            if (searchQuery.isBlank()) "No chats yet"
                                            else "No results for \"$searchQuery\"",
                                            color = Color.Gray, fontSize = 16.sp
                                        )
                                        if (searchQuery.isBlank()) {
                                            Spacer(Modifier.height(8.dp))
                                            Text("Tap + to start a conversation",
                                                color = Color.LightGray, fontSize = 13.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── helper ──────────────────────────────────────────────────────────────────

fun chatDisplayName(chat: Chat, currentUserId: String): String {
    return if (chat.isGroup) {
        chat.groupName.ifBlank { "Group" }
    } else {
        val otherId = chat.participants.firstOrNull { it != currentUserId } ?: ""
        chat.displayNames[otherId]
            ?: chat.participantNames[otherId]
            ?: "Unknown"
    }
}

// ─── ChatItem ────────────────────────────────────────────────────────────────

@Composable
fun ChatItem(
    chat: Chat,
    currentUserId: String,
    hasPending: Boolean = false,
    onClick: () -> Unit
) {
    val context     = LocalContext.current
    val displayName = chatDisplayName(chat, currentUserId)
    val initials    = displayName.take(1).uppercase()

    // תמונת הצד השני בצ'ט 1:1
    val otherId  = if (!chat.isGroup) chat.participants.firstOrNull { it != currentUserId } ?: "" else ""
    val photoUrl = if (!chat.isGroup) chat.participantPhotos[otherId] else null

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
            if (!photoUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(photoUrl).crossfade(true).build(),
                    contentDescription = displayName,
                    contentScale       = ContentScale.Crop,
                    modifier           = Modifier.fillMaxSize().clip(CircleShape)
                )
            } else {
                Text(initials, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                    color = CardifyColors.DarkGreen)
            }
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
                        Text(chat.unreadCount.toString(),
                            fontSize = 11.sp, color = Color.White,
                            fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ─── FriendChatItem ──────────────────────────────────────────────────────────

@Composable
fun FriendChatItem(friend: Friend, onClick: () -> Unit) {
    val context  = LocalContext.current
    val initials = friend.name.take(1).uppercase()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(CardifyColors.DarkGreen.copy(alpha = 0.03f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(CardifyColors.DarkGreen.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            if (!friend.photoUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(friend.photoUrl).crossfade(true).build(),
                    contentDescription = friend.name,
                    contentScale       = ContentScale.Crop,
                    modifier           = Modifier.fillMaxSize().clip(CircleShape)
                )
            } else {
                Text(initials, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                    color = CardifyColors.DarkGreen)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(friend.name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text("Tap to start a chat", fontSize = 12.sp, color = Color.Gray)
        }
        Icon(
            Icons.Default.Add, contentDescription = null,
            tint     = CardifyColors.DarkGreen,
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(CardifyColors.DarkGreen.copy(alpha = 0.1f))
                .padding(3.dp)
        )
    }
}

// ─── NewChatDialog ───────────────────────────────────────────────────────────

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
        text  = {
            Column {
                if (isGroup) {
                    OutlinedTextField(
                        value = groupName, onValueChange = { groupName = it },
                        label = { Text("Group name") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                }
                Text("Select friends:", fontSize = 13.sp, color = Color.Gray)
                Spacer(Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    items(friends.filter { it.status == "approved" }) { friend ->
                        val isChecked = friend.phone in selected
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isChecked) selected.remove(friend.phone)
                                    else selected.add(friend.phone)
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = {
                                    if (it) selected.add(friend.phone)
                                    else selected.remove(friend.phone)
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = CardifyColors.DarkGreen)
                            )
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.Default.Person, null,
                                tint = CardifyColors.DarkGreen, modifier = Modifier.size(20.dp))
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

// ─── formatChatTime ──────────────────────────────────────────────────────────

fun formatChatTime(timestamp: String): String {
    if (timestamp.isBlank()) return ""
    return try {
        val instant = when {
            timestamp.contains('T') && (timestamp.contains('+') || timestamp.endsWith('Z')) ->
                java.time.OffsetDateTime.parse(timestamp).toInstant()
            timestamp.contains('T') ->
                java.time.LocalDateTime.parse(timestamp)
                    .toInstant(java.time.ZoneOffset.UTC)
            timestamp.contains(' ') ->
                java.time.LocalDateTime.parse(
                    timestamp.replace(' ', 'T')
                ).toInstant(java.time.ZoneOffset.UTC)
            else -> return ""
        }
        val zoneId  = java.time.ZoneId.systemDefault()
        val localDt = instant.atZone(zoneId)
        val today   = java.time.LocalDate.now(zoneId)
        when (localDt.toLocalDate()) {
            today              -> "%02d:%02d".format(localDt.hour, localDt.minute)
            today.minusDays(1) -> "Yesterday"
            else               -> "%02d/%02d".format(localDt.dayOfMonth, localDt.monthValue)
        }
    } catch (e: Exception) {
        ""
    }
}