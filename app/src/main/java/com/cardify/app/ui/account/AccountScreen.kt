package com.cardify.app.ui.account

import android.widget.Toast
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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
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
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cardify.app.R
import com.cardify.app.data.UserSession
import com.cardify.app.ui.components.AppScaffold
import com.cardify.app.data.model.*

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

    var selectedFriend by remember { mutableStateOf<Friend?>(null) }
    var selectedRequest by remember { mutableStateOf<Friend?>(null) }
    var showAddFriend by remember { mutableStateOf(false) }

    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.refreshUserData()
        viewModel.loadFriendsData()
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
                Box(
                    modifier = Modifier.fillMaxWidth().height(250.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = teal)
                }
            } else {
                Spacer(modifier = Modifier.height(32.dp))
                ProfileSection(
                    name = name,
                    email = email,
                    phone = phone,
                    isAdmin = UserSession.isAdmin(),
                    onEditProfile = onEditProfile
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            FriendsSection(
                friends = friendsList,
                onFriendClick = { selectedFriend = it },
                onAddFriendClick = { showAddFriend = true }
            )

            Spacer(modifier = Modifier.height(32.dp))

            if (requestsList.isNotEmpty()) {
                RequestsSection(
                    requests = requestsList,
                    onRequestClick = { selectedRequest = it },
                    onConfirm = { viewModel.confirmFriendRequest(it) }
                )
                Spacer(modifier = Modifier.height(32.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Do you want to close this account?", fontSize = 13.sp, color = Color.Black)
                Text(
                    text = " log out",
                    fontSize = 13.sp,
                    color = Color.Red,
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .clickable { viewModel.logout(context, onLogoutSuccess = onLogout) }
                )
            }
            Spacer(modifier = Modifier.height(50.dp))
        }
    }

    if (showAddFriend) {
        AddFriendSheet(
            viewModel = viewModel,
            onDismiss = {
                showAddFriend = false
                viewModel.clearSearch()
            }
        )
    }

    selectedFriend?.let { friend ->
        FriendSheet(
            friend = friend,
            viewModel = viewModel,
            onDismiss = { selectedFriend = null }
        )
    }

    selectedRequest?.let { request ->
        FriendRequestSheet(
            friend = request,
            onConfirm = {
                viewModel.confirmFriendRequest(request)
                selectedRequest = null
            },
            onDelete = {
                viewModel.deleteFriendWithOptions(request, false, false)
                selectedRequest = null
            },
            onDismiss = { selectedRequest = null }
        )
    }
}

@Composable
fun ProfileSection(
    name: String,
    email: String,
    phone: String,
    isAdmin: Boolean = false,
    onEditProfile: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.user),
            contentDescription = "Profile Picture",
            modifier = Modifier.size(108.dp)
        )
        Spacer(modifier = Modifier.height(14.dp))

        Text(name, color = Color(0xFF0A0A0A), fontSize = 18.sp, fontWeight = FontWeight.Bold)

        // Badge אדמין - מופיע רק אם המשתמש הוא אדמין
        if (isAdmin) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(teal)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.Shield,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    "Admin",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        if (phone.isNotEmpty()) Text(phone, color = Color.Black, fontSize = 14.sp)
        Text(email, color = Color.Gray, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onEditProfile,
            modifier = Modifier.width(220.dp).height(40.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = teal)
        ) {
            Text("Edit Profile", fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun FriendsSection(
    friends: List<Friend>,
    onFriendClick: (Friend) -> Unit,
    onAddFriendClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Your Friends",
            color = teal,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 24.dp, bottom = 4.dp)
        )
        Text(
            text = "Click on profiles for more information",
            color = Color.Gray,
            fontSize = 12.sp,
            modifier = Modifier.padding(start = 24.dp, bottom = 14.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(friends) { friend ->
                FriendItem(friend = friend, onClick = { onFriendClick(friend) })
            }
            item { AddFriendButton(onClick = onAddFriendClick) }
        }
    }
}

@Composable
fun RequestsSection(
    requests: List<Friend>,
    onRequestClick: (Friend) -> Unit,
    onConfirm: (Friend) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Friend Requests",
            color = teal,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 24.dp, bottom = 4.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(requests) { friend ->
                RequestItem(
                    friend = friend,
                    onConfirm = { onConfirm(friend) },
                    onClick = { onRequestClick(friend) }
                )
            }
        }
    }
}

@Composable
fun FriendItem(friend: Friend, onClick: () -> Unit) {
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
                            .data(friend.photoUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = friend.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = Color.Gray
                    )
                }
            }

            if (friend.isPending) {
                Surface(
                    modifier = Modifier
                        .size(18.dp)
                        .offset(x = 2.dp, y = 2.dp),
                    shape = CircleShape,
                    color = Color(0xFFEF6C00),
                    border = BorderStroke(2.dp, Color.White)
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = "Pending Approval",
                        tint = Color.White,
                        modifier = Modifier
                            .padding(2.dp)
                            .size(12.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = friend.name ?: "Unknown",
            color = Color(0xFF5C5C5C),
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

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
                        .data(friend.photoUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = friend.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = Color.Gray
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = friend.name,
            color = Color(0xFF5C5C5C),
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .background(teal, shape = RoundedCornerShape(15.dp))
                .clickable { onConfirm() }
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text("Confirm", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

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
            Text("+", color = teal, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        }
    }
}