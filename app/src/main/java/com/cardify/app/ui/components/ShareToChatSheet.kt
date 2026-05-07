package com.cardify.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cardify.app.data.model.Chat
import com.cardify.app.data.model.Friend
import com.cardify.app.ui.home.CardifyColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareToChatSheet(
    friends:       List<Friend>,
    chats:         List<Chat> = emptyList(),
    currentUserId: String     = "",
    onDismiss:     () -> Unit,
    onSelect:      (Friend) -> Unit
) {
    val approvedFriends = remember(friends) { friends.filter { it.status == "approved" } }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = Color.White,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 4.dp)
            ) {
                Icon(Icons.Default.ChatBubbleOutline, null,
                    tint = CardifyColors.DarkGreen, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Share to Chat", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            Text(
                "Choose a friend to open a chat and share this transaction",
                fontSize = 13.sp, color = Color.Gray,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (approvedFriends.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.People, null,
                        tint = Color.LightGray, modifier = Modifier.size(52.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("No friends yet", color = Color.Gray,
                        fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Go to Account → Friends to add friends\nand share transactions with them.",
                        color = Color.LightGray, fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(approvedFriends, key = { it.phone }) { friend ->
                        val displayName = resolveDisplayName(friend, chats, currentUserId)
                        FriendShareRow(
                            friend       = friend,
                            displayName  = displayName,
                            originalName = friend.name,
                            onSelect     = { onSelect(friend) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FriendShareRow(
    friend:       Friend,
    displayName:  String,
    originalName: String,
    onSelect:     () -> Unit
) {
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF5F5F5))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // תמונת פרופיל — אמיתית אם קיימת
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(CardifyColors.DarkGreen.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            if (!friend.photoUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(friend.photoUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = friend.name,
                    contentScale       = ContentScale.Crop,
                    modifier           = Modifier.fillMaxSize().clip(CircleShape)
                )
            } else {
                Text(
                    displayName.take(1).uppercase(),
                    color      = CardifyColors.DarkGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 16.sp
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(displayName, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            if (displayName != originalName) {
                Text(originalName, fontSize = 11.sp, color = Color.Gray)
            }
        }

        Button(
            onClick        = onSelect,
            colors         = ButtonDefaults.buttonColors(containerColor = CardifyColors.DarkGreen),
            shape          = RoundedCornerShape(10.dp),
            modifier       = Modifier.height(36.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            Text("Chat", fontSize = 12.sp)
        }
    }
}

fun resolveDisplayName(
    friend:        Friend,
    chats:         List<Chat>,
    currentUserId: String
): String {
    val chat = chats.firstOrNull { c ->
        !c.isGroup && c.participants.any { pid ->
            pid != currentUserId && c.participantNames[pid] == friend.name
        }
    }
    if (chat != null) {
        val otherId     = chat.participants.firstOrNull { it != currentUserId } ?: ""
        val displayName = chat.displayNames[otherId]
        if (!displayName.isNullOrBlank()) return displayName
    }
    return friend.name
}