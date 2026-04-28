package com.cardify.app.ui.transactions

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.clickable
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
import com.cardify.app.ui.components.TransactionRow
import com.cardify.app.ui.components.TransactionRowVariant
import com.cardify.app.data.model.UploadedFile
import com.cardify.app.ui.components.toTransactionItem

// --- Cardify Colors ---
private val DarkGreen    = Color(0xFF006769)
private val IrregularRed = Color(0xFFE23125)
private val RegularGreen = Color(0xFF38D325)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    onNavigate: (String) -> Unit = {},
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
    val uploads          by viewModel.uploads.collectAsState()
    val selectedFileId   by viewModel.selectedFileId.collectAsState()
    val isLoadingUploads by viewModel.isLoadingUploads.collectAsState()

    // Share sheet state — lifted here so TransactionRow's onShareClick can open it
    var shareTarget      by remember { mutableStateOf<Transaction?>(null) }
    var filtersVisible   by remember { mutableStateOf(true) }
    var showFileHistory  by remember { mutableStateOf(false) }

    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    // Infinite scroll
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

    // Auto-share PDF after download
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

    // File History bottom sheet
    if (showFileHistory) {
        FileHistoryBottomSheet(
            uploads        = uploads,
            selectedFileId = selectedFileId,
            isLoading      = isLoadingUploads,
            onSelect       = { fileId ->
                viewModel.selectFile(fileId)
                showFileHistory = false
            },
            onDismiss = { showFileHistory = false }
        )
    }

    // Share bottom sheet
    shareTarget?.let { transaction ->
        ShareBottomSheet(
            friends   = friends,
            isSending = isSendingShare,
            onSend    = { friend ->
                viewModel.sendShare(friend.phone, transaction.id) {
                    Toast.makeText(context, "Shared!", Toast.LENGTH_SHORT).show()
                }
                shareTarget = null
            },
            onDismiss = { shareTarget = null }
        )
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
            // --- Search Bar + Filter Toggle ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = {
                        Text("Search by merchant name...", fontSize = 13.sp, color = Color.Gray)
                    },
                    leadingIcon  = { Icon(Icons.Default.Search, null, tint = Color.Gray) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Close, null, tint = Color.Gray)
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor      = DarkGreen,
                        unfocusedBorderColor    = Color(0xFFDDDDDD),
                        focusedContainerColor   = Color.White,
                        unfocusedContainerColor = Color.White
                    ),
                    singleLine = true
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = { filtersVisible = !filtersVisible },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (filtersVisible) DarkGreen.copy(alpha = 0.1f)
                            else Color(0xFFF0F0F0)
                        )
                ) {
                    Icon(
                        Icons.Default.FilterList,
                        contentDescription = if (filtersVisible) "Hide filters" else "Show filters",
                        tint = if (filtersVisible) DarkGreen else Color.Gray
                    )
                }
            }

            // --- Collapsible Section: Filters + Count + Buttons ---
            androidx.compose.animation.AnimatedVisibility(
                visible = filtersVisible,
                enter   = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(),
                exit    = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut()
            ) {
                Column {
                    // Filter Chips
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

                    // מספר עסקאות + אינדיקטור קובץ נבחר
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${transactions.size} transactions",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            modifier = Modifier.weight(1f)
                        )
                        if (selectedFileId != null) {
                            val selectedFile = uploads.find { it.id == selectedFileId }
                            if (selectedFile != null) {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(DarkGreen.copy(alpha = 0.1f))
                                        .clickable { viewModel.selectFile(null) }
                                        .padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        selectedFile.fileName.substringBeforeLast(".").take(12),
                                        fontSize = 11.sp,
                                        color = DarkGreen,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Icon(Icons.Default.Close, null,
                                        tint = DarkGreen, modifier = Modifier.size(12.dp))
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    // כפתורי Share Report + History
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick  = { viewModel.downloadPdf(context) },
                                modifier = Modifier.weight(1f).height(46.dp),
                                shape    = RoundedCornerShape(12.dp),
                                colors   = ButtonDefaults.buttonColors(containerColor = DarkGreen),
                                enabled  = !isDownloadingPdf
                            ) {
                                if (isDownloadingPdf) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("Generating...", color = Color.White, fontSize = 12.sp)
                                } else {
                                    Icon(Icons.Default.PictureAsPdf, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Share Report", color = Color.White,
                                        fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                            // כפתור היסטוריית קבצים
                            OutlinedButton(
                                onClick  = { showFileHistory = true },
                                modifier = Modifier.height(46.dp),
                                shape    = RoundedCornerShape(12.dp),
                                border   = androidx.compose.foundation.BorderStroke(1.dp, DarkGreen),
                                colors   = ButtonDefaults.outlinedButtonColors(contentColor = DarkGreen)
                            ) {
                                Icon(Icons.Default.History, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("History", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        if (manualOverrides.isNotEmpty()) {
                            UpdateProfileBanner(
                                count = manualOverrides.size,
                                onUpdate = {
                                    viewModel.updateProfile {
                                        Toast.makeText(
                                            context,
                                            "Profile updated successfully!",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            )
                        }
                    }  // end Column (buttons)
                }  // end Column (collapsible)
            }  // end AnimatedVisibility

            // --- Transaction List ---
            when {
                isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = DarkGreen)
                    }
                }
                transactions.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.SearchOff,
                                contentDescription = null,
                                tint = Color.LightGray,
                                modifier = Modifier.size(52.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text("No transactions found", color = Color.Gray)
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        state   = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        // ─── Using TransactionRow component ───
                        items(transactions, key = { it.id }) { transaction ->
                            val currentStatus = manualOverrides[transaction.id] ?: transaction.status
                            val txItem = transaction
                                .copy(status = currentStatus)
                                .toTransactionItem()

                            TransactionRowWithConfirm(
                                transaction = txItem,
                                onShareClick = { shareTarget = transaction },
                                onStatusConfirmed = { markIrregular ->
                                    val newStatus = if (markIrregular) "IRREGULAR" else "REGULAR"
                                    viewModel.updateTransactionStatus(transaction.id, newStatus)
                                }
                            )
                        }

                        // Loading more indicator
                        if (isLoadingMore) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
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
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    textAlign = TextAlign.Center,
                                    fontSize  = 12.sp,
                                    color     = Color.Gray
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
// File History Bottom Sheet
// ─────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileHistoryBottomSheet(
    uploads:        List<UploadedFile>,
    selectedFileId: String?,
    isLoading:      Boolean,
    onSelect:       (String?) -> Unit,
    onDismiss:      () -> Unit
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Uploaded Files",
                    fontWeight = FontWeight.Bold,
                    fontSize   = 18.sp,
                    modifier   = Modifier.weight(1f)
                )
                // כפתור "הכל" לניקוי הסינון
                if (selectedFileId != null) {
                    TextButton(onClick = { onSelect(null) }) {
                        Text("Show All", color = DarkGreen, fontSize = 13.sp)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            if (isLoading) {
                Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = DarkGreen)
                }
            } else if (uploads.isEmpty()) {
                Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.UploadFile, null,
                            tint = Color.LightGray, modifier = Modifier.size(40.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("No uploaded files yet", color = Color.Gray, fontSize = 14.sp)
                    }
                }
            } else {
                uploads.forEach { upload ->
                    val isSelected = upload.id == selectedFileId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) DarkGreen.copy(alpha = 0.08f)
                                else Color(0xFFF5F5F5)
                            )
                            .border(
                                width = if (isSelected) 1.5.dp else 0.dp,
                                color = if (isSelected) DarkGreen else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { onSelect(upload.id) }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // אייקון קובץ
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(DarkGreen.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.InsertDriveFile, null,
                                tint = DarkGreen, modifier = Modifier.size(22.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                upload.fileName.substringBeforeLast("."),
                                fontWeight = FontWeight.SemiBold,
                                fontSize   = 14.sp,
                                maxLines   = 1
                            )
                            Text(
                                "${upload.transactionCount} transactions · ${upload.uploadedAt.take(10)}",
                                fontSize = 12.sp,
                                color    = Color.Gray
                            )
                            if (upload.irregularCount > 0) {
                                Text(
                                    "${upload.irregularCount} irregular",
                                    fontSize = 11.sp,
                                    color    = IrregularRed,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        if (isSelected) {
                            Icon(Icons.Default.CheckCircle, null,
                                tint = DarkGreen, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// TransactionRow + Confirm Dialog wrapper
// ─────────────────────────────────────────────
@Composable
fun TransactionRowWithConfirm(
    transaction: com.cardify.app.ui.components.TransactionItem,
    onShareClick: () -> Unit,
    onStatusConfirmed: (Boolean) -> Unit
) {
    var showConfirm      by remember { mutableStateOf(false) }
    var pendingIrregular by remember { mutableStateOf(false) }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("Are you sure?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    if (transaction.isIrregular) "Mark this transaction as Regular?"
                    else "Mark this transaction as Suspicious (Irregular)?",
                    color = Color.Gray
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirm = false
                        onStatusConfirmed(pendingIrregular)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (transaction.isIrregular) Color(0xFF2E7D32) else IrregularRed
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) { Text("Yes", color = Color.White, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showConfirm = false },
                    shape = RoundedCornerShape(10.dp)
                ) { Text("Cancel") }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    TransactionRow(
        transaction = transaction,
        variant     = TransactionRowVariant.FULL,
        onShareClick = onShareClick,
        onStatusChange = { markIrregular ->
            pendingIrregular = markIrregular
            showConfirm = true
        }
    )
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
                    Icon(
                        Icons.Default.People,
                        contentDescription = null,
                        tint = Color.LightGray,
                        modifier = Modifier.size(52.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "You haven't added any friends yet",
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "Go to Account to add friends",
                        color     = Color.LightGray,
                        fontSize  = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier  = Modifier.padding(top = 4.dp)
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
        Icon(
            Icons.Default.AutoFixHigh,
            contentDescription = null,
            tint = DarkGreen,
            modifier = Modifier.size(20.dp)
        )
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