package com.cardify.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.cardify.app.R
import com.cardify.app.ui.chat.ChatViewModel
import com.cardify.app.ui.home.CardifyColors

val AppTeal = Color(0xFF006769)

sealed class NavigationItem(
    val route: String,
    val iconRes: Int,
    val label: String
) {
    object Home         : NavigationItem("home",         R.drawable.home,         "Home")
    object Chat         : NavigationItem("chat",         R.drawable.message,      "Chat")
    object Transactions : NavigationItem("transactions", R.drawable.transacions, "Transactions")
    object Stats        : NavigationItem("stats",        R.drawable.stats,        "Stats")
    object Account      : NavigationItem("account",      R.drawable.account,      "Account")
}

@Composable
fun CleanTopBar(onAccountClick: () -> Unit = {}) {
    Surface(
        color = AppTeal, // שימוש בצבע שהגדרת במפורש
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .statusBarsPadding() // שומר על רווח מסרגל הסוללה/שעון
                .height(70.dp)
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Cardify",
                color = Color.White,
                fontSize = 32.sp,
                fontFamily = FontFamily(Font(R.font.kelly_slab))
            )
            IconButton(onClick = onAccountClick) {
                Icon(
                    Icons.Default.AccountCircle,
                    contentDescription = "Account",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}
@Composable
fun AppScaffold(
    navController: NavHostController, // זה הפרמטר שחייב לעבור
    onNavigate: (String) -> Unit,
    topBarContent: (@Composable () -> Unit)? = null,
    chatViewModel: ChatViewModel = viewModel(),
    content: @Composable (PaddingValues) -> Unit
) {
    val items = listOf(
        NavigationItem.Home,
        NavigationItem.Chat,
        NavigationItem.Transactions,
        NavigationItem.Stats,
        NavigationItem.Account
    )
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "home"
    val unreadCount by chatViewModel.unreadCount.collectAsState()
    val activeColor = AppTeal

    // חישוב מיקום האנימציה
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val tabWidth = screenWidth / items.size

    // מציאת האינדקס (חשוב שזה יתעדכן בכל שינוי של currentRoute)
    val selectedIndex = remember(currentRoute) {
        items.indexOfFirst { item ->
            currentRoute == item.route || currentRoute.startsWith("${item.route}/")
        }.coerceAtLeast(0)
    }

    // אנימציה חלקה של ה-Offset
    val indicatorOffset by animateDpAsState(
        targetValue = tabWidth * selectedIndex,
        animationSpec = spring(stiffness = 500f, dampingRatio = 0.8f),
        label = "IndicatorAnimation"
    )

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Scaffold(
            topBar = {
                topBarContent?.invoke() ?: CleanTopBar(onAccountClick = { onNavigate("account") })
            },
            bottomBar = {
                Surface(
                    color = Color.White,
                    shadowElevation = 20.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Box שמכיל את האינדיקטור והתפריט אחד על השני
                    Box(modifier = Modifier.navigationBarsPadding().height(90.dp)) {

                        // האינדיקטור שזז באנימציה - נמצא בחלק העליון
                        Box(
                            modifier = Modifier
                                .offset(x = indicatorOffset)
                                .width(tabWidth)
                                .height(6.dp)
                                .background(
                                    color = activeColor,
                                    shape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)
                                )
                        )

                        NavigationBar(
                            containerColor = Color.Transparent,
                            modifier = Modifier.fillMaxSize(),
                            tonalElevation = 0.dp
                        ) {
                            items.forEach { item ->
                                val isSelected = currentRoute == item.route || currentRoute.startsWith("${item.route}/")

                                NavigationBarItem(
                                    selected = isSelected,
                                    onClick = { onNavigate(item.route) },
                                    icon = {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Spacer(modifier = Modifier.height(12.dp))
                                            BadgedBox(
                                                badge = {
                                                    if (item.route == "chat" && unreadCount > 0) {
                                                        Badge(containerColor = CardifyColors.IrregularRed) {
                                                            Text(
                                                                if (unreadCount > 9) "9+" else unreadCount.toString(),
                                                                fontSize = 9.sp, color = Color.White
                                                            )
                                                        }
                                                    }
                                                }
                                            ) {
                                                Icon(
                                                    painter = painterResource(id = item.iconRes),
                                                    contentDescription = item.label,
                                                    modifier = Modifier.size(26.dp)
                                                )
                                            }
                                        }
                                    },
                                    label = {
                                        Text(
                                            item.label,
                                            fontSize = 12.sp,
                                            color = if (isSelected) activeColor else Color.Black
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        indicatorColor = Color.Transparent,
                                        selectedIconColor = activeColor,
                                        unselectedIconColor = Color.Gray
                                    )
                                )
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