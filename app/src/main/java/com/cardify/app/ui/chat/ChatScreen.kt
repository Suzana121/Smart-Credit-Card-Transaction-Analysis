package com.cardify.app.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cardify.app.data.UserSession
import com.cardify.app.data.model.ChatMessage
import com.cardify.app.data.model.ChatTransaction
import com.cardify.app.data.model.ReplySnapshot
import com.cardify.app.ui.home.CardifyColors
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.core.view.WindowCompat
import androidx.compose.ui.platform.LocalView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    chatId: String,
    chatName: String,
    isGroup: Boolean = false,
    onBack: () -> Unit,
    initialPendingTxn: ChatTransaction? = null,
    viewModel: ChatViewModel = viewModel()
) {
    val context       = LocalContext.current
    val messages      by viewModel.messages.collectAsState()
    val isLoading     by viewModel.isLoading.collectAsState()
    val pendingTxn    by viewModel.pendingTransaction.collectAsState()
    val replyTo       by viewModel.replyTo.collectAsState()
    var text          by remember { mutableStateOf("") }
    val listState     = rememberLazyListState()
    val currentUserId = UserSession.userId ?: ""
    val scope         = rememberCoroutineScope()

    // מצב חיפוש
    var searchActive by remember { mutableStateOf(false) }
    var searchQuery  by remember { mutableStateOf("") }

    val matchIndices = remember(messages, searchQuery) {
        if (searchQuery.isBlank()) emptyList()
        else messages.indices.filter { i ->
            val msg = messages[i]
            msg.text.contains(searchQuery, ignoreCase = true) ||
                    (msg.transaction?.businessName?.contains(searchQuery, ignoreCase = true) == true)
        }
    }
    var currentMatchIndex by remember(matchIndices) { mutableIntStateOf(0) }


    LaunchedEffect(chatId) { viewModel.loadMessages(chatId) }
    LaunchedEffect(initialPendingTxn) { initialPendingTxn?.let { viewModel.setPendingTransaction(it) } }
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty() && !searchActive) listState.animateScrollToItem(messages.size - 1)
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        if (searchActive) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Search messages...",
                                    fontSize = 13.sp, color = Color.White.copy(alpha = 0.7f)) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor   = Color.White.copy(alpha = 0.5f),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                    focusedTextColor     = Color.White,
                                    unfocusedTextColor   = Color.White,
                                    cursorColor          = Color.White
                                ),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Text(chatName, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            if (searchActive) { searchActive = false; searchQuery = "" }
                            else onBack()
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
                            IconButton(onClick = { searchActive = true }) {
                                Icon(Icons.Default.Search, null, tint = Color.White)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor             = CardifyColors.DarkGreen,
                        titleContentColor          = Color.White,
                        navigationIconContentColor = Color.White
                    )
                )
            }
        },
        bottomBar = {
            Column {
                if (pendingTxn != null) {
                    PendingTransactionPreview(
                        txn      = pendingTxn!!,
                        onRemove = { viewModel.clearPendingTransaction() }
                    )
                }
                if (replyTo != null) {
                    ReplyPreview(
                        message  = replyTo!!,
                        onRemove = { viewModel.clearReplyTo() }
                    )
                }

                // ─── סרגל הקלדה ──────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
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
                            focusedBorderColor   = CardifyColors.DarkGreen,
                            unfocusedBorderColor = Color(0xFFDDDDDD)
                        )
                    )
                    Spacer(Modifier.width(8.dp))

                    val canSend = text.isNotBlank() || pendingTxn != null
                    IconButton(
                        onClick = {
                            if (canSend) {
                                viewModel.sendMessage(chatId = chatId, text = text, transaction = pendingTxn)
                                text = ""
                            }
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                if (canSend) CardifyColors.DarkGreen
                                else Color(0xFFCCCCCC),
                                RoundedCornerShape(22.dp)
                            )
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
                CircularProgressIndicator(color = CardifyColors.DarkGreen)
            }
        } else {
            val showScrollButton by remember {
                derivedStateOf {
                    listState.canScrollForward &&
                            listState.firstVisibleItemIndex < messages.size - 3
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                LazyColumn(
                    state         = listState,
                    modifier      = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFF5F5F5))
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding      = PaddingValues(vertical = 12.dp)
                ) {
                    items(messages, key = { it.id }) { message ->
                        val msgIndex    = messages.indexOf(message)
                        val isHighlight = searchQuery.isNotBlank() &&
                                matchIndices.isNotEmpty() &&
                                matchIndices.getOrNull(currentMatchIndex) == msgIndex

                        // כותרת תאריך — מציגה רק כשהיום משתנה
                        val showDateHeader = msgIndex == 0 ||
                                extractDateLabel(message.timestamp) !=
                                extractDateLabel(messages[msgIndex - 1].timestamp)

                        if (showDateHeader && message.timestamp.isNotBlank()) {
                            DateHeader(label = extractDateLabel(message.timestamp))
                        }

                        MessageBubble(
                            message        = message,
                            isMe           = message.senderId == currentUserId,
                            showSenderName = isGroup,
                            isHighlighted  = isHighlight,
                            searchQuery    = searchQuery,
                            onSwipeReply   = { viewModel.setReplyTo(message) }
                        )
                    }
                }

                // ─── כפתור גלילה לסוף ─────────────────────────────────
                AnimatedVisibility(
                    visible  = showScrollButton,
                    enter    = fadeIn() + scaleIn(),
                    exit     = fadeOut() + scaleOut(),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 12.dp, bottom = 12.dp)
                ) {
                    SmallFloatingActionButton(
                        onClick        = {
                            scope.launch {
                                if (messages.isNotEmpty())
                                    listState.animateScrollToItem(messages.size - 1)
                            }
                        },
                        containerColor = CardifyColors.DarkGreen,
                        contentColor   = Color.White,
                        shape          = CircleShape,
                        modifier       = Modifier.size(38.dp)
                    ) {
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            contentDescription = "Scroll to latest",
                            modifier           = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

// ─── MessageBubble ───────────────────────────────────────────────────────────

@Composable
fun MessageBubble(
    message: ChatMessage,
    isMe: Boolean,
    showSenderName: Boolean,
    isHighlighted: Boolean = false,
    searchQuery: String = "",
    onSwipeReply: () -> Unit
) {
    val offsetX   = remember { Animatable(0f) }
    val THRESHOLD = 80f
    val scope     = rememberCoroutineScope()

    val highlightAlpha by animateFloatAsState(
        targetValue   = if (isHighlighted) 0.25f else 0f,
        animationSpec = tween(300),
        label         = "highlight"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardifyColors.DarkGreen.copy(alpha = highlightAlpha))
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
                        scope.launch {
                            offsetX.snapTo((offsetX.value + dragAmount).coerceIn(0f, 120f))
                        }
                    }
                )
            }
            .offset { IntOffset(offsetX.value.roundToInt(), 0) },
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        if (offsetX.value > 20f) {
            Icon(
                imageVector = Icons.Default.Reply,
                contentDescription = null,
                tint     = CardifyColors.DarkGreen.copy(
                    alpha = (offsetX.value / THRESHOLD).coerceIn(0f, 1f)
                ),
                modifier = Modifier.size(22.dp).align(Alignment.Start).padding(start = 4.dp)
            )
        }

        if (!isMe && showSenderName) {
            Text(
                message.senderName,
                fontSize   = 11.sp,
                color      = CardifyColors.DarkGreen,
                fontWeight = FontWeight.SemiBold,
                modifier   = Modifier.padding(start = 4.dp, bottom = 2.dp)
            )
        }

        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .background(
                    if (isMe) CardifyColors.DarkGreen else Color.White,
                    RoundedCornerShape(
                        topStart    = 16.dp, topEnd = 16.dp,
                        bottomStart = if (isMe) 16.dp else 4.dp,
                        bottomEnd   = if (isMe) 4.dp  else 16.dp
                    )
                )
                .padding(12.dp)
        ) {
            if (message.replyToMessage != null) {
                QuotedMessage(reply = message.replyToMessage, isMe = isMe)
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

            Text(
                formatChatTime(message.timestamp),
                fontSize = 10.sp,
                color    = if (isMe) Color.White.copy(alpha = 0.7f) else Color.Gray,
                modifier = Modifier.align(Alignment.End).padding(top = 4.dp)
            )
        }
    }
}

// ─── שאר ה-Composables (ללא שינוי) ──────────────────────────────────────────

@Composable
fun QuotedMessage(reply: ReplySnapshot, isMe: Boolean) {
    val bgColor   = if (isMe) Color.White.copy(alpha = 0.15f) else Color(0xFFF0F0F0)
    val textColor = if (isMe) Color.White else Color.Black
    val nameColor = if (isMe) Color.White.copy(alpha = 0.9f) else CardifyColors.DarkGreen
    val barColor  = if (isMe) Color.White.copy(alpha = 0.6f) else CardifyColors.DarkGreen

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor, RoundedCornerShape(8.dp))
            .padding(end = 8.dp),
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
fun ReplyPreview(message: ChatMessage, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardifyColors.DarkGreen.copy(alpha = 0.08f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.width(3.dp).height(36.dp)
            .background(CardifyColors.DarkGreen, RoundedCornerShape(2.dp)))
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(message.senderName, fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold, color = CardifyColors.DarkGreen)
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isMe) Color.White.copy(alpha = 0.15f) else Color(0xFFF0F0F0),
                RoundedCornerShape(10.dp)
            )
            .padding(10.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(txn.businessName, fontWeight = FontWeight.Bold,
                fontSize = 13.sp, color = textColor, modifier = Modifier.weight(1f))
            Text("\u20AA${"%.2f".format(txn.amount)}", fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = if (txn.status == "IRREGULAR") Color(0xFFE23125) else textColor)
        }
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(txn.category, fontSize = 11.sp,
                color = if (isMe) Color.White.copy(alpha = 0.8f) else Color.Gray)
            Text(txn.date, fontSize = 11.sp,
                color = if (isMe) Color.White.copy(alpha = 0.8f) else Color.Gray)
        }
        val statusLabel = if (txn.status == "IRREGULAR") "⚠️ Irregular" else "✓ Regular"
        Text(statusLabel, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardifyColors.DarkGreen.copy(alpha = 0.1f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text("📊 ${txn.businessName}", fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold, color = CardifyColors.DarkGreen)
            Text("\u20AA${"%.2f".format(txn.amount)} • ${txn.status}",
                fontSize = 11.sp, color = Color.Gray)
        }
        TextButton(onClick = onRemove) { Text("Remove", color = Color.Red, fontSize = 12.sp) }
    }
}

// ─── כותרת תאריך ─────────────────────────────────────────────────────────────

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
        val zoneId  = java.time.ZoneId.systemDefault()
        val date    = instant.atZone(zoneId).toLocalDate()
        val today   = java.time.LocalDate.now(zoneId)
        when (date) {
            today               -> "Today"
            today.minusDays(1)  -> "Yesterday"
            else                -> "%02d/%02d/%d".format(date.dayOfMonth, date.monthValue, date.year)
        }
    } catch (e: Exception) { "" }
}

@Composable
fun DateHeader(label: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text       = label,
            fontSize   = 11.sp,
            color      = Color.White,
            fontWeight = FontWeight.SemiBold,
            modifier   = Modifier
                .background(Color(0xFFAAAAAA), RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = 3.dp)
        )
    }
}