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
import com.cardify.app.data.model.Friend
import com.cardify.app.ui.chat.ChatViewModel
import com.cardify.app.ui.home.HomeScreen
import com.cardify.app.ui.account.AccountScreen
import com.cardify.app.ui.edit_account.EditAccountScreen
import com.cardify.app.ui.shared_info.ShareWithFriendsScreen
import com.cardify.app.ui.transactions.TransactionsScreen
import com.cardify.app.ui.stats.StatsScreen
import com.cardify.app.ui.chat.ChatsScreen
import com.cardify.app.ui.chat.ChatScreen
import com.cardify.app.ui.components.ShareToChatSheet
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun AppNavigation(startDestination: String = "home") {
    val navController = rememberNavController()
    val gson          = remember { Gson() }

    // ─── מצב שיתוף גלובלי ────────────────────────────────────────────────────
    // כאשר המשתמש לוחץ Share to Chat ממסך כלשהו, שומרים את הטרנזקציה כאן
    // ופותחים bottom sheet לבחירת חבר, לפני הניווט לצ'ט.
    var pendingShareTxn    by remember { mutableStateOf<ChatTransaction?>(null) }
    var showFriendPicker   by remember { mutableStateOf(false) }
    val chatViewModelGlobal: ChatViewModel = viewModel()

    // ─── helper: navigate to chat with a specific friend + pending transaction ─
    fun navigateToChatWithFriend(friend: Friend, txn: ChatTransaction) {
        chatViewModelGlobal.loadFriends()
        chatViewModelGlobal.createChat(
            participantPhones = listOf(friend.phone),
            groupName         = "",
            onSuccess         = { chatId ->
                val txnJson = gson.toJson(txn)
                navController.navigate("wallet") {
                    popUpTo("home") { inclusive = false }
                    launchSingleTop = true
                }
                // קצת delay לאחר ניווט כדי שה-backStack יהיה מוכן
                navController.currentBackStackEntry
                    ?.savedStateHandle?.set("pendingTransaction", txnJson)
                // ניווט ישיר לצ'ט
                navController.navigate("chat/$chatId") {
                    launchSingleTop = true
                }
                navController.getBackStackEntry("chat/$chatId")
                    .savedStateHandle.apply {
                        set("pendingTransaction", txnJson)
                        set("chatName", friend.name)
                        set("isGroup", false)
                    }
            }
        )
        pendingShareTxn  = null
        showFriendPicker = false
    }

    // ─── Friend picker sheet (גלובלי, מעל כל המסכים) ─────────────────────────
    if (showFriendPicker && pendingShareTxn != null) {
        val friends by chatViewModelGlobal.friends.collectAsState()
        LaunchedEffect(Unit) { chatViewModelGlobal.loadFriends() }

        ShareToChatSheet(
            friends   = friends,
            onDismiss = { showFriendPicker = false; pendingShareTxn = null },
            onSelect  = { friend ->
                navigateToChatWithFriend(friend, pendingShareTxn!!)
            }
        )
    }

    NavHost(navController = navController, startDestination = startDestination) {

        // ─── Home ─────────────────────────────────────────────────────────────
        composable("home") {
            HomeScreen(
                onNavigate = { route ->
                    if (route != "home") navController.navigate(route) {
                        popUpTo("home") { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onShareClick = { transaction ->
                    // Share to Chat מהבית — פותח את picker הגלובלי
                    pendingShareTxn = ChatTransaction(
                        businessName = transaction.businessName,
                        amount       = transaction.amount,
                        date         = transaction.date,
                        category     = transaction.category,
                        status       = transaction.status,
                        explanation  = transaction.explanation ?: ""
                    )
                    showFriendPicker = true
                }
            )
        }

        // ─── Share with Friends (legacy) ──────────────────────────────────────
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

        // ─── Chats (wallet) ───────────────────────────────────────────────────
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
                    val chatName = if (chat.isGroup) {
                        chat.groupName.ifBlank { "Group" }
                    } else {
                        val otherId = chat.participants.firstOrNull {
                            it != (UserSession.userId ?: "")
                        } ?: ""
                        chat.participantNames[otherId] ?: "Chat"
                    }

                    navController.currentBackStackEntry
                        ?.savedStateHandle?.remove<String>("pendingTransaction")

                    navController.navigate("chat/${chat.id}") {
                        launchSingleTop = true
                    }

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

        // ─── Chat ─────────────────────────────────────────────────────────────
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

        // ─── Transactions ─────────────────────────────────────────────────────
        composable("transactions") {
            TransactionsScreen(
                onNavigate = { route ->
                    if (route != "transactions") navController.navigate(route) {
                        popUpTo("home") { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onShareToChat = { transaction ->
                    // Share to Chat מהטרנזקציות — אותו picker גלובלי
                    pendingShareTxn = ChatTransaction(
                        businessName = transaction.businessName,
                        amount       = transaction.amount,
                        date         = transaction.date,
                        category     = transaction.category,
                        status       = transaction.status,
                        explanation  = transaction.explanation ?: ""
                    )
                    showFriendPicker = true
                }
            )
        }

        // ─── Stats ────────────────────────────────────────────────────────────
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

        // ─── Account ──────────────────────────────────────────────────────────
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

        // ─── Edit Account ─────────────────────────────────────────────────────
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