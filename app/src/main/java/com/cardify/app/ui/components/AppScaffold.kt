package com.cardify.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

// Navigation Items - מעודכן לפי העיצוב החדש
sealed class NavigationItem(
    val route: String,
    val icon: ImageVector,
    val label: String
) {
    // 1. Home (בית) - ראשון משמאל
    object Home : NavigationItem("home", Icons.Default.Home, "Home")

    // 2. Shared Info (במקום Wallet)
    // הערה: שמרתי על ה-route כ-"wallet" בינתיים כדי לא לשבור קישורים, אבל שיניתי את התצוגה
    object SharedInfo : NavigationItem("wallet", Icons.Default.Description, "Shared Info")

    // 3. Transactions (במקום Activity)
    object Transactions : NavigationItem("activity", Icons.Default.List, "Transactions")

    // 4. Stats
    object Stats : NavigationItem("stats", Icons.Default.BarChart, "Stats")

    // 5. Account (חשבון) - אחרון מימין
    object Account : NavigationItem("account", Icons.Default.Person, "Account")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(
    title: String,
    currentRoute: String,
    onNavigate: (String) -> Unit,
    showBackButton: Boolean = false,
    onBackClick: () -> Unit = {},
    topBarContent: (@Composable () -> Unit)? = null,
    useCustomTopBar: Boolean = false,
    content: @Composable (PaddingValues) -> Unit
) {
    // === כאן קובעים את הסדר בסרגל למטה ===
    // סידרתי אותם משמאל לימין לפי העיצוב החדש
    val items = listOf(
        NavigationItem.Home,          // שמאל
        NavigationItem.SharedInfo,
        NavigationItem.Transactions,
        NavigationItem.Stats,
        NavigationItem.Account        // ימין
    )

    // עוטפים ב-LTR כדי שהסדר ישמר משמאל לימין גם במכשיר בעברית
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Scaffold(
            topBar = {
                if (useCustomTopBar && topBarContent != null) {
                    topBarContent()
                } else {
                    CenterAlignedTopAppBar(
                        title = {
                            Text(
                                text = title,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        },
                        navigationIcon = {
                            if (showBackButton) {
                                IconButton(onClick = onBackClick) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowBack,
                                        contentDescription = "Back",
                                        tint = Color.White
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = Color(0xFF0D7377)
                        )
                    )
                }
            },
            bottomBar = {
                NavigationBar(
                    containerColor = Color.White,
                    tonalElevation = 8.dp
                ) {
                    items.forEach { item ->
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label
                                )
                            },
                            label = {
                                Text(
                                    text = item.label,
                                    fontSize = 10.sp, // הקטנתי טיפה שייכנס יפה
                                    maxLines = 1
                                )
                            },
                            selected = currentRoute == item.route,
                            onClick = { onNavigate(item.route) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFF0D7377),
                                selectedTextColor = Color(0xFF0D7377),
                                unselectedIconColor = Color(0xFF666666).copy(0.6f),
                                unselectedTextColor = Color(0xFF666666).copy(0.6f),
                                indicatorColor = Color.Transparent
                            )
                        )
                    }
                }
            }
        ) { paddingValues ->
            content(paddingValues)
        }
    }
}