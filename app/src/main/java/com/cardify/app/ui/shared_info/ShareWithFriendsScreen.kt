package com.cardify.app.ui.shared_info

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cardify.app.data.model.Transaction
import com.cardify.app.ui.account.Friend
import com.cardify.app.ui.account.dummyFriends
import com.cardify.app.ui.components.AppScaffold
import com.cardify.app.ui.home.CardifyColors

@Composable
fun ShareWithFriendsScreen(
    transaction: Transaction?,
    onNavigate: (String) -> Unit,
    onNavigateBack: () -> Unit = {},
    viewModel: ShareWithFriendsViewModel = viewModel()
) {
    val context = LocalContext.current
    val isSending by viewModel.isSending.collectAsState()

    AppScaffold(currentRoute = "share_with_friends", onNavigate = onNavigate) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(padding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            item {
                Text(
                    "Share with your friends",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = CardifyColors.DarkGreen
                )
                Text(
                    "Choose who to share this transaction with",
                    fontSize = 13.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            item {
                transaction?.let {
                    TransactionSummaryCard(it)
                } ?: Text("No transaction selected.", color = Color.Gray, fontSize = 14.sp)
            }

            item {
                Text(
                    "Your contacts",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CardifyColors.DarkGreen
                )
            }

            items(dummyFriends) { friend ->
                FriendShareRow(
                    friend = friend,
                    enabled = !isSending,
                    onSend = {
                        val transactionId = transaction?.id ?: return@FriendShareRow
                        viewModel.sendShare(
                            sharedWith = friend.name,
                            transactionId = transactionId,
                            onSuccess = {
                                Toast.makeText(context, "Shared with ${friend.name}", Toast.LENGTH_SHORT).show()
                                onNavigateBack()
                            },
                            onError = { error ->
                                Toast.makeText(context, "Failed to share: $error", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun TransactionSummaryCard(transaction: Transaction) {
    val isIrregular = transaction.status == "IRREGULAR"
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardifyColors.TurquoiseBox),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isIrregular) CardifyColors.IrregularRed else CardifyColors.DarkGreen.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    transaction.businessName ?: "Unknown",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = CardifyColors.DarkGreen
                )
                Text(
                    "₪${transaction.amount}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = CardifyColors.DarkGreen
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(transaction.date ?: "", fontSize = 12.sp, color = Color.Gray)
                Text(
                    if (isIrregular) "Irregular" else "Regular",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isIrregular) CardifyColors.IrregularRed else CardifyColors.RegularGreen
                )
            }
        }
    }
}

@Composable
private fun FriendShareRow(friend: Friend, enabled: Boolean = true, onSend: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF9F9F9), RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = friend.photo),
            contentDescription = friend.name,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(friend.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            if (friend.phone.isNotEmpty()) {
                Text(friend.phone, fontSize = 12.sp, color = Color.Gray)
            }
        }
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(if (enabled) Color(0xFFE8FCE8) else Color(0xFFEEEEEE))
                .clickable(enabled = enabled) { onSend() }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Share,
                contentDescription = null,
                tint = Color(0xFF2E7D32),
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                "Send",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF2E7D32),
                modifier = Modifier.padding(end = 2.dp)
            )
        }
    }
}
