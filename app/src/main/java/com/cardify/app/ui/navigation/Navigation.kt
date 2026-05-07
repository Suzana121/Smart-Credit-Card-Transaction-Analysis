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
        // נקה גם מה-savedStateHandle של wallet
        try {
            navController.getBackStackEntry("wallet")
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

                // נקה wallet לפני ניווט
                try {
                    navController.getBackStackEntry("wallet")
                        .savedStateHandle.remove<String>("pendingTransaction")
                } catch (_: Exception) {}

                navController.navigate("wallet") {
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
        LaunchedEffect(Unit) { chatViewModelGlobal.loadFriends(); chatViewModelGlobal.loadChats() }

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
            // נקה pending גלובלי בכניסה למסך
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
                onNavigate = { route ->
                    if (route != "wallet") navController.navigate(route) {
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
                        // מנסה לאתר טלפון מרשימת החברים — תמיד, גם אם הרשימה עדיין נטענת
                        chatViewModelGlobal.friends.value
                            .firstOrNull { f -> f.name == chat.participantNames[otherId] }?.phone
                            ?: otherId  // fallback ל-userId — מספיק כדי שכפתור העט יופיע
                    } else ""

                    val displayNamesJson = try { gson.toJson(chat.displayNames) } catch (e: Exception) { "{}" }

                    // ── נקה את ה-pending מ-wallet לפני הניווט לצ'ט ──
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

        // ─── Chat ─────────────────────────────────────────────────────────────
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
                    // נקה מה-savedStateHandle של הצ'ט הנוכחי
                    backStackEntry.savedStateHandle.remove<String>("pendingTransaction")
                    // נקה גם מ-wallet אם קיים
                    try {
                        navController.getBackStackEntry("wallet")
                            .savedStateHandle.remove<String>("pendingTransaction")
                    } catch (_: Exception) {}
                },
                onNavigateToChat  = { targetChat ->
                    val currentUserId = UserSession.userId ?: ""
                    val targetName = if (targetChat.isGroup) {
                        targetChat.groupName.ifBlank { "Group" }
                    } else {
                        val otherId = targetChat.participants.firstOrNull { it != currentUserId } ?: ""
                        targetChat.displayNames[otherId]
                            ?: targetChat.participantNames[otherId]
                            ?: "Chat"
                    }
                    val targetDisplayNamesJson = try {
                        gson.toJson(targetChat.displayNames)
                    } catch (e: Exception) { "{}" }

                    navController.navigate("chat/${targetChat.id}") { launchSingleTop = true }
                    navController.getBackStackEntry("chat/${targetChat.id}")
                        .savedStateHandle.apply {
                            set("chatName",         targetName)
                            set("isGroup",          targetChat.isGroup)
                            set("displayNamesJson", targetDisplayNamesJson)
                            set("otherPhone",       "")
                        }
                },
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