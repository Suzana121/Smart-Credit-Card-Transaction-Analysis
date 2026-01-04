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

// Navigation Items
sealed class NavigationItem(
    val route: String,
    val icon: ImageVector,
    val label: String
) {
    object Activity : NavigationItem("activity", Icons.Default.TrendingUp, "Activity")
    object Wallet : NavigationItem("wallet", Icons.Default.Wallet, "Wallet")
    object Stats : NavigationItem("stats", Icons.Default.BarChart, "Stats")
    object Account : NavigationItem("account", Icons.Default.Person, "Account")
    object Home : NavigationItem("home", Icons.Default.Home, "Home")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(
    title: String,
    currentRoute: String,
    onNavigate: (String) -> Unit,
    showBackButton: Boolean = false,
    onBackClick: () -> Unit = {},
    topBarContent: @Composable () -> Unit = {},
    useCustomTopBar: Boolean = false,
    content: @Composable (PaddingValues) -> Unit
) {
    val items = listOf(
        NavigationItem.Activity,
        NavigationItem.Wallet,
        NavigationItem.Stats,
        NavigationItem.Account,
        NavigationItem.Home
    )

    Scaffold(
        topBar = {
            if (useCustomTopBar) {
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
                        containerColor = Color(0xFF0D7377) // הירוק שלך
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
                                fontSize = 11.sp
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
