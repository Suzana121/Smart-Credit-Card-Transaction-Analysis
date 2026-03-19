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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Warning
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
import com.cardify.app.ui.account.teal
import com.cardify.app.ui.components.AppScaffold
import com.cardify.app.ui.components.TransactionItem
import com.cardify.app.ui.components.TransactionRow
import com.cardify.app.ui.components.TransactionRowVariant

// =============================================
// Dummy Data
// =============================================

data class StatsTransaction(
    val id: String,
    val title: String,
    val date: String,
    val amount: Double,
    val category: String = "Other"
)

val allMonths = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

// עסקאות לפי חודש
val transactionsByMonth = mapOf(
    "Aug" to listOf(
        StatsTransaction("1", "Greg's Coffee", "10/08/2025", 20.0,   "Food"),
        StatsTransaction("2", "Pharmacy",       "12/08/2025", 120.0,  "Health"),
    ),
    "Sep" to listOf(
        StatsTransaction("3", "Temu",           "22/09/2025", 2000.0, "Shopping"),
        StatsTransaction("4", "Book Depository","15/09/2025", 200.0,  "Education"),
        StatsTransaction("5", "Fuel Station",   "08/09/2025", 280.0,  "Transport"),
    ),
    "Oct" to listOf(
        StatsTransaction("6", "Coffix",         "10/10/2025", 5.0,    "Food"),
        StatsTransaction("7", "Zara",           "15/10/2025", 450.0,  "Shopping"),
    ),
    "Nov" to listOf(
        StatsTransaction("8",  "Shufersal",     "05/11/2025", 350.0,  "Food"),
        StatsTransaction("9",  "Cstyle",        "20/11/2025", 599.0,  "Shopping"),
        StatsTransaction("10", "Netflix",       "01/11/2025", 45.0,   "Other"),
        StatsTransaction("11", "Gym",           "05/11/2025", 180.0,  "Health"),
        StatsTransaction("12", "Rav Kav",       "01/11/2025", 50.0,   "Transport"),
    ),
    "Dec" to listOf(
        StatsTransaction("13", "Amazon",        "10/12/2025", 1500.0, "Shopping"),
        StatsTransaction("14", "SuperPharm",    "10/12/2025", 95.0,   "Health"),
        StatsTransaction("15", "University Fee","01/12/2025", 3000.0, "Education"),
        StatsTransaction("16", "Electricity Bill","20/12/2025",320.0, "Other"),
    ),
    "Jan" to listOf(
        StatsTransaction("17", "Greg's Coffee", "10/01/2026", 20.0,   "Food"),
        StatsTransaction("18", "Pharmacy",      "12/01/2026", 120.0,  "Health"),
        StatsTransaction("19", "Zara",          "15/01/2026", 450.0,  "Shopping"),
        StatsTransaction("20", "Fuel Station",  "08/01/2026", 280.0,  "Transport"),
        StatsTransaction("21", "Netflix",       "01/01/2026", 45.0,   "Other"),
    ),
)

val availableMonths = transactionsByMonth.keys.toList()
    .sortedBy { allMonths.indexOf(it) }

fun fullMonthName(month: String): String = when (month) {
    "Jan" -> "January"
    "Feb" -> "February"
    "Mar" -> "March"
    "Apr" -> "April"
    "May" -> "May"
    "Jun" -> "June"
    "Jul" -> "July"
    "Aug" -> "August"
    "Sep" -> "September"
    "Oct" -> "October"
    "Nov" -> "November"
    "Dec" -> "December"
    else  -> month
}

fun getCategories(transactions: List<StatsTransaction>): List<CategoryData> {
    val pastelColors = listOf(
        Color(0xFF90CAF9),
        Color(0xFFEF9A9A),
        Color(0xFFA5D6A7),
        Color(0xFFFFCC80),
        Color(0xFFCE93D8),
        Color(0xFFB0BEC5),
    )
    val categoryNames = listOf("Food", "Shopping", "Health", "Education", "Transport", "Other")
    return categoryNames.mapIndexed { i, name ->
        CategoryData(name, transactions.filter { it.category == name }.sumOf { it.amount }, pastelColors[i])
    }.filter { it.amount > 0 }
}

val suspiciousTransactions = transactionsByMonth.values.flatten().filter { it.amount > 1000 }

data class CategoryData(val name: String, val amount: Double, val color: Color)

data class MonthData(val month: String, val amount: Double, val transactions: List<StatsTransaction>)

val dummyMonths = availableMonths.map { month ->
    val txs = transactionsByMonth[month] ?: emptyList()
    MonthData(month, txs.sumOf { it.amount }, txs)
}

// =============================================
// Section Container
// =============================================

@Composable
fun StatSection(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFDADBDD))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            content = content
        )
    }
}

// =============================================
// Main Screen — state משותף לחודש
// =============================================

@Composable
fun StatsScreen(onNavigate: (String) -> Unit) {
    // state משותף — כשמשתנה כאן, משתנה בשני הסקשנים
    var selectedMonthIndex by remember { mutableStateOf(availableMonths.size - 1) }
    val selectedMonth = availableMonths[selectedMonthIndex]
    val selectedTransactions = transactionsByMonth[selectedMonth] ?: emptyList()

    AppScaffold(currentRoute = "stats", onNavigate = onNavigate) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F5F5))
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                TotalSpendSection(
                    selectedMonth = selectedMonth,
                    transactions = selectedTransactions,
                    onPrevMonth = {
                        if (selectedMonthIndex > 0) selectedMonthIndex--
                    },
                    onNextMonth = {
                        if (selectedMonthIndex < availableMonths.size - 1) selectedMonthIndex++
                    },
                    hasPrev = selectedMonthIndex > 0,
                    hasNext = selectedMonthIndex < availableMonths.size - 1
                )
            }
            item {
                DonutChartSection(
                    selectedMonth = selectedMonth,
                    transactions = selectedTransactions,
                    onMonthSelected = { month ->
                        val idx = availableMonths.indexOf(month)
                        if (idx >= 0) selectedMonthIndex = idx
                    }
                )
            }
            item {
                BarChartSection(
                    selectedMonthName = availableMonths[selectedMonthIndex],
                    onMonthSelected = { month ->
                        val idx = availableMonths.indexOf(month)
                        if (idx >= 0) selectedMonthIndex = idx
                    }
                )
            }
        }
    }
}

// =============================================
// 1. Total Spend — עם חצים לחודש
// =============================================

@Composable
fun TotalSpendSection(
    selectedMonth: String,
    transactions: List<StatsTransaction>,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    hasPrev: Boolean,
    hasNext: Boolean
) {
    val totalSpend = transactions.sumOf { it.amount }
    val regular    = transactions.count { it.amount <= 1000 }
    val irregular  = transactions.count { it.amount > 1000 }
    var showSuspicious by remember { mutableStateOf(false) }
    val suspiciousInMonth = transactions.filter { it.amount > 1000 }

    val animatedAmount by animateFloatAsState(
        targetValue = totalSpend.toFloat(),
        animationSpec = tween(durationMillis = 800, easing = EaseOutCubic),
        label = "totalSpend"
    )

    StatSection {
        // חצים + חודש + סכום
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ניווט חודש
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onPrevMonth,
                    enabled = hasPrev
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowLeft,
                        contentDescription = "Previous month",
                        tint = if (hasPrev) teal else Color(0xFFDADBDD)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = fullMonthName(selectedMonth),
                        color = teal,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Total Spend",
                        color = Color(0xFF878C90),
                        fontSize = 13.sp
                    )
                }

                IconButton(
                    onClick = onNextMonth,
                    enabled = hasNext
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowRight,
                        contentDescription = "Next month",
                        tint = if (hasNext) teal else Color(0xFFDADBDD)
                    )
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
                    modifier = Modifier
                        .size(28.dp)
                        .border(1.dp, Color(0xFF4CAF50), RoundedCornerShape(8.dp))
                        .background(Color(0xFFE8F5E9), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("↑", color = Color(0xFF4CAF50), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("Regular", color = Color(0xFF878C90), fontSize = 9.sp)
                    Text(regular.toString(), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                }
            }

            VerticalDivider(
                modifier = Modifier.height(40.dp).padding(horizontal = 12.dp),
                color = Color(0xFFE7E8E9)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { showSuspicious = !showSuspicious }
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .border(1.dp, Color(0xFFF44336), RoundedCornerShape(8.dp))
                        .background(Color(0xFFFFEBEE), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("↓", color = Color.Red, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("Irregular", color = Color(0xFF878C90), fontSize = 9.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(irregular.toString(), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Red)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = if (showSuspicious) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = Color.Red,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = showSuspicious,
            enter = expandVertically(animationSpec = tween(300)) + fadeIn(),
            exit  = shrinkVertically(animationSpec = tween(300)) + fadeOut()
        ) {
            SuspiciousTransactionList(suspiciousInMonth)
        }
    }
}

@Composable
fun SuspiciousTransactionList(transactions: List<StatsTransaction>) {
    Column {
        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFE7E8E9))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Warning, contentDescription = null,
                tint = Color(0xFFF44336), modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Suspicious Transactions", fontSize = 14.sp,
                fontWeight = FontWeight.Bold, color = Color(0xFFF44336))
        }
        Spacer(modifier = Modifier.height(8.dp))
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
// 2. Donut Chart — מחובר לחודש הנבחר
// =============================================

@Composable
fun DonutChartSection(
    selectedMonth: String,
    transactions: List<StatsTransaction>,
    onMonthSelected: (String) -> Unit
) {
    val screenWidth = LocalConfiguration.current.screenWidthDp
    var selectedIndex     by remember { mutableStateOf(-1) }
    var lastSelectedIndex by remember { mutableStateOf(0) }
    var showMonthDropdown by remember { mutableStateOf(false) }

    // כשמשתנה החודש — אפס את הקטגוריה הנבחרת
    LaunchedEffect(selectedMonth) { selectedIndex = -1 }

    val categories = remember(transactions) { getCategories(transactions) }
    val total = categories.sumOf { it.amount }

    if (selectedIndex >= 0 && selectedIndex < categories.size) lastSelectedIndex = selectedIndex

    // אנימציה חלקה — כל פרוסה מאניימת מהמיקום הקודם שלה
    val sweepAngles = if (total > 0) categories.map { (it.amount / total * 360f).toFloat() }
    else categories.map { 0f }
    val startAngles = sweepAngles.runningFold(-90f) { acc, s -> acc + s }.dropLast(1)

    val animatedSweepAngles = sweepAngles.mapIndexed { i, target ->
        animateFloatAsState(
            targetValue = target,
            animationSpec = tween(durationMillis = 600, easing = EaseInOutCubic),
            label = "sweep_$i"
        ).value
    }
    val animatedStartAngles = animatedSweepAngles.runningFold(-90f) { acc, s -> acc + s }.dropLast(1)

    val centerAmount = if (selectedIndex >= 0 && selectedIndex < categories.size)
        "₪${"%.0f".format(categories[selectedIndex].amount)}"
    else
        "₪${"%.0f".format(total)}"
    val centerLabel = if (selectedIndex >= 0 && selectedIndex < categories.size)
        categories[selectedIndex].name
    else
        selectedMonth

    StatSection {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Expenses", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            Spacer(modifier = Modifier.weight(1f))
            Box {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFFF5F5F5),
                    modifier = Modifier.clickable { showMonthDropdown = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(selectedMonth, color = Color(0xFF878C90), fontSize = 10.sp)
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null,
                            tint = Color(0xFF878C90), modifier = Modifier.size(14.dp))
                    }
                }
                DropdownMenu(expanded = showMonthDropdown, onDismissRequest = { showMonthDropdown = false }) {
                    availableMonths.forEach { month ->
                        DropdownMenuItem(
                            text = { Text(month, fontSize = 13.sp) },
                            onClick = {
                                onMonthSelected(month)
                                showMonthDropdown = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (total == 0.0) {
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                contentAlignment = Alignment.Center) {
                Text("No data for $selectedMonth", color = Color(0xFF878C90), fontSize = 14.sp)
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.size(if (screenWidth > 600) 220.dp else 160.dp)) {
                        val strokeWidth = 36f
                        val inset = strokeWidth / 2
                        val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                        val topLeft = Offset(inset, inset)
                        val gap = 3f
                        categories.forEachIndexed { i, cat ->
                            val sweep = if (i < animatedSweepAngles.size) animatedSweepAngles[i] else 0f
                            val start = if (i < animatedStartAngles.size) animatedStartAngles[i] else -90f
                            val isSelected = i == selectedIndex
                            if (sweep > 0f) {
                                drawArc(
                                    color = if (isSelected) cat.color.copy(alpha = 1f) else cat.color.copy(alpha = 0.85f),
                                    startAngle = start + gap / 2f,
                                    sweepAngle = sweep - gap,
                                    useCenter = false,
                                    style = Stroke(width = if (isSelected) strokeWidth * 1.2f else strokeWidth),
                                    size = arcSize,
                                    topLeft = topLeft
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

            Spacer(modifier = Modifier.height(24.dp))

            val half = categories.size / 2 + categories.size % 2
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    categories.take(half).forEachIndexed { i, cat ->
                        LegendItem(cat = cat, isSelected = i == selectedIndex, onClick = {
                            selectedIndex = if (selectedIndex == i) -1 else i
                        })
                    }
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    categories.drop(half).forEachIndexed { i, cat ->
                        val realIndex = i + half
                        LegendItem(cat = cat, isSelected = realIndex == selectedIndex, onClick = {
                            selectedIndex = if (selectedIndex == realIndex) -1 else realIndex
                        })
                    }
                }
            }

            AnimatedVisibility(
                visible = selectedIndex >= 0,
                enter = expandVertically(animationSpec = tween(300)) + fadeIn(),
                exit  = shrinkVertically(animationSpec = tween(300)) + fadeOut()
            ) {
                if (lastSelectedIndex < categories.size) {
                    CategoryTransactionList(
                        categoryName = categories[lastSelectedIndex].name,
                        transactions = transactions.filter {
                            it.category == categories[lastSelectedIndex].name
                        }
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
            fontWeight = FontWeight.Bold, color = teal,
            modifier = Modifier.padding(bottom = 10.dp))
        transactions.forEach { tx ->
            TransactionRow(
                transaction = TransactionItem(
                    id = tx.id, title = tx.title, date = tx.date,
                    amount = tx.amount, isIrregular = tx.amount > 1000,
                    category = tx.category
                ),
                variant = TransactionRowVariant.COMPACT
            )
        }
    }
}

@Composable
fun LegendItem(cat: CategoryData, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(32.dp)
                .background(cat.color, RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(cat.name, fontSize = 13.sp,
                color = if (isSelected) teal else Color.Black,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
            val total = cat.amount + 0.001 // מניעת חלוקה באפס
            Text("${(cat.amount / total * 100).toInt()}%",
                fontSize = 11.sp, color = Color(0xFF878C90))
        }
    }
}

// =============================================
// 3. Bar Chart + Insights
// =============================================

@Composable
fun BarChartSection(
    selectedMonthName: String,
    onMonthSelected: (String) -> Unit
) {
    val screenWidth = LocalConfiguration.current.screenWidthDp
    var lastSelectedMonth by remember { mutableStateOf<MonthData?>(null) }
    val selectedMonth = dummyMonths.find { it.month == selectedMonthName }
    if (selectedMonth != null) lastSelectedMonth = selectedMonth
    val maxAmount = dummyMonths.maxOf { it.amount }

    // אנימציה בכניסה
    var barsVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(100)
        barsVisible = true
    }

    StatSection {
        Text("Monthly Overview", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        Spacer(modifier = Modifier.height(20.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(dummyMonths) { monthData ->
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
                        if (isSelected) {
                            Text("₪${"%,.0f".format(monthData.amount)}",
                                fontSize = 9.sp, fontWeight = FontWeight.Bold,
                                color = teal, textAlign = TextAlign.Center)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(animatedHeight)
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(if (isSelected) teal else Color(0xFFDADBDD))
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(monthData.month, fontSize = 12.sp,
                        color = if (isSelected) teal else Color(0xFF878C90),
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        textAlign = TextAlign.Center)
                }
            }
        }

        AnimatedVisibility(
            visible = selectedMonth != null,
            enter = expandVertically(animationSpec = tween(300)) + fadeIn(),
            exit  = shrinkVertically(animationSpec = tween(300)) + fadeOut()
        ) {
            lastSelectedMonth?.let { month ->
                MonthInsights(month = month)
            }
        }
    }
}

@Composable
fun MonthInsights(month: MonthData) {
    val txs = month.transactions
    val avg = if (txs.isNotEmpty()) txs.sumOf { it.amount } / txs.size else 0.0
    val highest = txs.maxByOrNull { it.amount }
    val mostFrequentTitle = txs.groupBy { it.title }.maxByOrNull { it.value.size }?.key ?: "-"

    Column {
        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = Color(0xFFE7E8E9))
        Text("${month.month} Insights", fontSize = 14.sp,
            fontWeight = FontWeight.Bold, color = teal,
            modifier = Modifier.padding(bottom = 12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            InsightItem(label = "Avg. Transaction", value = "₪${"%,.0f".format(avg)}")
            VerticalDivider(modifier = Modifier.height(40.dp), color = Color(0xFFE7E8E9))
            InsightItem(label = "Highest", value = "₪${highest?.amount?.toInt() ?: 0}")
            VerticalDivider(modifier = Modifier.height(40.dp), color = Color(0xFFE7E8E9))
            InsightItem(label = "Most frequent", value = mostFrequentTitle)
        }
    }
}

@Composable
fun InsightItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 9.sp, color = Color(0xFF878C90), textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold,
            color = Color.Black, textAlign = TextAlign.Center)
    }
}