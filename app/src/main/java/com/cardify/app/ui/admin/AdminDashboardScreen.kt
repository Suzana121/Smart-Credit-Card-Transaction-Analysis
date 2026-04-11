package com.cardify.app.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cardify.app.ui.components.AppScaffold
import com.cardify.app.ui.home.CardifyColors

@Composable
fun AdminDashboardScreen(
    onNavigate: (String) -> Unit = {},
    viewModel: AdminDashboardViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    AppScaffold(currentRoute = "stats", onNavigate = onNavigate) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F5F5))
                .padding(padding)
        ) {
            when (val state = uiState) {
                is AdminUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = CardifyColors.DarkGreen
                    )
                }
                is AdminUiState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Warning, null, tint = Color.Red, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(8.dp))
                        Text(state.message, color = Color.Gray)
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.loadDashboard() },
                            colors = ButtonDefaults.buttonColors(containerColor = CardifyColors.DarkGreen)
                        ) {
                            Text("Retry")
                        }
                    }
                }
                is AdminUiState.Success -> {
                    AdminDashboardContent(data = state.data)
                }
            }
        }
    }
}

@Composable
fun AdminDashboardContent(data: AdminDashboardData) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- כותרת ---
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Shield,
                    null,
                    tint = CardifyColors.DarkGreen,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        "Admin Dashboard",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = CardifyColors.DarkGreen
                    )
                    Text(
                        "System-wide overview",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        }

        // --- כרטיסי סטטיסטיקה ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Total Users",
                    value = data.totalUsers.toString(),
                    icon = Icons.Default.People,
                    color = CardifyColors.DarkGreen
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Transactions",
                    value = data.totalTransactions.toString(),
                    icon = Icons.Default.Receipt,
                    color = Color(0xFF1976D2)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Suspicious",
                    value = data.totalIrregular.toString(),
                    icon = Icons.Default.Warning,
                    color = CardifyColors.IrregularRed
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Fraud Rate",
                    value = "${data.irregularRate}%",
                    icon = Icons.Default.PieChart,
                    color = Color(0xFFE65100)
                )
            }
        }

        // --- עסקים חשודים ---
        item {
            DashboardCard(title = "Top Suspicious Businesses", icon = Icons.Default.Store) {
                if (data.topSuspiciousBusinesses.isEmpty()) {
                    Text("No suspicious businesses found", color = Color.Gray, fontSize = 13.sp)
                } else {
                    data.topSuspiciousBusinesses.forEachIndexed { index, business ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // מספר דירוג
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(CardifyColors.IrregularRed.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "${index + 1}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CardifyColors.IrregularRed
                                    )
                                }
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    business.businessName,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            // badge עם מספר דיווחים
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(CardifyColors.IrregularRed)
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    "${business.count} reports",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        if (index < data.topSuspiciousBusinesses.size - 1) {
                            HorizontalDivider(color = Color(0xFFF0F0F0))
                        }
                    }
                }
            }
        }

        // --- משתמשים עם הכי הרבה חשודות ---
        item {
            DashboardCard(title = "Users with Most Suspicious Activity", icon = Icons.Default.PersonSearch) {
                if (data.topUsersWithIrregular.isEmpty()) {
                    Text("No data available", color = Color.Gray, fontSize = 13.sp)
                } else {
                    data.topUsersWithIrregular.forEachIndexed { index, user ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Person,
                                    null,
                                    tint = CardifyColors.DarkGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(user.username, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Text(
                                "${user.irregularCount} suspicious",
                                color = CardifyColors.IrregularRed,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (index < data.topUsersWithIrregular.size - 1) {
                            HorizontalDivider(color = Color(0xFFF0F0F0))
                        }
                    }
                }
            }
        }

        // --- התפלגות קטגוריות ---
        item {
            DashboardCard(title = "Spending by Category (All Users)", icon = Icons.Default.BarChart) {
                val total = data.categoryBreakdown.sumOf { it.amount }
                if (data.categoryBreakdown.isEmpty()) {
                    Text("No data available", color = Color.Gray, fontSize = 13.sp)
                } else {
                    data.categoryBreakdown
                        .sortedByDescending { it.amount }
                        .forEach { cat ->
                            val percent = if (total > 0) (cat.amount / total * 100).toInt() else 0
                            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(cat.category, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        "₪${"%.0f".format(cat.amount)} ($percent%)",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                // Progress bar
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFFEEEEEE))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(percent / 100f)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(CardifyColors.DarkGreen)
                                    )
                                }
                            }
                        }
                }
            }
        }
    }
}

// --- קומפוננטות עזר ---

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector,
    color: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(8.dp))
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = color)
            Text(title, fontSize = 11.sp, color = Color.Gray)
        }
    }
}

@Composable
fun DashboardCard(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = CardifyColors.DarkGreen, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = CardifyColors.DarkGreen)
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFF0F0F0))
            content()
        }
    }
}