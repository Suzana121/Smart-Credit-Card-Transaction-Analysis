package com.cardify.app.ui.edit_account

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cardify.app.R
import com.cardify.app.ui.account.teal
import com.cardify.app.ui.components.AppScaffold
import com.cardify.app.ui.components.AppTeal

@Composable
fun EditAccountScreen(
    onNavigate: (String) -> Unit,
    onBack: () -> Unit
) {
    var name     by remember { mutableStateOf("Hailey David") }
    var email    by remember { mutableStateOf("hailey@gmail.com") }
    var phone    by remember { mutableStateOf("+972 52-212-3123") }
    var password by remember { mutableStateOf("123456") }

    // פותח גלריה — לחיצה על תמונת הפרופיל
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> /* TODO: טפל בתמונה שנבחרה */ }

    // פותח מצלמה — לחיצה על אייקון המצלמה
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap -> /* TODO: טפל בתמונה שצולמה */ }

    AppScaffold(
        currentRoute = "account",
        onNavigate = onNavigate
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(55.dp))

            // ---- תמונת פרופיל + אייקון מצלמה ----
            // Box חיצוני קצת יותר גדול מהתמונה כדי שהעיגול לא ייחתך
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(120.dp)
            ) {
                // תמונת פרופיל — לחיצה פותחת גלריה
                Image(
                    painter = painterResource(id = R.drawable.user),
                    contentDescription = "Profile Picture",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(108.dp)
                        .clickable { galleryLauncher.launch("image/*") }
                )

                // עיגול מצלמה — ממוקם בפינה ימין תחתון של התמונה
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .align(Alignment.BottomEnd)
                        .background(teal, shape = CircleShape)
                        .clickable { cameraLauncher.launch(null) },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.camera),
                        contentDescription = "Open camera",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // ---- שדות עריכה ----
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                EditField(label = "Fullname", value = name,     onValueChange = { name = it })
                EditField(label = "Email",    value = email,    onValueChange = { email = it })
                EditField(label = "Phone",    value = phone,    onValueChange = { phone = it })
                EditField(
                    label = "Password",
                    value = password,
                    onValueChange = { password = it },
                    isPassword = true,
                )
            }

            Spacer(modifier = Modifier.height(50.dp))

            // ---- כפתור עדכון ----
            Button(
                onClick = { onBack() },
                modifier = Modifier
                    .width(220.dp)
                    .height(38.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = teal)
            ) {
                Text("Update", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(40.dp))

            // ---- טקסט הערה ----
            Text(
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("Please make note ")
                    }
                    append("that changes will take effect the next time you sign in")
                },
                fontSize = 13.sp,
                color = Color.Black,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun EditField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    isPassword: Boolean = false,
    placeholder: String = ""
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = teal,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = Color.Gray, fontSize = 14.sp) },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp),
            visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor      = AppTeal,
                unfocusedBorderColor    = AppTeal,
                focusedContainerColor   = Color.White,
                unfocusedContainerColor = Color.White,
            ),
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, color = Color.Gray)
        )
    }
}