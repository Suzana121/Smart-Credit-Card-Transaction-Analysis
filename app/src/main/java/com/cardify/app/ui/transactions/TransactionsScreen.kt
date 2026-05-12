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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cardify.app.data.model.ActiveFilters
import com.cardify.app.data.model.Friend
import com.cardify.app.data.model.Transaction
import com.cardify.app.data.model.UploadedFile
import com.cardify.app.ui.components.TransactionRow
import com.cardify.app.ui.components.TransactionRowVariant
import com.cardify.app.ui.components.toTransactionItem

private val DarkGreen    = Color(0xFF006769)
private val IrregularRed = Color(0xFFE23125)

fun categoryMatchesGroup(category: String, group: String): Boolean {
    return when (group) {
        "Food"      -> category.contains("Food", ignoreCase = true) ||
                category.contains("Restaurant", ignoreCase = true) ||
                category.contains("Grocery", ignoreCase = true) ||
                category.contains("Cafe", ignoreCase = true) ||
                category.contains("מזון", ignoreCase = true) ||
                category.contains("מסעדות", ignoreCase = true)
        "Shopping"  -> category.contains("Shopping", ignoreCase = true) ||
                category.contains("Fashion", ignoreCase = true) ||
                category.contains("Electronics", ignoreCase = true) ||
                category.contains("Sports", ignoreCase = true) ||
                category.contains("אופנה", ignoreCase = true) ||
                category.contains("קניות", ignoreCase = true)
        "Transport" -> category.contains("Transport", ignoreCase = true) ||
                category.contains("Automotive", ignoreCase = true) ||
                category.contains("Travel", ignoreCase = true) ||
                category.contains("תחבורה", ignoreCase = true)
        "Finance"   -> category.contains("Finance", ignoreCase = true) ||
                category.contains("Insurance", ignoreCase = true) ||
                category.contains("Bank", ignoreCase = true) ||
                category.contains("ביטוח", ignoreCase = true) ||
                category.contains("בנק", ignoreCase = true)
        "Health"    -> category.contains("Health", ignoreCase = true) ||
                category.contains("בריאות", ignoreCase = true)
        "Education" -> category.contains("Education", ignoreCase = true) ||
                category.contains("חינוך", ignoreCase = true)
        "Other"     -> category.contains("Other", ignoreCase = true) ||
                category.contains("General", ignoreCase = true) ||
                category.contains("Government", ignoreCase = true) ||
                category.contains("Telecom", ignoreCase = true) ||
                category.contains("Entertainment", ignoreCase = true) ||
                category.contains("שונות", ignoreCase = true) ||
                category.contains("כללי", ignoreCase = true)
        else        -> category.equals(group, ignoreCase = true)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    navController: NavHostController,
    onNavigate: (String) -> Unit = {},
    onShareToChat: ((Transaction) -> Unit)? = null,
    viewModel: TransactionsViewModel = viewModel()
) {
    val context          = LocalContext.current
    val transactions     by viewModel.filteredTransactions.collectAsState()
    val isLoading        by viewModel.isLoading.collectAsState()
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
    val filterOptions    by viewModel.filterOptions.collectAsState()
    val activeFilters    by viewModel.activeFilters.collectAsState()
    val isLoadingFilters by viewModel.isLoadingFilters.collectAsState()

    var showHistory     by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var bannerDismissed by remember { mutableStateOf(false) }
    var actionsVisible  by remember { mutableStateOf(true) }

    LaunchedEffect(manualOverrides) {
        if (manualOverrides.isNotEmpty()) bannerDismissed = false
    }

    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    if (showHistory) {
        HistoryBottomSheet(
            uploads        = uploads,
            selectedFileId = selectedFileId,
            onSelectFile   = { fileId -> viewModel.selectFile(fileId); showHistory = false },
            onDismiss      = { showHistory = false }
        )
    }

    if (showFilterSheet) {
        FilterBottomSheet(
            filterOptions = filterOptions,
            activeFilters = activeFilters,
            isLoading     = isLoadingFilters,
            onApply       = { filters -> viewModel.applyFilters(filters); showFilterSheet = false },
            onClearAll    = { viewModel.clearFilters(); showFilterSheet = false },
            onDismiss     = { showFilterSheet = false }
        )
    }

    LaunchedEffect(listState) {
        snapshotFlow {
            val layoutInfo  = listState.layoutInfo
            val total       = layoutInfo.totalItemsCount
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            total > 0 && lastVisible >= total - 3
        }.collect { nearEnd ->
            if (nearEnd && hasMore && !isLoadingMore && !isLoading) viewModel.loadMore()
        }
    }

    LaunchedEffect(pdfUri) {
        pdfUri?.let { uri -> viewModel.sharePdf(context, uri); viewModel.clearPdfUri() }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let { Toast.makeText(context, it, Toast.LENGTH_LONG).show(); viewModel.clearError() }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F5F5))) {

        // ── Search bar + חץ הסתרה ──
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value         = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder   = { Text("Search Transaction", fontSize = 13.sp, color = Color.Gray) },
                leadingIcon   = { Icon(Icons.Default.Search, null, tint = Color.Gray) },
                trailingIcon  = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Close, null, tint = Color.Gray)
                        }
                    }
                },
                modifier  = Modifier.weight(1f),
                shape     = RoundedCornerShape(12.dp),
                colors    = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor      = DarkGreen,
                    unfocusedBorderColor    = Color(0xFFDDDDDD),
                    focusedContainerColor   = Color.White,
                    unfocusedContainerColor = Color.White
                ),
                singleLine = true
            )
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick  = { actionsVisible = !actionsVisible },
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFFDDDDDD), RoundedCornerShape(12.dp))
                    .background(Color.White)
            ) {
                Icon(
                    if (actionsVisible) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint     = DarkGreen,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // ── Filter + Date chips ──
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            val hasActiveFilters = !activeFilters.isEmpty
            FilterChip(
                selected    = hasActiveFilters,
                onClick     = { showFilterSheet = true },
                label       = { Text("Filter", fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.FilterList, null, modifier = Modifier.size(14.dp)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor   = DarkGreen,
                    selectedLabelColor       = Color.White,
                    selectedLeadingIconColor = Color.White
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled             = true,
                    selected            = hasActiveFilters,
                    borderColor         = Color(0xFFCCCCCC),
                    selectedBorderColor = DarkGreen
                )
            )
            val hasDateFilter = activeFilters.dateFrom != null || activeFilters.dateTo != null
            FilterChip(
                selected    = hasDateFilter,
                onClick     = { showFilterSheet = true },
                label = {
                    Text(
                        if (hasDateFilter) "${activeFilters.dateFrom ?: ""} – ${activeFilters.dateTo ?: ""}"
                        else "Date",
                        fontSize = 12.sp
                    )
                },
                leadingIcon = { Icon(Icons.Default.DateRange, null, modifier = Modifier.size(14.dp)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor   = DarkGreen,
                    selectedLabelColor       = Color.White,
                    selectedLeadingIconColor = Color.White
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled             = true,
                    selected            = hasDateFilter,
                    borderColor         = Color(0xFFCCCCCC),
                    selectedBorderColor = DarkGreen
                )
            )
        }

        // ── PDF + History (ניתן להסתרה) ──
        AnimatedVisibility(
            visible = actionsVisible,
            enter   = expandVertically() + fadeIn(),
            exit    = shrinkVertically() + fadeOut()
        ) {
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick  = { viewModel.downloadPdf(context) },
                    modifier = Modifier.weight(1f).height(40.dp),
                    shape    = RoundedCornerShape(10.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = DarkGreen),
                    enabled  = !isDownloadingPdf
                ) {
                    if (isDownloadingPdf) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(6.dp))
                        Text("Generating...", color = Color.White, fontSize = 11.sp)
                    } else {
                        Icon(Icons.Default.PictureAsPdf, null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Share Report", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                OutlinedButton(
                    onClick  = { showHistory = true },
                    modifier = Modifier.weight(1f).height(40.dp),
                    shape    = RoundedCornerShape(10.dp),
                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = DarkGreen),
                    border   = androidx.compose.foundation.BorderStroke(1.dp, DarkGreen)
                ) {
                    Icon(Icons.Default.History, null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("History", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // ── Profile update banner ──
        if (manualOverrides.isNotEmpty() && !bannerDismissed) {
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                UpdateProfileBanner(
                    count     = manualOverrides.size,
                    onUpdate  = {
                        viewModel.updateProfile {
                            Toast.makeText(context, "Profile updated!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onDismiss = { bannerDismissed = true }
                )
            }
        } else if (manualOverrides.isNotEmpty() && bannerDismissed) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(DarkGreen.copy(alpha = 0.1f))
                    .clickable { bannerDismissed = false }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.AutoFixHigh, null, tint = DarkGreen, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text("${manualOverrides.size} pending update", fontSize = 12.sp, color = DarkGreen, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                Icon(Icons.Default.KeyboardArrowRight, null, tint = DarkGreen, modifier = Modifier.size(14.dp))
            }
        }

        // ── Count ──
        Text(
            "${transactions.size} transactions",
            fontSize = 12.sp, color = Color.Gray,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        // ── List ──
        when {
            isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = DarkGreen)
                }
            }
            transactions.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.SearchOff, null, tint = Color.LightGray, modifier = Modifier.size(52.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("No transactions found", color = Color.Gray)
                    }
                }
            }
            else -> {
                LazyColumn(
                    state               = listState,
                    modifier            = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding      = PaddingValues(bottom = 16.dp)
                ) {
                    items(transactions, key = { it.id }) { transaction ->
                        TransactionCard(
                            transaction    = transaction,
                            friends        = friends,
                            isSending      = isSendingShare,
                            onToggleStatus = { id, newStatus -> viewModel.updateTransactionStatus(id, newStatus) },
                            onSendShare    = { friendPhone, txnId ->
                                viewModel.sendShare(friendPhone, txnId) {
                                    Toast.makeText(context, "Shared!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onShareToChat = onShareToChat
                        )
                    }
                    if (!hasMore && transactions.isNotEmpty()) {
                        item {
                            Text(
                                "All transactions loaded",
                                modifier  = Modifier.fillMaxWidth().padding(16.dp),
                                textAlign = TextAlign.Center,
                                fontSize  = 12.sp, color = Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }
}


// ─────────────────────────────────────────────
// Transaction Card — משתמש ב-TransactionRow המשותף
// ─────────────────────────────────────────────
@Composable
fun TransactionCard(
    transaction:    Transaction,
    friends:        List<Friend>,
    isSending:      Boolean,
    onToggleStatus: (String, String) -> Unit,
    onSendShare:    (String, String) -> Unit,
    onShareToChat:  ((Transaction) -> Unit)? = null
) {
    var showConfirmDialog by remember { mutableStateOf(false) }
    var showShareSheet    by remember { mutableStateOf(false) }
    var isIrregular       by remember(transaction.id) { mutableStateOf(transaction.status == "IRREGULAR") }

    LaunchedEffect(transaction.status) { isIrregular = transaction.status == "IRREGULAR" }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Are you sure?", fontWeight = FontWeight.Bold) },
            text  = {
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
                        onToggleStatus(transaction.id, if (isIrregular) "IRREGULAR" else "REGULAR")
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isIrregular) Color(0xFF2E7D32) else IrregularRed),
                    shape = RoundedCornerShape(10.dp)
                ) { Text("Yes", color = Color.White, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                OutlinedButton(onClick = { showConfirmDialog = false },
                    shape = RoundedCornerShape(10.dp)) { Text("Cancel") }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (showShareSheet) {
        ShareBottomSheet(
            friends   = friends,
            isSending = isSending,
            onSend    = { friend -> onSendShare(friend.phone, transaction.id); showShareSheet = false },
            onDismiss = { showShareSheet = false }
        )
    }

    // category display map
    val catDisplay = mapOf(
        "מזון וצריכה" to "Food & Grocery", "מסעדות, קפה וברים" to "Restaurants & Cafes",
        "מסעדות" to "Restaurants", "אופנה" to "Fashion", "בריאות" to "Health",
        "תחבורה" to "Transport", "חינוך" to "Education",
        "שירותי תקשורת" to "Telecommunications", "עירייה וממשלה" to "Government",
        "שונות" to "Other", "כללי" to "General", "בידור" to "Entertainment",
        "קניות" to "Shopping"
    ).getOrDefault(transaction.category, transaction.category)

    TransactionRow(
        transaction    = transaction.toTransactionItem(),
        variant        = TransactionRowVariant.FULL,
        onShareClick   = {
            if (onShareToChat != null) onShareToChat(transaction)
            else showShareSheet = true
        },
        onStatusChange = { showConfirmDialog = true },
        extraExpandedContent = {
            // ── שורת קטגוריה ──
            Row {
                Text("Category: ", fontSize = 12.sp, color = Color.Gray)
                Text(catDisplay, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
            // ── הסבר irregular ──
            if (isIrregular) {
                val explanationText = when {
                    !transaction.explanation.isNullOrBlank() &&
                            transaction.explanation != "Unusual transaction pattern detected" ->
                        transaction.explanation
                    else -> "Transaction pattern deviates from your usual spending behavior"
                }
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(IrregularRed.copy(alpha = 0.08f))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Warning, null,
                        tint = IrregularRed, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(explanationText, color = IrregularRed,
                        fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    )
}


// ─────────────────────────────────────────────
// Filter Bottom Sheet
// ─────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBottomSheet(
    filterOptions: com.cardify.app.data.model.FilterOptions,
    activeFilters: ActiveFilters,
    isLoading:     Boolean,
    onApply:       (ActiveFilters) -> Unit,
    onClearAll:    () -> Unit,
    onDismiss:     () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedCats by remember(activeFilters) { mutableStateOf(activeFilters.selectedCategories) }
    var txnType      by remember(activeFilters) { mutableStateOf(activeFilters.transactionType) }
    var sliderMin    by remember(filterOptions, activeFilters) {
        mutableStateOf(activeFilters.minAmount?.toFloat() ?: filterOptions.minAmount.toFloat())
    }
    var sliderMax    by remember(filterOptions, activeFilters) {
        mutableStateOf(activeFilters.maxAmount?.toFloat() ?: filterOptions.maxAmount.toFloat())
    }
    var dateFrom     by remember(activeFilters) { mutableStateOf(activeFilters.dateFrom ?: "") }
    var dateTo       by remember(activeFilters) { mutableStateOf(activeFilters.dateTo ?: "") }

    val absMin = filterOptions.minAmount.toFloat()
    val absMax = filterOptions.maxAmount.toFloat().coerceAtLeast(absMin + 1f)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = Color.White,
        shape            = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
                .imePadding()
        ) {
            Row(
                modifier          = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, null, tint = Color.Gray)
                }
                Spacer(Modifier.width(8.dp))
                Text("Filters", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f))
                TextButton(onClick = onClearAll) {
                    Text("Clear all", color = DarkGreen, fontSize = 13.sp)
                }
            }

            Text("Categories", fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement   = Arrangement.spacedBy(6.dp),
                modifier              = Modifier.fillMaxWidth()
            ) {
                filterOptions.categories.forEach { cat ->
                    val isSelected = cat in selectedCats
                    FilterChip(
                        selected    = isSelected,
                        onClick     = {
                            selectedCats = if (isSelected) selectedCats - cat else selectedCats + cat
                        },
                        label       = { Text(cat, fontSize = 12.sp) },
                        leadingIcon = if (isSelected) {{
                            Icon(Icons.Default.Check, null, modifier = Modifier.size(12.dp))
                        }} else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor   = DarkGreen,
                            selectedLabelColor       = Color.White,
                            selectedLeadingIconColor = Color.White
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled             = true,
                            selected            = isSelected,
                            borderColor         = Color(0xFFCCCCCC),
                            selectedBorderColor = DarkGreen
                        )
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            HorizontalDivider(color = Color(0xFFEEEEEE))
            Spacer(Modifier.height(16.dp))

            Text("Transaction Type", fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(null to "All", "REGULAR" to "Regular", "IRREGULAR" to "Irregular")
                    .forEach { (value, label) ->
                        val isSelected = txnType == value
                        FilterChip(
                            selected = isSelected,
                            onClick  = { txnType = value },
                            label    = { Text(label, fontSize = 12.sp) },
                            colors   = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = DarkGreen,
                                selectedLabelColor     = Color.White
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled             = true,
                                selected            = isSelected,
                                borderColor         = Color(0xFFCCCCCC),
                                selectedBorderColor = DarkGreen
                            )
                        )
                    }
            }

            Spacer(Modifier.height(20.dp))
            HorizontalDivider(color = Color(0xFFEEEEEE))
            Spacer(Modifier.height(16.dp))

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text("Transaction Amount", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text("₪${sliderMin.toInt()} – ₪${sliderMax.toInt()}",
                    fontSize = 12.sp, color = DarkGreen, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(8.dp))
            RangeSlider(
                value         = sliderMin..sliderMax,
                onValueChange = { range -> sliderMin = range.start; sliderMax = range.endInclusive },
                valueRange    = absMin..absMax,
                modifier      = Modifier.fillMaxWidth(),
                colors        = SliderDefaults.colors(thumbColor = DarkGreen, activeTrackColor = DarkGreen)
            )

            Spacer(Modifier.height(20.dp))
            HorizontalDivider(color = Color(0xFFEEEEEE))
            Spacer(Modifier.height(16.dp))

            Text("Date", fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 8.dp))
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value         = dateFrom,
                    onValueChange = { dateFrom = it },
                    placeholder   = { Text("DD/MM/YY", fontSize = 11.sp, color = Color.Gray) },
                    modifier      = Modifier.weight(1f),
                    shape         = RoundedCornerShape(10.dp),
                    singleLine    = true,
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = DarkGreen,
                        unfocusedBorderColor = Color(0xFFDDDDDD)
                    )
                )
                Text("–", color = Color.Gray, fontSize = 16.sp)
                OutlinedTextField(
                    value         = dateTo,
                    onValueChange = { dateTo = it },
                    placeholder   = { Text("DD/MM/YY", fontSize = 11.sp, color = Color.Gray) },
                    modifier      = Modifier.weight(1f),
                    shape         = RoundedCornerShape(10.dp),
                    singleLine    = true,
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = DarkGreen,
                        unfocusedBorderColor = Color(0xFFDDDDDD)
                    )
                )
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    onApply(
                        ActiveFilters(
                            selectedCategories = selectedCats,
                            transactionType    = txnType,
                            minAmount          = if (sliderMin > absMin) sliderMin.toDouble() else null,
                            maxAmount          = if (sliderMax < absMax) sliderMax.toDouble() else null,
                            dateFrom           = dateFrom.ifBlank { null },
                            dateTo             = dateTo.ifBlank { null }
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = DarkGreen)
            ) {
                Text("Apply Filters", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}


// ─────────────────────────────────────────────
// History Bottom Sheet
// ─────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryBottomSheet(
    uploads:        List<UploadedFile>,
    selectedFileId: String?,
    onSelectFile:   (String?) -> Unit,
    onDismiss:      () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = Color.White,
        shape            = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp)
        ) {
            Text("Upload History", fontWeight = FontWeight.Bold, fontSize = 18.sp,
                modifier = Modifier.padding(bottom = 16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selectedFileId == null) DarkGreen.copy(alpha = 0.1f) else Color(0xFFF5F5F5))
                    .border(1.dp, if (selectedFileId == null) DarkGreen else Color.Transparent, RoundedCornerShape(10.dp))
                    .clickable { onSelectFile(null) }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.FolderOpen, null,
                    tint = if (selectedFileId == null) DarkGreen else Color.Gray,
                    modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text("All Files (Latest)", fontWeight = FontWeight.SemiBold,
                    color = if (selectedFileId == null) DarkGreen else Color.Black)
            }

            Spacer(Modifier.height(8.dp))

            uploads.forEach { file ->
                val isSelected = file.id == selectedFileId
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) DarkGreen.copy(alpha = 0.1f) else Color(0xFFF5F5F5))
                        .border(1.dp, if (isSelected) DarkGreen else Color.Transparent, RoundedCornerShape(10.dp))
                        .clickable { onSelectFile(file.id) }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.InsertDriveFile, null,
                        tint = if (isSelected) DarkGreen else Color.Gray,
                        modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(file.fileName, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                            color = if (isSelected) DarkGreen else Color.Black)
                        Text("${file.transactionCount} transactions • ${file.irregularCount} irregular",
                            fontSize = 11.sp, color = Color.Gray)
                    }
                    if (isSelected) {
                        Icon(Icons.Default.CheckCircle, null,
                            tint = DarkGreen, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(Modifier.height(8.dp))
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
    val context    = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = Color.White,
        shape            = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp)
        ) {
            Text("Share with a Friend", fontWeight = FontWeight.Bold, fontSize = 18.sp,
                modifier = Modifier.padding(bottom = 16.dp))

            if (friends.isEmpty()) {
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.People, null, tint = Color.LightGray, modifier = Modifier.size(52.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("You haven't added any friends yet", color = Color.Gray, textAlign = TextAlign.Center)
                    Text("Go to Account to add friends", color = Color.LightGray,
                        fontSize = 12.sp, textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp))
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
                            modifier = Modifier.size(40.dp).clip(CircleShape)
                                .background(DarkGreen.copy(alpha = 0.15f)),
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
                                Text(friend.name.take(1).uppercase(),
                                    color = DarkGreen, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(friend.name, modifier = Modifier.weight(1f),
                            fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
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
fun UpdateProfileBanner(
    count:     Int,
    onUpdate:  () -> Unit,
    onDismiss: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkGreen.copy(alpha = 0.1f))
            .border(1.dp, DarkGreen, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.AutoFixHigh, null, tint = DarkGreen, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Profile Update Available", fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp, color = DarkGreen)
            Text("You corrected $count transaction(s). Update your profile to improve future detection.",
                fontSize = 11.sp, color = Color.Gray)
        }
        Spacer(Modifier.width(8.dp))
        Button(
            onClick        = onUpdate,
            colors         = ButtonDefaults.buttonColors(containerColor = DarkGreen),
            shape          = RoundedCornerShape(10.dp),
            modifier       = Modifier.height(34.dp),
            contentPadding = PaddingValues(horizontal = 12.dp)
        ) { Text("Update", fontSize = 12.sp) }
        Spacer(Modifier.width(4.dp))
        IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.Close, null, tint = DarkGreen, modifier = Modifier.size(16.dp))
        }
    }
}