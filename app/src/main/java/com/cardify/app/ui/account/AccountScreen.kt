package com.cardify.app.ui.account

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cardify.app.ui.components.AppScaffold
import com.cardify.app.data.model.*
import com.cardify.app.R

val teal = Color(0xFF006769)

@Composable
fun AccountScreen(
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit,
    onEditProfile: () -> Unit,
    viewModel: AccountViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val name by viewModel.username.collectAsState()
    val email by viewModel.email.collectAsState()
    val phone by viewModel.phone.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val friendsList by viewModel.friends.collectAsState()
    val requestsList by viewModel.requests.collectAsState()
    val suggestions by viewModel.syncResults.collectAsState()

    var selectedFriend by remember { mutableStateOf<Friend?>(null) }
    var selectedRequest by remember { mutableStateOf<Friend?>(null) }
    var selectedSuggestion by remember { mutableStateOf<Friend?>(null) }
    var showAddFriend by remember { mutableStateOf(false) }

    val context = LocalContext.current

    val contactsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.syncContacts(context)
            Toast.makeText(context, "Syncing contacts...", Toast.LENGTH_SHORT).show()
        }
    }

    AppScaffold(currentRoute = "account", onNavigate = onNavigate) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().height(250.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = teal)
                }
            } else {
                Spacer(modifier = Modifier.height(32.dp))
                ProfileSection(name = name, email = email, phone = phone, onEditProfile = onEditProfile)
            }

            Spacer(modifier = Modifier.height(32.dp))

            // חיבור ה-ViewModel לפעולות ב-Section
            FriendsSection(
                friends = friendsList,
                suggestions = suggestions,
                onFriendClick = { selectedFriend = it },
                onAddFriendClick = { showAddFriend = true },
                onSyncContactsClick = { contactsLauncher.launch(Manifest.permission.READ_CONTACTS) },
                onSuggestionClick = { selectedSuggestion = it },
                onAddSuggestionClick = { viewModel.sendFriendRequest(it) }
            )

            if (requestsList.isNotEmpty()) {
                Spacer(modifier = Modifier.height(32.dp))
                RequestsSection(
                    requests = requestsList,
                    onRequestClick = { selectedRequest = it },
                    onConfirmRequest = { viewModel.confirmFriendRequest(it) }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
            LogoutFooter(onLogout = { viewModel.logout(context, onLogoutSuccess = onLogout) })
            Spacer(modifier = Modifier.height(50.dp))
        }
    }

    // Sheets (מידע מורחב)
    if (showAddFriend) {
        AddFriendSheet(viewModel = viewModel, onDismiss = { showAddFriend = false; viewModel.clearSearch() })
    }

    selectedFriend?.let { friend ->
        FriendSheet(friend = friend, viewModel = viewModel, onDismiss = { selectedFriend = null })
    }

    selectedRequest?.let { request ->
        FriendRequestSheet(
            friend = request,
            onConfirm = { viewModel.confirmFriendRequest(request); selectedRequest = null },
            onDelete = { viewModel.deleteFriendWithOptions(request, false, false); selectedRequest = null },
            onDismiss = { selectedRequest = null }
        )
    }

    selectedSuggestion?.let { suggestion ->
        SuggestionItem(
            suggestion = suggestion,
            onAddClick = { viewModel.sendFriendRequest(suggestion); selectedSuggestion = null },
            onDelete = { selectedSuggestion = null },
            onDismiss = { selectedSuggestion = null }
        )
    }
}

// --- Sections ---

@Composable
fun FriendsSection(
    friends: List<Friend>,
    suggestions: List<Friend>,
    onFriendClick: (Friend) -> Unit,
    onAddFriendClick: () -> Unit,
    onSyncContactsClick: () -> Unit,
    onSuggestionClick: (Friend) -> Unit,
    onAddSuggestionClick: (Friend) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(title = "Your Friends", subtitle = "", showSync = true, onSyncClick = onSyncContactsClick)
        Spacer(modifier = Modifier.height(14.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(friends) { friend ->
                FriendItem(friend = friend, onClick = { onFriendClick(friend) })
            }
            item { AddFriendButton(onClick = onAddFriendClick) }
        }

        if (suggestions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(32.dp))

            // הוספת onSyncClick = {} פותרת את השגיאה
            SectionHeader(
                title = "People you may know",
                subtitle = "From your contacts",
                showSync = false,
                onSyncClick = {}
            )

            Spacer(modifier = Modifier.height(14.dp))
        }
            LazyRow(
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(suggestions) { suggestion ->
                    SuggestionCard(
                        suggestion = suggestion,
                        onAddClick = { onAddSuggestionClick(suggestion) },
                        onClick = { onSuggestionClick(suggestion) }
                    )
                }
            }
        }
    }


@Composable
fun RequestsSection(
    requests: List<Friend>,
    onRequestClick: (Friend) -> Unit,
    onConfirmRequest: (Friend) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Friend Requests",
            color = teal,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 24.dp, bottom = 12.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(requests) { friend ->
                RequestItem(
                    friend = friend,
                    onConfirm = { onConfirmRequest(friend) },
                    onClick = { onRequestClick(friend) }
                )
            }
        }
    }
}

// --- Items (הקומפוננטות הקטנות ברשימה) ---

@Composable
fun FriendItem(friend: Friend, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(70.dp).clickable { onClick() }
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            Surface(
                modifier = Modifier.size(60.dp),
                shape = CircleShape,
                color = Color(0xFFF7F8F9),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE7E8E9))
            ) {
                Image(
                    painter = painterResource(id = friend.photoResource),
                    contentDescription = friend.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            if (friend.isPending) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFFFF3E0),
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.clock__2_),
                        contentDescription = "Pending",
                        tint = Color(0xFFEF6C00),
                        modifier = Modifier.padding(4.dp)
                    )
                }
            }
        }
        Text(text = friend.name, fontSize = 12.sp, maxLines = 1, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
fun RequestItem(friend: Friend, onConfirm: () -> Unit, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(80.dp)
    ) {
        Surface(
            modifier = Modifier.size(60.dp).clickable { onClick() },
            shape = CircleShape,
            color = Color(0xFFFFF3E0),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFB74D))
        ) {
            Image(
                painter = painterResource(id = friend.photoResource),
                contentDescription = friend.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        Text(text = friend.name, fontSize = 12.sp, maxLines = 1, modifier = Modifier.padding(top = 4.dp))
        Button(
            onClick = onConfirm,
            modifier = Modifier.padding(top = 4.dp).height(26.dp).fillMaxWidth(),
            contentPadding = PaddingValues(0.dp),
            colors = ButtonDefaults.buttonColors(containerColor = teal),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Confirm", fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SuggestionCard(suggestion: Friend, onAddClick: () -> Unit, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(80.dp)
    ) {
        Surface(
            modifier = Modifier.size(60.dp).clickable { onClick() },
            shape = CircleShape,
            color = Color(0xFFF7F8F9),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE7E8E9))
        ) {
            Image(
                painter = painterResource(id = suggestion.photoResource),
                contentDescription = suggestion.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        Text(text = suggestion.name, fontSize = 12.sp, maxLines = 1, modifier = Modifier.padding(top = 4.dp))
        Button(
            onClick = onAddClick,
            modifier = Modifier.padding(top = 4.dp).height(26.dp).fillMaxWidth(),
            contentPadding = PaddingValues(0.dp),
            colors = ButtonDefaults.buttonColors(containerColor = teal),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Add", fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// --- UI Helpers ---

@Composable
fun SectionHeader(title: String, subtitle: String, showSync: Boolean, onSyncClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            if (subtitle.isNotEmpty()) Text(text = subtitle, fontSize = 12.sp, color = Color.Gray)
        }
        if (showSync) {
            IconButton(onClick = onSyncClick) {
                Icon(Icons.Default.Contacts, contentDescription = "Sync", tint = teal)
            }
        }
    }
}

@Composable
fun ProfileSection(name: String, email: String, phone: String, onEditProfile: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(modifier = Modifier.size(100.dp), shape = CircleShape, color = teal.copy(alpha = 0.1f)) {
            Box(contentAlignment = Alignment.Center) {
                Text(text = name.take(1).uppercase(), fontSize = 40.sp, fontWeight = FontWeight.Bold, color = teal)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = name, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(text = email, color = Color.Gray)
        Text(text = phone, color = Color.Gray)
        Button(
            onClick = onEditProfile,
            modifier = Modifier.padding(horizontal = 8.dp).height(26.dp).width(75.dp),
            contentPadding = PaddingValues(0.dp),
            colors = ButtonDefaults.buttonColors(containerColor = teal),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Edit Profile", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun AddFriendButton(onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }) {
        Surface(modifier = Modifier.size(60.dp), shape = CircleShape, color = Color.White, border = androidx.compose.foundation.BorderStroke(1.dp, teal)) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Add", tint = teal, modifier = Modifier.padding(16.dp))
        }
        Text(text = "Add", fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp), color = teal)
    }
}

@Composable
fun LogoutFooter(onLogout: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        Text("Do you want to close this account?", fontSize = 13.sp, color = Color.Black)
        Text(text = " log out", fontSize = 13.sp, color = Color.Red, modifier = Modifier.padding(start = 4.dp).clickable { onLogout() })
    }
}