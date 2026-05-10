package com.cardify.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Group
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareToChatSheet(
    friends:       List<Friend>,
    chats:         List<Chat>  = emptyList(),
    currentUserId: String      = "",
    onDismiss:     () -> Unit,
    onSelect:      (Friend) -> Unit,
    onSelectGroup: ((Chat) -> Unit)? = null
) {
    val approvedFriends = remember(friends) { friends.filter { it.status == "approved" } }
    val groupChats      = remember(chats) { chats.filter { it.isGroup } }
    val hasAnything     = approvedFriends.isNotEmpty() || groupChats.isNotEmpty()

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
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Share to Chat", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            Text(
                "Choose a friend or group to share this transaction",
                fontSize = 13.sp, color = Color.Gray,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (!hasAnything) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.People, null,
                        tint = Color.LightGray, modifier = Modifier.size(52.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("No friends or groups yet",
                        color = Color.Gray, fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Add approved friends or create a group chat\nto share transactions.",
                        color = Color.LightGray, fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 460.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (approvedFriends.isNotEmpty()) {
                        item {
                            Text("Friends", fontSize = 12.sp, color = Color.Gray,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(bottom = 4.dp))
                        }
                        items(approvedFriends, key = { "f_${it.phone}" }) { friend ->
                            val displayName = resolveDisplayName(friend, chats, currentUserId)
                            FriendShareRow(
                                friend       = friend,
                                displayName  = displayName,
                                originalName = friend.name,
                                onSelect     = { onSelect(friend) }
                            )
                        }
                    }

                    if (groupChats.isNotEmpty()) {
                        item {
                            Text("Groups", fontSize = 12.sp, color = Color.Gray,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                        }
                        items(groupChats, key = { "g_${it.id}" }) { chat ->
                            GroupShareRow(
                                chat     = chat,
                                onSelect = { onSelectGroup?.invoke(chat) ?: onDismiss() }
                            )
                        }
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
        Box(
            modifier = Modifier
                .size(42.dp).clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            if (!friend.photoUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(friend.photoUrl).crossfade(true).build(),
                    contentDescription = friend.name,
                    contentScale       = ContentScale.Crop,
                    modifier           = Modifier.fillMaxSize().clip(CircleShape)
                )
            } else {
                Text(displayName.take(1).uppercase(),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(displayName, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            if (displayName != originalName)
                Text(originalName, fontSize = 11.sp, color = Color.Gray)
        }
        Button(
            onClick        = onSelect,
            colors         = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary),
            shape          = RoundedCornerShape(10.dp),
            modifier       = Modifier.height(36.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) { Text("Chat", fontSize = 12.sp) }
    }
}

@Composable
private fun GroupShareRow(
    chat:     Chat,
    onSelect: () -> Unit
) {
    val groupName = chat.groupName.ifBlank { "Group" }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF5F5F5))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp).clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Group, null,
                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(groupName, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text("${chat.participants.size} members", fontSize = 11.sp, color = Color.Gray)
        }
        Button(
            onClick        = onSelect,
            colors         = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary),
            shape          = RoundedCornerShape(10.dp),
            modifier       = Modifier.height(36.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) { Text("Share", fontSize = 12.sp) }
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
        val otherId = chat.participants.firstOrNull { it != currentUserId } ?: ""
        val nick    = chat.displayNames[otherId]
        if (!nick.isNullOrBlank()) return nick
    }
    return friend.name
}