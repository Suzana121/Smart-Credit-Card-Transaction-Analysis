package com.cardify.app.ui.account

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cardify.app.data.model.Friend

/**
 * Modal bottom sheet showing details for an existing friend or a pending outgoing request.
 *
 * Tapping the delete action opens a confirmation [AlertDialog]. For non-pending friends the
 * dialog offers optional checkboxes to also delete shared files sent or received. Confirming
 * delegates to [AccountViewModel.deleteFriendWithOptions].
 *
 * @param friend The [Friend] whose details are displayed.
 * @param viewModel The [AccountViewModel] that handles the delete operation.
 * @param onDismiss Called when the sheet is dismissed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendSheet(
    friend: Friend,
    viewModel: AccountViewModel,
    onDismiss: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deleteSent by remember { mutableStateOf(false) }
    var deleteReceived by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                val titleText = if (friend.isPending) "Cancel Friend Request?" else "Delete ${friend.name}?"
                Text(text = titleText, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    val bodyText = if (friend.isPending) {
                        "Are you sure you want to cancel this friend request?"
                    } else {
                        "Remove this friend? This cannot be undone."
                    }
                    Text(text = bodyText, color = Color.Gray)

                    if (!friend.isPending) {
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { deleteSent = !deleteSent }
                        ) {
                            Checkbox(checked = deleteSent, onCheckedChange = { deleteSent = it }, colors = CheckboxDefaults.colors(checkedColor = teal))
                            Text("Delete files I shared with them", fontSize = 14.sp)
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { deleteReceived = !deleteReceived }
                        ) {
                            Checkbox(checked = deleteReceived, onCheckedChange = { deleteReceived = it }, colors = CheckboxDefaults.colors(checkedColor = teal))
                            Text("Delete files they shared with me", fontSize = 14.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (friend.isPending) {
                            viewModel.deleteFriendWithOptions(friend, false, false)
                        } else {
                            viewModel.deleteFriendWithOptions(friend, deleteSent, deleteReceived)
                        }
                        showDeleteDialog = false
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    val buttonText = if (friend.isPending) "Cancel Request" else "Delete"
                    Text(buttonText, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .padding(bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(Color(0xFFF0F0F0), RoundedCornerShape(40.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(40.dp), tint = Color.Gray)
            }
            Spacer(modifier = Modifier.height(16.dp))

            Text(text = friend.name, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(text = friend.phone, fontSize = 16.sp, color = Color.Gray)

            if (friend.isPending) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFFFF3E0)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = Color(0xFFEF6C00),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Pending Approval",
                            color = Color(0xFFEF6C00),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDeleteDialog = true },
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF7F8F9)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFD32F2F))
                    Spacer(modifier = Modifier.width(16.dp))

                    val actionText = if (friend.isPending) "Cancel Friend Request" else "Delete Friend"
                    Text(actionText, color = Color(0xFFD32F2F), fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}