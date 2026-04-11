package com.cardify.app.ui.account

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Modal bottom sheet for sending a friend request by phone number.
 *
 * The user types a phone number and taps the search icon to look up the account.
 * If a match is found a card shows the user's name and phone with an "Add" button.
 * A loading spinner replaces the search icon while the lookup is in progress.
 *
 * @param viewModel The [AccountViewModel] that owns the search state and handles the
 *   friend-request API call.
 * @param onDismiss Called when the sheet is dismissed (drag or cancel). Also clears the
 *   search state via [AccountViewModel.clearSearch].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFriendSheet(
    viewModel: AccountViewModel,
    onDismiss: () -> Unit
) {
    var phoneInput by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
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
                text = "Add a friend",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            OutlinedTextField(
                value = phoneInput,
                onValueChange = { phoneInput = it },
                label = { Text("Phone number") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                trailingIcon = {
                    if (viewModel.isSearching) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = teal)
                    } else {
                        IconButton(onClick = { viewModel.searchUser(phoneInput) }) {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = teal)
                        }
                    }
                }
            )

            if (viewModel.searchErrorMessage != null) {
                Text(text = viewModel.searchErrorMessage!!, color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            AnimatedVisibility(visible = viewModel.searchedUser != null) {
                viewModel.searchedUser?.let { user ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F8F9)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text(text = user.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text(text = user.phone, fontSize = 14.sp, color = Color.Gray)
                            }
                            Button(
                                onClick = { viewModel.sendFriendRequest(user.phone) { onDismiss() } },
                                colors = ButtonDefaults.buttonColors(containerColor = teal)
                            ) {
                                Text("Add")
                            }
                        }
                    }
                }
            }
        }
    }
}