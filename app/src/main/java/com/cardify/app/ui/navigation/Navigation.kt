package com.cardify.app.ui.navigation

import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.cardify.app.ui.activity.ActivityScreen
import com.cardify.app.ui.home.HomeScreen
import com.cardify.app.ui.account.AccountScreen
import com.cardify.app.ui.stats1.StatsScreen     // ייבוא המסך מהתיקייה החדשה
import com.cardify.app.ui.stats1.StatsViewModel  // ייבוא ה-ViewModel מהתיקייה החדשה

@Composable
fun AppNavigation(
    startDestination: String = "home"
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // === מסך הבית ===
        composable("home") {
            HomeScreen(
                onNavigate = { route ->
                    if (route != "home") {
                        navController.navigate(route) {
                            popUpTo("home") { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                }
            )
        }

        // === מסך הטרנזקציות (Activity) ===
        composable("activity") {
            ActivityScreen(
                onNavigate = { route ->
                    if (route != "activity") {
                        navController.navigate(route) {
                            if (route == "home") {
                                popUpTo("home") { inclusive = false }
                            }
                            launchSingleTop = true
                        }
                    }
                }
            )
        }

        composable("wallet") {
            // TODO: WalletScreen - ניצור בהמשך
        }

        // === מסך הסטטיסטיקות (Stats) - מעודכן ===
        composable("stats") {
            // יצירת ה-ViewModel עבור המסך
            val statsViewModel: StatsViewModel = viewModel()

            StatsScreen(
                viewModel = statsViewModel,
                currentRoute = "stats",
                onNavigate = { route ->
                    if (route != "stats") {
                        navController.navigate(route) {
                            launchSingleTop = true
                        }
                    }
                }
            )
        }

        // === מסך החשבון (Account) ===
        composable("account") {
            AccountScreen(
                onNavigate = { route ->
                    if (route != "account") {
                        navController.navigate(route) {
                            launchSingleTop = true
                        }
                    }
                },
                onLogout = {
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}