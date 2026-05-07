package com.cardify.app.ui.edit_account

import EditAccountViewModel
import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.cardify.app.R
import com.cardify.app.ui.components.AppScaffold
import com.cardify.app.ui.components.AppTeal
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAccountScreen(
    onNavigate: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: EditAccountViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val context = LocalContext.current

    var showImagePicker by remember { mutableStateOf(false) }
    var cameraImageUri  by remember { mutableStateOf<Uri?>(null) }

    // גלריה
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let { viewModel.uploadProfileImage(it, context) } }

    // מצלמה
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) cameraImageUri?.let { viewModel.uploadProfileImage(it, context) }
    }

    // הרשאת מצלמה
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            val tempFile = File(context.cacheDir, "camera_profile_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempFile)
            cameraImageUri = uri
            cameraLauncher.launch(uri)
        }
    }

    fun openCamera() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            val tempFile = File(context.cacheDir, "camera_profile_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempFile)
            cameraImageUri = uri
            cameraLauncher.launch(uri)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Bottom Sheet לבחירת מקור תמונה
    if (showImagePicker) {
        ModalBottomSheet(
            onDismissRequest = { showImagePicker = false },
            containerColor   = Color.White,
            shape            = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Column(
                modifier            = Modifier.fillMaxWidth()
                    .padding(horizontal = 24.dp).padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Profile Photo", fontSize = 18.sp, fontWeight = FontWeight.Bold,
                    color = Color.Black, modifier = Modifier.padding(bottom = 8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF5F5F5))
                        .clickable { showImagePicker = false; openCamera() }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center) {
                        Image(painter = painterResource(id = R.drawable.camera),
                            contentDescription = null, modifier = Modifier.size(22.dp))
                    }
                    Column {
                        Text("Take a photo", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Text("Use your camera", fontSize = 12.sp, color = Color.Gray)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF5F5F5))
                        .clickable { showImagePicker = false; galleryLauncher.launch("image/*") }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center) {
                        Text("🖼", fontSize = 20.sp)
                    }
                    Column {
                        Text("Choose from gallery", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Text("Pick an existing photo", fontSize = 12.sp, color = Color.Gray)
                    }
                }

                TextButton(onClick = { showImagePicker = false }, modifier = Modifier.fillMaxWidth()) {
                    Text("Cancel", color = Color.Gray, fontSize = 15.sp)
                }
            }
        }
    }

    AppScaffold(currentRoute = "account", onNavigate = onNavigate) { padding ->
        if (viewModel.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
                    .padding(padding)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(55.dp))

                // ── תמונת פרופיל ──
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(120.dp)) {
                    when {
                        viewModel.isUploading -> {
                            Box(Modifier.size(108.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        !viewModel.profileImage.isNullOrEmpty() -> {
                            AsyncImage(
                                model              = viewModel.profileImage,
                                contentDescription = "Profile Picture",
                                contentScale       = ContentScale.Crop,
                                modifier           = Modifier.size(108.dp).clip(CircleShape)
                                    .clickable { showImagePicker = true },
                                error = painterResource(id = R.drawable.user)
                            )
                        }
                        else -> {
                            Image(
                                painter            = painterResource(id = R.drawable.user),
                                contentDescription = "Profile Picture",
                                contentScale       = ContentScale.Crop,
                                modifier           = Modifier.size(108.dp).clip(CircleShape)
                                    .clickable { showImagePicker = true }
                            )
                        }
                    }
                    Box(
                        modifier = Modifier.size(30.dp).align(Alignment.BottomEnd)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                            .clickable(enabled = !viewModel.isUploading) { showImagePicker = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Image(painter = painterResource(id = R.drawable.camera),
                            contentDescription = "Change photo", modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(Modifier.height(40.dp))

                // ── שדות עריכה ──
                Column(
                    modifier            = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    EditField("Full Name", viewModel.name,  { viewModel.name = it })
                    EditField("Email",     viewModel.email, { viewModel.email = it })
                    EditField("Phone",     viewModel.phone, { viewModel.phone = it },
                        placeholder = "e.g. 0521234567")

                    // שדה סיסמה עם אייקון עין ודרישות חיות
                    PasswordFieldWithRequirements(
                        value         = viewModel.password,
                        onValueChange = { viewModel.password = it }
                    )
                }

                // הודעת שגיאה
                viewModel.errorMessage?.let { error ->
                    Text(text = error, color = Color.Red, fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 12.dp, start = 24.dp, end = 24.dp))
                }

                Spacer(Modifier.height(50.dp))

                Button(
                    onClick  = { viewModel.updateAccountDetails(onSuccess = { onBack() }) },
                    modifier = Modifier.width(220.dp).height(38.dp),
                    shape    = RoundedCornerShape(12.dp),
                    enabled  = !viewModel.isUpdating && !viewModel.isUploading,
                    colors   = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    if (viewModel.isUpdating) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("Update", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.height(40.dp))

                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("Please note ") }
                        append("that changes will take effect the next time you sign in")
                    },
                    fontSize = 13.sp, color = Color.Black, textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )

                Spacer(Modifier.height(40.dp))
            }
        }
    }
}

// ── שדה סיסמה עם אייקון עין + דרישות חיות ────────────────────────────────────

@Composable
fun PasswordFieldWithRequirements(
    value:         String,
    onValueChange: (String) -> Unit
) {
    var passwordVisible by remember { mutableStateOf(false) }

    // חישוב דרישות בזמן אמת
    val hasMinLength   = value.length >= 8
    val hasUppercase   = value.any { it.isUpperCase() }
    val hasLowercase   = value.any { it.isLowerCase() }
    val hasDigit       = value.any { it.isDigit() }
    val hasSpecial     = value.any { !it.isLetterOrDigit() }
    val showRequirements = value.isNotEmpty()

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = "Password", fontSize = 16.sp, fontWeight = FontWeight.Bold,
            color = AppTeal, modifier = Modifier.padding(start = 4.dp, bottom = 6.dp))

        OutlinedTextField(
            value         = value,
            onValueChange = onValueChange,
            placeholder   = { Text("Leave empty to keep current", color = Color.Gray, fontSize = 14.sp) },
            modifier      = Modifier.fillMaxWidth().height(56.dp),
            shape         = RoundedCornerShape(12.dp),
            visualTransformation = if (passwordVisible) VisualTransformation.None
            else PasswordVisualTransformation(),
            trailingIcon  = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector        = ImageVector.vectorResource(
                            if (passwordVisible) R.drawable.ic_eye_open
                            else R.drawable.ic_eye_closed
                        ),
                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                        tint               = Color.Gray
                    )
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor      = AppTeal,
                unfocusedBorderColor    = AppTeal,
                focusedContainerColor   = Color.White,
                unfocusedContainerColor = Color.White,
            ),
            singleLine = true,
            textStyle  = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, color = Color.Gray)
        )

        // דרישות סיסמה — מופיעות רק כשמתחילים להקליד
        AnimatedVisibility(
            visible = showRequirements,
            enter   = expandVertically() + fadeIn(),
            exit    = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFF5F5F5))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("Password requirements:", fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold, color = Color.DarkGray,
                    modifier = Modifier.padding(bottom = 4.dp))
                PasswordRequirementRow("At least 8 characters",       hasMinLength)
                PasswordRequirementRow("At least one uppercase letter", hasUppercase)
                PasswordRequirementRow("At least one lowercase letter", hasLowercase)
                PasswordRequirementRow("At least one number",           hasDigit)
                PasswordRequirementRow("At least one special character (!@#\$...)", hasSpecial)
            }
        }
    }
}

@Composable
private fun PasswordRequirementRow(text: String, isMet: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text     = if (isMet) "✓" else "✗",
            color    = if (isMet) Color(0xFF2E7D32) else Color(0xFFE23125),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(20.dp)
        )
        Text(
            text     = text,
            fontSize = 12.sp,
            color    = if (isMet) Color(0xFF2E7D32) else Color.Gray
        )
    }
}

// ── שדה עריכה רגיל ───────────────────────────────────────────────────────────

@Composable
fun EditField(
    label:         String,
    value:         String,
    onValueChange: (String) -> Unit,
    placeholder:   String = ""
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, fontSize = 16.sp, fontWeight = FontWeight.Bold,
            color = AppTeal, modifier = Modifier.padding(start = 4.dp, bottom = 6.dp))
        OutlinedTextField(
            value         = value,
            onValueChange = onValueChange,
            placeholder   = { Text(placeholder, color = Color.Gray, fontSize = 14.sp) },
            modifier      = Modifier.fillMaxWidth().height(56.dp),
            shape         = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor      = AppTeal,
                unfocusedBorderColor    = AppTeal,
                focusedContainerColor   = Color.White,
                unfocusedContainerColor = Color.White,
            ),
            singleLine = true,
            textStyle  = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, color = Color.Gray)
        )
    }
}