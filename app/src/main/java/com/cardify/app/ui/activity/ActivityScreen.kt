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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.util.*

// ======= קומפוננט ראשי =======
@Composable
fun ActivityScreen(
    onNavigateToHome: () -> Unit = {},
    onNavigateToWallet: () -> Unit = {},
    onNavigateToStats: () -> Unit = {},
    onNavigateToAccount: () -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }

    Scaffold(
        topBar = {
            ActivityTopBar(
                searchQuery = searchQuery,
                onSearchChange = { searchQuery = it },
                onFilterClick = { /* פתיחת דיאלוג פילטר */ }
            )
        },
        bottomBar = {
            BottomNavigationBar(
                selectedTab = 1, // Activity is tab 1
                onNavigateToHome = onNavigateToHome,
                onNavigateToWallet = onNavigateToWallet,
                onNavigateToStats = onNavigateToStats,
                onNavigateToAccount = onNavigateToAccount
            )
        },
        containerColor = Color(0xFFF5F5F5)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // רשימת עסקאות
            TransactionsList(
                transactions = getSampleTransactions(),
                searchQuery = searchQuery
            )
        }
    }
}

// ======= כותרת עליונה =======
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
        // Status Bar Spacer
        Spacer(modifier = Modifier.height(48.dp))

        // Back button and Title
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = Color.White,
                modifier = Modifier
                    .size(24.dp)
                    .clickable { /* חזרה */ }
            )

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = "Activity",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Search Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = {
                    Text(
                        text = "Search payment",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color.Gray
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent
                ),
                shape = RoundedCornerShape(25.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Filter Button
            IconButton(
                onClick = onFilterClick,
                modifier = Modifier
                    .size(50.dp)
                    .background(Color.White.copy(alpha = 0.2f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Filter",
                    tint = Color.White
                )
            }
        }
    }
}

// ======= רשימת עסקאות =======
@Composable
fun TransactionsList(
    transactions: List<Transaction>,
    searchQuery: String
) {
    val filteredTransactions = remember(transactions, searchQuery) {
        if (searchQuery.isEmpty()) {
            transactions
        } else {
            transactions.filter {
                it.title.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(filteredTransactions) { transaction ->
            TransactionCard(transaction = transaction)
        }

        // Bottom spacing
        item {
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

// ======= כרטיס עסקה בודדת =======
@Composable
fun TransactionCard(transaction: Transaction) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* פתיחת פרטי עסקה */ },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // אייקון
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(CardifyColors.Primary, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = transaction.icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // פרטים
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = transaction.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black
                )

                Text(
                    text = transaction.date,
                    fontSize = 13.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Total",
                    fontSize = 12.sp,
                    color = Color.Gray
                )

                Text(
                    text = formatCurrency(transaction.amount),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // סטטוס וכפתור
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Status Badge
                Surface(
                    color = when (transaction.status) {
                        TransactionStatus.SUCCESS -> Color(0xFFE8F5E9)
                        TransactionStatus.CANCELLED -> Color(0xFFFFEBEE)
                        TransactionStatus.PENDING -> Color(0xFFFFF3E0)
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = transaction.status.displayName,
                        color = when (transaction.status) {
                            TransactionStatus.SUCCESS -> Color(0xFF4CAF50)
                            TransactionStatus.CANCELLED -> Color(0xFFE57373)
                            TransactionStatus.PENDING -> Color(0xFFFF9800)
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                // Pay Again Button
                Button(
                    onClick = { /* שלם שוב */ },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CardifyColors.ScanButton
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Text(
                        text = "Pay Again",
                        fontSize = 12.sp,
                        color = Color.Black,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Menu Icon
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More options",
                    tint = Color.Gray,
                    modifier = Modifier
                        .size(20.dp)
                        .clickable { /* תפריט אופציות */ }
                )
            }
        }
    }
}

// ======= Bottom Navigation =======
@Composable
fun BottomNavigationBar(
    selectedTab: Int,
    onNavigateToHome: () -> Unit,
    onNavigateToWallet: () -> Unit,
    onNavigateToStats: () -> Unit,
    onNavigateToAccount: () -> Unit
) {
    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 8.dp,
        modifier = Modifier.height(70.dp)
    ) {
        val items = listOf(
            NavItem("Home", Icons.Default.Home, 0, onNavigateToHome),
            NavItem("Activity", Icons.Default.List, 1, {}),
            NavItem("Wallet", Icons.Default.AccountBalanceWallet, 2, onNavigateToWallet),
            NavItem("Stats", Icons.Default.BarChart, 3, onNavigateToStats),
            NavItem("Account", Icons.Default.Person, 4, onNavigateToAccount)
        )

        items.forEach { item ->
            NavigationBarItem(
                selected = selectedTab == item.index,
                onClick = item.onClick,
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        fontSize = 11.sp
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = CardifyColors.Primary,
                    selectedTextColor = CardifyColors.Primary,
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray,
                    indicatorColor = CardifyColors.Primary.copy(alpha = 0.1f)
                )
            )
        }
    }
}

// ======= נתונים =======
data class NavItem(
    val label: String,
    val icon: ImageVector,
    val index: Int,
    val onClick: () -> Unit
)

data class Transaction(
    val id: String,
    val title: String,
    val date: String,
    val amount: Double,
    val status: TransactionStatus,
    val icon: ImageVector,
    val category: String
)

enum class TransactionStatus(val displayName: String) {
    SUCCESS("Success"),
    CANCELLED("Cancelled"),
    PENDING("Pending")
}

object CardifyColors {
    val Primary = Color(0xFF0D7377)
    val PrimaryLight = Color(0xFF14FFEC)
    val ScanButton = Color(0xFFA0FF9D)
}

// ======= פונקציות עזר =======
fun formatCurrency(amount: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale.US)
    return format.format(amount)
}

// ======= נתונים פיקטיביים =======
fun getSampleTransactions(): List<Transaction> {
    return listOf(
        Transaction(
            id = "1",
            title = "Electricity Bills",
            date = "Jan 24th, 2024",
            amount = 1178.00,
            status = TransactionStatus.SUCCESS,
            icon = Icons.Default.ElectricBolt,
            category = "Bills"
        ),
        Transaction(
            id = "2",
            title = "Health Insurance",
            date = "Jan 21st, 2024",
            amount = 532.00,
            status = TransactionStatus.SUCCESS,
            icon = Icons.Default.LocalHospital,
            category = "Insurance"
        ),
        Transaction(
            id = "3",
            title = "Travel & Hotel",
            date = "Jan 19th, 2024",
            amount = 2754.00,
            status = TransactionStatus.CANCELLED,
            icon = Icons.Default.Flight,
            category = "Travel"
        ),
        Transaction(
            id = "4",
            title = "Debit & Credit Card",
            date = "Jan 18th, 2024",
            amount = 1245.00,
            status = TransactionStatus.SUCCESS,
            icon = Icons.Default.CreditCard,
            category = "Banking"
        ),
        Transaction(
            id = "5",
            title = "Internet Service",
            date = "Jan 15th, 2024",
            amount = 89.99,
            status = TransactionStatus.SUCCESS,
            icon = Icons.Default.Wifi,
            category = "Bills"
        ),
        Transaction(
            id = "6",
            title = "Grocery Shopping",
            date = "Jan 12th, 2024",
            amount = 456.50,
            status = TransactionStatus.SUCCESS,
            icon = Icons.Default.ShoppingCart,
            category = "Shopping"
        ),
        Transaction(
            id = "7",
            title = "Gas Station",
            date = "Jan 10th, 2024",
            amount = 78.00,
            status = TransactionStatus.SUCCESS,
            icon = Icons.Default.LocalGasStation,
            category = "Transport"
        ),
        Transaction(
            id = "8",
            title = "Restaurant",
            date = "Jan 8th, 2024",
            amount = 125.00,
            status = TransactionStatus.PENDING,
            icon = Icons.Default.Restaurant,
            category = "Food"
        )
    )
}