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

// צבע Teal מותאם אישית למותג Cardify
private val tealColor = Color(0xFF006769)

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

    // דיאלוג אישור מחיקה או ביטול בקשה
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

                    // הצגת אפשרויות ניקוי קבצים רק אם הם כבר חברים (לא בסטטוס ממתין)
                    if (!friend.isPending) {
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { deleteSent = !deleteSent }
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = deleteSent,
                                onCheckedChange = { deleteSent = it },
                                colors = CheckboxDefaults.colors(checkedColor = tealColor)
                            )
                            Text("Delete files I shared with them", fontSize = 14.sp)
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { deleteReceived = !deleteReceived }
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = deleteReceived,
                                onCheckedChange = { deleteReceived = it },
                                colors = CheckboxDefaults.colors(checkedColor = tealColor)
                            )
                            Text("Delete files they shared with me", fontSize = 14.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        // קריאה לפונקציה המעודכנת ב-ViewModel עם פרמטרים של מחיקה חכמה[cite: 3]
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
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    // התפריט התחתון (Bottom Sheet) שמציג את פרטי החבר
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // אייקון פרופיל
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(Color(0xFFF0F0F0), RoundedCornerShape(40.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = Color.Gray
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            // שם ומספר טלפון
            Text(text = friend.name, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(text = friend.phone, fontSize = 16.sp, color = Color.Gray)

            // חיווי ויזואלי אם הבקשה עדיין בסטטוס ממתין (Pending)
            if (friend.isPending) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFFFF3E0)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = Color(0xFFEF6C00),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
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

            // כפתור פעולה למחיקה או ביטול
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDeleteDialog = true },
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFFFF5F5)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = Color(0xFFD32F2F)
                    )
                    Spacer(modifier = Modifier.width(12.dp))

                    val actionText = if (friend.isPending) "Cancel Friend Request" else "Delete Friend"
                    Text(
                        text = actionText,
                        color = Color(0xFFD32F2F),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}