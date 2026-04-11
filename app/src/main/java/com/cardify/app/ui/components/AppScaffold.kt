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
import com.cardify.app.R

/** Primary teal brand colour used across shared UI components. */
val AppTeal = Color(0xFF006769)

/**
 * Sealed class representing a bottom navigation bar destination.
 *
 * Each subclass defines the [route], [icon], and [label] for its tab.
 */
sealed class NavigationItem(
    /** Compose navigation route string for this destination. */
    val route: String,
    /** Material icon shown in the bottom bar tab. */
    val icon: ImageVector,
    /** Short label displayed beneath the icon. */
    val label: String
) {
    /** Home screen — transaction overview. */
    object Home         : NavigationItem("home",         Icons.Default.Home,        "Home")

    /** Shared-info screen — sent and received shares. */
    object SharedInfo   : NavigationItem("shared-info",  Icons.Default.Description, "Shared Info")

    /** Transactions screen — full filterable transaction list. */
    object Transactions : NavigationItem("transactions", Icons.Default.List,        "Transactions")

    /** Stats screen — spending analytics (or admin dashboard for admins). */
    object Stats        : NavigationItem("stats",        Icons.Default.BarChart,    "Stats")

    /** Account screen — user profile and friends. */
    object Account      : NavigationItem("account",      Icons.Default.Person,      "Account")
}

/**
 * Shared top app bar showing the Cardify logo and an account icon button.
 *
 * @param onAccountClick Called when the account icon in the top-right corner is tapped.
 */
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

/**
 * Scaffold wrapper used by every main screen in the app.
 *
 * Provides a consistent [CleanTopBar] (or an optional custom top bar), a bottom
 * navigation bar with all five main destinations, and enforces LTR layout direction
 * for consistency across locales.
 *
 * @param currentRoute The route of the currently active destination, used to highlight
 *   the correct bottom nav item.
 * @param onNavigate Called with the destination route when a bottom nav item is tapped.
 * @param topBarContent Optional override for the top bar. When provided it replaces the
 *   default [CleanTopBar]. Pass `null` (default) to use the standard top bar.
 * @param content The screen content. Receives [PaddingValues] that account for the
 *   top and bottom bars and must be applied by the caller.
 */
@Composable
fun AppScaffold(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    topBarContent: (@Composable () -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit
) {
    val items = listOf(
        NavigationItem.Home,
        NavigationItem.SharedInfo,
        NavigationItem.Transactions,
        NavigationItem.Stats,
        NavigationItem.Account
    )

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
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label, fontSize = 10.sp, maxLines = 1) },
                            selected = currentRoute == item.route,
                            onClick = { onNavigate(item.route) },
                            colors = NavigationBarItemDefaults.colors(
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
