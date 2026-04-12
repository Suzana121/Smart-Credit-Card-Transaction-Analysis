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

/**
 * Screen that lets the authenticated user update their name, email, phone, and password.
 *
 * Displays a loading spinner while [EditAccountViewModel.isLoading] is `true`. Once loaded,
 * shows the profile avatar (tapping opens the gallery picker), four [EditField] inputs, and
 * an "Update" button. A note informs the user that changes take effect on the next sign-in.
 *
 * @param onNavigate Called with the destination route when a bottom nav item is tapped.
 * @param onBack Called on a successful update (typically pops the back stack).
 * @param viewModel The [EditAccountViewModel] managing field state and the update API call.
 */
@Composable
fun EditAccountScreen(
    onNavigate: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: EditAccountViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> /* TODO: העלאת תמונה */ }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap -> /* TODO: העלאת תמונה */ }

    AppScaffold(
        currentRoute = "account",
        onNavigate = onNavigate
    ) { padding ->
        if (viewModel.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = teal)
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
                Spacer(modifier = Modifier.height(55.dp))

                // ---- תמונת פרופיל ----
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(120.dp)) {
                    Image(
                        painter = painterResource(id = R.drawable.user),
                        contentDescription = "Profile Picture",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(108.dp).clickable { galleryLauncher.launch("image/*") }
                    )
                    Box(
                        modifier = Modifier.size(30.dp).align(Alignment.BottomEnd).background(teal, shape = CircleShape).clickable { cameraLauncher.launch(null) },
                        contentAlignment = Alignment.Center
                    ) {
                        Image(painter = painterResource(id = R.drawable.camera), contentDescription = "Open camera", modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                // ---- שדות עריכה ----
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    EditField(label = "Fullname", value = viewModel.name, onValueChange = { viewModel.name = it })
                    EditField(label = "Email", value = viewModel.email, onValueChange = { viewModel.email = it })
                    EditField(label = "Phone", value = viewModel.phone, onValueChange = { viewModel.phone = it })
                    EditField(label = "Password", value = viewModel.password, onValueChange = { viewModel.password = it }, isPassword = true, placeholder = "Leave empty to keep current")
                }

                viewModel.errorMessage?.let {
                    Text(text = it, color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                }

                Spacer(modifier = Modifier.height(50.dp))

                Button(
                    onClick = { viewModel.updateAccountDetails(onSuccess = { onBack() }) },
                    modifier = Modifier.width(220.dp).height(38.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !viewModel.isUpdating,
                    colors = ButtonDefaults.buttonColors(containerColor = teal)
                ) {
                    if (viewModel.isUpdating) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Update", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                Text(
                    text = buildAnnotatedString {
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) { append("Please make note ") }
                        append("that changes will take effect the next time you sign in")
                    },
                    fontSize = 13.sp, color = Color.Black, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp)
                )
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

/**
 * A labelled outlined text field used in the edit-account form.
 *
 * @param label The field label displayed above the input and styled in teal.
 * @param value The current field value.
 * @param onValueChange Called on every keystroke with the updated value.
 * @param isPassword When `true` the input is visually obscured with [PasswordVisualTransformation].
 * @param placeholder Hint text shown inside the field when [value] is empty.
 */
@Composable
fun EditField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    isPassword: Boolean = false,
    placeholder: String = ""
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = teal, modifier = Modifier.padding(start = 4.dp, bottom = 6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = Color.Gray, fontSize = 14.sp) },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp),
            visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AppTeal,
                unfocusedBorderColor = AppTeal,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
            ),
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, color = Color.Gray)
        )
    }
}