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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cardify.app.data.model.Friend
import com.cardify.app.ui.home.CardifyColors

/**
 * Bottom sheet לשיתוף טרנזקציה לצ'ט.
 * מציג רשימת חברים מאושרים — לחיצה על חבר פותחת/יוצרת צ'ט ושולחת את הטרנזקציה.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareToChatSheet(
    friends:   List<Friend>,
    onDismiss: () -> Unit,
    onSelect:  (Friend) -> Unit   // הקורא מטפל ביצירת הצ'ט והניווט
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
            // כותרת
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 4.dp)
            ) {
                Icon(
                    Icons.Default.ChatBubbleOutline,
                    contentDescription = null,
                    tint     = CardifyColors.DarkGreen,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Share to Chat",
                    fontWeight = FontWeight.Bold,
                    fontSize   = 18.sp
                )
            }
            Text(
                "Choose a friend to open a chat and share this transaction",
                fontSize = 13.sp,
                color    = Color.Gray,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (approvedFriends.isEmpty()) {
                // מצב ריק — אין חברים
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.People,
                        contentDescription = null,
                        tint     = Color.LightGray,
                        modifier = Modifier.size(52.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "No friends yet",
                        color      = Color.Gray,
                        fontSize   = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign  = TextAlign.Center
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Go to Account → Friends to add friends\nand share transactions with them.",
                        color     = Color.LightGray,
                        fontSize  = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier            = Modifier.heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(approvedFriends, key = { it.phone }) { friend ->
                        FriendShareRow(
                            friend   = friend,
                            onSelect = { onSelect(friend) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FriendShareRow(
    friend:   Friend,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF5F5F5))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // אות ראשית
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(CardifyColors.DarkGreen.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                friend.name.take(1).uppercase(),
                color      = CardifyColors.DarkGreen,
                fontWeight = FontWeight.Bold,
                fontSize   = 16.sp
            )
        }

        Spacer(Modifier.width(12.dp))

        Text(
            friend.name,
            modifier   = Modifier.weight(1f),
            fontWeight = FontWeight.SemiBold,
            fontSize   = 14.sp
        )

        Button(
            onClick  = onSelect,
            colors   = ButtonDefaults.buttonColors(containerColor = CardifyColors.DarkGreen),
            shape    = RoundedCornerShape(10.dp),
            modifier = Modifier.height(36.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            Text("Chat", fontSize = 12.sp)
        }
    }
}