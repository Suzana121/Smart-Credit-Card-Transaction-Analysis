package com.cardify.app.ui.activity

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cardify.app.ui.components.AppScaffold
import java.text.NumberFormat
import java.util.*

// ==========================================
// 1. המסך עצמו (UI בלבד)
// ==========================================
@Composable
fun ActivityScreen(
    onNavigate: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    AppScaffold(
        currentRoute = "activity",
        onNavigate = onNavigate,
        topBarContent = {
            ActivityTopBar(
                searchQuery = searchQuery,
                onSearchChange = { searchQuery = it },
                onFilterClick = { /* פילטר */ }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .background(Color(0xFFF5F5F5))
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            TransactionsList(
                transactions = getSampleTransactions(),
                searchQuery = searchQuery
            )
        }
    }
}

// ==========================================
// רכיבי עזר (TopBar, List, Card)
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityTopBar(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onFilterClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardifyColors.Primary)
            .padding(bottom = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(20.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Transactions",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { /* תפריט */ }) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menu",
                    tint = Color.White
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Search payment", color = Color.Gray, fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                modifier = Modifier.weight(1f).height(50.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent
                ),
                shape = RoundedCornerShape(25.dp),
                singleLine = true
            )
        }
    }
}

@Composable
fun TransactionsList(transactions: List<Transaction>, searchQuery: String) {
    val filtered = remember(transactions, searchQuery) {
        if (searchQuery.isEmpty()) transactions else transactions.filter { it.title.contains(searchQuery, true) }
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(filtered) { transaction -> TransactionCard(transaction) }
        item { Spacer(modifier = Modifier.height(8.dp)) }
    }
}

@Composable
fun TransactionCard(transaction: Transaction) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = transaction.date,
                fontSize = 12.sp,
                color = Color.Black,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(70.dp)
            )

            Text(
                text = transaction.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = "₪${transaction.amount.toInt()}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = if (transaction.amount > 1000) "Irregular" else "Regular",
                color = if (transaction.amount > 1000) Color.Red else Color(0xFF4CAF50),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// נתונים
data class Transaction(val id: String, val title: String, val date: String, val amount: Double)
object CardifyColors { val Primary = Color(0xFF0D7377); val ScanButton = Color(0xFFA0FF9D) }
fun getSampleTransactions(): List<Transaction> {
    return listOf(
        Transaction("1", "Greg's Coffee", "10/11/2025", 20.0),
        Transaction("2", "Temu", "22/11/2025", 2000.0),
        Transaction("3", "Coffix", "10/11/2025", 5.0),
        Transaction("4", "Book Depository", "10/11/2025", 200.0),
        Transaction("5", "Cstyle", "10/11/2025", 599.0)
    )
}