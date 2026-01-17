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
        // === מסך הבית ===
        composable("home") {
            HomeScreen(
                onNavigate = { route ->
                    if (route != "home") { // מונע טעינה מחדש אם אנחנו כבר בבית
                        navController.navigate(route) {
                            popUpTo("home") { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                }
            )
        }

        // === מסך הטרנזקציות (Activity) - התיקון כאן ===
        composable("activity") {
            ActivityScreen(
                // עכשיו מעבירים רק פונקציה אחת שמקבלת את שם המסך (route)
                onNavigate = { route ->
                    if (route != "activity") {
                        navController.navigate(route) {
                            // כשחוזרים לבית, לא משאירים את ההיסטוריה פתוחה
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

        composable("stats") {
            // TODO: StatsScreen - ניצור בהמשך
        }

        composable("account") {
            // TODO: AccountScreen - ניצור בהמשך
        }
    }
}