package com.cardify.app.ui.chat

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cardify.app.data.UserSession
import com.cardify.app.data.model.Chat
import com.cardify.app.data.model.ChatMessage
import com.cardify.app.data.model.ChatTransaction
import com.cardify.app.data.model.ReplySnapshot
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    chatId: String,
    chatName: String,
    isGroup: Boolean = false,
    displayNames: Map<String, String> = emptyMap(),
    otherPhone: String = "",
    onTransactionSent: (() -> Unit)? = null,
    onNavigateToChat: ((Chat) -> Unit)? = null,
    onBack: () -> Unit,
    initialPendingTxn: ChatTransaction? = null,
    externalNicknames: Map<String, String> = emptyMap(),
    onSetNickname: (phone: String, nickname: String) -> Unit = { _, _ -> },
    viewModel: ChatViewModel = viewModel()
) {
    val messages      by viewModel.messages.collectAsState()
    val isLoading     by viewModel.isLoading.collectAsState()
    val pendingTxn    by viewModel.pendingTransaction.collectAsState()
    val replyTo       by viewModel.replyTo.collectAsState()
    val chats         by viewModel.chats.collectAsState()
    val friends       by viewModel.friends.collectAsState()
    var text          by remember { mutableStateOf("") }
    val listState     = rememberLazyListState()
    val currentUserId = UserSession.userId ?: ""
    val scope         = rememberCoroutineScope()
    val nicknames     = externalNicknames

    var currentGroupName by remember(chatId, chats) {
        mutableStateOf(
            if (isGroup) chats.firstOrNull { it.id == chatId }?.groupName
                ?.takeIf { it.isNotBlank() } ?: chatName
            else chatName
        )
    }

    val displayChatName = remember(otherPhone, nicknames, currentGroupName, isGroup) {
        if (isGroup) currentGroupName
        else if (otherPhone.isNotBlank()) nicknames[otherPhone]?.takeIf { it.isNotBlank() } ?: chatName
        else chatName
    }

    val senderPhones: Map<String, String> = remember(messages, friends) {
        val nameToPhone = friends.associate { it.name to it.phone }
        messages.associate { msg -> msg.senderId to (nameToPhone[msg.senderName] ?: "") }
    }

    var showNicknameDialog  by remember { mutableStateOf(false) }
    var nicknameInput       by remember { mutableStateOf("") }
    var showGroupNameDialog by remember { mutableStateOf(false) }
    var groupNameInput      by remember { mutableStateOf("") }

    var searchActive      by remember { mutableStateOf(false) }
    var searchQuery       by remember { mutableStateOf("") }
    val matchIndices = remember(messages, searchQuery) {
        if (searchQuery.isBlank()) emptyList()
        else messages.indices.filter { i ->
            val msg = messages[i]
            msg.text.contains(searchQuery, ignoreCase = true) ||
                    (msg.transaction?.businessName?.contains(searchQuery, ignoreCase = true) == true)
        }
    }
    var currentMatchIndex by remember(matchIndices) { mutableIntStateOf(0) }

    var selectedMessage    by remember { mutableStateOf<ChatMessage?>(null) }
    var showMessageSheet   by remember { mutableStateOf(false) }
    var showEmojiPicker    by remember { mutableStateOf(false) }
    var emojiTargetMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var showForwardSheet   by remember { mutableStateOf(false) }
    var forwardMessage     by remember { mutableStateOf<ChatMessage?>(null) }
    var messageToDelete    by remember { mutableStateOf<ChatMessage?>(null) }

    val floatingDateLabel by remember {
        derivedStateOf {
            messages.getOrNull(listState.firstVisibleItemIndex)
                ?.timestamp?.let { extractDateLabel(it) } ?: ""
        }
    }
    val showFloatingDate by remember {
        derivedStateOf { floatingDateLabel.isNotBlank() && listState.firstVisibleItemIndex > 0 }
    }

    LaunchedEffect(chatId) {
        viewModel.loadMessages(chatId)
        viewModel.loadChats()
        viewModel.loadFriends()
    }
    LaunchedEffect(initialPendingTxn) {
        initialPendingTxn?.let { viewModel.setPendingTransaction(it) }
    }
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty() && !searchActive) listState.animateScrollToItem(messages.size - 1)
    }

    // ─── Delete dialog ────────────────────────────────────────────────────────
    if (messageToDelete != null) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            AlertDialog(
                onDismissRequest = { messageToDelete = null },
                title = { Text("Delete Message", fontWeight = FontWeight.Bold) },
                text  = { Text("Are you sure you want to delete this message? This cannot be undone.") },
                confirmButton = {
                    Button(
                        onClick = {
                            messageToDelete?.let { viewModel.deleteMessage(chatId, it.id) }
                            messageToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE23125))
                    ) { Text("Delete") }
                },
                dismissButton = {
                    TextButton(onClick = { messageToDelete = null }) { Text("Cancel") }
                },
                shape = RoundedCornerShape(16.dp)
            )
        }
    }

    // ─── Nickname dialog (1:1) ────────────────────────────────────────────────
    if (showNicknameDialog && !isGroup) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Dialog(onDismissRequest = { showNicknameDialog = false }) {
                Surface(
                    shape    = RoundedCornerShape(16.dp),
                    color    = Color.White,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text("Rename $chatName",
                            fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(Modifier.height(16.dp))

                        if (otherPhone.isNotBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.07f))
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.Phone, null,
                                    tint     = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(otherPhone, fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color      = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(Modifier.height(12.dp))
                        }

                        Text("Set a custom name for $chatName.\nLeave blank to use the original name.",
                            fontSize = 13.sp, color = Color.Gray)
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value         = nicknameInput,
                            onValueChange = { nicknameInput = it },
                            label         = { Text("Custom name") },
                            singleLine    = true,
                            modifier      = Modifier.fillMaxWidth(),
                            colors        = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary)
                        )
                        Spacer(Modifier.height(24.dp))
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { showNicknameDialog = false }) { Text("Cancel") }
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    onSetNickname(otherPhone, nicknameInput.trim())
                                    showNicknameDialog = false
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary)
                            ) { Text("Save") }
                        }
                    }
                }
            }
        }
    }

    // ─── Group dialog — שם + רשימת חברים עם מספרי טלפון ─────────────────────
    if (showGroupNameDialog && isGroup) {
        val groupMembers = remember(chats, friends, chatId) {
            val chat = chats.firstOrNull { it.id == chatId }
            chat?.participants
                ?.filter { it != currentUserId }
                ?.map { participantId ->
                    val name  = chat.displayNames[participantId]
                        ?: chat.participantNames[participantId]
                        ?: participantId
                    val phone = friends.firstOrNull { f ->
                        f.phone == participantId ||
                                f.name  == chat.participantNames[participantId]
                    }?.phone
                        ?: if (participantId.all { c -> c.isDigit() || c == '+' || c == '-' }) participantId else ""
                    Pair(name, phone)
                } ?: emptyList()
        }

        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Dialog(onDismissRequest = { showGroupNameDialog = false }) {
                Surface(
                    shape    = RoundedCornerShape(16.dp),
                    color    = Color.White,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text("Edit Group",
                            fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(Modifier.height(16.dp))

                        // ─── שם קבוצה ─────────────────────────────────
                        OutlinedTextField(
                            value         = groupNameInput,
                            onValueChange = { groupNameInput = it },
                            label         = { Text("Group name") },
                            singleLine    = true,
                            modifier      = Modifier.fillMaxWidth(),
                            colors        = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary)
                        )

                        // ─── חברי הקבוצה ──────────────────────────────
                        if (groupMembers.isNotEmpty()) {
                            Spacer(Modifier.height(16.dp))
                            Text("Members (${groupMembers.size})",
                                fontSize = 12.sp, color = Color.Gray,
                                fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(8.dp))
                            groupMembers.forEach { (name, phone) ->
                                Row(
                                    modifier          = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier         = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(name.take(1).uppercase(),
                                            fontSize = 13.sp, fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary)
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text(name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                        if (phone.isNotBlank()) {
                                            Text(phone, fontSize = 11.sp, color = Color.Gray)
                                        }
                                    }
                                }
                                Spacer(Modifier.height(4.dp))
                            }
                        }

                        Spacer(Modifier.height(20.dp))
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { showGroupNameDialog = false }) { Text("Cancel") }
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    val trimmed = groupNameInput.trim()
                                    if (trimmed.isNotBlank()) {
                                        viewModel.updateGroupName(chatId, trimmed) {
                                            currentGroupName = trimmed
                                        }
                                        showGroupNameDialog = false
                                    }
                                },
                                enabled = groupNameInput.isNotBlank(),
                                colors  = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary)
                            ) { Text("Save") }
                        }
                    }
                }
            }
        }
    }

    if (showMessageSheet && selectedMessage != null) {
        val msg = selectedMessage!!
        MessageOptionsSheet(
            message   = msg,
            isMe      = msg.senderId == currentUserId,
            onDismiss = { showMessageSheet = false; selectedMessage = null },
            onReact   = { emojiTargetMessage = msg; showMessageSheet = false; showEmojiPicker = true },
            onReply   = { viewModel.setReplyTo(msg); showMessageSheet = false },
            onForward = { forwardMessage = msg; showMessageSheet = false; showForwardSheet = true },
            onDelete  = { messageToDelete = msg; showMessageSheet = false }
        )
    }

    if (showEmojiPicker && emojiTargetMessage != null) {
        EmojiPickerSheet(
            message       = emojiTargetMessage!!,
            currentUserId = currentUserId,
            onDismiss     = { showEmojiPicker = false; emojiTargetMessage = null },
            onEmojiPick   = { emoji ->
                viewModel.reactToMessage(chatId, emojiTargetMessage!!.id, emoji, currentUserId)
                showEmojiPicker = false; emojiTargetMessage = null
            }
        )
    }

    if (showForwardSheet && forwardMessage != null) {
        ForwardSheet(
            chats         = chats,
            currentUserId = currentUserId,
            currentChatId = chatId,
            onDismiss     = { showForwardSheet = false; forwardMessage = null },
            onSelect      = { targetChat ->
                viewModel.forwardMessage(targetChatId = targetChat.id, message = forwardMessage!!)
                showForwardSheet = false; forwardMessage = null
                onNavigateToChat?.invoke(targetChat)
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (searchActive) {
                        OutlinedTextField(
                            value         = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder   = { Text("Search messages...", fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.7f)) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor   = Color.White.copy(alpha = 0.5f),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                focusedTextColor     = Color.White,
                                unfocusedTextColor   = Color.White,
                                cursorColor          = Color.White),
                            shape    = RoundedCornerShape(20.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text(displayChatName, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (searchActive) { searchActive = false; searchQuery = "" } else onBack()
                    }) { Icon(Icons.Default.ArrowBack, null) }
                },
                actions = {
                    if (searchActive) {
                        if (matchIndices.isNotEmpty()) {
                            Text("${currentMatchIndex + 1}/${matchIndices.size}",
                                color = Color.White, fontSize = 12.sp,
                                modifier = Modifier.padding(end = 4.dp))
                            IconButton(onClick = {
                                if (currentMatchIndex > 0) {
                                    currentMatchIndex--
                                    scope.launch { listState.animateScrollToItem(matchIndices[currentMatchIndex]) }
                                }
                            }) { Icon(Icons.Default.KeyboardArrowUp, null, tint = Color.White) }
                            IconButton(onClick = {
                                if (currentMatchIndex < matchIndices.size - 1) {
                                    currentMatchIndex++
                                    scope.launch { listState.animateScrollToItem(matchIndices[currentMatchIndex]) }
                                }
                            }) { Icon(Icons.Default.KeyboardArrowDown, null, tint = Color.White) }
                        } else if (searchQuery.isNotBlank()) {
                            Text("No results", color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp, modifier = Modifier.padding(end = 8.dp))
                        }
                        IconButton(onClick = { searchActive = false; searchQuery = "" }) {
                            Icon(Icons.Default.Close, null, tint = Color.White)
                        }
                    } else {
                        IconButton(onClick = {
                            if (isGroup) {
                                groupNameInput      = currentGroupName
                                showGroupNameDialog = true
                            } else {
                                nicknameInput      = nicknames[otherPhone] ?: ""
                                showNicknameDialog = true
                            }
                        }) { Icon(Icons.Default.Edit, null, tint = Color.White) }
                        IconButton(onClick = { searchActive = true }) {
                            Icon(Icons.Default.Search, null, tint = Color.White)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor             = MaterialTheme.colorScheme.primary,
                    titleContentColor          = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier.fillMaxWidth().background(Color.White)
            ) {
                if (pendingTxn != null) {
                    PendingTransactionPreview(
                        txn      = pendingTxn!!,
                        onRemove = { viewModel.clearPendingTransaction() }
                    )
                }
                if (replyTo != null) {
                    ReplyPreview(
                        message      = replyTo!!,
                        nicknames    = nicknames,
                        senderPhones = senderPhones,
                        onRemove     = { viewModel.clearReplyTo() }
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value         = text,
                        onValueChange = { text = it },
                        modifier      = Modifier.weight(1f),
                        placeholder   = { Text("Message...", fontSize = 14.sp) },
                        shape         = RoundedCornerShape(24.dp),
                        maxLines      = 3,
                        colors        = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color(0xFFDDDDDD))
                    )
                    Spacer(Modifier.width(8.dp))
                    val canSend = text.isNotBlank() || pendingTxn != null
                    IconButton(
                        onClick = {
                            if (canSend) {
                                val hadPendingTxn = pendingTxn != null
                                viewModel.sendMessage(
                                    chatId       = chatId,
                                    text         = text,
                                    transaction  = pendingTxn,
                                    nicknames    = nicknames,
                                    senderPhones = senderPhones
                                )
                                text = ""
                                if (hadPendingTxn) onTransactionSent?.invoke()
                            }
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                if (canSend) MaterialTheme.colorScheme.primary else Color(0xFFCCCCCC),
                                RoundedCornerShape(22.dp))
                    ) {
                        Icon(Icons.Default.Send, null, tint = Color.White,
                            modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            val showScrollButton by remember {
                derivedStateOf {
                    listState.canScrollForward &&
                            listState.firstVisibleItemIndex < messages.size - 3
                }
            }
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                LazyColumn(
                    state          = listState,
                    modifier       = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFF5F5F5))
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    items(messages, key = { it.id }) { message ->
                        val msgIndex    = messages.indexOf(message)
                        val isHighlight = searchQuery.isNotBlank() &&
                                matchIndices.getOrNull(currentMatchIndex) == msgIndex
                        val showDateHeader = msgIndex == 0 ||
                                extractDateLabel(message.timestamp) !=
                                extractDateLabel(messages[msgIndex - 1].timestamp)
                        if (showDateHeader && message.timestamp.isNotBlank()) {
                            DateHeader(label = extractDateLabel(message.timestamp))
                        }
                        MessageBubble(
                            message         = message,
                            isMe            = message.senderId == currentUserId,
                            showSenderName  = isGroup,
                            isHighlighted   = isHighlight,
                            searchQuery     = searchQuery,
                            currentUserId   = currentUserId,
                            nicknames       = nicknames,
                            senderPhones    = senderPhones,
                            onSwipeReply    = { viewModel.setReplyTo(message) },
                            onLongPress     = {
                                if (!message.deleted) {
                                    selectedMessage  = message
                                    showMessageSheet = true
                                }
                            },
                            onReactionClick = { emoji ->
                                viewModel.reactToMessage(chatId, message.id, emoji, currentUserId)
                            }
                        )
                    }
                }

                AnimatedVisibility(
                    visible  = showFloatingDate,
                    enter    = fadeIn(), exit = fadeOut(),
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp)
                ) {
                    Text(text = floatingDateLabel, fontSize = 11.sp,
                        color = Color.White, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .background(Color(0xFFAAAAAA), RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 3.dp))
                }

                AnimatedVisibility(
                    visible  = showScrollButton,
                    enter    = fadeIn() + scaleIn(), exit = fadeOut() + scaleOut(),
                    modifier = Modifier.align(Alignment.BottomEnd)
                        .padding(end = 12.dp, bottom = 12.dp)
                ) {
                    SmallFloatingActionButton(
                        onClick = {
                            scope.launch {
                                if (messages.isNotEmpty())
                                    listState.animateScrollToItem(messages.size - 1)
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor   = Color.White,
                        shape          = CircleShape,
                        modifier       = Modifier.size(38.dp)
                    ) {
                        Icon(Icons.Default.KeyboardArrowDown, null, modifier = Modifier.size(22.dp))
                    }
                }
            }
        }
    }
}

// ─── MessageOptionsSheet ──────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageOptionsSheet(
    message:   ChatMessage,
    isMe:      Boolean,
    onDismiss: () -> Unit,
    onReact:   () -> Unit,
    onReply:   () -> Unit,
    onForward: () -> Unit,
    onDelete:  () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = Color.White,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!message.deleted) {
                val preview = when {
                    message.audioUrl != null    -> "🎤 Voice message"
                    message.transaction != null -> "📊 ${message.transaction.businessName}"
                    message.text.isNotBlank()   -> message.text.take(80)
                    else                        -> ""
                }
                if (preview.isNotBlank()) {
                    Box(modifier = Modifier.fillMaxWidth()
                        .background(Color(0xFFF5F5F5))
                        .padding(horizontal = 20.dp, vertical = 10.dp)) {
                        Text(preview, fontSize = 13.sp, color = Color.Gray,
                            maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            OptionRow("😊", "React",   MaterialTheme.colorScheme.primary, onReact)
            OptionRow("↩",  "Reply",   MaterialTheme.colorScheme.primary, onReply)
            if (!message.deleted) OptionRow("↪", "Forward", MaterialTheme.colorScheme.primary, onForward)
            if (isMe && !message.deleted) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
                OptionRow("🗑", "Delete", Color(0xFFE23125), onDelete)
            }
        }
    }
}

@Composable
private fun OptionRow(icon: String, label: String, color: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, fontSize = 22.sp)
        Spacer(Modifier.width(16.dp))
        Text(label, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = color)
    }
}

// ─── EmojiPickerSheet ─────────────────────────────────────────────────────────

private val EMOJI_CATEGORIES = listOf(
    "😊 Smileys"  to listOf("😀","😃","😄","😁","😆","😅","🤣","😂","🙂","🙃","😉","😊","😇","🥰","😍","🤩","😘","😗","😚","😙","🥲","😋","😛","😜","🤪","😝","🤑","🤗","🤭","🤫","🤔","🤐","🤨","😐","😑","😶","😏","😒","🙄","😬","🤥","😌","😔","😪","🤤","😴","😷","🤒","🤕","🤢","🤧","🥵","🥶","🥴","😵","🤯","🤠","🥳","😎","🤓","🧐","😕","😟","🙁","☹️","😮","😯","😲","😳","🥺","😦","😧","😨","😰","😥","😢","😭","😱","😖","😣","😞","😓","😩","😫","🥱","😤","😡","😠","🤬","😈","👿","💀","☠️","💩","🤡","👹","👺","👻","👽","👾","🤖"),
    "👋 Gestures" to listOf("👋","🤚","🖐","✋","🖖","👌","🤌","🤏","✌️","🤞","🤟","🤘","🤙","👈","👉","👆","🖕","👇","☝️","👍","👎","✊","👊","🤛","🤜","👏","🙌","👐","🤲","🤝","🙏","✍️","💅","🤳","💪","🦾","🦿","🦵","🦶","👂","🦻","👃","🧠","🦷","🦴","👀","👁","👅","👄"),
    "❤️ Hearts"   to listOf("❤️","🧡","💛","💚","💙","💜","🖤","🤍","🤎","💔","❤️‍🔥","❤️‍🩹","❣️","💕","💞","💓","💗","💖","💘","💝","💟"),
    "🎉 Party"    to listOf("🎉","🎊","🎈","🎁","🎀","🎗","🎟","🎫","🎖","🏆","🥇","🥈","🥉","🏅","🎪","🤹","🎭","🩰","🎨","🎬","🎤","🎧","🎼","🎹","🥁","🪘","🎷","🎺","🎸","🪕","🎻","🪗","🎲","♟","🎯","🎳","🎮","🎰","🧩"),
    "🐶 Animals"  to listOf("🐶","🐱","🐭","🐹","🐰","🦊","🐻","🐼","🐨","🐯","🦁","🐮","🐷","🐸","🐵","🙈","🙉","🙊","🐒","🐔","🐧","🐦","🐤","🦆","🦅","🦉","🦇","🐝","🐛","🦋","🐌","🐞","🐜","🦟","🦗","🕷","🦂","🐢","🐍","🦎","🐙","🦑","🦐","🦞","🦀","🐡","🐠","🐟","🐬","🐳","🐋","🦈","🐊","🐅","🐆","🦓","🦍","🐘","🦛","🦏","🐪","🐫","🦒","🦘","🐃","🐂","🐄","🐎","🐖","🐏","🐑","🦙","🐐","🦌","🐕","🐩","🐈","🐓","🦃","🦚","🦜","🦢","🦩","🕊","🐇","🦝","🦨","🦡","🦦","🦥","🐁","🐀","🐿","🦔"),
    "🍕 Food"     to listOf("🍕","🍔","🌮","🌯","🥙","🧆","🥚","🍳","🥘","🍲","🥣","🥗","🍿","🧈","🥞","🧇","🥓","🥩","🍗","🍖","🌭","🍟","🍱","🍘","🍙","🍚","🍛","🍜","🍝","🍠","🍢","🍣","🍤","🍥","🥮","🍡","🥟","🥠","🥡","🍦","🍧","🍨","🍩","🍪","🎂","🍰","🧁","🥧","🍫","🍬","🍭","🍮","🍯","🍼","🥛","☕","🫖","🍵","🧃","🥤","🧋","🍶","🍺","🍻","🥂","🍷","🥃","🍸","🍹","🧉","🍾","🧊"),
    "🏠 Places"   to listOf("🏠","🏡","🏢","🏣","🏤","🏥","🏦","🏨","🏩","🏪","🏫","🏬","🏭","🏯","🏰","💒","🗼","🗽","⛪","🕌","🛕","🕍","⛩","🕋","⛲","⛺","🌁","🌃","🏙","🌄","🌅","🌆","🌇","🌉","🌌","🌠","🎇","🎆","🗺","🧭","🏔","⛰","🌋","🗻","🏕","🏖","🏜","🏝","🏞","🏟","🏛","🎡","🎢","🎠","⛱","🏗","🌐","🗾","🧱","🛤","🛣"),
    "✈️ Travel"   to listOf("✈️","🚀","🛸","🚁","🛶","⛵","🚤","🛥","🛳","⛴","🚢","🚂","🚃","🚄","🚅","🚆","🚇","🚈","🚉","🚊","🚝","🚞","🚋","🚌","🚍","🚎","🚐","🚑","🚒","🚓","🚔","🚕","🚖","🚗","🚘","🚙","🛻","🚚","🚛","🚜","🏎","🏍","🛵","🛺","🚲","🛴","🛹","🛼","🚏","🛣","🛤","⛽","🚨","🚥","🚦","🛑","🚧","⚓","⛵","🚤"),
    "⚽ Sports"   to listOf("⚽","🏀","🏈","⚾","🥎","🎾","🏐","🏉","🥏","🎱","🪀","🏓","🏸","🏒","🏑","🥍","🏏","🪃","🥅","⛳","🪁","🏹","🎣","🤿","🥊","🥋","🎽","🛹","🛼","🛷","⛸","🥌","🎿","⛷","🏂","🪂","🏋️","🤸","🤺","🏇","⛹","🤾","🏌","🏄","🚣","🧘","🏊","🚴","🤼","🤽","🧗","🏆","🥇","🥈","🥉","🏅","🎖","🏵","🎗","🎫","🎟","🎪"),
    "💡 Objects"  to listOf("💡","🔦","🕯","🪔","🧯","🛢","💰","💴","💵","💶","💷","💸","💳","🪙","💹","✉️","📧","📨","📩","📤","📥","📦","📫","📪","📬","📭","📮","🗳","✏️","✒️","🖊","🖋","📝","📁","📂","🗂","📅","📆","🗒","🗓","📇","📈","📉","📊","📋","📌","📍","🗺","📎","🖇","📏","📐","✂️","🗃","🗄","🗑","🔒","🔓","🔏","🔐","🔑","🗝","🔨","🪓","⛏","⚒","🛠","🗡","⚔️","🔫","🪃","🛡","🪚","🔧","🪛","🔩","⚙️","🗜","⚖️","🦯","🔗","⛓","🪝","🧲","🪜","⚗️","🪣","🔭","🔬","🩻","🩹","🩺","💊","💉","🩸","🧬","🦠","🧫","🧪","🌡","🧹","🪣","🧺","🧻","🚽","🚱","🚿","🛁","🛀","🪥","🧼","🫧","🪒","🧴","🧷","🧹","🧺","🧻","🪣","🧼","🫧")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmojiPickerSheet(
    message:       ChatMessage,
    currentUserId: String,
    onDismiss:     () -> Unit,
    onEmojiPick:   (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    var selectedCategory by remember { mutableIntStateOf(0) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = Color.White,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().height(440.dp)) {
            Text("React", fontWeight = FontWeight.Bold, fontSize = 16.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            ScrollableTabRow(selectedTabIndex = selectedCategory,
                containerColor = Color.White,
                contentColor   = MaterialTheme.colorScheme.primary,
                edgePadding    = 8.dp
            ) {
                EMOJI_CATEGORIES.forEachIndexed { index, (label, _) ->
                    Tab(selected = selectedCategory == index,
                        onClick  = { selectedCategory = index },
                        text     = { Text(label.split(" ").first(), fontSize = 18.sp) })
                }
            }
            val emojis         = EMOJI_CATEGORIES[selectedCategory].second
            val myCurrentEmoji = message.reactions[currentUserId] ?: ""
            LazyColumn(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                val rows = emojis.chunked(8)
                items(rows) { row ->
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly) {
                        row.forEach { emoji ->
                            val isSelected = emoji == myCurrentEmoji
                            Box(
                                modifier = Modifier.size(42.dp).clip(CircleShape)
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent)
                                    .clickable { onEmojiPick(emoji) },
                                contentAlignment = Alignment.Center
                            ) { Text(emoji, fontSize = 22.sp) }
                        }
                        repeat(8 - row.size) { Spacer(Modifier.size(42.dp)) }
                    }
                }
            }
        }
    }
}

// ─── ForwardSheet ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForwardSheet(
    chats:         List<Chat>,
    currentUserId: String,
    currentChatId: String = "",
    onDismiss:     () -> Unit,
    onSelect:      (Chat) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val otherChats = remember(chats, currentChatId) { chats.filter { it.id != currentChatId } }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = Color.White,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
            Text("Forward to...", fontWeight = FontWeight.Bold, fontSize = 18.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp))
            if (otherChats.isEmpty()) {
                Column(modifier = Modifier.fillMaxWidth()
                    .padding(vertical = 40.dp, horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("💬", fontSize = 40.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("No other chats to forward to",
                        fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Color.DarkGray)
                    Spacer(Modifier.height(6.dp))
                    Text("Start a new conversation first,\nthen you can forward messages there.",
                        fontSize = 13.sp, color = Color.Gray,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    items(otherChats, key = { it.id }) { chat ->
                        val displayName = if (chat.isGroup) chat.groupName.ifBlank { "Group" }
                        else {
                            val otherId = chat.participants.firstOrNull { it != currentUserId } ?: ""
                            chat.displayNames[otherId] ?: chat.participantNames[otherId] ?: "Chat"
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { onSelect(chat) }
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(46.dp).clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(displayName.take(1).uppercase(), fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(displayName, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                if (chat.lastMessage.isNotBlank()) {
                                    Text(chat.lastMessage, fontSize = 12.sp, color = Color.Gray,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                            Icon(Icons.Default.Send, null,
                                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        }
                        HorizontalDivider(modifier = Modifier.padding(start = 78.dp),
                            color = Color(0xFFEEEEEE))
                    }
                }
            }
        }
    }
}

// ─── MessageBubble ────────────────────────────────────────────────────────────

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MessageBubble(
    message: ChatMessage,
    isMe: Boolean,
    showSenderName: Boolean,
    isHighlighted: Boolean = false,
    searchQuery: String = "",
    currentUserId: String = "",
    nicknames: Map<String, String> = emptyMap(),
    senderPhones: Map<String, String> = emptyMap(),
    onSwipeReply: () -> Unit,
    onLongPress: () -> Unit = {},
    onReactionClick: (String) -> Unit = {}
) {
    val offsetX   = remember { Animatable(0f) }
    val THRESHOLD = 80f
    val scope     = rememberCoroutineScope()

    val senderDisplayName = remember(message.senderId, nicknames, senderPhones) {
        val phone = senderPhones[message.senderId] ?: ""
        if (phone.isNotBlank()) nicknames[phone]?.takeIf { it.isNotBlank() } ?: message.senderName
        else message.senderName
    }

    val highlightAlpha by animateFloatAsState(
        targetValue = if (isHighlighted) 0.25f else 0f,
        animationSpec = tween(300), label = "highlight"
    )

    if (message.deleted) {
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
            horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
        ) {
            Box(modifier = Modifier.widthIn(max = 200.dp)
                .background(Color(0xFFEEEEEE), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("🗑", fontSize = 14.sp)
                    Text("Message deleted", fontSize = 13.sp,
                        color = Color.Gray, fontStyle = FontStyle.Italic)
                }
            }
        }
        return
    }

    Column(
        modifier = Modifier.fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary.copy(alpha = highlightAlpha))
            .padding(vertical = if (isHighlighted) 2.dp else 0.dp)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        scope.launch {
                            if (offsetX.value > THRESHOLD) onSwipeReply()
                            offsetX.animateTo(0f, spring(stiffness = Spring.StiffnessMedium))
                        }
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        scope.launch { offsetX.snapTo((offsetX.value + dragAmount).coerceIn(0f, 120f)) }
                    }
                )
            }
            .offset { IntOffset(offsetX.value.roundToInt(), 0) },
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        if (offsetX.value > 20f) {
            Icon(Icons.Default.Reply, null,
                tint = MaterialTheme.colorScheme.primary.copy(
                    alpha = (offsetX.value / THRESHOLD).coerceIn(0f, 1f)),
                modifier = Modifier.size(22.dp).align(Alignment.Start).padding(start = 4.dp))
        }
        if (!isMe && showSenderName) {
            Text(senderDisplayName, fontSize = 11.sp,
                color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp))
        }
        if (message.forwarded) {
            Row(modifier = Modifier.padding(bottom = 2.dp,
                start = if (isMe) 0.dp else 4.dp, end = if (isMe) 4.dp else 0.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("↪ ", fontSize = 11.sp, color = Color.Gray)
                Text("Forwarded", fontSize = 11.sp, color = Color.Gray, fontStyle = FontStyle.Italic)
            }
        }
        Column(
            modifier = Modifier.widthIn(max = 280.dp)
                .background(
                    if (isMe) MaterialTheme.colorScheme.primary else Color.White,
                    RoundedCornerShape(
                        topStart    = 16.dp, topEnd     = 16.dp,
                        bottomStart = if (isMe) 16.dp else 4.dp,
                        bottomEnd   = if (isMe) 4.dp  else 16.dp))
                .pointerInput(isMe) { detectTapGestures(onLongPress = { onLongPress() }) }
                .padding(12.dp)
        ) {
            if (message.replyToMessage != null) {
                QuotedMessage(reply = message.replyToMessage, isMe = isMe,
                    nicknames = nicknames, senderPhones = senderPhones)
                Spacer(Modifier.height(6.dp))
            }
            if (message.transaction != null) {
                TransactionCard(txn = message.transaction, isMe = isMe)
                if (message.text.isNotBlank()) Spacer(Modifier.height(6.dp))
            }
            if (message.text.isNotBlank()) {
                Text(message.text, fontSize = 14.sp,
                    color = if (isMe) Color.White else Color.Black)
            }
            Text(formatChatTime(message.timestamp), fontSize = 10.sp,
                color = if (isMe) Color.White.copy(alpha = 0.7f) else Color.Gray,
                modifier = Modifier.align(Alignment.End).padding(top = 4.dp))
        }
        if (message.reactions.isNotEmpty()) {
            ReactionsBar(reactions = message.reactions, currentUserId = currentUserId,
                isMe = isMe, onReactionClick = onReactionClick)
        }
    }
}

// ─── ReactionsBar ─────────────────────────────────────────────────────────────

@Composable
fun ReactionsBar(
    reactions: Map<String, String>,
    currentUserId: String,
    isMe: Boolean,
    onReactionClick: (String) -> Unit
) {
    val grouped = reactions.values.groupBy { it }.mapValues { it.value.size }
    val myEmoji = reactions[currentUserId] ?: ""
    LazyRow(
        modifier = Modifier.padding(top = 3.dp,
            start = if (isMe) 0.dp else 4.dp, end = if (isMe) 4.dp else 0.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(grouped.entries.toList()) { (emoji, count) ->
            val isMine = emoji == myEmoji
            Box(modifier = Modifier.clip(RoundedCornerShape(12.dp))
                .background(if (isMine) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color(0xFFF0F0F0))
                .clickable { onReactionClick(emoji) }
                .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(buildAnnotatedString {
                    append(emoji)
                    if (count > 1) {
                        append(" ")
                        withStyle(SpanStyle(fontSize = 11.sp, color = Color.Gray)) { append(count.toString()) }
                    }
                }, fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun QuotedMessage(
    reply: ReplySnapshot,
    isMe: Boolean,
    nicknames: Map<String, String> = emptyMap(),
    senderPhones: Map<String, String> = emptyMap()
) {
    val bgColor   = if (isMe) Color.White.copy(alpha = 0.15f) else Color(0xFFF0F0F0)
    val textColor = if (isMe) Color.White else Color.Black
    val nameColor = if (isMe) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.primary
    val barColor  = if (isMe) Color.White.copy(alpha = 0.6f) else MaterialTheme.colorScheme.primary
    Row(modifier = Modifier.fillMaxWidth()
        .background(bgColor, RoundedCornerShape(8.dp)).padding(end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.width(3.dp).height(36.dp)
            .background(barColor, RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp)))
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.padding(vertical = 6.dp)) {
            Text(reply.senderName, fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold, color = nameColor)
            val preview = when {
                reply.isAudio                   -> "🎤 Voice message"
                reply.businessName.isNotBlank() -> "📊 ${reply.businessName}"
                reply.text.isNotBlank()         -> reply.text.take(60)
                else                            -> ""
            }
            if (preview.isNotBlank()) {
                Text(preview, fontSize = 12.sp, color = textColor.copy(alpha = 0.75f),
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
fun ReplyPreview(
    message: ChatMessage,
    nicknames: Map<String, String> = emptyMap(),
    senderPhones: Map<String, String> = emptyMap(),
    onRemove: () -> Unit
) {
    val phone       = senderPhones[message.senderId] ?: ""
    val displayName = if (phone.isNotBlank())
        nicknames[phone]?.takeIf { it.isNotBlank() } ?: message.senderName
    else message.senderName
    Row(modifier = Modifier.fillMaxWidth()
        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
        .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.width(3.dp).height(36.dp)
            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp)))
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(displayName, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary)
            val preview = when {
                message.audioUrl != null    -> "🎤 Voice message"
                message.transaction != null -> "📊 ${message.transaction.businessName}"
                else                        -> message.text.take(60)
            }
            Text(preview, fontSize = 12.sp, color = Color.Gray,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
            Text("✕", fontSize = 14.sp, color = Color.Gray)
        }
    }
}

@Composable
fun TransactionCard(txn: ChatTransaction, isMe: Boolean) {
    val textColor = if (isMe) Color.White else Color.Black
    Column(modifier = Modifier.fillMaxWidth()
        .background(if (isMe) Color.White.copy(alpha = 0.15f) else Color(0xFFF0F0F0),
            RoundedCornerShape(10.dp)).padding(10.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(txn.businessName, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                color = textColor, modifier = Modifier.weight(1f))
            Text("₪${"%.2f".format(txn.amount)}", fontWeight = FontWeight.Bold, fontSize = 13.sp,
                color = if (txn.status == "IRREGULAR") Color(0xFFE23125) else textColor)
        }
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(txn.category, fontSize = 11.sp,
                color = if (isMe) Color.White.copy(alpha = 0.8f) else Color.Gray)
            Text(txn.date, fontSize = 11.sp,
                color = if (isMe) Color.White.copy(alpha = 0.8f) else Color.Gray)
        }
        Text(if (txn.status == "IRREGULAR") "⚠️ Irregular" else "✓ Regular",
            fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
            color = if (txn.status == "IRREGULAR") Color(0xFFE23125)
            else if (isMe) Color.White.copy(alpha = 0.9f) else Color(0xFF38D325))
        if (txn.explanation.isNotBlank() && txn.status == "IRREGULAR") {
            Spacer(Modifier.height(4.dp))
            Text(txn.explanation, fontSize = 10.sp,
                color = if (isMe) Color.White.copy(alpha = 0.7f) else Color.Gray)
        }
    }
}

@Composable
fun PendingTransactionPreview(txn: ChatTransaction, onRemove: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth()
        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
        .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text("📊 ${txn.businessName}", fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
            Text("₪${"%.2f".format(txn.amount)} • ${txn.status}",
                fontSize = 11.sp, color = Color.Gray)
        }
        TextButton(onClick = onRemove) {
            Text("Remove", color = Color.Red, fontSize = 12.sp)
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
fun extractDateLabel(timestamp: String): String {
    if (timestamp.isBlank()) return ""
    return try {
        val instant = when {
            timestamp.contains('T') && (timestamp.contains('+') || timestamp.endsWith('Z')) ->
                java.time.OffsetDateTime.parse(timestamp).toInstant()
            timestamp.contains('T') ->
                java.time.LocalDateTime.parse(timestamp).toInstant(java.time.ZoneOffset.UTC)
            timestamp.contains(' ') ->
                java.time.LocalDateTime.parse(timestamp.replace(' ', 'T'))
                    .toInstant(java.time.ZoneOffset.UTC)
            else -> return ""
        }
        val zoneId = java.time.ZoneId.systemDefault()
        val date   = instant.atZone(zoneId).toLocalDate()
        val today  = java.time.LocalDate.now(zoneId)
        when (date) {
            today              -> "Today"
            today.minusDays(1) -> "Yesterday"
            else -> "%02d/%02d/%d".format(date.dayOfMonth, date.monthValue, date.year)
        }
    } catch (e: Exception) { "" }
}

@Composable
fun DateHeader(label: String) {
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        contentAlignment = Alignment.Center) {
        Text(text = label, fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .background(Color(0xFFAAAAAA), RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = 3.dp))
    }
}