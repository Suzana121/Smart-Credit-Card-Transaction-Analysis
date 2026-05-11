package com.cardify.app.ui.navigation

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.gson.Gson
import com.cardify.app.data.UserSession
import com.cardify.app.data.model.Chat
import com.cardify.app.data.model.ChatTransaction
import com.cardify.app.data.model.Friend
import com.cardify.app.ui.account.AccountScreen
import com.cardify.app.ui.account.AccountViewModel
import com.cardify.app.ui.chat.ChatScreen
import com.cardify.app.ui.chat.ChatViewModel
import com.cardify.app.ui.chat.ChatOpenRequest
import com.cardify.app.ui.chat.ChatsScreen
import com.cardify.app.ui.components.ShareToChatSheet
import com.cardify.app.ui.edit_account.EditAccountScreen
import com.cardify.app.ui.home.HomeScreen
import com.cardify.app.ui.stats.StatsScreen
import com.cardify.app.ui.transactions.TransactionsScreen
import com.cardify.app.ui.components.AppScaffold

@RequiresApi(Build.VERSION_CODES.O)
@SuppressLint("UnusedContentLambdaTargetStateParameter")
@Composable
fun AppNavigation(startDestination: String = "home") {

    val navController = rememberNavController()
    val gson          = remember { Gson() }

    val pendingShareTxn = remember { mutableStateOf<ChatTransaction?>(null) }
    var showFriendPicker by remember { mutableStateOf(false) }

    val chatViewModelGlobal:    ChatViewModel    = viewModel()
    val accountViewModelGlobal: AccountViewModel = viewModel()

    val globalNicknames by accountViewModelGlobal.nicknames.collectAsState()

    val navigationOrder = listOf("home", "chat", "transactions", "stats", "account")

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute      = navBackStackEntry?.destination?.route ?: "home"
    val isTopLevelTab     = navigationOrder.any { currentRoute == it }
    val isNestedChatScreen = currentRoute.startsWith("chat/")

    fun getTransitionSpec(initialRoute: String?, targetRoute: String?): EnterTransition {
        val i = navigationOrder.indexOfFirst { initialRoute?.startsWith(it) == true }.coerceAtLeast(0)
        val t = navigationOrder.indexOfFirst { targetRoute?.startsWith(it) == true }.coerceAtLeast(0)
        return if (t > i) slideInHorizontally(animationSpec = tween(300), initialOffsetX = { it }) + fadeIn()
        else             slideInHorizontally(animationSpec = tween(300), initialOffsetX = { -it }) + fadeIn()
    }

    fun getExitSpec(initialRoute: String?, targetRoute: String?): ExitTransition {
        val i = navigationOrder.indexOfFirst { initialRoute?.startsWith(it) == true }.coerceAtLeast(0)
        val t = navigationOrder.indexOfFirst { targetRoute?.startsWith(it) == true }.coerceAtLeast(0)
        return if (t > i) slideOutHorizontally(animationSpec = tween(300), targetOffsetX = { -it / 4 }) + fadeOut()
        else              slideOutHorizontally(animationSpec = tween(300), targetOffsetX = { it / 4 }) + fadeOut()
    }

    fun swipeTab(toRight: Boolean) {
        val idx  = navigationOrder.indexOf(currentRoute)
        if (idx < 0) return
        val next = if (toRight) idx + 1 else idx - 1
        if (next < 0 || next >= navigationOrder.size) return
        val route = navigationOrder[next]
        if (navController.currentDestination?.route != route) {
            navController.navigate(route) {
                popUpTo("home") { inclusive = false }
                launchSingleTop = true
            }
        }
    }

    fun clearPendingEverywhere() {
        pendingShareTxn.value = null
        showFriendPicker      = false
        try { navController.getBackStackEntry("chat")
            .savedStateHandle.remove<String>("pendingTransaction") } catch (_: Exception) { }
    }

    // ─── פותח צ'ט ספציפי — כעת מקבל ChatOpenRequest עם phone מובטח ──
    fun openChatRequest(request: ChatOpenRequest, pendingJson: String?) {
        val chat      = request.chat
        val chatName  = if (chat.isGroup) chat.groupName.ifBlank { "Group" }
        else {
            val me      = UserSession.userId ?: ""
            val otherId = chat.participants.firstOrNull { it != me } ?: ""
            chat.displayNames[otherId] ?: chat.participantNames[otherId] ?: "Chat"
        }
        navController.navigate("chat/${chat.id}") { launchSingleTop = true }
        navController.getBackStackEntry("chat/${chat.id}").savedStateHandle.apply {
            set("pendingTransaction", pendingJson)
            set("chatName",           chatName)
            set("isGroup",            chat.isGroup)
            set("displayNamesJson",   gson.toJson(chat.displayNames))
            set("otherPhone",         request.otherPhone)   // ← phone בטוח
        }
    }

    fun navigateToChatWithFriend(friend: Friend, txn: ChatTransaction) {
        val currentUserId = UserSession.userId ?: ""
        val existingChat  = chatViewModelGlobal.chats.value.firstOrNull { chat ->
            !chat.isGroup && chat.participants.size == 2 &&
                    chat.participants.any { it != currentUserId } &&
                    run {
                        val otherId = chat.participants.first { it != currentUserId }
                        chat.participantNames[otherId] == friend.name ||
                                chatViewModelGlobal.friends.value
                                    .firstOrNull { f -> f.phone == friend.phone }
                                    ?.name == chat.participantNames[otherId]
                    }
        }
        val txnJson = gson.toJson(txn)
        fun openChat(chatId: String) {
            try { navController.getBackStackEntry("chat")
                .savedStateHandle.remove<String>("pendingTransaction") } catch (_: Exception) { }
            navController.navigate("chat") {
                popUpTo("home") { inclusive = false }; launchSingleTop = true }
            navController.navigate("chat/$chatId") { launchSingleTop = true }
            navController.getBackStackEntry("chat/$chatId").savedStateHandle.apply {
                set("pendingTransaction", txnJson)
                set("chatName",           friend.name)
                set("isGroup",            false)
                set("displayNamesJson",   "{}")
                set("otherPhone",         friend.phone)   // ← phone ידוע בוודאות
            }
        }
        if (existingChat != null) { openChat(existingChat.id); clearPendingEverywhere() }
        else {
            chatViewModelGlobal.loadFriends()
            chatViewModelGlobal.createChat(listOf(friend.phone), "") { chatId ->
                openChat(chatId); clearPendingEverywhere()
            }
        }
    }

    fun navigateToChatWithGroup(chat: Chat, txn: ChatTransaction) {
        val txnJson   = gson.toJson(txn)
        val groupName = chat.groupName.ifBlank { "Group" }
        try { navController.getBackStackEntry("chat")
            .savedStateHandle.remove<String>("pendingTransaction") } catch (_: Exception) { }
        navController.navigate("chat") {
            popUpTo("home") { inclusive = false }; launchSingleTop = true }
        navController.navigate("chat/${chat.id}") { launchSingleTop = true }
        navController.getBackStackEntry("chat/${chat.id}").savedStateHandle.apply {
            set("pendingTransaction", txnJson)
            set("chatName",           groupName)
            set("isGroup",            true)
            set("displayNamesJson",   gson.toJson(chat.displayNames))
            set("otherPhone",         "")
        }
        clearPendingEverywhere()
    }

    if (showFriendPicker && pendingShareTxn.value != null) {
        val friends by chatViewModelGlobal.friends.collectAsState()
        val chats   by chatViewModelGlobal.chats.collectAsState()
        val approved = remember(friends) { friends.filter { it.status == "approved" } }
        LaunchedEffect(Unit) {
            chatViewModelGlobal.loadFriends()
            chatViewModelGlobal.loadChats()
        }
        ShareToChatSheet(
            friends       = approved,
            chats         = chats,
            currentUserId = UserSession.userId ?: "",
            onDismiss     = { clearPendingEverywhere() },
            onSelect      = { friend -> navigateToChatWithFriend(friend, pendingShareTxn.value!!) },
            onSelectGroup = { groupChat -> navigateToChatWithGroup(groupChat, pendingShareTxn.value!!) }
        )
    }

    AppScaffold(
        navController  = navController,
        onNavigate     = { route ->
            if (navController.currentDestination?.route != route) {
                navController.navigate(route) {
                    popUpTo("home") { inclusive = false }
                    launchSingleTop = true
                }
            }
        },
        topBarContent  = if (isNestedChatScreen) ({ }) else null,
        hideBottomBar  = isNestedChatScreen
    ) { paddingValues ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .then(
                    if (isTopLevelTab) {
                        var drag = 0f
                        Modifier.pointerInput(currentRoute) {
                            detectHorizontalDragGestures(
                                onDragStart      = { drag = 0f },
                                onDragEnd        = {
                                    if (drag < -80f) swipeTab(toRight = true)
                                    else if (drag > 80f) swipeTab(toRight = false)
                                    drag = 0f
                                },
                                onHorizontalDrag = { _, amount -> drag += amount }
                            )
                        }
                    } else Modifier
                )
        ) {
            NavHost(
                navController      = navController,
                startDestination   = startDestination,
                enterTransition    = { getTransitionSpec(initialState.destination.route, targetState.destination.route) },
                exitTransition     = { getExitSpec(initialState.destination.route, targetState.destination.route) },
                popEnterTransition = { getTransitionSpec(initialState.destination.route, targetState.destination.route) },
                popExitTransition  = { getExitSpec(initialState.destination.route, targetState.destination.route) }
            ) {
                composable("home") {
                    HomeScreen(
                        navController = navController,
                        onNavigate    = { route -> navController.navigate(route) },
                        onShareClick  = { transaction ->
                            pendingShareTxn.value = ChatTransaction(
                                businessName = transaction.businessName,
                                amount       = transaction.amount,
                                date         = transaction.date,
                                category     = transaction.category,
                                status       = transaction.status,
                                explanation  = transaction.explanation
                            )
                            chatViewModelGlobal.loadFriends()
                            chatViewModelGlobal.loadChats()
                            showFriendPicker = true
                        }
                    )
                }

                composable("chat") {
                    val entry       = navController.currentBackStackEntry
                    val pendingJson = entry?.savedStateHandle?.get<String>("pendingTransaction")
                    val pendingTxn  = pendingJson?.let { gson.fromJson(it, ChatTransaction::class.java) }
                    ChatsScreen(
                        navController = navController,
                        onNavigate    = { route -> navController.navigate(route) },
                        onOpenChat    = { request ->
                            entry?.savedStateHandle?.remove<String>("pendingTransaction")
                            openChatRequest(request, pendingJson)
                        },
                        pendingTransaction = pendingTxn
                    )
                }

                composable(
                    route     = "chat/{chatId}",
                    arguments = listOf(navArgument("chatId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val handle           = backStackEntry.savedStateHandle
                    val pendingJson      = handle.get<String>("pendingTransaction")
                    val displayNamesJson = handle.get<String>("displayNamesJson") ?: "{}"
                    ChatScreen(
                        chatId            = backStackEntry.arguments?.getString("chatId") ?: "",
                        chatName          = handle.get<String>("chatName") ?: "Chat",
                        isGroup           = handle.get<Boolean>("isGroup") ?: false,
                        displayNames      = gson.fromJson(displayNamesJson, Map::class.java) as Map<String, String>,
                        otherPhone        = handle.get<String>("otherPhone") ?: "",
                        externalNicknames = globalNicknames,
                        onSetNickname     = { phone, nick -> accountViewModelGlobal.setNickname(phone, nick) },
                        onTransactionSent = {
                            handle.remove<String>("pendingTransaction")
                            try { navController.getBackStackEntry("chat")
                                .savedStateHandle.remove<String>("pendingTransaction") }
                            catch (_: Exception) { }
                        },
                        onNavigateToChat  = { target ->
                            navController.navigate("chat/${target.id}") { launchSingleTop = true } },
                        onBack            = { navController.popBackStack() },
                        initialPendingTxn = pendingJson?.let { gson.fromJson(it, ChatTransaction::class.java) },
                        viewModel         = chatViewModelGlobal  // ← אותו instance כמו AppScaffold
                    )
                }

                composable("transactions") {
                    TransactionsScreen(
                        navController = navController,
                        onNavigate    = { route -> navController.navigate(route) },
                        onShareToChat = { transaction ->
                            pendingShareTxn.value = ChatTransaction(
                                businessName = transaction.businessName,
                                amount       = transaction.amount,
                                date         = transaction.date,
                                category     = transaction.category,
                                status       = transaction.status,
                                explanation  = transaction.explanation ?: ""
                            )
                            chatViewModelGlobal.loadFriends()
                            chatViewModelGlobal.loadChats()
                            showFriendPicker = true
                        }
                    )
                }

                composable("stats") {
                    StatsScreen(
                        navController = navController,
                        onNavigate    = { route -> navController.navigate(route) }
                    )
                }

                composable("account") {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    AccountScreen(
                        navController = navController,
                        onNavigate    = { route -> navController.navigate(route) },
                        onEditProfile = { navController.navigate("edit_account") },
                        onLogout      = {
                            val intent = android.content.Intent(
                                context, com.cardify.app.ui.login.LoginActivity::class.java)
                            intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                                    android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                            context.startActivity(intent)
                        }
                    )
                }

                composable("edit_account") {
                    EditAccountScreen(
                        navController = navController,
                        onNavigate    = { route -> navController.navigate(route) },
                        onBack        = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}