package com.cardify.app.ui.account

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cardify.app.R
import com.cardify.app.data.model.Friend

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendRequestSheet(
    friend:    Friend,
    onConfirm: () -> Unit,
    onDelete:  () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor   = Color.White,
        shape            = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── תמונת פרופיל ──────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(80.dp)
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
                        modifier           = Modifier.fillMaxSize(),
                        error              = painterResource(R.drawable.user)
                    )
                } else {
                    Icon(
                        Icons.Default.Person, null,
                        modifier = Modifier.size(40.dp),
                        tint     = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text       = "Friend Request",
                fontSize   = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── פרטי החבר ─────────────────────────────────────────────────────
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(16.dp),
                color    = Color(0xFFF7F8F9),
                border   = BorderStroke(1.dp, Color(0xFFE7E8E9))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Name",  color = MaterialTheme.colorScheme.primary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(friend.name, color = Color.Black, fontSize = 16.sp)
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color    = Color(0xFFE7E8E9)
                    )
                    Text("Phone number", color = MaterialTheme.colorScheme.primary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(friend.phone, color = Color.Black, fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── כפתור אישור ───────────────────────────────────────────────────
            Button(
                onClick  = { onConfirm(); onDismiss() },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(12.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Confirm Request", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── מחיקת בקשה ────────────────────────────────────────────────────
            Text(
                text     = "Delete request",
                fontSize = 14.sp,
                color    = Color(0xFFD32F2F),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onDelete(); onDismiss() }
            )
        }
    }
}