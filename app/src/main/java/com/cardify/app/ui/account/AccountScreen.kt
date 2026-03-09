package com.cardify.app.ui.account

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cardify.app.R
import com.cardify.app.ui.components.AppScaffold

val teal = Color(0xFF006769)

data class Friend(
    val name: String,
    val photo: Int = R.drawable.user
)

val dummyFriends = listOf(
    Friend("Sarah"),
    Friend("Dana Levi"),
    Friend("Noa Bar"),
    Friend("Yael Katz"),
    Friend("Tal Mizrahi"),
)

val dummyRequests = listOf(
    Friend("Sarah"),
    Friend("Dana Levi"),
    Friend("Noa Bar"),
    Friend("Yael Katz"),
    Friend("Tal Mizrahi"),
)

@Composable
fun AccountScreen(
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit,
    onEditProfile: () -> Unit
) {
    AppScaffold(currentRoute = "account", onNavigate = onNavigate) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            ProfileSection(onEditProfile = onEditProfile)
            Spacer(modifier = Modifier.height(32.dp))
            FriendsSection()
            Spacer(modifier = Modifier.height(32.dp))
            RequestsSection()
            Spacer(modifier = Modifier.height(40.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("do you want to close this account?", fontSize = 13.sp, color = Color.Black)
                Text(
                    " log out",
                    fontSize = 13.sp,
                    color = Color.Red,
                    modifier = Modifier.clickable { onLogout() }
                )
            }

            Spacer(modifier = Modifier.height(50.dp))
        }
    }
}

@Composable
fun ProfileSection(onEditProfile: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 32.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.user),
            contentDescription = "Profile Picture",
            modifier = Modifier.size(108.dp)
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text("Hailey David", color = Color(0xFF0A0A0A), fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(4.dp))
        Text("+972 52-212-3123", color = Color(0xFF000000), fontSize = 14.sp)
        Spacer(modifier = Modifier.height(2.dp))
        Text("hailey@gmail.com", color = Color(0xFF535252), fontSize = 14.sp)
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { onEditProfile() },
            modifier = Modifier
                .width(220.dp)
                .height(38.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = teal)
        ) {
            Text("Edit your profile", fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun FriendsSection() {
    Spacer(modifier = Modifier.height(5.dp))
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "People you are friends with",
            color = teal,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 24.dp, bottom = 4.dp)
        )
        Text(
            "Click on the profiles to see shared information",
            color = Color.Gray,
            fontSize = 12.sp,
            modifier = Modifier.padding(start = 24.dp, bottom = 14.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(dummyFriends) { friend -> FriendItem(friend) }
            item { AddFriendButton(onClick = { /* TODO */ }) }
        }
    }
}

@Composable
fun RequestsSection() {
    Spacer(modifier = Modifier.height(14.dp))
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "These people want to be your friends",
            color = teal,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 24.dp, bottom = 4.dp)
        )
        Text(
            "Click on the profiles to see more details",
            color = Color.Gray,
            fontSize = 12.sp,
            modifier = Modifier.padding(start = 24.dp, bottom = 14.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(dummyRequests) { friend ->
                RequestItem(friend, onConfirm = { /* TODO */ })
            }
        }
    }
}

@Composable
fun FriendItem(friend: Friend) {
    Column(
        modifier = Modifier
            .width(72.dp)
            .clickable { /* TODO */ },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = friend.photo),
            contentDescription = friend.name,
            modifier = Modifier.size(56.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            friend.name,
            color = Color(0xFF5C5C5C),
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            lineHeight = 14.sp
        )
    }
}

@Composable
fun AddFriendButton(onClick: () -> Unit) {
    Column(
        modifier = Modifier.width(72.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(10.dp))
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

@Composable
fun RequestItem(friend: Friend, onConfirm: () -> Unit) {
    Column(
        modifier = Modifier
            .width(72.dp)
            .clickable { /* TODO */ },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = friend.photo),
            contentDescription = friend.name,
            modifier = Modifier.size(56.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            friend.name,
            color = Color(0xFF5C5C5C),
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            lineHeight = 14.sp
        )
        Spacer(modifier = Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .background(teal, shape = RoundedCornerShape(15.dp))
                .clickable { onConfirm() }
                .padding(horizontal = 6.dp, vertical = 1.dp)
        ) {
            Text("Confirm", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(modifier = Modifier.height(10.dp))
    }
}