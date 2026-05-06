package com.cardify.app.ui.account

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.ui.platform.LocalLayoutDirection
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
fun FriendSheet(
    friend:    Friend,
    viewModel: AccountViewModel,
    onDismiss: () -> Unit
) {
    val context      = LocalContext.current
    val nicknames    by viewModel.nicknames.collectAsState()
    val currentNick  = nicknames[friend.phone] ?: ""
    val displayName  = currentNick.ifBlank { friend.name }

    var showDeleteDialog   by remember { mutableStateOf(false) }
    var showNicknameDialog by remember { mutableStateOf(false) }
    var deleteSent         by remember { mutableStateOf(false) }
    var deleteReceived     by remember { mutableStateOf(false) }

    // ─── דיאלוג מחיקה ────────────────────────────────────────────
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text(
                    if (friend.isPending) "Cancel Friend Request?" else "Delete $displayName?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        if (friend.isPending) "Are you sure you want to cancel this friend request?"
                        else "Remove this friend? This cannot be undone.",
                        color = Color.Gray
                    )
                    if (!friend.isPending) {
                        Spacer(Modifier.height(16.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { deleteSent = !deleteSent }
                        ) {
                            Checkbox(checked = deleteSent, onCheckedChange = { deleteSent = it },
                                colors = CheckboxDefaults.colors(checkedColor = teal))
                            Text("Delete files I shared with them", fontSize = 14.sp)
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { deleteReceived = !deleteReceived }
                        ) {
                            Checkbox(checked = deleteReceived, onCheckedChange = { deleteReceived = it },
                                colors = CheckboxDefaults.colors(checkedColor = teal))
                            Text("Delete files they shared with me", fontSize = 14.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteFriendWithOptions(friend, deleteSent, deleteReceived)
                        showDeleteDialog = false
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text(if (friend.isPending) "Cancel Request" else "Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    // ─── דיאלוג שינוי כינוי ──────────────────────────────────────
    if (showNicknameDialog) {
        var nicknameInput by remember { mutableStateOf(currentNick) }
        CompositionLocalProvider(LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr) {
            AlertDialog(
                onDismissRequest = { showNicknameDialog = false },
                title = { Text("Rename ${friend.name}", fontWeight = FontWeight.Bold) },
                text  = {
                    Column {
                        Text(
                            "Set a custom name for ${friend.name}.\nLeave blank to use the original name.",
                            fontSize = 13.sp, color = Color.Gray
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value         = nicknameInput,
                            onValueChange = { nicknameInput = it },
                            label         = { Text("Custom name") },
                            singleLine    = true,
                            modifier      = Modifier.fillMaxWidth(),
                            colors        = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = teal)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.setNickname(friend.phone, nicknameInput.trim())
                            showNicknameDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = teal)
                    ) { Text("Save") }
                },
                dismissButton = {
                    TextButton(onClick = { showNicknameDialog = false }) { Text("Cancel") }
                },
                shape = RoundedCornerShape(16.dp)
            )
        }
    }

    // ─── Bottom Sheet ─────────────────────────────────────────────
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .padding(bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── תמונת פרופיל ──
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
                    Icon(Icons.Default.Person, null,
                        modifier = Modifier.size(40.dp), tint = Color.Gray)
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(displayName, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            if (currentNick.isNotBlank()) {
                Text(friend.name, fontSize = 13.sp, color = Color.Gray)
            }
            Text(friend.phone, fontSize = 16.sp, color = Color.Gray)

            if (friend.isPending) {
                Spacer(Modifier.height(8.dp))
                Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFFFFF3E0)) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Timer, null,
                            tint = Color(0xFFEF6C00), modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Pending Approval", color = Color(0xFFEF6C00),
                            fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ─── שינוי כינוי ──────────────────────────────────────
            Surface(
                modifier = Modifier.fillMaxWidth().clickable { showNicknameDialog = true },
                shape    = RoundedCornerShape(12.dp),
                color    = Color(0xFFF7F8F9)
            ) {
                Row(modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Edit, null, tint = teal)
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("Rename contact", color = teal, fontWeight = FontWeight.Medium)
                        if (currentNick.isNotBlank()) {
                            Text("Currently: $currentNick", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // ─── מחיקה ────────────────────────────────────────────
            Surface(
                modifier = Modifier.fillMaxWidth().clickable { showDeleteDialog = true },
                shape    = RoundedCornerShape(12.dp),
                color    = Color(0xFFF7F8F9)
            ) {
                Row(modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Delete, null, tint = Color(0xFFD32F2F))
                    Spacer(Modifier.width(16.dp))
                    Text(
                        if (friend.isPending) "Cancel Friend Request" else "Delete Friend",
                        color = Color(0xFFD32F2F), fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}