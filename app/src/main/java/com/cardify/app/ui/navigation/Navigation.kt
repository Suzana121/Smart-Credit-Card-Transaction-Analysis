package com.cardify.app.ui.navigation

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.gson.Gson
import com.cardify.app.data.UserSession
import com.cardify.app.data.model.ChatTransaction
import com.cardify.app.data.model.Friend
import com.cardify.app.ui.account.AccountScreen
import com.cardify.app.ui.account.AccountViewModel
import com.cardify.app.ui.chat.ChatScreen
import com.cardify.app.ui.chat.ChatViewModel
import com.cardify.app.ui.chat.ChatsScreen
import com.cardify.app.ui.components.ShareToChatSheet
import com.cardify.app.ui.edit_account.EditAccountScreen
import com.cardify.app.ui.home.HomeScreen
import com.cardify.app.ui.stats.StatsScreen
import com.cardify.app.ui.transactions.TransactionsScreen
import com.cardify.app.ui.components.AppScaffold
import androidx.compose.ui.Modifier

@RequiresApi(Build.VERSION_CODES.O)
@SuppressLint("UnusedContentLambdaTargetStateParameter")
@Composable
fun AppNavigation(startDestination: String = "home") {

    val navController = rememberNavController()
    val gson = remember { Gson() }

    val pendingShareTxn = remember { mutableStateOf<ChatTransaction?>(null) }
    var showFriendPicker by remember { mutableStateOf(false) }

    val chatViewModelGlobal: ChatViewModel = viewModel()
    val accountViewModelGlobal: AccountViewModel = viewModel()

    // סדר העמודים לצורך קביעת כיוון האנימציה
    val navigationOrder = listOf("home", "chat", "transactions", "stats", "account")

    // פונקציית עזר לחישוב כיוון האנימציה (ימינה או שמאלה)
    fun getTransitionSpec(initialRoute: String?, targetRoute: String?): EnterTransition {
        val initialIndex = navigationOrder.indexOfFirst { initialRoute?.startsWith(it) == true }.coerceAtLeast(0)
        val targetIndex = navigationOrder.indexOfFirst { targetRoute?.startsWith(it) == true }.coerceAtLeast(0)

        return if (targetIndex > initialIndex) {
            slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) + fadeIn()
        } else {
            slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(300)) + fadeIn()
        }
    }

    fun getExitSpec(initialRoute: String?, targetRoute: String?): ExitTransition {
        val initialIndex = navigationOrder.indexOfFirst { initialRoute?.startsWith(it) == true }.coerceAtLeast(0)
        val targetIndex = navigationOrder.indexOfFirst { targetRoute?.startsWith(it) == true }.coerceAtLeast(0)

        return if (targetIndex > initialIndex) {
            slideOutHorizontally(targetOffsetX = { -it / 4 }, animationSpec = tween(300)) + fadeOut()
        } else {
            slideOutHorizontally(targetOffsetX = { it / 4 }, animationSpec = tween(300)) + fadeOut()
        }
    }

    fun clearPendingEverywhere() {
        pendingShareTxn.value = null
        showFriendPicker = false
        try {
            navController.getBackStackEntry("chat")
                .savedStateHandle.remove<String>("pendingTransaction")
        } catch (_: Exception) { }
    }

    fun navigateToChatWithFriend(friend: Friend, txn: ChatTransaction) {
        chatViewModelGlobal.loadFriends()
        chatViewModelGlobal.createChat(
            participantPhones = listOf(friend.phone),
            groupName = "",
            onSuccess = { chatId ->
                val txnJson = gson.toJson(txn)
                try {
                    navController.getBackStackEntry("chat")
                        .savedStateHandle.remove<String>("pendingTransaction")
                } catch (_: Exception) { }

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

    if (showFriendPicker && pendingShareTxn.value != null) {
        val friends by chatViewModelGlobal.friends.collectAsState()
        val chats by chatViewModelGlobal.chats.collectAsState()

        LaunchedEffect(Unit) {
            chatViewModelGlobal.loadFriends()
            chatViewModelGlobal.loadChats()
        }

        ShareToChatSheet(
            friends = friends,
            chats = chats,
            currentUserId = UserSession.userId ?: "",
            onDismiss = { clearPendingEverywhere() },
            onSelect = { navigateToChatWithFriend(it, pendingShareTxn.value!!) }
        )
    }

    AppScaffold(
        navController = navController,
        onNavigate = { route ->
            if (navController.currentDestination?.route != route) {
                navController.navigate(route) {
                    popUpTo("home") { inclusive = false }
                    launchSingleTop = true
                }
            }
        }
    ) { paddingValues ->
        // ה-NavHost נמצא כאן כמעטפת יציבה, האנימציות מוגדרות בתוכו
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(paddingValues),
            enterTransition = { getTransitionSpec(initialState.destination.route, targetState.destination.route) },
            exitTransition = { getExitSpec(initialState.destination.route, targetState.destination.route) },
            popEnterTransition = { getTransitionSpec(initialState.destination.route, targetState.destination.route) },
            popExitTransition = { getExitSpec(initialState.destination.route, targetState.destination.route) }
        ) {
            composable("home") {
                HomeScreen(
                    navController = navController,
                    onNavigate = { route -> navController.navigate(route) },
                    onShareClick = { transaction ->
                        pendingShareTxn.value = ChatTransaction(
                            businessName = transaction.businessName,
                            amount = transaction.amount,
                            date = transaction.date,
                            category = transaction.category,
                            status = transaction.status,
                            explanation = transaction.explanation
                        )
                        showFriendPicker = true
                    }
                )
            }

            composable("chat") {
                val entry = navController.currentBackStackEntry
                val pendingJson = entry?.savedStateHandle?.get<String>("pendingTransaction")
                val pendingTxn = if (pendingJson != null) gson.fromJson(pendingJson, ChatTransaction::class.java) else null

                ChatsScreen(
                    navController = navController,
                    onNavigate = { route -> navController.navigate(route) },
                    onOpenChat = { chat ->
                        val currentUserId = UserSession.userId ?: ""
                        val chatName = if (chat.isGroup) chat.groupName.ifBlank { "Group" } else {
                            val otherId = chat.participants.first { it != currentUserId }
                            chat.displayNames[otherId] ?: chat.participantNames[otherId] ?: "Chat"
                        }
                        val otherPhone = if (!chat.isGroup) {
                            val otherId = chat.participants.first { it != currentUserId }
                            chatViewModelGlobal.friends.value.firstOrNull { it.name == chat.participantNames[otherId] }?.phone ?: otherId
                        } else ""

                        entry?.savedStateHandle?.remove<String>("pendingTransaction")
                        navController.navigate("chat/${chat.id}") { launchSingleTop = true }
                        navController.getBackStackEntry("chat/${chat.id}").savedStateHandle.apply {
                            set("pendingTransaction", pendingJson)
                            set("chatName", chatName)
                            set("isGroup", chat.isGroup)
                            set("displayNamesJson", gson.toJson(chat.displayNames))
                            set("otherPhone", otherPhone)
                        }
                    },
                    pendingTransaction = pendingTxn
                )
            }

            composable(
                route = "chat/{chatId}",
                arguments = listOf(navArgument("chatId") { type = NavType.StringType })
            ) { backStackEntry ->
                val handle = backStackEntry.savedStateHandle
                val pendingJson = handle.get<String>("pendingTransaction")
                val displayNamesJson = handle.get<String>("displayNamesJson") ?: "{}"

                ChatScreen(
                    chatId = backStackEntry.arguments?.getString("chatId") ?: "",
                    chatName = handle.get<String>("chatName") ?: "Chat",
                    isGroup = handle.get<Boolean>("isGroup") ?: false,
                    displayNames = gson.fromJson(displayNamesJson, Map::class.java) as Map<String, String>,
                    otherPhone = handle.get<String>("otherPhone") ?: "",
                    onSetNickname = { phone, nick -> accountViewModelGlobal.setNickname(phone, nick) },
                    onTransactionSent = {
                        handle.remove<String>("pendingTransaction")
                        try { navController.getBackStackEntry("chat").savedStateHandle.remove<String>("pendingTransaction") } catch (_: Exception) {}
                    },
                    onNavigateToChat = { target -> navController.navigate("chat/${target.id}") { launchSingleTop = true } },
                    onBack = { navController.popBackStack() },
                    initialPendingTxn = if (pendingJson != null) gson.fromJson(pendingJson, ChatTransaction::class.java) else null
                )
            }

            composable("transactions") {
                TransactionsScreen(navController = navController, onNavigate = { route -> navController.navigate(route) })
            }

            composable("stats") {
                StatsScreen(navController = navController, onNavigate = { route -> navController.navigate(route) })
            }

            composable("account") {
                val context = androidx.compose.ui.platform.LocalContext.current
                AccountScreen(
                    navController = navController,
                    onNavigate = { route -> navController.navigate(route) },
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
                    navController = navController,
                    onNavigate = { route -> navController.navigate(route) },
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}