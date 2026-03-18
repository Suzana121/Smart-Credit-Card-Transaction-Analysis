package com.cardify.app.ui.shared_info

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cardify.app.data.model.ShareItem
import com.cardify.app.ui.components.AppScaffold
import com.cardify.app.ui.home.CardifyColors
import kotlin.math.roundToInt

private val TurquoiseChip = Color(0xFFE6F7F7)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedInfoScreen(
    onNavigate: (String) -> Unit,
    viewModel: SharedInfoViewModel = viewModel()
) {
    val filterState by viewModel.filterState.collectAsState()
    val filteredShares by viewModel.filteredShares.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var showFilterSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    AppScaffold(currentRoute = "wallet", onNavigate = onNavigate) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            // Title + Refresh
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Shared Info", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = CardifyColors.DarkGreen)
                    IconButton(onClick = { viewModel.fetchShares() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = CardifyColors.DarkGreen)
                    }
                }
            }

            // Direction toggle
            item {
                DirectionToggle(selected = filterState.direction, onSelect = { viewModel.setDirection(it) })
            }

            // Filter + Date pill buttons
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterPillButton(
                        label = "Filter",
                        icon = { Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        hasActive = filterState.hasActiveFilters,
                        onClick = { showFilterSheet = true }
                    )
                    FilterPillButton(
                        label = "Date",
                        icon = { Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        hasActive = filterState.dateFrom.isNotBlank() || filterState.dateTo.isNotBlank(),
                        onClick = { showFilterSheet = true }
                    )
                }
            }

            // Content
            when {
                isLoading -> item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = CardifyColors.DarkGreen)
                    }
                }
                errorMessage != null -> item {
                    Text(errorMessage ?: "", color = Color.Red, fontSize = 13.sp, modifier = Modifier.padding(8.dp))
                }
                filteredShares.isEmpty() -> item {
                    Text(
                        if (filterState.direction == "outgoing") "You haven't shared any transactions yet."
                        else "No transactions have been shared with you yet.",
                        color = Color.Gray, fontSize = 14.sp,
                        modifier = Modifier.padding(vertical = 24.dp)
                    )
                }
                else -> items(filteredShares) { share ->
                    ShareItemCard(share = share, direction = filterState.direction)
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    // Bottom sheet — wrapped in LTR so it isn't affected by system RTL locale
    if (showFilterSheet) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            sheetState = sheetState,
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            FilterSheetContent(
                initial = filterState,
                onApply = { pending ->
                    viewModel.applyFilters(pending)
                    showFilterSheet = false
                },
                onClearAll = {
                    viewModel.clearFilters()
                    showFilterSheet = false
                },
                onClose = { showFilterSheet = false }
            )
        }
        } // end CompositionLocalProvider
    }
}

// ─────────────────────────────────────────────────────────────
// Pill button shown in the main screen
// ─────────────────────────────────────────────────────────────
@Composable
private fun FilterPillButton(
    label: String,
    icon: @Composable () -> Unit,
    hasActive: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50.dp))
            .background(TurquoiseChip)
            .border(
                width = if (hasActive) 1.5.dp else 0.dp,
                color = if (hasActive) CardifyColors.DarkGreen else Color.Transparent,
                shape = RoundedCornerShape(50.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        icon()
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = CardifyColors.DarkGreen)
    }
}

// ─────────────────────────────────────────────────────────────
// Bottom sheet content — uses local pending state
// ─────────────────────────────────────────────────────────────
@Composable
private fun FilterSheetContent(
    initial: ShareFilterState,
    onApply: (ShareFilterState) -> Unit,
    onClearAll: () -> Unit,
    onClose: () -> Unit
) {
    // Local pending state — only committed when Apply is tapped
    var pending by remember { mutableStateOf(initial) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = CardifyColors.DarkGreen)
            }
            Text(
                "Filters",
                modifier = Modifier.weight(1f),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            TextButton(onClick = { pending = ShareFilterState(direction = pending.direction) }) {
                Text("Clear all", fontSize = 13.sp, color = CardifyColors.DarkGreen)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Categories ──
        SheetSectionLabel("Categories")
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CATEGORY_OPTIONS.forEach { cat ->
                val selected = pending.selectedCategories.contains(cat)
                SelectableChip(
                    label = cat,
                    selected = selected,
                    onClick = {
                        pending = pending.copy(
                            selectedCategories = if (selected)
                                pending.selectedCategories - cat
                            else
                                pending.selectedCategories + cat
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── Transaction Type ──
        SheetSectionLabel("Transaction Type")
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("REGULAR", "IRREGULAR").forEach { type ->
                val selected = pending.status == type
                SelectableChip(
                    label = type.lowercase().replaceFirstChar { it.uppercase() },
                    selected = selected,
                    onClick = {
                        pending = pending.copy(status = if (selected) "ALL" else type)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── Amount Range ──
        SheetSectionLabel("Transaction Amount")
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("₪${pending.amountRange.start.roundToInt()}", fontSize = 12.sp, color = Color.Gray)
            Text("₪${pending.amountRange.endInclusive.roundToInt()}", fontSize = 12.sp, color = Color.Gray)
        }
        RangeSlider(
            value = pending.amountRange,
            onValueChange = { pending = pending.copy(amountRange = it) },
            valueRange = 0f..AMOUNT_MAX,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = CardifyColors.DarkGreen,
                activeTrackColor = CardifyColors.DarkGreen,
                inactiveTrackColor = Color(0xFFCCCCCC)
            )
        )

        Spacer(modifier = Modifier.height(20.dp))

        // ── Date Range ──
        SheetSectionLabel("Date")
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DateField(
                value = pending.dateFrom,
                onValueChange = { pending = pending.copy(dateFrom = it) },
                placeholder = "From",
                modifier = Modifier.weight(1f)
            )
            Text("–", fontSize = 18.sp, color = Color.Gray)
            DateField(
                value = pending.dateTo,
                onValueChange = { pending = pending.copy(dateTo = it) },
                placeholder = "To",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // ── Apply button ──
        Button(
            onClick = { onApply(pending) },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = CardifyColors.DarkGreen)
        ) {
            Text("Apply Filters", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
private fun SheetSectionLabel(text: String) {
    Text(text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
}

@Composable
private fun SelectableChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg    = if (selected) Color(0xFFD4F5D4) else TurquoiseChip
    val border = if (selected) CardifyColors.DarkGreen else Color(0xFFCCEEEE)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(50.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = CardifyColors.DarkGreen)
    }
}

@Composable
private fun DateField(value: String, onValueChange: (String) -> Unit, placeholder: String, modifier: Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, fontSize = 12.sp, color = Color.Gray) },
        leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(18.dp), tint = CardifyColors.DarkGreen) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = CardifyColors.DarkGreen,
            unfocusedBorderColor = Color(0xFFCCCCCC),
            focusedLabelColor = CardifyColors.DarkGreen,
            cursorColor = CardifyColors.DarkGreen
        )
    )
}

// ─────────────────────────────────────────────────────────────
// Direction toggle
// ─────────────────────────────────────────────────────────────
@Composable
private fun DirectionToggle(selected: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF0F0F0), RoundedCornerShape(12.dp))
            .padding(4.dp)
    ) {
        DirectionTab("You → Friends", selected == "outgoing", Modifier.weight(1f)) { onSelect("outgoing") }
        DirectionTab("Friends → You", selected == "incoming", Modifier.weight(1f)) { onSelect("incoming") }
    }
}

@Composable
private fun DirectionTab(label: String, isSelected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .background(if (isSelected) CardifyColors.DarkGreen else Color.Transparent, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
            color = if (isSelected) Color.White else Color.Gray)
    }
}

// ─────────────────────────────────────────────────────────────
// Share item card
// ─────────────────────────────────────────────────────────────
@Composable
private fun ShareItemCard(share: ShareItem, direction: String) {
    val txn = share.transaction
    val isIrregular = txn?.status == "IRREGULAR"
    val friendLabel = if (direction == "outgoing") "Shared with: ${share.sharedWith}"
                      else "Shared by: ${share.sharedBy}"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(
            1.dp, if (isIrregular) CardifyColors.IrregularRed else Color(0xFFDDDDDD)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(friendLabel, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = CardifyColors.DarkGreen)
            if (txn != null) {
                HorizontalDivider(color = Color(0xFFEEEEEE))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(txn.businessName, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text("₪${txn.amount}", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(txn.date, fontSize = 12.sp, color = Color.Gray)
                    Text(
                        txn.status, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                        color = if (isIrregular) CardifyColors.IrregularRed else CardifyColors.RegularGreen
                    )
                }
                if (txn.category.isNotBlank()) {
                    Text(txn.category, fontSize = 11.sp, color = Color(0xFF888888))
                }
            } else {
                Text("Transaction details unavailable", fontSize = 12.sp, color = Color.Gray)
            }
            if (share.date.isNotBlank()) {
                Text("Shared on: ${share.date.take(10)}", fontSize = 11.sp, color = Color(0xFFAAAAAA))
            }
        }
    }
}
