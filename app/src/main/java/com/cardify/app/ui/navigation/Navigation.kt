package com.cardify.app.ui.navigation

import androidx.compose.runtime.*
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.cardify.app.data.UserSession
import com.cardify.app.ui.home.HomeScreen
import com.cardify.app.ui.account.AccountScreen
import com.cardify.app.ui.admin.AdminDashboardScreen
import com.cardify.app.ui.edit_account.EditAccountScreen
import com.cardify.app.ui.shared_info.SharedInfoScreen
import com.cardify.app.ui.shared_info.ShareWithFriendsScreen
import com.cardify.app.ui.stats.StatsScreen
import com.cardify.app.ui.transactions.TransactionsScreen
import com.cardify.app.ui.chat.ChatScreen

/**
 * Root Compose navigation graph for the Cardify application.
 *
 * Defines all in-app destinations and wires up their arguments. The bottom navigation
 * bar uses `popUpTo("home")` so back-stack depth stays shallow when switching tabs.
 *
 * Destinations:
 * - `home` — transaction overview and file upload
 * - `transactions` — searchable/filterable full transaction list
 * - `shared-info` — sent and received transaction shares
 * - `share_with_friends/{transactionId}` — friend picker for sharing a specific transaction
 * - `chat/{shareId}/{friendName}` — per-share chat conversation
 * - `account` — user profile and friends management
 * - `edit_account` — profile editing form
 * - `stats` — spending statistics (or admin dashboard for admin users)
 *
 * @param startDestination The route to display first. Defaults to `"home"`.
 */
@Composable
fun AppNavigation(
    startDestination: String = "home"
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {

        composable("home") {
            HomeScreen(
                onNavigate = { route ->
                    if (route != "home") {
                        navController.navigate(route) {
                            popUpTo("home") { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                },
                onShareClick = { transaction ->
                    navController.navigate("share_with_friends/${transaction.id}")
                }
            )
        }

        composable(
            route = "share_with_friends/{transactionId}",
            arguments = listOf(
                navArgument("transactionId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val txnId = backStackEntry.arguments?.getString("transactionId")
            ShareWithFriendsScreen(
                transactionId = txnId,
                onNavigate = { route ->
                    navController.navigate(route) {
                        popUpTo("home") { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable("shared-info") {
            SharedInfoScreen(
                onNavigate = { route ->
                    if (route != "shared-info") {
                        navController.navigate(route) {
                            popUpTo("home") { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                }
            )
        }

        composable("transactions") {
            TransactionsScreen(
                onNavigate = { route ->
                    if (route != "transactions") {
                        navController.navigate(route) {
                            popUpTo("home") { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                }
            )
        }

        composable("account") {
            val context = androidx.compose.ui.platform.LocalContext.current
            AccountScreen(
                onNavigate = { route ->
                    if (route != "account") {
                        navController.navigate(route) { launchSingleTop = true }
                    }
                },
                onEditProfile = { navController.navigate("edit_account") },
                onLogout = {
                    val intent = android.content.Intent(context, com.cardify.app.ui.login.LoginActivity::class.java)
                    intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                    context.startActivity(intent)
                }
            )
        }

        composable("edit_account") {
            EditAccountScreen(
                onNavigate = { route ->
                    if (route != "edit_account") {
                        navController.navigate(route) { launchSingleTop = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "chat/{shareId}/{friendName}",
            arguments = listOf(
                navArgument("shareId") { type = NavType.StringType },
                navArgument("friendName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val shareId = backStackEntry.arguments?.getString("shareId") ?: ""
            val friendName = backStackEntry.arguments?.getString("friendName") ?: ""
            ChatScreen(
                shareId = shareId,
                friendName = friendName,
                onBack = { navController.popBackStack() }
            )
        }

        // Admin users see the admin dashboard; regular users see the stats screen.
        composable("stats") {
            if (UserSession.isAdmin()) {
                AdminDashboardScreen(
                    onNavigate = { route ->
                        if (route != "stats") {
                            navController.navigate(route) {
                                popUpTo("home") { inclusive = false }
                                launchSingleTop = true
                            }
                        }
                    }
                )
            } else {
                StatsScreen(
                    onNavigate = { route ->
                        if (route != "stats") {
                            navController.navigate(route) {
                                popUpTo("home") { inclusive = false }
                                launchSingleTop = true
                            }
                        }
                    }
                )
            }
        }
    }
}
