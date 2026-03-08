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

        // === שאר המסכים (activity, stats...) נשארים כאן ===
        composable("activity") { /* ... */ }
        composable("stats") { /* ... */ }

        // === מסך החשבון (Account) ===
        composable("account") {
            val context = androidx.compose.ui.platform.LocalContext.current
            AccountScreen(
                onNavigate = { route ->
                    if (route != "account") {
                        navController.navigate(route) { launchSingleTop = true }
                    }
                },
                onLogout = {
                    // התיקון להתנתקות: סוגרים את ה-MainActivity ופותחים את ה-LoginActivity
                    val intent = android.content.Intent(context, com.cardify.app.ui.login.LoginActivity::class.java)
                    intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                    context.startActivity(intent)
                }
            )
        }
    }
}