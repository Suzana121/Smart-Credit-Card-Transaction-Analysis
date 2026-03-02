package com.cardify.app.ui.account

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image // הוספנו את זה כדי שהשגיאה תיעלם
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.cardify.app.R
import com.cardify.app.ui.components.AppScaffold

@Composable
fun AccountScreen(
    viewModel: AccountViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit
) {
    // שליפת הנתונים מה-Session דרך ה-ViewModel
    val username by viewModel.username.collectAsState()
    val email by viewModel.email.collectAsState()

    AppScaffold(
        title = "Account",
        currentRoute = "account",
        onNavigate = onNavigate
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(30.dp)) }

            // --- תמונת פרופיל (הקובץ המאוחד מהפיגמה) ---
            item {
                Image(
                    painter = painterResource(id = R.drawable.profile_placeholder),
                    contentDescription = "Profile Picture",
                    modifier = Modifier.size(117.dp) // לפי מידות פיגמה
                )
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }

            // --- שדות המידע לפי העיצוב ---
            item { AccountInfoField(label = "Fullname", value = username) }
            item { AccountInfoField(label = "Email", value = email) }
            item { AccountInfoField(label = "Phone", value = "052-212-3123") }

            item { Spacer(modifier = Modifier.height(32.dp)) }

            // --- כפתור התנתקות ---
            item {
                Button(
                    onClick = { viewModel.logout(onLogout) },
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0D7377) // הצבע מה-Theme
                    ),
                    shape = RoundedCornerShape(25.dp) // רדיוס לפי פיגמה
                ) {
                    Text("Logout", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
fun AccountInfoField(label: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {
        Text(
            text = label,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            modifier = Modifier.padding(start = 12.dp, bottom = 4.dp)
        )
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(45.dp), // גובה מותאם לנגישות ורספונסיביות
            shape = RoundedCornerShape(25.dp),
            border = BorderStroke(1.dp, Color(0xFF006769)), // צבע Stroke מהפיגמה
            color = Color.White
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(text = value, color = Color.Gray, fontSize = 14.sp)
            }
        }
    }
}