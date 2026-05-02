package com.cardify.app.ui.navigation

import androidx.compose.runtime.*
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.gson.Gson
import com.cardify.app.data.UserSession
import com.cardify.app.data.model.Chat
import com.cardify.app.data.model.ChatTransaction
import com.cardify.app.ui.home.HomeScreen
import com.cardify.app.ui.account.AccountScreen
import com.cardify.app.ui.edit_account.EditAccountScreen
import com.cardify.app.ui.shared_info.SharedInfoScreen
import com.cardify.app.ui.shared_info.ShareWithFriendsScreen
import com.cardify.app.ui.transactions.TransactionsScreen
import com.cardify.app.ui.stats.StatsScreen
import com.cardify.app.ui.chat.ChatsScreen
import com.cardify.app.ui.chat.ChatScreen

@Composable
fun AppNavigation(startDestination: String = "home") {
    val navController = rememberNavController()
    val gson = remember { Gson() }

    NavHost(navController = navController, startDestination = startDestination) {

        composable("home") {
            HomeScreen(
                onNavigate = { route ->
                    if (route != "home") navController.navigate(route) {
                        popUpTo("home") { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onShareClick = { transaction ->
                    navController.navigate("share_with_friends/${transaction.id}")
                }
            )
        }

        composable(
            route = "share_with_friends/{transactionId}",
            arguments = listOf(navArgument("transactionId") { type = NavType.StringType })
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

        // wallet = ChatsScreen
        composable("wallet") {
            val entry       = navController.currentBackStackEntry
            val pendingJson = entry?.savedStateHandle?.get<String>("pendingTransaction")
            val pendingTxn  = pendingJson?.let {
                try { gson.fromJson(it, ChatTransaction::class.java) } catch (e: Exception) { null }
            }

            ChatsScreen(
                onNavigate = { route ->
                    if (route != "wallet") navController.navigate(route) {
                        popUpTo("home") { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onOpenChat = { chat ->
                    // חישוב שם הצ'אט — שם המשתמש השני בפרטי, שם הקבוצה בקבוצתי
                    val chatName = if (chat.isGroup) {
                        chat.groupName.ifBlank { "Group" }
                    } else {
                        val otherId = chat.participants.firstOrNull {
                            it != (UserSession.userId ?: "")
                        } ?: ""
                        chat.participantNames[otherId] ?: "Chat"
                    }

                    // ניקוי pending מה-wallet entry
                    navController.currentBackStackEntry
                        ?.savedStateHandle?.remove<String>("pendingTransaction")

                    navController.navigate("chat/${chat.id}") {
                        launchSingleTop = true
                    }

                    // שמירת מידע ב-chat entry
                    navController.getBackStackEntry("chat/${chat.id}")
                        .savedStateHandle.apply {
                            set("pendingTransaction", pendingJson)
                            set("chatName", chatName)
                            set("isGroup", chat.isGroup)
                        }
                },
                pendingTransaction = pendingTxn
            )
        }

        // chat/{chatId}
        composable(
            route = "chat/{chatId}",
            arguments = listOf(navArgument("chatId") { type = NavType.StringType })
        ) { backStackEntry ->
            val chatId      = backStackEntry.arguments?.getString("chatId") ?: ""
            val chatName    = backStackEntry.savedStateHandle.get<String>("chatName") ?: "Chat"
            val isGroup     = backStackEntry.savedStateHandle.get<Boolean>("isGroup") ?: false
            val pendingJson = backStackEntry.savedStateHandle.get<String>("pendingTransaction")
            val pendingTxn  = pendingJson?.let {
                try { gson.fromJson(it, ChatTransaction::class.java) } catch (e: Exception) { null }
            }

            ChatScreen(
                chatId            = chatId,
                chatName          = chatName,
                isGroup           = isGroup,
                onBack            = { navController.popBackStack() },
                initialPendingTxn = pendingTxn
            )
        }

        composable("transactions") {
            TransactionsScreen(
                onNavigate = { route ->
                    if (route != "transactions") navController.navigate(route) {
                        popUpTo("home") { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onShareToChat = { transaction ->
                    val chatTxn = ChatTransaction(
                        businessName = transaction.businessName,
                        amount      = transaction.amount,
                        date        = transaction.date,
                        category    = transaction.category,
                        status      = transaction.status,
                        explanation = transaction.explanation
                    )
                    val json = gson.toJson(chatTxn)
                    navController.navigate("wallet") {
                        popUpTo("home") { inclusive = false }
                        launchSingleTop = true
                    }
                    navController.currentBackStackEntry
                        ?.savedStateHandle?.set("pendingTransaction", json)
                }
            )
        }

        composable("stats") {
            StatsScreen(
                onNavigate = { route ->
                    if (route != "stats") navController.navigate(route) {
                        popUpTo("home") { inclusive = false }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable("account") {
            val context = androidx.compose.ui.platform.LocalContext.current
            AccountScreen(
                onNavigate = { route ->
                    if (route != "account") navController.navigate(route) {
                        launchSingleTop = true
                    }
                },
                onEditProfile = { navController.navigate("edit_account") },
                onLogout = {
                    val intent = android.content.Intent(
                        context, com.cardify.app.ui.login.LoginActivity::class.java
                    )
                    intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                            android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                    context.startActivity(intent)
                }
            )
        }

        composable("edit_account") {
            EditAccountScreen(
                onNavigate = { route ->
                    if (route != "edit_account") navController.navigate(route) {
                        launchSingleTop = true
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
    }
}