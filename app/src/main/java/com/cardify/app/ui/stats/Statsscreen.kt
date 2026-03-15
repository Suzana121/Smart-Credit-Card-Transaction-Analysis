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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cardify.app.ui.account.teal
import com.cardify.app.ui.activity.Transaction
import com.cardify.app.ui.components.AppScaffold
import androidx.compose.ui.platform.LocalConfiguration

// =============================================
// Dummy Data
// =============================================

val dummyStatsTransactions = listOf(
    Transaction("1",  "Greg's Coffee",    "10/11/2025", 20.0),
    Transaction("2",  "Temu",             "22/11/2025", 2000.0),
    Transaction("3",  "Coffix",           "10/11/2025", 5.0),
    Transaction("4",  "Book Depository",  "15/11/2025", 200.0),
    Transaction("5",  "Cstyle",           "20/11/2025", 599.0),
    Transaction("6",  "Shufersal",        "05/12/2025", 350.0),
    Transaction("7",  "Netflix",          "01/12/2025", 45.0),
    Transaction("8",  "Amazon",           "10/12/2025", 1500.0),
    Transaction("9",  "Pharmacy",         "12/12/2025", 120.0),
    Transaction("10", "University Fee",   "01/01/2026", 3000.0),
    Transaction("11", "Gym",              "05/01/2026", 180.0),
    Transaction("12", "SuperPharm",       "10/01/2026", 95.0),
    Transaction("13", "Zara",             "15/01/2026", 450.0),
    Transaction("14", "Electricity Bill", "20/01/2026", 320.0),
    Transaction("15", "Restaurant",       "22/01/2026", 210.0),
)

val suspiciousTransactions = dummyStatsTransactions.filter { it.amount > 1000 }

data class CategoryData(val name: String, val amount: Double, val color: Color)

val pastelColors = listOf(
    Color(0xFF90CAF9), // כחול פסטל
    Color(0xFFA5D6A7), // ירוק פסטל
    Color(0xFFFFCC80), // כתום פסטל
    Color(0xFFEF9A9A), // אדום פסטל
    Color(0xFFCE93D8), // סגול פסטל
)

val dummyCategories = listOf(
    CategoryData("Food",      3200.0, pastelColors[0]),
    CategoryData("Health",    1800.0, pastelColors[1]),
    CategoryData("Education", 2500.0, pastelColors[2]),
    CategoryData("Mortgage",  4100.0, pastelColors[3]),
    CategoryData("Other",     1200.0, pastelColors[4]),
)

data class MonthData(val month: String, val amount: Double, val transactions: List<Transaction>)

val dummyMonths = listOf(
    MonthData("Aug", 3200.0, dummyStatsTransactions.take(2)),
    MonthData("Sep", 4100.0, dummyStatsTransactions.take(3)),
    MonthData("Oct", 3600.0, dummyStatsTransactions.take(2)),
    MonthData("Nov", 2820.0, dummyStatsTransactions.subList(0, 5)),
    MonthData("Dec", 5015.0, dummyStatsTransactions.subList(5, 9)),
    MonthData("Jan", 4255.0, dummyStatsTransactions.subList(9, 15)),
    MonthData("Feb", 3800.0, dummyStatsTransactions.take(4)),
    MonthData("Mar", 4500.0, dummyStatsTransactions.take(5)),
)

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
// Main Screen
// =============================================

@Composable
fun StatsScreen(onNavigate: (String) -> Unit) {
    AppScaffold(currentRoute = "stats", onNavigate = onNavigate) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F5F5))
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item { TotalSpendSection() }
            item { DonutChartSection() }
            item { BarChartSection() }
        }
    }
}

// =============================================
// 1. Total Spend + Regular/Irregular
//    לחיצה על Irregular → עסקאות חשודות
// =============================================

@Composable
fun TotalSpendSection() {
    val totalSpend = dummyStatsTransactions.sumOf { it.amount }
    val regular    = dummyStatsTransactions.count { it.amount <= 1000 }
    val irregular  = dummyStatsTransactions.count { it.amount > 1000 }
    var showSuspicious by remember { mutableStateOf(false) }

    val animatedAmount by animateFloatAsState(
        targetValue = totalSpend.toFloat(),
        animationSpec = tween(durationMillis = 1200, easing = EaseOutCubic),
        label = "totalSpend"
    )

    StatSection {
        // סכום כולל
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Total Spend", color = Color(0xFF878C90), fontSize = 18.sp)
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                "₪${"%,.2f".format(animatedAmount)}",
                fontSize = if (LocalConfiguration.current.screenWidthDp > 600) 42.sp else 34.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = Color(0xFFE7E8E9))

        // Regular / Irregular
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Regular
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

            // Irregular — לחיצה פותחת עסקאות חשודות
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

        // עסקאות חשודות
        AnimatedVisibility(
            visible = showSuspicious,
            enter = expandVertically(animationSpec = tween(300)) + fadeIn(),
            exit  = shrinkVertically(animationSpec = tween(300)) + fadeOut()
        ) {
            Column {
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFE7E8E9))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null,
                        tint = Color(0xFFF44336), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Suspicious Transactions",
                        fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF44336)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                suspiciousTransactions.forEach { tx -> StatsTransactionRow(tx) }
            }
        }
    }
}

// =============================================
// 2. Donut Chart — צבעים פסטליים, % באמצע
//    לחיצה על שם → עסקאות של קטגוריה
// =============================================

@Composable
fun DonutChartSection() {
    val screenWidth = LocalConfiguration.current.screenWidthDp
    var selectedIndex     by remember { mutableStateOf(-1) }
    var selectedMonth     by remember { mutableStateOf("Monthly") }
    var showMonthDropdown by remember { mutableStateOf(false) }
    val months = listOf("Monthly", "Jan", "Feb", "Mar", "Aug", "Sep", "Oct", "Nov", "Dec")
    val total  = dummyCategories.sumOf { it.amount }

    val animatedSweep by animateFloatAsState(
        targetValue = 360f,
        animationSpec = tween(durationMillis = 1000, easing = EaseOutCubic),
        label = "donutSweep"
    )

    val sweepAngles = dummyCategories.map { (it.amount / total * 360f).toFloat() }
    val startAngles = sweepAngles.runningFold(-90f) { acc, s -> acc + s }.dropLast(1)

    val centerAmount = if (selectedIndex >= 0)
        "₪${"%.0f".format(dummyCategories[selectedIndex].amount)}"
    else
        "₪${"%.0f".format(total)}"
    val centerLabel = if (selectedIndex >= 0)
        dummyCategories[selectedIndex].name
    else
        "Total"

    StatSection {
        // כותרת + dropdown
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
                    months.forEach { month ->
                        DropdownMenuItem(
                            text = { Text(month, fontSize = 13.sp) },
                            onClick = { selectedMonth = month; showMonthDropdown = false }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Donut + מקרא
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // Donut Chart
            Box(contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.size(if (screenWidth > 600) 220.dp else 160.dp)) {
                    val strokeWidth = 36f
                    val inset = strokeWidth / 2
                    val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                    val topLeft = Offset(inset, inset)
                    val gap = 3f

                    dummyCategories.forEachIndexed { i, cat ->
                        val sweep = (sweepAngles[i] * animatedSweep / 360f)
                        val isSelected = i == selectedIndex
                        drawArc(
                            color = if (isSelected) cat.color.copy(alpha = 1f) else cat.color.copy(alpha = 0.85f),
                            startAngle = startAngles[i] + gap / 2f,
                            sweepAngle = sweep - gap,
                            useCenter = false,
                            style = Stroke(
                                width = if (isSelected) strokeWidth * 1.2f else strokeWidth
                            ),
                            size = arcSize,
                            topLeft = topLeft
                        )
                    }
                }

                // טקסט באמצע
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        centerAmount,
                        fontSize = if (screenWidth > 600) 24.sp else 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Text(
                        centerLabel,
                        fontSize = 11.sp,
                        color = Color(0xFF878C90)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // מקרא — 2 עמודות
        val half = dummyCategories.size / 2 + dummyCategories.size % 2
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                dummyCategories.take(half).forEachIndexed { i, cat ->
                    LegendItem(cat = cat, isSelected = i == selectedIndex, onClick = {
                        selectedIndex = if (selectedIndex == i) -1 else i
                    })
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                dummyCategories.drop(half).forEachIndexed { i, cat ->
                    val realIndex = i + half
                    LegendItem(cat = cat, isSelected = realIndex == selectedIndex, onClick = {
                        selectedIndex = if (selectedIndex == realIndex) -1 else realIndex
                    })
                }
            }
        }

        // עסקאות לפי קטגוריה
        AnimatedVisibility(
            visible = selectedIndex >= 0,
            enter = expandVertically(animationSpec = tween(300)) + fadeIn(),
            exit  = shrinkVertically(animationSpec = tween(300)) + fadeOut()
        ) {
            if (selectedIndex >= 0) {
                Column {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = Color(0xFFE7E8E9))
                    Text(
                        "${dummyCategories[selectedIndex].name} Transactions",
                        fontSize = 14.sp, fontWeight = FontWeight.Bold, color = teal,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                    dummyStatsTransactions.take(5).forEach { tx -> StatsTransactionRow(tx) }
                }
            }
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
            Text(
                cat.name,
                fontSize = 13.sp,
                color = if (isSelected) teal else Color.Black,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
            Text(
                "${(cat.amount / dummyCategories.sumOf { it.amount } * 100).toInt()}%",
                fontSize = 11.sp,
                color = Color(0xFF878C90)
            )
        }
    }
}

// =============================================
// 3. Bar Chart + Insights per month
// =============================================

@Composable
fun BarChartSection() {
    val screenWidth = LocalConfiguration.current.screenWidthDp
    var selectedMonth by remember { mutableStateOf<MonthData?>(null) }
    val maxAmount = dummyMonths.maxOf { it.amount }

    StatSection {
        Text("Monthly Overview", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        Spacer(modifier = Modifier.height(20.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(dummyMonths) { monthData ->
                val isSelected = monthData == selectedMonth
                val animatedHeight by animateFloatAsState(
                    targetValue = (monthData.amount / maxAmount).toFloat(),
                    animationSpec = tween(800, easing = EaseOutCubic),
                    label = "bar_${monthData.month}"
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(if (screenWidth > 600) 64.dp else 44.dp)
                        .clickable { selectedMonth = if (selectedMonth == monthData) null else monthData }
                ) {
                    Box(modifier = Modifier.height(20.dp), contentAlignment = Alignment.BottomCenter) {
                        if (isSelected) {
                            Text(
                                "₪${"%,.0f".format(monthData.amount)}",
                                fontSize = 9.sp, fontWeight = FontWeight.Bold, color = teal,
                                textAlign = TextAlign.Center
                            )
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
                    Text(
                        monthData.month,
                        fontSize = 12.sp,
                        color = if (isSelected) teal else Color(0xFF878C90),
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Insights לחודש הנבחר
        AnimatedVisibility(
            visible = selectedMonth != null,
            enter = expandVertically(animationSpec = tween(300)) + fadeIn(),
            exit  = shrinkVertically(animationSpec = tween(300)) + fadeOut()
        ) {
            selectedMonth?.let { month ->
                val txs = month.transactions
                val avg = if (txs.isNotEmpty()) txs.sumOf { it.amount } / txs.size else 0.0
                val highest = txs.maxByOrNull { it.amount }
                val mostFrequentTitle = txs.groupBy { it.title }.maxByOrNull { it.value.size }?.key ?: "-"

                Column {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = Color(0xFFE7E8E9))

                    Text(
                        "${month.month} Insights",
                        fontSize = 14.sp, fontWeight = FontWeight.Bold, color = teal,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        InsightItem(label = "Avg. Transaction", value = "\u20AA${"%,.0f".format(avg)}")
                        VerticalDivider(modifier = Modifier.height(40.dp), color = Color(0xFFE7E8E9))
                        InsightItem(label = "Highest", value = "\u20AA${highest?.amount?.toInt() ?: 0}")
                        VerticalDivider(modifier = Modifier.height(40.dp), color = Color(0xFFE7E8E9))
                        InsightItem(label = "Most frequent", value = mostFrequentTitle)
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
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Black,
            textAlign = TextAlign.Center)
    }
}

// =============================================
// שורת עסקה
// =============================================

@Composable
fun StatsTransactionRow(transaction: Transaction) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(transaction.date, fontSize = 12.sp, color = Color.Black,
            fontWeight = FontWeight.Bold, modifier = Modifier.width(70.dp))
        Text(transaction.title, fontSize = 14.sp, fontWeight = FontWeight.Bold,
            color = Color.Black, modifier = Modifier.weight(1f))
        Text("₪${transaction.amount.toInt()}", fontSize = 14.sp,
            fontWeight = FontWeight.Bold, color = Color.Black)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = if (transaction.amount > 1000) "Irregular" else "Regular",
            color = if (transaction.amount > 1000) Color.Red else Color(0xFF4CAF50),
            fontSize = 12.sp, fontWeight = FontWeight.Bold
        )
    }
    HorizontalDivider(color = Color(0xFFF0F0F0))
}