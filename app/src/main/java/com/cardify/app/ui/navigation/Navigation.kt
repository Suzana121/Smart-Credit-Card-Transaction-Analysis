package com.cardify.app.ui.navigation

import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.cardify.app.ui.activity.ActivityScreen
import com.cardify.app.ui.home.HomeScreen

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
                    navController.navigate(route) {
                        popUpTo("home") { inclusive = false }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable("activity") {
            ActivityScreen(
                onNavigateToHome = {
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onNavigateToWallet = {
                    navController.navigate("wallet") {
                        launchSingleTop = true
                    }
                },
                onNavigateToStats = {
                    navController.navigate("stats") {
                        launchSingleTop = true
                    }
                },
                onNavigateToAccount = {
                    navController.navigate("account") {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable("wallet") {
            // TODO: WalletScreen - ניצור בהמשך
        }

        composable("stats") {
            // TODO: StatsScreen - ניצור בהמשך
        }

        composable("account") {
            // TODO: AccountScreen - ניצור בהמשך
        }
    }
}
