package com.cardify.app.ui.navigation

import androidx.compose.runtime.*
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.gson.Gson
import com.cardify.app.data.UserSession
import com.cardify.app.data.model.ChatTransaction
import com.cardify.app.data.model.Friend
import com.cardify.app.ui.chat.ChatViewModel
import com.cardify.app.ui.home.HomeScreen
import com.cardify.app.ui.account.AccountScreen
import com.cardify.app.ui.edit_account.EditAccountScreen
import com.cardify.app.ui.transactions.TransactionsScreen
import com.cardify.app.ui.stats.StatsScreen
import com.cardify.app.ui.chat.ChatsScreen
import com.cardify.app.ui.chat.ChatScreen
import com.cardify.app.ui.components.ShareToChatSheet
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cardify.app.ui.account.AccountViewModel

@Composable
fun AppNavigation(startDestination: String = "home") {
    val navController = rememberNavController()
    val gson          = remember { Gson() }

    var pendingShareTxn    by remember { mutableStateOf<ChatTransaction?>(null) }
    var showFriendPicker   by remember { mutableStateOf(false) }

    val chatViewModelGlobal: ChatViewModel = viewModel()
    val accountViewModelGlobal: AccountViewModel = viewModel()

    fun clearPendingEverywhere() {
        pendingShareTxn  = null
        showFriendPicker = false
        try {
            navController.getBackStackEntry("chat")
                .savedStateHandle.remove<String>("pendingTransaction")
        } catch (_: Exception) {}
    }

    fun navigateToChatWithFriend(friend: Friend, txn: ChatTransaction) {
        chatViewModelGlobal.loadFriends()
        chatViewModelGlobal.createChat(
            participantPhones = listOf(friend.phone),
            groupName         = "",
            onSuccess         = { chatId ->
                val txnJson = gson.toJson(txn)

                try {
                    navController.getBackStackEntry("chat")
                        .savedStateHandle.remove<String>("pendingTransaction")
                } catch (_: Exception) {}

                // מעבר למסך ה-Chats הראשי (כדי שיהיה ב-Backstack) ואז לצ'אט הספציפי
                navController.navigate("chat") {
                    popUpTo("home") { inclusive = false }
                    launchSingleTop = true
                }
                navController.navigate("chat/$chatId") {
                    launchSingleTop = true
                }

                navController.getBackStackEntry("chat/$chatId")
                    .savedStateHandle.apply {
                        set("pendingTransaction", txnJson)
                        set("chatName", friend.name)
                        set("isGroup", false)
                        set("displayNamesJson", "{}")
                    }
            }
        )
        clearPendingEverywhere()
    }

    if (showFriendPicker && pendingShareTxn != null) {
        val friends by chatViewModelGlobal.friends.collectAsState()
        val chats   by chatViewModelGlobal.chats.collectAsState()
        LaunchedEffect(Unit) {
            chatViewModelGlobal.loadFriends()
            chatViewModelGlobal.loadChats()
        }

        val currentUserId = UserSession.userId ?: ""

        ShareToChatSheet(
            friends       = friends,
            chats         = chats,
            currentUserId = currentUserId,
            onDismiss     = { clearPendingEverywhere() },
            onSelect      = { friend -> navigateToChatWithFriend(friend, pendingShareTxn!!) }
        )
    }

    NavHost(navController = navController, startDestination = startDestination) {

        // ─── Home ──────────────────────────────────────────────────────────
        composable("home") {
            HomeScreen(
                navController = navController,
                onNavigate = { route ->
                    if (route != "home") navController.navigate(route) {
                        popUpTo("home") { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onShareClick = { transaction ->
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

        // ─── Chats List (היה wallet בעבר) ──────────────────────────────────
        composable("chat") {
            LaunchedEffect(Unit) {
                pendingShareTxn  = null
                showFriendPicker = false
            }

            val entry       = navController.currentBackStackEntry
            val pendingJson = entry?.savedStateHandle?.get<String>("pendingTransaction")
            val pendingTxn  = pendingJson?.let {
                try { gson.fromJson(it, ChatTransaction::class.java) } catch (e: Exception) { null }
            }

            ChatsScreen(
                navController = navController,
                onNavigate = { route ->
                    if (route != "chat") navController.navigate(route) {
                        popUpTo("home") { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onOpenChat = { chat ->
                    val currentUserId = UserSession.userId ?: ""
                    val chatName = if (chat.isGroup) {
                        chat.groupName.ifBlank { "Group" }
                    } else {
                        val otherId = chat.participants.firstOrNull { it != currentUserId } ?: ""
                        chat.displayNames[otherId]
                            ?: chat.participantNames[otherId]
                            ?: "Chat"
                    }

                    val otherPhone = if (!chat.isGroup) {
                        val otherId = chat.participants.firstOrNull { it != currentUserId } ?: ""
                        chatViewModelGlobal.friends.value
                            .firstOrNull { f -> f.name == chat.participantNames[otherId] }?.phone
                            ?: otherId
                    } else ""

                    val displayNamesJson = try { gson.toJson(chat.displayNames) } catch (e: Exception) { "{}" }

                    entry?.savedStateHandle?.remove<String>("pendingTransaction")

                    navController.navigate("chat/${chat.id}") { launchSingleTop = true }

                    navController.getBackStackEntry("chat/${chat.id}")
                        .savedStateHandle.apply {
                            set("pendingTransaction", pendingJson)
                            set("chatName", chatName)
                            set("isGroup", chat.isGroup)
                            set("displayNamesJson", displayNamesJson)
                            set("otherPhone", otherPhone)
                        }
                },
                pendingTransaction = pendingTxn
            )
        }

        // ─── Single Chat Room ──────────────────────────────────────────────
        composable(
            route = "chat/{chatId}",
            arguments = listOf(navArgument("chatId") { type = NavType.StringType })
        ) { backStackEntry ->
            val chatId           = backStackEntry.arguments?.getString("chatId") ?: ""
            val chatName         = backStackEntry.savedStateHandle.get<String>("chatName") ?: "Chat"
            val isGroup          = backStackEntry.savedStateHandle.get<Boolean>("isGroup") ?: false
            val pendingJson      = backStackEntry.savedStateHandle.get<String>("pendingTransaction")
            val displayNamesJson = backStackEntry.savedStateHandle.get<String>("displayNamesJson") ?: "{}"
            val otherPhone       = backStackEntry.savedStateHandle.get<String>("otherPhone") ?: ""

            val pendingTxn = pendingJson?.let {
                try { gson.fromJson(it, ChatTransaction::class.java) } catch (e: Exception) { null }
            }

            @Suppress("UNCHECKED_CAST")
            val displayNames: Map<String, String> = try {
                gson.fromJson(displayNamesJson, Map::class.java) as Map<String, String>
            } catch (e: Exception) { emptyMap() }

            ChatScreen(
                chatId            = chatId,
                chatName          = chatName,
                isGroup           = isGroup,
                displayNames      = displayNames,
                otherPhone        = otherPhone,
                onSetNickname     = { phone, nickname ->
                    accountViewModelGlobal.setNickname(phone, nickname)
                },
                onTransactionSent = {
                    backStackEntry.savedStateHandle.remove<String>("pendingTransaction")
                    try {
                        navController.getBackStackEntry("chat")
                            .savedStateHandle.remove<String>("pendingTransaction")
                    } catch (_: Exception) {}
                },
                onNavigateToChat  = { targetChat ->
                    navController.navigate("chat/${targetChat.id}") { launchSingleTop = true }
                },
                onBack            = { navController.popBackStack() },
                initialPendingTxn = pendingTxn
            )
        }

        // ─── Transactions ───────────────────────────────────────────────────
        composable("transactions") {
            TransactionsScreen(
                navController = navController,
                onNavigate = { route ->
                    if (route != "transactions") navController.navigate(route) {
                        popUpTo("home") { inclusive = false }
                        launchSingleTop = true
                    }
                }
            )
        }

        // ─── Stats ──────────────────────────────────────────────────────────
        composable("stats") {
            StatsScreen(
                navController = navController,
                onNavigate = { route ->
                    if (route != "stats") navController.navigate(route) {
                        popUpTo("home") { inclusive = false }
                        launchSingleTop = true
                    }
                }
            )
        }

        // ─── Account ────────────────────────────────────────────────────────
        composable("account") {
            val context = androidx.compose.ui.platform.LocalContext.current
            AccountScreen(
                navController = navController,
                onNavigate = { route ->
                    if (route != "account") navController.navigate(route) {
                        popUpTo("home") { inclusive = false }
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

        // ─── Edit Account ───────────────────────────────────────────────────
        composable("edit_account") {
            EditAccountScreen(
                navController = navController,
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