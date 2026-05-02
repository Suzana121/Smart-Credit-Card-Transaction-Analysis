package com.cardify.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cardify.app.R
import com.cardify.app.ui.chat.ChatViewModel
import com.cardify.app.ui.home.CardifyColors

val AppTeal = Color(0xFF006769)

sealed class NavigationItem(
    val route: String,
    val icon: ImageVector,
    val label: String
) {
    object Home         : NavigationItem("home",         Icons.Default.Home,        "Home")
    object SharedInfo   : NavigationItem("wallet",       Icons.Default.Description, "Shared Info")
    object Transactions : NavigationItem("transactions", Icons.Default.List,        "Transactions")
    object Stats        : NavigationItem("stats",        Icons.Default.BarChart,    "Stats")
    object Account      : NavigationItem("account",      Icons.Default.Person,      "Account")
}

@Composable
fun CleanTopBar(onAccountClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .background(AppTeal)
            .padding(horizontal = 24.dp)
            .padding(top = 40.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "Cardify",
            color = Color.White,
            fontSize = 42.sp,
            fontFamily = FontFamily(Font(R.font.kelly_slab))
        )
        IconButton(onClick = onAccountClick) {
            Icon(
                Icons.Default.AccountCircle,
                contentDescription = "Account",
                tint = Color.White,
                modifier = Modifier.size(34.dp)
            )
        }
    }
}

@Composable
fun AppScaffold(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    topBarContent: (@Composable () -> Unit)? = null,
    chatViewModel: ChatViewModel = viewModel(),
    content: @Composable (PaddingValues) -> Unit
) {
    val items = listOf(
        NavigationItem.Home,
        NavigationItem.SharedInfo,
        NavigationItem.Transactions,
        NavigationItem.Stats,
        NavigationItem.Account
    )

    val unreadCount by chatViewModel.unreadCount.collectAsState()

    LaunchedEffect(Unit) { chatViewModel.fetchUnreadCount() }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Scaffold(
            topBar = {
                topBarContent?.invoke() ?: CleanTopBar(
                    onAccountClick = { onNavigate("account") }
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = Color.White,
                    tonalElevation = 8.dp
                ) {
                    items.forEach { item ->
                        NavigationBarItem(
                            icon = {
                                // badge על Shared Info
                                if (item.route == "wallet" && unreadCount > 0) {
                                    BadgedBox(
                                        badge = {
                                            Badge(containerColor = CardifyColors.IrregularRed) {
                                                Text(
                                                    if (unreadCount > 9) "9+" else unreadCount.toString(),
                                                    fontSize = 9.sp, color = Color.White
                                                )
                                            }
                                        }
                                    ) {
                                        Icon(item.icon, contentDescription = item.label)
                                    }
                                } else {
                                    Icon(item.icon, contentDescription = item.label)
                                }
                            },
                            label    = { Text(item.label, fontSize = 10.sp, maxLines = 1) },
                            selected = currentRoute == item.route,
                            onClick  = { onNavigate(item.route) },
                            colors   = NavigationBarItemDefaults.colors(
                                selectedIconColor   = AppTeal,
                                selectedTextColor   = AppTeal,
                                unselectedIconColor = Color(0xFF666666).copy(0.6f),
                                unselectedTextColor = Color(0xFF666666).copy(0.6f),
                                indicatorColor      = Color.Transparent
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