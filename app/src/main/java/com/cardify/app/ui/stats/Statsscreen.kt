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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cardify.app.data.model.StatsResponse // ודאי שה-Import תואם למיקום המודל שלך
import com.cardify.app.ui.account.teal
import com.cardify.app.ui.components.AppScaffold
import com.cardify.app.ui.components.TransactionItem
import com.cardify.app.ui.components.TransactionRow
import com.cardify.app.ui.components.TransactionRowVariant

// --- עזרי תצוגה ---
fun fullMonthName(month: String): String = when (month) {
    "Jan" -> "January" "Feb" -> "February" "Mar" -> "March"
    "Apr" -> "April" "May" -> "May" "Jun" -> "June"
    "Jul" -> "July" "Aug" -> "August" "Sep" -> "September"
    "Oct" -> "October" "Nov" -> "November" "Dec" -> "December"
    else -> month
}

// פונקציית עזר להתאמת צבעים לקטגוריות מהשרת
// בתוך Statsscreen.kt
fun getCategoryColor(categoryName: String?): Color { // שינוי ל-String? מאפשר לקבל null בלי לקרוס
    return when (categoryName) {
        "מסעדות, קפה וברים" -> Color(0xFFFF9800)
        "קניות" -> Color(0xFF2196F3)
        "בילוי ופנאי" -> Color(0xFF9C27B0)
        // הוסיפי כאן את שאר הקטגוריות שלך
        else -> Color.Gray // צבע ברירת מחדל לכל מה שלא מזוהה או null
    }
}

@Composable
fun StatsScreen(
    onNavigate: (String) -> Unit,
    viewModel: StatsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedMonth = viewModel.availableMonths[viewModel.selectedMonthIndex]

    AppScaffold(currentRoute = "stats", onNavigate = onNavigate) { padding ->
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F5F5)).padding(padding)) {
            when (val state = uiState) {
                is StatsUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = teal)
                }
                is StatsUiState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center).clickable { viewModel.loadData() },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.Gray)
                        Text(text = state.message, color = Color.Gray)
                    }
                }
                is StatsUiState.Success -> {
                    // העברת אובייקט ה-Response המלא
                    StatsContent(state.data, selectedMonth, viewModel)
                }
            }
        }
    }
}

@Composable
fun StatsContent(data: StatsResponse, selectedMonth: String, viewModel: StatsViewModel) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            TotalSpendSection(
                selectedMonth = selectedMonth,
                data = data,
                onPrevMonth = { viewModel.changeMonth(viewModel.selectedMonthIndex - 1) },
                onNextMonth = { viewModel.changeMonth(viewModel.selectedMonthIndex + 1) },
                hasPrev = viewModel.selectedMonthIndex > 0,
                hasNext = viewModel.selectedMonthIndex < viewModel.availableMonths.size - 1
            )
        }
        item { DonutChartSection(data) }
        item { SimpleInsights(selectedMonth, data) }
    }
}

@Composable
fun TotalSpendSection(
    selectedMonth: String,
    data: StatsResponse,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    hasPrev: Boolean,
    hasNext: Boolean
) {
    var showIrregularList by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFDADBDD))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPrevMonth, enabled = hasPrev) {
                    Icon(Icons.Default.KeyboardArrowLeft, null, tint = if (hasPrev) teal else Color.LightGray)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(fullMonthName(selectedMonth), color = teal, fontWeight = FontWeight.Bold)
                    Text("Total Spend", color = Color.Gray, fontSize = 12.sp)
                }
                IconButton(onClick = onNextMonth, enabled = hasNext) {
                    Icon(Icons.Default.KeyboardArrowRight, null, tint = if (hasNext) teal else Color.LightGray)
                }
            }

            Text(
                text = "₪${"%,.2f".format(data.totalSpend)}",
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = Color(0xFFF0F0F0))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                InsightMiniItem("Regular", data.regularTransactionsCount.toString(), Color(0xFF4CAF50))
                VerticalDivider(modifier = Modifier.height(40.dp), color = Color(0xFFF0F0F0))
                InsightMiniItem(
                    label = "Irregular",
                    value = data.irregularTransactionsCount.toString(),
                    color = Color.Red,
                    isClickable = true,
                    onClick = { showIrregularList = !showIrregularList }
                )
            }

            AnimatedVisibility(visible = showIrregularList) {
                Column {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFF0F0F0))
                    Text("Suspicious Activities", color = Color.Red, fontSize = 13.sp, fontWeight = FontWeight.Bold)

                    // במידה ותוסיפי רשימת עסקאות למודל ה-Response בעתיד, תוכלי להציג אותן כאן
                    if (data.irregularTransactionsCount == 0) {
                        Text("No suspicious activities found.", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top = 8.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun DonutChartSection(data: StatsResponse) {
    val categories = data.expensesByCategory
    val total = data.totalSpend

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFDADBDD))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Expenses by Category", fontWeight = FontWeight.Bold)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                if (total > 0) {
                    Canvas(modifier = Modifier.size(140.dp)) {
                        var startAngle = -90f
                        categories.forEach { cat ->
                            val sweep = (cat.amount / total).toFloat() * 360f

                            // שימוש ב-categoryName ובדיקת null בטוחה
                            val color = getCategoryColor(cat.category ?: "Other")

                            drawArc(
                                color = color,
                                startAngle = startAngle,
                                sweepAngle = sweep,
                                useCenter = false,
                                style = Stroke(30f),
                                size = Size(size.width, size.height)
                            )
                            startAngle += sweep
                        }
                    }
                }
                Text("₪${total.toInt()}", fontWeight = FontWeight.Bold)
            }
        }
    }
}
@Composable
fun SimpleInsights(month: String, data: StatsResponse) {
    // ניתן להוסיף חישובים נוספים במודל השרת ולהציגם כאן
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp), color = Color.White, border = BorderStroke(1.dp, Color(0xFFDADBDD))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("${fullMonthName(month)} Insights", fontWeight = FontWeight.Bold, color = teal)
            Text("Total categories tracked: ${data.expensesByCategory.size}", fontSize = 14.sp)
        }
    }
}

@Composable
fun InsightMiniItem(label: String, value: String, color: Color, isClickable: Boolean = false, onClick: () -> Unit = {}) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = if (isClickable) Modifier.clickable { onClick() } else Modifier
    ) {
        Box(
            modifier = Modifier.size(24.dp).background(color.copy(alpha = 0.1f), RoundedCornerShape(4.dp)).border(1.dp, color, RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(if (color == Color.Red) "!" else "✓", color = color, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(label, fontSize = 10.sp, color = Color.Gray)
            Text(value, fontWeight = FontWeight.Bold, color = color)
        }
    }
}