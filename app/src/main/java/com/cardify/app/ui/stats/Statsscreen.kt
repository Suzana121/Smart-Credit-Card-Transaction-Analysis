package com.cardify.app.ui.stats

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cardify.app.ui.components.AppScaffold
import com.cardify.app.ui.components.TransactionItem
import com.cardify.app.ui.components.TransactionRow
import com.cardify.app.ui.components.TransactionRowVariant
import androidx.navigation.NavHostController


// ─── צבעים לקטגוריות ───
private val categoryColors = listOf(
    Color(0xFF90CAF9), Color(0xFFEF9A9A), Color(0xFFA5D6A7),
    Color(0xFFFFCC80), Color(0xFFCE93D8), Color(0xFFB0BEC5),
)

fun fullMonthName(month: String): String = when (month) {
    "Jan" -> "January";  "Feb" -> "February"; "Mar" -> "March"
    "Apr" -> "April";    "May" -> "May";       "Jun" -> "June"
    "Jul" -> "July";     "Aug" -> "August";    "Sep" -> "September"
    "Oct" -> "October";  "Nov" -> "November";  "Dec" -> "December"
    else  -> month
}

// ─── Section Container ───
@Composable
fun StatSection(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFDADBDD))
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp), content = content)
    }
}

// =============================================
// Main Screen
// =============================================

@Composable
fun StatsScreen(
    navController: NavHostController, // הוספת הפרמטר כאן
    onNavigate: (String) -> Unit,
    viewModel: StatsViewModel = viewModel()
) {
    val uiState    by viewModel.uiState.collectAsState()
    val monthIndex = viewModel.selectedMonthIndex
    val monthName  = viewModel.allMonths[monthIndex]


        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F5F5))
        ) {
            when (val state = uiState) {
                is StatsUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                is StatsUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .clickable { viewModel.loadData() },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Refresh, null, tint = Color.Gray)
                        Spacer(Modifier.height(8.dp))
                        Text(state.message, color = Color.Gray)
                        Text("Tap to retry", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                    }
                }
                is StatsUiState.Success -> {
                    StatsContent(
                        data       = state.data,
                        monthName  = monthName,
                        monthIndex = monthIndex,
                        viewModel  = viewModel
                    )
                }
            }
        }
    }


@Composable
fun StatsContent(
    data:       StatsData,
    monthName:  String,
    monthIndex: Int,
    viewModel:  StatsViewModel
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            TotalSpendSection(
                selectedMonth = monthName,
                data          = data,
                onPrevMonth   = { viewModel.changeMonth(monthIndex - 1) },
                onNextMonth   = { viewModel.changeMonth(monthIndex + 1) },
                hasPrev       = monthIndex > 0,
                hasNext       = monthIndex < viewModel.allMonths.size - 1
            )
        }
        item {
            DonutChartSection(
                selectedMonth = monthName,
                data          = data,
                onMonthSelected = { month ->
                    val idx = viewModel.allMonths.indexOf(month)
                    if (idx >= 0) viewModel.changeMonth(idx)
                }
            )
        }
        item {
            BarChartSection(
                selectedMonthName  = monthName,
                monthlyExpenses    = data.monthlyExpenses,
                selectedMonthTotal = data.totalSpend,
                onMonthSelected    = { month ->
                    val idx = viewModel.allMonths.indexOf(month)
                    if (idx >= 0) viewModel.changeMonth(idx)
                }
            )
        }
    }
}

// =============================================
// 1. Total Spend
// =============================================
@Composable
fun TotalSpendSection(
    selectedMonth: String,
    data: StatsData,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    hasPrev: Boolean,
    hasNext: Boolean
) {
    var showSuspicious by remember { mutableStateOf(false) }
    val suspiciousTxs  = data.transactions.filter { it.isIrregular }

    val animatedAmount by animateFloatAsState(
        targetValue    = data.totalSpend.toFloat(),
        animationSpec  = tween(800, easing = EaseOutCubic),
        label          = "totalSpend"
    )

    StatSection {
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPrevMonth, enabled = hasPrev) {
                    Icon(Icons.Default.KeyboardArrowLeft, null,
                        tint = if (hasPrev) MaterialTheme.colorScheme.primary else Color(0xFFDADBDD))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(fullMonthName(selectedMonth), color = MaterialTheme.colorScheme.primary,
                        fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("Total Spend", color = Color(0xFF878C90), fontSize = 13.sp)
                }
                IconButton(onClick = onNextMonth, enabled = hasNext) {
                    Icon(Icons.Default.KeyboardArrowRight, null,
                        tint = if (hasNext) MaterialTheme.colorScheme.primary else Color(0xFFDADBDD))
                }
            }
            Text(
                "₪${"%,.2f".format(animatedAmount)}",
                fontSize = if (LocalConfiguration.current.screenWidthDp > 600) 42.sp else 34.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = Color(0xFFE7E8E9))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(28.dp)
                        .border(1.dp, Color(0xFF4CAF50), RoundedCornerShape(8.dp))
                        .background(Color(0xFFE8F5E9), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) { Text("↑", color = Color(0xFF4CAF50), fontSize = 16.sp, fontWeight = FontWeight.Bold) }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("Regular", color = Color(0xFF878C90), fontSize = 9.sp)
                    Text(data.regularCount.toString(), fontSize = 20.sp,
                        fontWeight = FontWeight.Bold, color = Color.Black)
                }
            }

            VerticalDivider(modifier = Modifier.height(40.dp).padding(horizontal = 12.dp),
                color = Color(0xFFE7E8E9))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { showSuspicious = !showSuspicious }
            ) {
                Box(
                    modifier = Modifier.size(28.dp)
                        .border(1.dp, Color(0xFFF44336), RoundedCornerShape(8.dp))
                        .background(Color(0xFFFFEBEE), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) { Text("↓", color = Color.Red, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("Irregular", color = Color(0xFF878C90), fontSize = 9.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(data.irregularCount.toString(), fontSize = 20.sp,
                            fontWeight = FontWeight.Bold, color = Color.Red)
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            if (showSuspicious) Icons.Default.KeyboardArrowUp
                            else Icons.Default.KeyboardArrowDown,
                            null, tint = Color.Red, modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = showSuspicious,
            enter = expandVertically(tween(300)) + fadeIn(),
            exit  = shrinkVertically(tween(300)) + fadeOut()
        ) {
            SuspiciousTransactionList(suspiciousTxs)
        }
    }
}

@Composable
fun SuspiciousTransactionList(transactions: List<StatsTransaction>) {
    Column {
        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFE7E8E9))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Warning, null, tint = Color(0xFFF44336), modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Suspicious Transactions", fontSize = 14.sp,
                fontWeight = FontWeight.Bold, color = Color(0xFFF44336))
        }
        Spacer(Modifier.height(8.dp))
        if (transactions.isEmpty()) {
            Text("No suspicious transactions this month 🎉",
                fontSize = 13.sp, color = Color(0xFF878C90),
                modifier = Modifier.padding(vertical = 8.dp))
        } else {
            transactions.forEach { tx ->
                TransactionRow(
                    transaction = TransactionItem(
                        id = tx.id, title = tx.title, date = tx.date,
                        amount = tx.amount, isIrregular = true, category = tx.category
                    ),
                    variant = TransactionRowVariant.COMPACT
                )
            }
        }
    }
}

// =============================================
// 2. Donut Chart
// =============================================
@Composable
fun DonutChartSection(
    selectedMonth: String,
    data: StatsData,
    onMonthSelected: (String) -> Unit
) {
    val screenWidth = LocalConfiguration.current.screenWidthDp
    var selectedIndex     by remember { mutableStateOf(-1) }
    var lastSelectedIndex by remember { mutableStateOf(0) }
    var showMonthDropdown by remember { mutableStateOf(false) }

    LaunchedEffect(selectedMonth) { selectedIndex = -1 }

    // המרת קטגוריות מה-ViewModel לפורמט התצוגה
    val categories = remember(data.categories) {
        data.categories.mapIndexed { i, cat ->
            Triple(cat.name, cat.amount, categoryColors[i % categoryColors.size])
        }
    }
    val total = categories.sumOf { it.second }

    if (selectedIndex >= 0 && selectedIndex < categories.size) lastSelectedIndex = selectedIndex

    val sweepAngles = if (total > 0) categories.map { (it.second / total * 360f).toFloat() }
    else categories.map { 0f }

    val animatedSweepAngles = sweepAngles.mapIndexed { i, target ->
        animateFloatAsState(target, tween(600, easing = EaseInOutCubic), label = "sweep_$i").value
    }
    val animatedStartAngles = animatedSweepAngles.runningFold(-90f) { acc, s -> acc + s }.dropLast(1)

    val centerAmount = if (selectedIndex >= 0 && selectedIndex < categories.size)
        "₪${"%.0f".format(categories[selectedIndex].second)}"
    else "₪${"%.0f".format(total)}"
    val centerLabel = if (selectedIndex >= 0 && selectedIndex < categories.size)
        categories[selectedIndex].first else selectedMonth

    val availableMonths = data.monthlyExpenses.map { it.month }

    StatSection {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Expenses", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            Spacer(Modifier.weight(1f))
            Box {
                Surface(
                    shape = RoundedCornerShape(20.dp), color = Color(0xFFF5F5F5),
                    modifier = Modifier.clickable { showMonthDropdown = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(selectedMonth, color = Color(0xFF878C90), fontSize = 10.sp)
                        Icon(Icons.Default.KeyboardArrowDown, null,
                            tint = Color(0xFF878C90), modifier = Modifier.size(14.dp))
                    }
                }
                DropdownMenu(expanded = showMonthDropdown, onDismissRequest = { showMonthDropdown = false }) {
                    availableMonths.forEach { month ->
                        DropdownMenuItem(
                            text = { Text(month, fontSize = 13.sp) },
                            onClick = { onMonthSelected(month); showMonthDropdown = false }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        if (total == 0.0) {
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                contentAlignment = Alignment.Center) {
                Text("No data for $selectedMonth", color = Color(0xFF878C90), fontSize = 14.sp)
            }
        } else {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center) {
                Box(contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.size(if (screenWidth > 600) 220.dp else 160.dp)) {
                        val strokeWidth = 36f
                        val inset = strokeWidth / 2
                        val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                        val topLeft = Offset(inset, inset)
                        val gap = 3f
                        categories.forEachIndexed { i, (_, _, color) ->
                            val sweep = if (i < animatedSweepAngles.size) animatedSweepAngles[i] else 0f
                            val start = if (i < animatedStartAngles.size) animatedStartAngles[i] else -90f
                            val isSelected = i == selectedIndex
                            if (sweep > 0f) {
                                drawArc(
                                    color = if (isSelected) color.copy(alpha = 1f) else color.copy(alpha = 0.85f),
                                    startAngle = start + gap / 2f,
                                    sweepAngle = sweep - gap,
                                    useCenter = false,
                                    style = Stroke(width = if (isSelected) strokeWidth * 1.2f else strokeWidth),
                                    size = arcSize, topLeft = topLeft
                                )
                            }
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(centerAmount,
                            fontSize = if (screenWidth > 600) 24.sp else 18.sp,
                            fontWeight = FontWeight.Bold, color = Color.Black)
                        Text(centerLabel, fontSize = 11.sp, color = Color(0xFF878C90))
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            val half = categories.size / 2 + categories.size % 2
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    categories.take(half).forEachIndexed { i, (name, amount, color) ->
                        LegendItem(name, amount, total, color, i == selectedIndex) {
                            selectedIndex = if (selectedIndex == i) -1 else i
                        }
                    }
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    categories.drop(half).forEachIndexed { i, (name, amount, color) ->
                        val realIndex = i + half
                        LegendItem(name, amount, total, color, realIndex == selectedIndex) {
                            selectedIndex = if (selectedIndex == realIndex) -1 else realIndex
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = selectedIndex >= 0,
                enter = expandVertically(tween(300)) + fadeIn(),
                exit  = shrinkVertically(tween(300)) + fadeOut()
            ) {
                if (lastSelectedIndex < categories.size) {
                    val catName = categories[lastSelectedIndex].first
                    CategoryTransactionList(
                        categoryName = catName,
                        transactions = data.transactions.filter { it.category == catName }
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryTransactionList(categoryName: String, transactions: List<StatsTransaction>) {
    Column {
        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = Color(0xFFE7E8E9))
        Text("$categoryName Transactions", fontSize = 14.sp,
            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 10.dp))
        transactions.forEach { tx ->
            TransactionRow(
                transaction = TransactionItem(
                    id = tx.id, title = tx.title, date = tx.date,
                    amount = tx.amount, isIrregular = tx.isIrregular,
                    category = tx.category
                ),
                variant = TransactionRowVariant.COMPACT
            )
        }
    }
}

@Composable
fun LegendItem(
    name: String, amount: Double, total: Double, color: Color,
    isSelected: Boolean, onClick: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onClick() }) {
        Box(modifier = Modifier.width(4.dp).height(32.dp).background(color, RoundedCornerShape(2.dp)))
        Spacer(Modifier.width(8.dp))
        Column {
            Text(name, fontSize = 13.sp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Black,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
            Text("${if (total > 0) (amount / total * 100).toInt() else 0}%",
                fontSize = 11.sp, color = Color(0xFF878C90))
        }
    }
}

// =============================================
// 3. Bar Chart
// =============================================
@Composable
fun BarChartSection(
    selectedMonthName: String,
    monthlyExpenses: List<MonthlyExpense>,
    selectedMonthTotal: Double,
    onMonthSelected: (String) -> Unit
) {
    val screenWidth = LocalConfiguration.current.screenWidthDp
    val maxAmount = monthlyExpenses.maxOfOrNull { it.amount } ?: 1.0

    var barsVisible by remember { mutableStateOf(false) }
    LaunchedEffect(monthlyExpenses) {
        kotlinx.coroutines.delay(100)
        barsVisible = true
    }

    StatSection {
        Text("Monthly Overview", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        Spacer(Modifier.height(20.dp))

        if (monthlyExpenses.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                contentAlignment = Alignment.Center) {
                Text("No monthly data available", color = Color(0xFF878C90), fontSize = 14.sp)
            }
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(monthlyExpenses) { monthData ->
                    val isSelected = monthData.month == selectedMonthName
                    val animatedHeight by animateFloatAsState(
                        targetValue = if (barsVisible) (monthData.amount / maxAmount).toFloat() else 0f,
                        animationSpec = tween(800, easing = EaseOutCubic),
                        label = "bar_${monthData.month}"
                    )
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(if (screenWidth > 600) 64.dp else 44.dp)
                            .clickable { onMonthSelected(monthData.month) }
                    ) {
                        Box(modifier = Modifier.height(20.dp), contentAlignment = Alignment.BottomCenter) {
                            // מציג סכום רק אם נבחר וגם יש נתונים בחודש הנוכחי
                            if (isSelected && selectedMonthTotal > 0) {
                                Text("₪${"%,.0f".format(selectedMonthTotal)}",
                                    fontSize = 9.sp, fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(120.dp),
                            contentAlignment = Alignment.BottomCenter) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(animatedHeight)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFFDADBDD))
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(monthData.month, fontSize = 12.sp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF878C90),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            textAlign = TextAlign.Center)
                    }
                }
            }

            // Insights — מוצג רק אם יש נתונים לחודש הנבחר (לפי totalSpend מהשרת)
            AnimatedVisibility(
                visible = selectedMonthTotal > 0,
                enter   = expandVertically(tween(300)) + fadeIn(),
                exit    = shrinkVertically(tween(300)) + fadeOut()
            ) {
                Column {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp),
                        color = Color(0xFFE7E8E9))
                    Text("$selectedMonthName Overview", fontSize = 14.sp,
                        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 12.dp))
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly) {
                        InsightItem("Total Spend", "₪${"%,.0f".format(selectedMonthTotal)}")
                    }
                }
            }
        }
    }
}

@Composable
fun InsightItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 9.sp, color = Color(0xFF878C90), textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold,
            color = Color.Black, textAlign = TextAlign.Center)
    }
}