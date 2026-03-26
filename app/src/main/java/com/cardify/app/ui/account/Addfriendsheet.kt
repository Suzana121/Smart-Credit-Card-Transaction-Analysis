package com.cardify.app.ui.account

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFriendSheet(
    onSend: (phone: String) -> Unit,
    onDismiss: () -> Unit
) {
    var phone by remember { mutableStateOf("") }
    var searchedName by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp),
        ) {
            Text(
                "Add a friend by phone",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                "Enter their phone number to find them on Cardify",
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // החלפתי את AddFriendField בשדה טקסט מובנה ומעוצב
            OutlinedTextField(
                value = phone,
                onValueChange = {
                    phone = it
                    isError = false
                },
                label = { Text("Phone number") },
                placeholder = { Text("e.g. 0501234567") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = teal,
                    unfocusedBorderColor = Color(0xFFE7E8E9),
                    focusedLabelColor = teal
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            // הצגת הודעת שגיאה אם המספר קצר מדי (אופציונלי)
            if (isError) {
                Text(
                    "Please enter a valid phone number",
                    color = Color.Red,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 16.dp, start = 4.dp)
                )
            }

            Button(
                onClick = {
                    if (phone.length >= 9) {
                        onSend(phone)
                        onDismiss()
                    } else {
                        isError = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = phone.length >= 9,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = teal)
            ) {
                Text("Send Request", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}