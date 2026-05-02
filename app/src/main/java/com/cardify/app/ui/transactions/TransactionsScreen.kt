package com.cardify.app.ui.transactions

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cardify.app.data.model.Friend
import com.cardify.app.data.model.Transaction
import com.cardify.app.ui.components.AppScaffold

// --- Cardify Colors ---
private val DarkGreen    = Color(0xFF006769)
private val IrregularRed = Color(0xFFE23125)
private val RegularGreen = Color(0xFF38D325)
private val LightBg      = Color(0xFFF5F5F5)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    onNavigate: (String) -> Unit = {},
    onShareToChat: ((com.cardify.app.data.model.Transaction) -> Unit)? = null,
    viewModel: TransactionsViewModel = viewModel()
) {
    val context          = LocalContext.current
    val transactions     by viewModel.filteredTransactions.collectAsState()
    val isLoading        by viewModel.isLoading.collectAsState()
    val activeFilter     by viewModel.activeFilter.collectAsState()
    val searchQuery      by viewModel.searchQuery.collectAsState()
    val friends          by viewModel.friends.collectAsState()
    val isSendingShare   by viewModel.isSendingShare.collectAsState()
    val isDownloadingPdf by viewModel.isDownloadingPdf.collectAsState()
    val pdfUri           by viewModel.pdfUri.collectAsState()
    val errorMessage     by viewModel.errorMessage.collectAsState()
    val manualOverrides  by viewModel.manualOverrides.collectAsState()
    val isLoadingMore    by viewModel.isLoadingMore.collectAsState()
    val hasMore          by viewModel.hasMore.collectAsState()

    // LazyColumn state לזיהוי הגעה לסוף
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    // Infinite Scroll - טעינה כשמגיעים לסוף
    LaunchedEffect(listState) {
        snapshotFlow {
            val layoutInfo  = listState.layoutInfo
            val total       = layoutInfo.totalItemsCount
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            total > 0 && lastVisible >= total - 3
        }.collect { nearEnd ->
            if (nearEnd && hasMore && !isLoadingMore && !isLoading) {
                viewModel.loadMore()
            }
        }
    }

    // PDF שיתוף אוטומטי אחרי הורדה
    LaunchedEffect(pdfUri) {
        pdfUri?.let { uri ->
            viewModel.sharePdf(context, uri)
            viewModel.clearPdfUri()
        }
    }

    // Error toast
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    AppScaffold(
        currentRoute = "transactions",
        onNavigate   = onNavigate
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // --- Search Bar ---
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Search by merchant name...", fontSize = 13.sp, color = Color.Gray) },
                leadingIcon  = { Icon(Icons.Default.Search, null, tint = Color.Gray) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Close, null, tint = Color.Gray)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = DarkGreen,
                    unfocusedBorderColor = Color(0xFFDDDDDD),
                    focusedContainerColor   = Color.White,
                    unfocusedContainerColor = Color.White
                ),
                singleLine = true
            )

            // --- Filter Chips ---
            LazyRow(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(TransactionFilter.entries) { filter ->
                    val isSelected = filter == activeFilter
                    FilterChip(
                        selected = isSelected,
                        onClick  = { viewModel.setFilter(filter) },
                        label = {
                            Text(
                                when (filter) {
                                    TransactionFilter.ALL       -> "All"
                                    TransactionFilter.REGULAR   -> "Regular"
                                    TransactionFilter.IRREGULAR -> "Irregular"
                                },
                                fontSize = 12.sp
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = DarkGreen,
                            selectedLabelColor     = Color.White
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled             = true,
                            selected            = isSelected,
                            borderColor         = Color(0xFFDDDDDD),
                            selectedBorderColor = DarkGreen
                        )
                    )
                }
            }

            Spacer(Modifier.height(4.dp))
            Text(
                "${transactions.size} transactions",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(Modifier.height(4.dp))

            // --- PDF + Profile Banner ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.downloadPdf(context) },
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DarkGreen),
                    enabled = !isDownloadingPdf
                ) {
                    if (isDownloadingPdf) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Generating...", color = Color.White)
                    } else {
                        Icon(Icons.Default.PictureAsPdf, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Download & Share Report", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
                if (manualOverrides.isNotEmpty()) {
                    UpdateProfileBanner(
                        count = manualOverrides.size,
                        onUpdate = {
                            viewModel.updateProfile {
                                Toast.makeText(context,
                                    "Profile updated successfully!",
                                    Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }

            // --- List ---
            when {
                isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = DarkGreen)
                    }
                }
                transactions.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.SearchOff, null,
                                tint = Color.LightGray, modifier = Modifier.size(52.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("No transactions found", color = Color.Gray)
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        state   = listState,
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(transactions, key = { it.id }) { transaction ->
                            TransactionCard(
                                transaction   = transaction,
                                friends       = friends,
                                isSending     = isSendingShare,
                                onToggleStatus = { id, newStatus ->
                                    viewModel.updateTransactionStatus(id, newStatus)
                                },
                                onSendShare   = { friendPhone, txnId ->
                                    viewModel.sendShare(friendPhone, txnId) {
                                        Toast.makeText(context, "Shared!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onShareToChat = onShareToChat
                            )
                        }
                        // Loading More Indicator
                        if (isLoadingMore) {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        color = DarkGreen,
                                        modifier = Modifier.size(28.dp),
                                        strokeWidth = 2.5.dp
                                    )
                                }
                            }
                        }
                        // End of list indicator
                        if (!hasMore && transactions.isNotEmpty()) {
                            item {
                                Text(
                                    "All transactions loaded",
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    textAlign = TextAlign.Center,
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// Transaction Card
// ─────────────────────────────────────────────
@Composable
fun TransactionCard(
    transaction:   Transaction,
    friends:       List<Friend>,
    isSending:     Boolean,
    onToggleStatus: (String, String) -> Unit,
    onSendShare:   (String, String) -> Unit,
    onShareToChat: ((Transaction) -> Unit)? = null
) {
    var isExpanded          by remember { mutableStateOf(false) }
    var showConfirmDialog   by remember { mutableStateOf(false) }
    var showShareSheet      by remember { mutableStateOf(false) }
    var isIrregular         by remember(transaction.id) { mutableStateOf(transaction.status == "IRREGULAR") }

    // Sync with external changes
    LaunchedEffect(transaction.status) {
        isIrregular = transaction.status == "IRREGULAR"
    }

    // --- Confirm Dialog ---
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Are you sure?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    if (isIrregular) "Mark this transaction as Regular?"
                    else "Mark this transaction as Suspicious (Irregular)?",
                    color = Color.Gray
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        isIrregular = !isIrregular
                        val newStatus = if (isIrregular) "IRREGULAR" else "REGULAR"
                        onToggleStatus(transaction.id, newStatus)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isIrregular) Color(0xFF2E7D32) else IrregularRed
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) { Text("Yes", color = Color.White, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showConfirmDialog = false },
                    shape = RoundedCornerShape(10.dp)
                ) { Text("Cancel") }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    // --- Share Bottom Sheet ---
    if (showShareSheet) {
        ShareBottomSheet(
            friends   = friends,
            isSending = isSending,
            onSend    = { friend ->
                onSendShare(friend.phone, transaction.id)
                showShareSheet = false
            },
            onDismiss = { showShareSheet = false }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded },
        shape  = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            if (isIrregular) IrregularRed else Color(0xFFEEEEEE)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // --- Main Row ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status indicator dot
                Icon(
                    imageVector = when {
                        isIrregular -> Icons.Default.Warning
                        transaction.category.contains("Food", ignoreCase = true) ||
                                transaction.category.contains("Restaurant", ignoreCase = true) ||
                                transaction.category.contains("Grocery", ignoreCase = true) -> Icons.Default.Restaurant
                        transaction.category.contains("Transport", ignoreCase = true) -> Icons.Default.DirectionsCar
                        transaction.category.contains("Health", ignoreCase = true) -> Icons.Default.LocalHospital
                        transaction.category.contains("Education", ignoreCase = true) -> Icons.Default.School
                        transaction.category.contains("Entertainment", ignoreCase = true) -> Icons.Default.Movie
                        transaction.category.contains("Travel", ignoreCase = true) -> Icons.Default.Flight
                        transaction.category.contains("Finance", ignoreCase = true) ||
                                transaction.category.contains("Bank", ignoreCase = true) -> Icons.Default.AccountBalance
                        transaction.category.contains("Shopping", ignoreCase = true) ||
                                transaction.category.contains("Fashion", ignoreCase = true) -> Icons.Default.ShoppingBag
                        transaction.category.contains("Telecom", ignoreCase = true) -> Icons.Default.PhoneAndroid
                        else -> Icons.Default.Receipt
                    },
                    contentDescription = null,
                    tint = if (isIrregular) IrregularRed else DarkGreen,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        transaction.businessName,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "₪${transaction.amount}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        if (isIrregular) "Irregular" else "Regular",
                        color    = if (isIrregular) IrregularRed else RegularGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(Modifier.width(6.dp))
                Icon(
                    if (isExpanded) Icons.Default.KeyboardArrowUp
                    else Icons.Default.KeyboardArrowDown,
                    null,
                    tint = Color.Gray,
                    modifier = Modifier.size(18.dp)
                )
            }

            // --- Expanded Section ---
            AnimatedVisibility(
                visible = isExpanded,
                enter   = expandVertically() + fadeIn(),
                exit    = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFAFAFA))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    HorizontalDivider(color = Color(0xFFEEEEEE))
                    Spacer(Modifier.height(8.dp))

                    Row {
                        Text("Date: ", fontSize = 12.sp, color = Color.Gray)
                        Text(transaction.date, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(4.dp))

                    // Category with translation
                    val catDisplay = mapOf(
                        "מזון וצריכה" to "Food & Grocery", "מסעדות, קפה וברים" to "Restaurants & Cafes",
                        "מסעדות" to "Restaurants", "אופנה" to "Fashion", "בריאות" to "Health",
                        "תחבורה" to "Transport", "חינוך" to "Education",
                        "שירותי תקשורת" to "Telecommunications", "עירייה וממשלה" to "Government",
                        "שונות" to "Other", "כללי" to "General", "בידור" to "Entertainment",
                        "קניות" to "Shopping"
                    ).getOrDefault(transaction.category, transaction.category)
                    Row {
                        Text("Category: ", fontSize = 12.sp, color = Color.Gray)
                        Text(catDisplay, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    // XAI Explanation
                    val explanationText = when {
                        !transaction.explanation.isNullOrBlank() &&
                                transaction.explanation != "Unusual transaction pattern detected" ->
                            transaction.explanation
                        isIrregular -> "Transaction pattern deviates from your usual spending behavior"
                        else -> ""
                    }
                    if (isIrregular) {
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(IrregularRed.copy(alpha = 0.08f))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Warning, null,
                                tint = IrregularRed,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                explanationText,
                                color    = IrregularRed,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Share button - פותח צ'אט אם זמין, אחרת bottom sheet
                        OutlinedButton(
                            onClick = {
                                if (onShareToChat != null) {
                                    onShareToChat(transaction)
                                } else {
                                    showShareSheet = true
                                }
                            },
                            shape   = RoundedCornerShape(10.dp),
                            colors  = ButtonDefaults.outlinedButtonColors(
                                contentColor = DarkGreen
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, DarkGreen),
                            modifier = Modifier.weight(1f).height(38.dp)
                        ) {
                            Icon(Icons.Default.Share, null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Share to Chat", fontSize = 11.sp)
                        }

                        // Toggle status button
                        Button(
                            onClick = { showConfirmDialog = true },
                            shape   = RoundedCornerShape(10.dp),
                            colors  = ButtonDefaults.buttonColors(
                                containerColor = if (isIrregular) Color(0xFF2E7D32) else IrregularRed
                            ),
                            modifier = Modifier.weight(1f).height(38.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp)
                        ) {
                            Icon(
                                if (isIrregular) Icons.Default.CheckCircle else Icons.Default.Warning,
                                null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(3.dp))
                            Text(
                                if (isIrregular) "Mark Regular" else "Mark Irregular",
                                fontSize = 10.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// Share Bottom Sheet
// ─────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareBottomSheet(
    friends:   List<Friend>,
    isSending: Boolean,
    onSend:    (Friend) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = Color.White,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                "Share with a Friend",
                fontWeight = FontWeight.Bold,
                fontSize   = 18.sp,
                modifier   = Modifier.padding(bottom = 16.dp)
            )

            if (friends.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.People, null,
                        tint = Color.LightGray, modifier = Modifier.size(52.dp))
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "You haven't added any friends yet",
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "Go to Account to add friends",
                        color    = Color.LightGray,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            } else {
                friends.forEach { friend ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF5F5F5))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(DarkGreen.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                friend.name.take(1).uppercase(),
                                color      = DarkGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize   = 16.sp
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(
                            friend.name,
                            modifier   = Modifier.weight(1f),
                            fontWeight = FontWeight.SemiBold,
                            fontSize   = 14.sp
                        )
                        Button(
                            onClick  = { onSend(friend) },
                            enabled  = !isSending,
                            colors   = ButtonDefaults.buttonColors(containerColor = DarkGreen),
                            shape    = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(36.dp)
                        ) { Text("Send", fontSize = 12.sp) }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// Update Profile Banner
// ─────────────────────────────────────────────
@Composable
fun UpdateProfileBanner(count: Int, onUpdate: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkGreen.copy(alpha = 0.1f))
            .border(1.dp, DarkGreen, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.AutoFixHigh, null,
            tint = DarkGreen, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Profile Update Available",
                fontWeight = FontWeight.SemiBold,
                fontSize   = 13.sp,
                color      = DarkGreen
            )
            Text(
                "You corrected $count transaction(s). Update your profile to improve future detection.",
                fontSize = 11.sp,
                color    = Color.Gray
            )
        }
        Spacer(Modifier.width(8.dp))
        Button(
            onClick  = onUpdate,
            colors   = ButtonDefaults.buttonColors(containerColor = DarkGreen),
            shape    = RoundedCornerShape(10.dp),
            modifier = Modifier.height(34.dp),
            contentPadding = PaddingValues(horizontal = 12.dp)
        ) { Text("Update", fontSize = 12.sp) }
    }
}