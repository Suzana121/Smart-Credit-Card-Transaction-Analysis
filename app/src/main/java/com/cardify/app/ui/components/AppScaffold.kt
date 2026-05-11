package com.cardify.app.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.cardify.app.R
import com.cardify.app.ui.chat.ChatViewModel

sealed class NavigationItem(val route: String, val iconRes: Int, val label: String) {
    object Home         : NavigationItem("home",         R.drawable.home,        "Home")
    object Chat         : NavigationItem("chat",         R.drawable.message,     "Chat")
    object Transactions : NavigationItem("transactions", R.drawable.transacions, "Transacions")
    object Stats        : NavigationItem("stats",        R.drawable.stats,       "Stats")
    object Account      : NavigationItem("account",      R.drawable.account,     "Account")
}

@Composable
fun CleanTopBar() {
    Surface(
        color           = MaterialTheme.colorScheme.primary,
        shadowElevation = 4.dp,
        modifier        = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier              = Modifier.statusBarsPadding().height(70.dp).padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            androidx.compose.foundation.Image(
                painter            = painterResource(R.drawable.logo),
                contentDescription = "Cardify Logo",
                modifier           = Modifier.height(40.dp).wrapContentWidth()
            )
        }
    }
}

@Composable
fun AppScaffold(
    navController:  NavHostController,
    onNavigate:     (String) -> Unit,
    topBarContent:  (@Composable () -> Unit)? = null,
    hideBottomBar:  Boolean = false,           // ← חדש: מסתיר את הסרגל התחתון
    chatViewModel:  ChatViewModel = viewModel(),
    content:        @Composable (PaddingValues) -> Unit
) {
    val items = listOf(
        NavigationItem.Home,
        NavigationItem.Chat,
        NavigationItem.Transactions,
        NavigationItem.Stats,
        NavigationItem.Account
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute      = navBackStackEntry?.destination?.route ?: "home"
    val unreadCount       by chatViewModel.unreadCount.collectAsState()

    val configuration = LocalConfiguration.current
    val tabWidth      = configuration.screenWidthDp.dp / items.size

    val selectedIndex by remember(currentRoute) {
        derivedStateOf {
            items.indexOfFirst { currentRoute.startsWith(it.route) }.coerceAtLeast(0)
        }
    }

    val indicatorOffset by animateDpAsState(
        targetValue   = tabWidth * selectedIndex,
        animationSpec = spring(stiffness = 500f, dampingRatio = 0.8f),
        label         = "IndicatorAnimation"
    )

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Scaffold(
            modifier       = Modifier.fillMaxSize(),
            containerColor = Color(0xFFF5F5F5),
            topBar = {
                Box(modifier = Modifier.fillMaxWidth().zIndex(10f)) {
                    topBarContent?.invoke() ?: CleanTopBar()
                }
            },
            bottomBar = {
                // ─── מוצג רק כשלא בתוך צ'ט ──────────────────────────
                if (!hideBottomBar) {
                    Surface(
                        color           = Color.White,
                        shadowElevation = 20.dp,
                        modifier        = Modifier.fillMaxWidth().zIndex(10f)
                    ) {
                        Box(modifier = Modifier.navigationBarsPadding().height(75.dp)) {
                            Box(
                                modifier = Modifier
                                    .offset(x = indicatorOffset)
                                    .width(tabWidth)
                                    .height(6.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)
                                    )
                            )
                            NavigationBar(
                                containerColor = Color.Transparent,
                                tonalElevation = 0.dp
                            ) {
                                items.forEach { item ->
                                    val isSelected = currentRoute.startsWith(item.route)
                                    NavigationBarItem(
                                        selected = isSelected,
                                        onClick  = { onNavigate(item.route) },
                                        icon = {
                                            BadgedBox(badge = {
                                                if (item.route == "chat" && unreadCount > 0) {
                                                    Badge(containerColor = Color(0xFFDB0000)) {
                                                        Text(
                                                            if (unreadCount > 9) "9+" else unreadCount.toString(),
                                                            fontSize = 9.sp,
                                                            color    = Color.White
                                                        )
                                                    }
                                                }
                                            }) {
                                                Icon(
                                                    painterResource(item.iconRes),
                                                    contentDescription = item.label,
                                                    modifier           = Modifier.size(26.dp)
                                                )
                                            }
                                        },
                                        label = {
                                            Text(
                                                item.label,
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                color    = if (isSelected)
                                                    MaterialTheme.colorScheme.primary
                                                else Color.Gray
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            indicatorColor = Color.Transparent
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        ) { paddingValues ->
            content(paddingValues)
        }
    }
}