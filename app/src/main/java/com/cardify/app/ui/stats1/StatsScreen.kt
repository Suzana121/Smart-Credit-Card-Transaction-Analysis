package com.cardify.app.ui.stats1

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cardify.app.ui.components.AppScaffold

@Composable
fun StatsScreen(
    viewModel: StatsViewModel, // המוח שחיברנו קודם
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    AppScaffold(
        title = "Stats",
        currentRoute = currentRoute,
        onNavigate = onNavigate
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF8F9FA)) // רקע בהיר ונקי
        ) {
            when (val state = uiState) {
                is StatsUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Color(0xFF0D7377))
                }
                is StatsUiState.Error -> {
                    Text(text = state.message, color = Color.Red, modifier = Modifier.align(Alignment.Center))
                }
                is StatsUiState.Success -> {
                    // כאן אנחנו בונים את המסך לפי הפיגמה
                    val data = state.data
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(20.dp), // מרווח מהצדדים לפי הפיגמה
                        verticalArrangement = Arrangement.spacedBy(20.dp) // מרווח של 20dp בין כרטיסים
                    ) {
                        // 1. כרטיס Total Spend (גובה 111dp בפיגמה)
                        item {
                            StatCard(height = 111.dp) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Total Spend", fontSize = 14.sp, color = Color.Gray)
                                    Text("₪${data.totalSpend}", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // 2. כרטיסי עסקאות (Regular/Irregular)
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(20.dp)
                            ) {
                                TransactionCountCard(modifier = Modifier.weight(1f), title = "Regular", count = data.regularTransactionsCount, isUp = true)
                                TransactionCountCard(modifier = Modifier.weight(1f), title = "Irregular", count = data.irregularTransactionsCount, isUp = false)
                            }
                        }

                        // 3. כרטיס Expense Pie Chart (גובה 258dp בפיגמה)
                        item {
                            StatCard(height = 258.dp) {
                                Text("Expense by Category", fontWeight = FontWeight.Bold)
                                // כאן יבוא גרף הפאי
                            }
                        }

                        // 4. כרטיס Expense Bar Chart (גובה 258dp בפיגמה)
                        item {
                            StatCard(height = 258.dp) {
                                Text("Monthly Expense", fontWeight = FontWeight.Bold)
                                // כאן יבוא גרף העמודות
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(
    height: androidx.compose.ui.unit.Dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(height),
        shape = RoundedCornerShape(10.dp), // פינות מעוגלות 10dp לפי הפיגמה
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp), // פאדינג פנימי לפי הפיגמה
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            content = content
        )
    }
}

@Composable
fun TransactionCountCard(modifier: Modifier, title: String, count: Int, isUp: Boolean) {
    Card(
        modifier = modifier.height(69.dp),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "$count", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(text = title, fontSize = 12.sp, color = Color.Gray)
        }
    }
}