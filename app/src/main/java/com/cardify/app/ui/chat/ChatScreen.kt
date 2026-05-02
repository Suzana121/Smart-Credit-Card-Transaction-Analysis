package com.cardify.app.ui.chat

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
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
    val messages      by viewModel.messages.collectAsState()
    val isLoading     by viewModel.isLoading.collectAsState()
    val pendingTxn    by viewModel.pendingTransaction.collectAsState()
    val replyTo       by viewModel.replyTo.collectAsState()
    var text          by remember { mutableStateOf("") }
    val listState     = rememberLazyListState()
    val currentUserId = UserSession.userId ?: ""

    LaunchedEffect(chatId) { viewModel.loadMessages(chatId) }

    LaunchedEffect(initialPendingTxn) {
        initialPendingTxn?.let { viewModel.setPendingTransaction(it) }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(chatName, fontWeight = FontWeight.SemiBold, fontSize = 16.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CardifyColors.DarkGreen,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        bottomBar = {
            Column {
                // Preview עסקה pending
                if (pendingTxn != null) {
                    PendingTransactionPreview(
                        txn = pendingTxn!!,
                        onRemove = { viewModel.clearPendingTransaction() }
                    )
                }
                // Preview תגובה
                if (replyTo != null) {
                    ReplyPreview(
                        message  = replyTo!!,
                        onRemove = { viewModel.clearReplyTo() }
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Message...", fontSize = 14.sp) },
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CardifyColors.DarkGreen,
                            unfocusedBorderColor = Color(0xFFDDDDDD)
                        )
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (text.isNotBlank() || pendingTxn != null) {
                                viewModel.sendMessage(
                                    chatId      = chatId,
                                    text        = text,
                                    transaction = pendingTxn
                                )
                                text = ""
                            }
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .background(CardifyColors.DarkGreen, RoundedCornerShape(22.dp))
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
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF5F5F5))
                    .padding(padding)
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(messages, key = { it.id }) { message ->
                    MessageBubble(
                        message        = message,
                        isMe           = message.senderId == currentUserId,
                        showSenderName = isGroup,
                        onSwipeReply   = { viewModel.setReplyTo(message) }
                    )
                }
            }
        }
    }
}

@Composable
fun MessageBubble(
    message: ChatMessage,
    isMe: Boolean,
    showSenderName: Boolean,
    onSwipeReply: () -> Unit
) {
    val offsetX   = remember { Animatable(0f) }
    val THRESHOLD = 80f
    val scope     = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        scope.launch {
                            if (offsetX.value > THRESHOLD) {
                                onSwipeReply()
                            }
                            offsetX.animateTo(
                                targetValue = 0f,
                                animationSpec = spring(stiffness = Spring.StiffnessMedium)
                            )
                        }
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        scope.launch {
                            val newOffset = (offsetX.value + dragAmount).coerceIn(0f, 120f)
                            offsetX.snapTo(newOffset)
                        }
                    }
                )
            }
            .offset { IntOffset(offsetX.value.roundToInt(), 0) },
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        // אייקון reply שמופיע בזמן גרירה
        if (offsetX.value > 20f) {
            Icon(
                imageVector = Icons.Default.Reply,
                contentDescription = null,
                tint = CardifyColors.DarkGreen.copy(
                    alpha = (offsetX.value / THRESHOLD).coerceIn(0f, 1f)
                ),
                modifier = Modifier
                    .size(22.dp)
                    .align(Alignment.Start)
                    .padding(start = 4.dp)
            )
        }

        // שם שולח — רק בצ'אט קבוצתי ורק להודעות של אחרים
        if (!isMe && showSenderName) {
            Text(
                message.senderName,
                fontSize = 11.sp,
                color = CardifyColors.DarkGreen,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
            )
        }

        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .background(
                    if (isMe) CardifyColors.DarkGreen else Color.White,
                    RoundedCornerShape(
                        topStart = 16.dp, topEnd = 16.dp,
                        bottomStart = if (isMe) 16.dp else 4.dp,
                        bottomEnd   = if (isMe) 4.dp  else 16.dp
                    )
                )
                .padding(12.dp)
        ) {
            // ציטוט ההודעה המקורית — מוצג מעל כמו בווצאפ
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
                color = if (isMe) Color.White.copy(alpha = 0.7f) else Color.Gray,
                modifier = Modifier.align(Alignment.End).padding(top = 4.dp)
            )
        }
    }
}

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
        // קו ירוק בצד שמאל — בדיוק כמו בווצאפ
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(36.dp)
                .background(barColor, RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp))
        )
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.padding(vertical = 6.dp)) {
            Text(
                reply.senderName,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = nameColor
            )
            val preview = when {
                reply.businessName.isNotBlank() -> "📊 ${reply.businessName}"
                reply.text.isNotBlank()         -> reply.text.take(60)
                else                            -> ""
            }
            if (preview.isNotBlank()) {
                Text(
                    preview,
                    fontSize = 12.sp,
                    color = textColor.copy(alpha = 0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
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
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(36.dp)
                .background(CardifyColors.DarkGreen, RoundedCornerShape(2.dp))
        )
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(
                message.senderName,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = CardifyColors.DarkGreen
            )
            Text(
                if (message.transaction != null)
                    "📊 ${message.transaction.businessName}"
                else
                    message.text.take(60),
                fontSize = 12.sp,
                color = Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
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
        TextButton(onClick = onRemove) {
            Text("Remove", color = Color.Red, fontSize = 12.sp)
        }
    }
}