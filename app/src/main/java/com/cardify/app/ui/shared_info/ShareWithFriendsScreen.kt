/*
package com.cardify.app.ui.shared_info

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.navigation.NavHostController
import com.cardify.app.ui.components.AppScaffold

@Composable
fun ShareWithFriendsScreen(
    navController: NavHostController,
    transactionId: String?, // קבלת ה-ID מהניווט
    onNavigate: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: ShareWithFriendsViewModel = viewModel()
) {
    val friends by viewModel.friends.collectAsState()
    val isSending by viewModel.isSending.collectAsState()
    val context = LocalContext.current
    val teal = Color(0xFF006769)

    // טעינת רשימת החברים במידה והיא לא נטענה אוטומטית ב-init של ה-VM
    LaunchedEffect(Unit) {
        viewModel.loadFriends()
    }

    AppScaffold(navController = navController,currentRoute = "wallet", onNavigate = onNavigate) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Text(
                "Share with Friends",
                modifier = Modifier.padding(16.dp),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(friends) { friend ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = friend.photoResource),
                            contentDescription = null,
                            modifier = Modifier.size(50.dp).clip(CircleShape)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(friend.name, modifier = Modifier.weight(1f))

                        Button(
                            onClick = {
                                // בדיקה שיש לנו ID תקין לפני השליחה
                                if (transactionId != null) {
                                    viewModel.sendShare(friend.phone, transactionId) {
                                        Toast.makeText(context, "Shared with ${friend.name}!", Toast.LENGTH_SHORT).show()
                                        onBack()
                                    }
                                } else {
                                    Toast.makeText(context, "Error: Transaction ID missing", Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = !isSending,
                            colors = ButtonDefaults.buttonColors(containerColor = teal)
                        ) {
                            Text("Send")
                        }
                    }
                }
            }
        }
    }
}*/
