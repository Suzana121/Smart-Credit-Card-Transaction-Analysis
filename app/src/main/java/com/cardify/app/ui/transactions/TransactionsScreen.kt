package com.cardify.app.ui.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cardify.app.R
import com.cardify.app.data.model.Transaction
import com.cardify.app.ui.components.AppScaffold
import com.cardify.app.ui.components.TransactionRow
import com.cardify.app.ui.components.TransactionRowVariant
import com.cardify.app.ui.components.TransactionItem as TransactionRowItem
import com.cardify.app.ui.home.CardifyColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    onNavigate: (String) -> Unit = {},
    viewModel: TransactionsViewModel = viewModel()
) {
    val transactions by viewModel.filteredTransactions.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isPaginationLoading by viewModel.isPaginationLoading.collectAsState() // חדש
    val activeFilter by viewModel.activeFilter.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val friends by viewModel.friends.collectAsState()
    val isSendingShare by viewModel.isSendingShare.collectAsState()

    val ibmPlexSans = FontFamily(
        Font(R.font.ibm_plex_sans_regular, FontWeight.Normal),
        Font(R.font.ibm_plex_sans_semibold, FontWeight.SemiBold)
    )

    // --- ניהול מצב הגלילה לצורך Pagination ---
    val listState = rememberLazyListState()

    // זיהוי הגעה לסוף הרשימה כדי לטעון עוד נתונים
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastVisibleIndex ->
                // אם הגענו לאחד ה-3 האחרונים ברשימה, תטען עוד
                if (lastVisibleIndex != null && lastVisibleIndex >= transactions.size - 3 && !isLoading && !isPaginationLoading) {
                    viewModel.fetchTransactions(isFirstLoad = false)
                }
            }
    }

    AppScaffold(currentRoute = "transactions", onNavigate = onNavigate) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(padding)
        ) {
            // --- Search Bar ---
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = {
                    Text(
                        "Search Transaction לפי שם בית העסק",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                },
                leadingIcon = {
                    Icon(Icons.Default.Search, null, tint = Color.Gray)
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Close, null, tint = Color.Gray)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CardifyColors.DarkGreen,
                    unfocusedBorderColor = Color(0xFFDDDDDD)
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
                        onClick = { viewModel.setFilter(filter) },
                        label = {
                            Text(
                                when (filter) {
                                    TransactionFilter.ALL -> "All"
                                    TransactionFilter.REGULAR -> "Regular"
                                    TransactionFilter.IRREGULAR -> "Irregular"
                                },
                                fontSize = 12.sp,
                                fontFamily = ibmPlexSans
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CardifyColors.DarkGreen,
                            selectedLabelColor = Color.White
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = Color(0xFFDDDDDD),
                            selectedBorderColor = CardifyColors.DarkGreen
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // --- Count ---
            Text(
                "${transactions.size} transactions loaded",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.padding(horizontal = 16.dp),
                fontFamily = ibmPlexSans
            )

            Spacer(modifier = Modifier.height(4.dp))

            // --- List ---
            when {
                isLoading && transactions.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = CardifyColors.DarkGreen)
                    }
                }
                transactions.isEmpty() && !isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Search, null,
                                tint = Color.LightGray,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "No transactions found",
                                color = Color.Gray,
                                fontFamily = ibmPlexSans
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        state = listState, // שידוך ה-state לגלילה
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(transactions, key = { it.id }) { transaction ->
                            TransactionRow(
                                transaction = transaction.toRowItem(),
                                variant = TransactionRowVariant.FULL,
                                friends = friends,
                                isSendingShare = isSendingShare,
                                onSendShare = { friend ->
                                    viewModel.sendShare(friend.phone, transaction.id) {}
                                },
                                onStatusChange = { isNowIrregular ->
                                    val newStatus = if (isNowIrregular) "IRREGULAR" else "REGULAR"
                                    viewModel.updateTransactionStatus(transaction.id, newStatus)
                                }
                            )
                        }

                        // אינדיקטור טעינה בתחתית הרשימה בזמן Pagination
                        if (isPaginationLoading) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = CardifyColors.DarkGreen,
                                        strokeWidth = 2.dp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun Transaction.toRowItem() = TransactionRowItem(
    id = this.id,
    title = this.businessName,
    date = this.date,
    amount = this.amount,
    isIrregular = this.status == "IRREGULAR",
    category = this.category
)