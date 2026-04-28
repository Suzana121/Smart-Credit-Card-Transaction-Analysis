package com.cardify.app.ui.home

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cardify.app.R
import com.cardify.app.data.UserSession
import com.cardify.app.data.model.Transaction
import com.cardify.app.ui.components.AppScaffold
import com.cardify.app.ui.components.TransactionRow
import com.cardify.app.ui.components.TransactionRowVariant
import com.cardify.app.ui.components.toTransactionItem

object CardifyColors {
    val DarkGreen      = Color(0xFF006769)
    val LightGreenText = Color(0xFF9FE88D)
    val TurquoiseBox   = Color(0xFFE6F7F7)
    val DashedBorder   = Color(0xFF006769)
    val IrregularRed   = Color(0xFFE23125)
    val RegularGreen   = Color(0xFF38D325)
    val TextPrimary    = Color(0xFF1A1A1A)
}

@Composable
fun HomeScreen(
    onNavigate: (String) -> Unit = {},
    onShareClick: (Transaction) -> Unit = {},
    viewModel: HomeViewModel = viewModel()
) {
    val context       = LocalContext.current
    var expanded      by remember { mutableStateOf(false) }
    var selectedLimit by remember { mutableStateOf("5") }
    var selectedFile  by remember { mutableStateOf<Uri?>(null) }

    val transactions    by viewModel.transactions.collectAsState()
    val isLoading       by viewModel.isLoading.collectAsState()
    val isUploading     by viewModel.isUploading.collectAsState()
    val uploadMessage   by viewModel.uploadMessage.collectAsState()
    val manualOverrides by viewModel.manualOverrides.collectAsState()

    val ibmPlexSans = FontFamily(
        Font(R.font.ibm_plex_sans_regular, FontWeight.Normal),
        Font(R.font.ibm_plex_sans_semibold, FontWeight.SemiBold)
    )

    LaunchedEffect(Unit) { viewModel.fetchTransactions(limit = selectedLimit.toIntOrNull() ?: 5) }
    LaunchedEffect(uploadMessage) {
        uploadMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            if (it.contains("Success", ignoreCase = true)) selectedFile = null
        }
    }

    AppScaffold(currentRoute = "home", onNavigate = onNavigate) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(padding)
        ) {
            if (manualOverrides.isNotEmpty()) {
                UpdateProfileBannerHome(
                    count = manualOverrides.size,
                    onUpdate = {
                        viewModel.updateProfile {
                            Toast.makeText(context, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                item {
                    Spacer(Modifier.height(10.dp))
                    Column(Modifier.fillMaxWidth()) {
                        val greeting = when (java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)) {
                            in 5..11  -> "Good Morning!"
                            in 12..16 -> "Good Afternoon!"
                            in 17..20 -> "Good Evening!"
                            else      -> "Good Night!"
                        }
                        Text(
                            greeting,
                            color = CardifyColors.LightGreenText,
                            fontSize = 18.sp,
                            fontFamily = ibmPlexSans
                        )
                        Text(
                            text       = UserSession.username ?: "Guest",
                            modifier   = Modifier.offset(y = (-12).dp),
                            color      = CardifyColors.DarkGreen,
                            fontSize   = 34.sp,
                            fontFamily = ibmPlexSans,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Upload
                item {
                    UploadSection(
                        selectedFile    = selectedFile,
                        isUploading     = isUploading,
                        ibmPlexSans     = ibmPlexSans,
                        onFileSelected  = { selectedFile = it },
                        onUploadClicked = { selectedFile?.let { viewModel.uploadFile(it, context) } }
                    )
                }

                // Title + Filter
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Your Last Transactions",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = ibmPlexSans
                        )
                        FilterDropdown(
                            selectedLimit  = selectedLimit,
                            expanded       = expanded,
                            onExpandChange = { expanded = it },
                            onLimitSelect  = {
                                selectedLimit = it
                                expanded = false
                                viewModel.fetchTransactions(limit = it.toIntOrNull() ?: 5)
                            },
                            ibmPlexSans = ibmPlexSans
                        )
                    }
                }

                if (isLoading) {
                    item {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = CardifyColors.DarkGreen)
                        }
                    }
                } else if (transactions.isEmpty()) {
                    item {
                        Text(
                            "No transactions found.",
                            color = Color.Gray,
                            fontFamily = ibmPlexSans,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    }
                } else {
                    items(transactions) { transaction ->
                        val currentStatus = manualOverrides[transaction.id] ?: transaction.status
                        val txItem = transaction
                            .copy(status = currentStatus)
                            .toTransactionItem()

                        TransactionRowWithConfirm(
                            transaction = txItem,
                            onShareClick = { onShareClick(transaction) },
                            onStatusConfirmed = { markIrregular ->
                                val newStatus = if (markIrregular) "IRREGULAR" else "REGULAR"
                                viewModel.updateTransactionStatus(transaction.id, newStatus)
                            }
                        )
                    }
                }

                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

// ─────────────────────────────────────────────
// TransactionRow + Confirm Dialog wrapper
// ─────────────────────────────────────────────
@Composable
fun TransactionRowWithConfirm(
    transaction: com.cardify.app.ui.components.TransactionItem,
    onShareClick: () -> Unit,
    onStatusConfirmed: (Boolean) -> Unit
) {
    var showConfirm      by remember { mutableStateOf(false) }
    var pendingIrregular by remember { mutableStateOf(false) }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("Are you sure?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    if (transaction.isIrregular) "Mark this transaction as Regular?"
                    else "Mark this transaction as Suspicious (Irregular)?",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirm = false
                        onStatusConfirmed(pendingIrregular)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (transaction.isIrregular) Color(0xFF2E7D32)
                        else CardifyColors.IrregularRed
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) { Text("Yes", color = Color.White, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showConfirm = false },
                    shape   = RoundedCornerShape(10.dp)
                ) { Text("Cancel") }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    TransactionRow(
        transaction    = transaction,
        variant        = TransactionRowVariant.FULL,
        onShareClick   = onShareClick,
        onStatusChange = { markIrregular ->
            pendingIrregular = markIrregular
            showConfirm = true
        }
    )
}

// ─────────────────────────────────────────────
// Upload Section
// ─────────────────────────────────────────────
@Composable
fun UploadSection(
    selectedFile: Uri?,
    isUploading: Boolean,
    ibmPlexSans: FontFamily,
    onFileSelected: (Uri?) -> Unit,
    onUploadClicked: () -> Unit
) {
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { onFileSelected(it) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            "Upload Your Latest Transactions",
            fontSize   = 15.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = ibmPlexSans
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFE0F2F1).copy(alpha = 0.5f))
                .drawBehind {
                    drawRoundRect(
                        color  = CardifyColors.DashedBorder.copy(alpha = 0.4f),
                        style  = Stroke(
                            width = 2f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                        ),
                        cornerRadius = CornerRadius(10.dp.toPx())
                    )
                }
                .clickable { launcher.launch("*/*") },
            contentAlignment = Alignment.Center
        ) {
            if (isUploading) {
                CircularProgressIndicator(color = CardifyColors.DarkGreen)
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        painterResource(R.drawable.ic_upload_custom),
                        contentDescription = null,
                        tint     = CardifyColors.DarkGreen,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (selectedFile != null) "File Ready" else "Tap to choose file",
                        fontWeight = FontWeight.SemiBold,
                        fontSize   = 13.sp
                    )
                    Text(
                        selectedFile?.lastPathSegment ?: "CSV, XLS up to 10MB",
                        fontSize = 11.sp,
                        color    = Color.Gray
                    )
                }
            }
        }
        Button(
            onClick  = onUploadClicked,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape    = RoundedCornerShape(12.dp),
            colors   = ButtonDefaults.buttonColors(
                containerColor = CardifyColors.LightGreenText,
                contentColor   = Color.Black
            )
        ) { Text("Upload", fontSize = 15.sp, fontWeight = FontWeight.Bold) }
    }
}

// ─────────────────────────────────────────────
// Update Profile Banner
// ─────────────────────────────────────────────
@Composable
fun UpdateProfileBannerHome(count: Int, onUpdate: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardifyColors.DarkGreen.copy(alpha = 0.1f))
            .border(1.dp, CardifyColors.DarkGreen, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.AutoFixHigh, contentDescription = null,
            tint = CardifyColors.DarkGreen, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Profile Update Available",
                fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                color = CardifyColors.DarkGreen)
            Text(
                "You corrected $count transaction(s). Update your profile to improve future detection.",
                fontSize = 11.sp, color = Color.Gray)
        }
        Spacer(Modifier.width(8.dp))
        Button(
            onClick  = onUpdate,
            colors   = ButtonDefaults.buttonColors(containerColor = CardifyColors.DarkGreen),
            shape    = RoundedCornerShape(10.dp),
            modifier = Modifier.height(34.dp),
            contentPadding = PaddingValues(horizontal = 12.dp)
        ) { Text("Update", fontSize = 12.sp) }
    }
}

// ─────────────────────────────────────────────
// Filter Dropdown
// ─────────────────────────────────────────────
@Composable
fun FilterDropdown(
    selectedLimit: String,
    expanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    onLimitSelect: (String) -> Unit,
    ibmPlexSans: FontFamily
) {
    Box(
        modifier = Modifier
            .width(110.dp)
            .background(CardifyColors.TurquoiseBox, RoundedCornerShape(10.dp))
            .clickable { onExpandChange(!expanded) }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Limit: $selectedLimit",
                    fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = ibmPlexSans)
                Spacer(Modifier.weight(1f))
                Icon(
                    if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null, modifier = Modifier.size(14.dp)
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column {
                    listOf("5", "10", "15").forEach {
                        Text(it,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onLimitSelect(it) }
                                .padding(vertical = 4.dp),
                            fontSize = 12.sp)
                    }
                }
            }
        }
    }
}