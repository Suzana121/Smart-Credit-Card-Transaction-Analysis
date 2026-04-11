package com.cardify.app.ui.stats

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp // לשימוש בטקסט של Compose
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cardify.app.data.model.StatsResponse
import com.cardify.app.ui.account.teal
import com.cardify.app.ui.components.AppScaffold

import com.cardify.app.R


/**
 * Converts a 3-letter month abbreviation to its full English name.
 *
 * @param month A 3-letter abbreviation such as `"Jan"` or `"Dec"`.
 * @return The full month name, or [month] unchanged if it is not recognised.
 */
fun fullMonthName(month: String): String = when (month) {
    "Jan" -> "January" "Feb" -> "February" "Mar" -> "March"
    "Apr" -> "April" "May" -> "May" "Jun" -> "June"
    "Jul" -> "July" "Aug" -> "August" "Sep" -> "September"
    "Oct" -> "October" "Nov" -> "November" "Dec" -> "December"
    else -> month
}

/**
 * Returns the brand colour associated with a spending category name.
 *
 * @param categoryName The category name (case-insensitive), e.g. `"Food"` or `"Shopping"`.
 * @return A [Color] value for the category, or a neutral grey for unrecognised categories.
 */
fun getCategoryColor(categoryName: String?): Color {
    return when (categoryName?.lowercase()) {
        "food" -> Color(0xFF006769)
        "health" -> Color(0xFF40A578)
        "shopping" -> Color(0xFF9DDE8B)
        "transport" -> Color(0xFFE6FF94)
        "education" -> Color(0xFF2196F3)
        else -> Color(0xFF9E9E9E) // צבע אפור לקטגוריית "Other"
    }
}

/**
 * Spending statistics screen showing monthly totals, a donut chart, and basic insights.
 *
 * Handles the three [StatsUiState] cases: a loading spinner, a tap-to-retry error state,
 * and the full [StatsContent] on success.
 *
 * @param onNavigate Called with the destination route when a bottom nav item is tapped.
 * @param viewModel The [StatsViewModel] providing state and month navigation.
 */
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
                    StatsContent(state.data, selectedMonth, viewModel)
                }
            }
        }
    }
}

/**
 * Lays out the three statistics sections inside a [LazyColumn]: the total spend card with
 * month navigation, the donut chart, and the insights summary.
 *
 * @param data The [StatsResponse] containing spending totals and category breakdown.
 * @param selectedMonth The 3-letter abbreviation of the currently displayed month.
 * @param viewModel Used to navigate between months via [StatsViewModel.changeMonth].
 */
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

/**
 * Card showing the selected month's total spend with left/right navigation arrows,
 * regular and irregular transaction counts, and an expandable suspicious-activity list.
 *
 * @param selectedMonth The 3-letter month abbreviation displayed in the header.
 * @param data The [StatsResponse] providing spend totals and transaction counts.
 * @param onPrevMonth Called when the left arrow is tapped.
 * @param onNextMonth Called when the right arrow is tapped.
 * @param hasPrev Whether there is an earlier month available (enables the left arrow).
 * @param hasNext Whether there is a later month available (enables the right arrow).
 */
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
                    if (data.irregularTransactionsCount == 0) {
                        Text("No suspicious activities found.", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top = 8.dp))
                    }
                }
            }
        }
    }
}

/**
 * Renders a pie/donut chart using the third-party [ir.mahozad.android.PieChart] view wrapped
 * in an [AndroidView]. Each slice represents a spending category from [StatsResponse.expensesByCategory].
 *
 * @param data The [StatsResponse] whose [StatsResponse.expensesByCategory] populates the chart.
 */
@Composable
fun DonutChartSection(data: StatsResponse) {
    val categories = data.expensesByCategory
    val total = data.totalSpend

    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .height(350.dp) // 1. הגדלנו את הגובה הכולל
            .padding(vertical = 8.dp),
        factory = { ctx ->
            val view = android.view.LayoutInflater.from(ctx)
                .inflate(R.layout.pie_chart_layout, null) as ir.mahozad.android.PieChart

            view.apply {
                holeRatio = 0f
                labelsColor = android.graphics.Color.WHITE
                labelType = ir.mahozad.android.PieChart.LabelType.INSIDE

                isAnimationEnabled = true
                isLegendEnabled = false

                // 2. הקטנו את ה-Padding כדי שהפאי עצמו יגדל
                val p = 16
                setPadding(p, p, p, p)
            }
            view
        },
        update = { view ->
            if (total > 0) {
                view.slices = categories.map { cat ->
                    ir.mahozad.android.PieChart.Slice(
                        fraction = (cat.amount / total).toFloat(),
                        color = getCategoryColor(cat.category).toArgb(),
                        label = cat.category ?: ""
                    )
                }
            }
        }
    )
}
/**
 * Small summary card showing the number of distinct spending categories tracked in [month].
 *
 * @param month The 3-letter month abbreviation used in the section heading.
 * @param data The [StatsResponse] providing the category list.
 */
@Composable
fun SimpleInsights(month: String, data: StatsResponse) {
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

/**
 * Small coloured indicator showing a labelled numeric value (e.g. "Regular 12").
 *
 * @param label Short description of the metric.
 * @param value The numeric string to display.
 * @param color Accent colour applied to the icon box and value text.
 * @param isClickable When `true` the item responds to tap events via [onClick].
 * @param onClick Called when the item is tapped (only active when [isClickable] is `true`).
 */
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