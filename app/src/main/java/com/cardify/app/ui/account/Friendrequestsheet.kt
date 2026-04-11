package com.cardify.app.ui.account

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cardify.app.data.model.Friend

/**
 * Modal bottom sheet presented when the user taps an incoming friend request.
 *
 * Shows the requester's avatar, name, and phone number along with "Confirm Request"
 * and "Delete request" actions.
 *
 * @param friend The [Friend] who sent the request.
 * @param onConfirm Called when "Confirm Request" is tapped (also dismisses the sheet).
 * @param onDelete Called when "Delete request" is tapped (also dismisses the sheet).
 * @param onDismiss Called when the sheet is dismissed without taking an action.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendRequestSheet(
    friend: Friend,
    onConfirm: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(painter = painterResource(id = friend.photoResource), contentDescription = friend.name, modifier = Modifier.size(80.dp).padding(bottom = 8.dp))
            Text(text = "Friend Request", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = Color(0xFFF7F8F9), border = BorderStroke(1.dp, Color(0xFFE7E8E9))) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Name", color = teal, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(friend.name, color = Color.Black, fontSize = 16.sp)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFE7E8E9))
                    Text("Phone number", color = teal, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(friend.phone, color = Color.Black, fontSize = 16.sp)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = { onConfirm(); onDismiss() }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = teal)) {
                Text("Confirm Request", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Delete request", fontSize = 14.sp, color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onDelete(); onDismiss() })
        }
    }
}