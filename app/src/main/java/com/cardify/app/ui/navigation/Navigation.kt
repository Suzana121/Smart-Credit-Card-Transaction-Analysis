package com.cardify.app.ui.navigation

import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.cardify.app.data.model.Transaction
import com.cardify.app.ui.home.HomeScreen
import com.cardify.app.ui.account.AccountScreen
import com.cardify.app.ui.edit_account.EditAccountScreen
import com.cardify.app.ui.shared_info.SharedInfoScreen
import com.cardify.app.ui.shared_info.ShareWithFriendsScreen

@Composable
fun AppNavigation(
    startDestination: String = "home"
) {
    val navController = rememberNavController()
    var selectedTransaction by remember { mutableStateOf<Transaction?>(null) }

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
                    selectedTransaction = transaction
                    navController.navigate("share_with_friends") {
                        launchSingleTop = true
                    }
                }
            )
        }

        // Bottom nav "Shared Info" — standalone screen
        composable("wallet") {
            SharedInfoScreen(
                onNavigate = { route ->
                    if (route != "wallet") {
                        navController.navigate(route) {
                            popUpTo("home") { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                }
            )
        }

        // Opened from Share button on a transaction card
        composable("share_with_friends") {
            ShareWithFriendsScreen(
                transaction = selectedTransaction,
                onNavigate = { route ->
                    navController.navigate(route) {
                        popUpTo("home") { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("activity") { /* ... */ }
        composable("stats") { /* ... */ }

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
    }
}
