package com.cardify.app.ui.account

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cardify.app.R
import com.cardify.app.ui.components.AppScaffold
import com.cardify.app.data.model.*

@Composable
fun AccountScreen(
    navController: NavHostController, // הוספת הפרמטר החסר
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit,
    onEditProfile: () -> Unit,
    viewModel: AccountViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val name         by viewModel.username.collectAsState()
    val email        by viewModel.email.collectAsState()
    val phone        by viewModel.phone.collectAsState()
    val profileImage by viewModel.profileImage.collectAsState()
    val isLoading    by viewModel.isLoading.collectAsState()
    val friendsList  by viewModel.friends.collectAsState()
    val requestsList by viewModel.requests.collectAsState()
    val suggestions  by viewModel.syncResults.collectAsState()

    var selectedFriend     by remember { mutableStateOf<Friend?>(null) }
    var selectedRequest    by remember { mutableStateOf<Friend?>(null) }
    var selectedSuggestion by remember { mutableStateOf<Friend?>(null) }
    var showAddFriend      by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        viewModel.refreshUserData()
        viewModel.loadFriendsData()
        viewModel.loadNicknames()
    }

    val contactsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.syncContacts(context)
            Toast.makeText(context, "Syncing contacts...", Toast.LENGTH_SHORT).show()
        }
    }

    AppScaffold(
        navController = navController, // העברת ה-Controller לאנימציה ולפתרון השגיאה
        onNavigate    = onNavigate
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(padding)
                .verticalScroll(scrollState) // הוספת גלילה לכל המסך
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(250.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                Spacer(modifier = Modifier.height(24.dp))
                ProfileSection(
                    name          = name,
                    email         = email,
                    phone         = phone,
                    profileImage  = profileImage,
                    onEditProfile = onEditProfile
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            FriendsSection(
                friends             = friendsList,
                suggestions         = suggestions,
                viewModel           = viewModel,
                onFriendClick       = { selectedFriend = it },
                onAddFriendClick    = { showAddFriend = true },
                onSyncContactsClick = { contactsLauncher.launch(Manifest.permission.READ_CONTACTS) },
                onSuggestionClick   = { selectedSuggestion = it },
                onAddSuggestionClick = { viewModel.sendFriendRequest(it) }
            )

            if (requestsList.isNotEmpty()) {
                Spacer(modifier = Modifier.height(32.dp))
                RequestsSection(
                    requests       = requestsList,
                    onRequestClick = { selectedRequest = it },
                    onConfirm      = { viewModel.confirmFriendRequest(it) }
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Do you want to close this account?", fontSize = 13.sp, color = Color.Black)
                Text(
                    text = " log out",
                    fontSize = 13.sp,
                    color = Color.Red,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .clickable { viewModel.logout(context, onLogoutSuccess = onLogout) }
                )
            }
            Spacer(modifier = Modifier.height(50.dp))
        }
    }

    // --- Dialogs & Sheets ---
    if (showAddFriend) {
        AddFriendSheet(
            viewModel = viewModel,
            onDismiss = { showAddFriend = false; viewModel.clearSearch() }
        )
    }

    selectedFriend?.let { friend ->
        FriendSheet(
            friend    = friend,
            viewModel = viewModel,
            onDismiss = { selectedFriend = null }
        )
    }

    selectedRequest?.let { request ->
        FriendRequestSheet(
            friend    = request,
            onConfirm = {
                viewModel.confirmFriendRequest(request)
                selectedRequest = null
            },
            onDelete  = {
                viewModel.deleteFriendWithOptions(request, false, false)
                selectedRequest = null
            },
            onDismiss = { selectedRequest = null }
        )
    }

    selectedSuggestion?.let { suggestion ->
        // כאן הנחתי שיש לך רכיב מתאים להצגת הצעה, אם לא - FriendSheet יכול להתאים
        FriendSheet(
            friend    = suggestion,
            viewModel = viewModel,
            onDismiss = { selectedSuggestion = null }
        )
    }
}

// ─── FriendsSection ──────────────────────────────────────────────────────────

@Composable
fun FriendsSection(
    friends:             List<Friend>,
    suggestions:         List<Friend>,
    viewModel:           AccountViewModel,
    onFriendClick:       (Friend) -> Unit,
    onAddFriendClick:    () -> Unit,
    onSyncContactsClick: () -> Unit,
    onSuggestionClick:   (Friend) -> Unit,
    onAddSuggestionClick: (Friend) -> Unit
) {
    val nicknames by viewModel.nicknames.collectAsState()

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text       = "Your Friends",
                    color      = MaterialTheme.colorScheme.primary,
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text     = "Click on profiles for more information",
                    color    = Color.Gray,
                    fontSize = 12.sp
                )
            }
            IconButton(onClick = onSyncContactsClick) {
                Icon(Icons.Default.Contacts, contentDescription = "Sync", tint = MaterialTheme.colorScheme.primary)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        LazyRow(
            contentPadding        = PaddingValues(start = 24.dp, end = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(friends) { friend ->
                val displayName = nicknames[friend.phone]?.takeIf { it.isNotBlank() } ?: friend.name
                FriendItem(
                    friend      = friend,
                    displayName = displayName,
                    onClick     = { onFriendClick(friend) }
                )
            }
            item { AddFriendButton(onClick = onAddFriendClick) }
        }

        if (suggestions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(32.dp))

            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                Text(
                    text       = "People you may know",
                    color      = MaterialTheme.colorScheme.primary,
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(text = "From your contacts", fontSize = 12.sp, color = Color.Gray)
            }

            Spacer(modifier = Modifier.height(14.dp))

            LazyRow(
                contentPadding        = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(suggestions) { suggestion ->
                    SuggestionCard(
                        suggestion = suggestion,
                        onAddClick = { onAddSuggestionClick(suggestion) },
                        onClick    = { onSuggestionClick(suggestion) }
                    )
                }
            }
        }
    }
}

// ─── RequestsSection ─────────────────────────────────────────────────────────

@Composable
fun RequestsSection(
    requests:       List<Friend>,
    onRequestClick: (Friend) -> Unit,
    onConfirm:      (Friend) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text       = "Friend Requests",
            color      = MaterialTheme.colorScheme.primary,
            fontSize   = 18.sp,
            fontWeight = FontWeight.SemiBold,
            modifier   = Modifier.padding(start = 24.dp, bottom = 4.dp)
        )
        LazyRow(
            contentPadding        = PaddingValues(start = 24.dp, end = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(requests) { friend ->
                RequestItem(
                    friend    = friend,
                    onConfirm = { onConfirm(friend) },
                    onClick   = { onRequestClick(friend) }
                )
            }
        }
    }
}

// ─── FriendItem ──────────────────────────────────────────────────────────────

@Composable
fun FriendItem(
    friend:      Friend,
    displayName: String,
    onClick:     () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier.width(72.dp).clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF0F0F0)),
                contentAlignment = Alignment.Center
            ) {
                if (!friend.photoUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(friend.photoUrl).crossfade(true).build(),
                        contentDescription = friend.name,
                        contentScale       = ContentScale.Crop,
                        modifier           = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        Icons.Default.Person, null,
                        modifier = Modifier.size(28.dp),
                        tint     = Color.Gray
                    )
                }
            }

            if (friend.isPending) {
                Surface(
                    modifier = Modifier.size(18.dp).offset(x = 2.dp, y = 2.dp),
                    shape    = CircleShape,
                    color    = Color(0xFFEF6C00),
                    border   = BorderStroke(2.dp, Color.White)
                ) {
                    Icon(
                        Icons.Default.Timer, "Pending",
                        tint     = Color.White,
                        modifier = Modifier.padding(2.dp).size(12.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text      = displayName,
            color     = Color(0xFF5C5C5C),
            fontSize  = 11.sp,
            textAlign = TextAlign.Center,
            maxLines  = 1,
            overflow  = TextOverflow.Ellipsis
        )
    }
}

// ─── RequestItem ─────────────────────────────────────────────────────────────

@Composable
fun RequestItem(friend: Friend, onConfirm: () -> Unit, onClick: () -> Unit) {
    val context = LocalContext.current

    Column(
        modifier = Modifier.width(72.dp).clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Color(0xFFF0F0F0)),
            contentAlignment = Alignment.Center
        ) {
            if (!friend.photoUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(friend.photoUrl).crossfade(true).build(),
                    contentDescription = friend.name,
                    contentScale       = ContentScale.Crop,
                    modifier           = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    Icons.Default.Person, null,
                    modifier = Modifier.size(28.dp),
                    tint     = Color.Gray
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            friend.name,
            color     = Color(0xFF5C5C5C),
            fontSize  = 11.sp,
            textAlign = TextAlign.Center,
            maxLines  = 1,
            overflow  = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(15.dp))
                .clickable { onConfirm() }
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text("Confirm", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ─── SuggestionCard ──────────────────────────────────────────────────────────

@Composable
fun SuggestionCard(suggestion: Friend, onAddClick: () -> Unit, onClick: () -> Unit) {
    val context = LocalContext.current

    Column(
        modifier = Modifier.width(72.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Color(0xFFF0F0F0))
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            if (!suggestion.photoUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(suggestion.photoUrl).crossfade(true).build(),
                    contentDescription = suggestion.name,
                    contentScale       = ContentScale.Crop,
                    modifier           = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    Icons.Default.Person, null,
                    modifier = Modifier.size(28.dp),
                    tint     = Color.Gray
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            suggestion.name,
            color     = Color(0xFF5C5C5C),
            fontSize  = 11.sp,
            textAlign = TextAlign.Center,
            maxLines  = 1,
            overflow  = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(15.dp))
                .clickable { onAddClick() }
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text("Add", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ─── AddFriendButton ─────────────────────────────────────────────────────────

@Composable
fun AddFriendButton(onClick: () -> Unit) {
    Column(modifier = Modifier.width(72.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(Color(0xFFE6F7F7), shape = RoundedCornerShape(12.dp))
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Text("+", color = MaterialTheme.colorScheme.primary, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ─── ProfileSection ──────────────────────────────────────────────────────────

@Composable
fun ProfileSection(
    name:          String,
    email:         String,
    phone:         String,
    profileImage:  String,
    onEditProfile: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (profileImage.isNotEmpty()) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(profileImage)
                    .crossfade(true)
                    .build(),
                contentDescription = "Profile Picture",
                contentScale       = ContentScale.Crop,
                modifier           = Modifier
                    .size(108.dp)
                    .clip(CircleShape),
                error = painterResource(id = R.drawable.user)
            )
        } else {
            Image(
                painter            = painterResource(id = R.drawable.user),
                contentDescription = "Profile Picture",
                modifier           = Modifier
                    .size(108.dp)
                    .clip(CircleShape)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))
        Text(name,  color = Color(0xFF0A0A0A), fontSize = 18.sp, fontWeight = FontWeight.Bold)
        if (phone.isNotEmpty()) Text(phone, color = Color.Black, fontSize = 14.sp)
        Text(email, color = Color.Gray, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick  = onEditProfile,
            modifier = Modifier.width(220.dp).height(40.dp),
            shape    = RoundedCornerShape(12.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("Edit Profile", fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}