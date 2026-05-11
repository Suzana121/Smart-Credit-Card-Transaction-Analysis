package com.cardify.app.ui.home

import android.net.Uri
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.navigation.NavHostController
import com.cardify.app.R
import com.cardify.app.data.UserSession
import com.cardify.app.data.model.Transaction
import com.cardify.app.ui.components.AppScaffold
import com.cardify.app.ui.components.TransactionRow
import com.cardify.app.ui.components.TransactionRowVariant
import com.cardify.app.ui.components.toTransactionItem
import com.cardify.app.ui.components.ProcessingOverlay
import java.util.Calendar
import com.cardify.app.ui.components.TransactionItem

@Composable
fun HomeScreen(
    navController: NavHostController,
    onNavigate: (String) -> Unit = {},
    onShareClick: (Transaction) -> Unit = {},
    viewModel: HomeViewModel = viewModel()
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme

    var expanded        by remember { mutableStateOf(false) }
    var selectedLimit   by remember { mutableStateOf("5") }
    var selectedFile    by remember { mutableStateOf<Uri?>(null) }
    var bannerDismissed by remember { mutableStateOf(false) }

    val transactions    by viewModel.transactions.collectAsState()
    val isLoading       by viewModel.isLoading.collectAsState()
    val isUploading     by viewModel.isUploading.collectAsState()
    val isProcessing    by viewModel.isProcessing.collectAsState()
    val isSuccess       by viewModel.isSuccess.collectAsState()
    val manualOverrides by viewModel.manualOverrides.collectAsState()

    val ibmPlexSans = FontFamily(
        Font(R.font.ibm_plex_sans_regular, FontWeight.Normal),
        Font(R.font.ibm_plex_sans_semibold, FontWeight.SemiBold)
    )



    // לוגיקת ברכה לפי שעה
    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 5..11 -> "Good morning,"
            in 12..17 -> "Good afternoon,"
            in 18..21 -> "Good evening,"
            else -> "Good night,"
        }
    }

    LaunchedEffect(Unit) { viewModel.fetchTransactions(limit = selectedLimit.toIntOrNull() ?: 5) }
    LaunchedEffect(manualOverrides) { if (manualOverrides.isNotEmpty()) bannerDismissed = false }

        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colorScheme.background)
            ) {
                // באנר עדכון מודל AI
                if (manualOverrides.isNotEmpty()) {
                    if (!bannerDismissed) {
                        UpdateProfileBannerHome(
                            count     = manualOverrides.size,
                            onUpdate  = {
                                viewModel.updateProfile {
                                    Toast.makeText(context, "Profile updated!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onDismiss = { bannerDismissed = true },
                            modifier  = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    } else {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(colorScheme.primary.copy(alpha = 0.1f))
                                .clickable { bannerDismissed = false }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.AutoFixHigh, null,
                                tint = colorScheme.primary, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("${manualOverrides.size} pending update",
                                fontSize = 12.sp, color = colorScheme.primary,
                                fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Spacer(Modifier.height(15.dp))
                        Column(Modifier.fillMaxWidth()) {
                            Text(
                                text = greeting,
                                color = colorScheme.onBackground.copy(alpha = 0.6f),
                                fontSize = 18.sp,
                                fontFamily = ibmPlexSans
                            )
                            Text(
                                text       = UserSession.username ?: "Guest",
                                modifier   = Modifier.offset(y = (-8).dp),
                                color      = colorScheme.primary,
                                fontSize   = 34.sp,
                                fontFamily = ibmPlexSans,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    item {
                        UploadSection(
                            selectedFile    = selectedFile,
                            isUploading     = isUploading,
                            ibmPlexSans     = ibmPlexSans,
                            onFileSelected  = { selectedFile = it },
                            onUploadClicked = { selectedFile?.let { viewModel.uploadFile(it, context) } }
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Transactions",
                                fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                                fontFamily = ibmPlexSans, color = colorScheme.onBackground)
                            FilterDropdown(
                                selectedLimit  = selectedLimit,
                                expanded       = expanded,
                                onExpandChange = { expanded = it },
                                onLimitSelect  = {
                                    selectedLimit = it
                                    expanded = false
                                    viewModel.fetchTransactions(limit = it.toInt())
                                },
                                ibmPlexSans = ibmPlexSans
                            )
                        }
                    }

                    if (isLoading) {
                        item {
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = colorScheme.primary)
                            }
                        }
                    } else {
                        items(transactions) { transaction ->
                            val currentStatus = manualOverrides[transaction.id] ?: transaction.status
                            val txItem = transaction.copy(status = currentStatus).toTransactionItem()
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

            if (isProcessing) {
                ProcessingOverlay(isSuccess = isSuccess)
            }
        }
    }

fun getFileSize(context: android.content.Context, uri: Uri): String {
    return try {
        context.contentResolver.openFileDescriptor(uri, "r")?.use {
            val size = it.statSize
            when {
                size < 1024 -> "$size B"
                size < 1024 * 1024 -> "${size / 1024} KB"
                else -> String.format("%.2f MB", size.toDouble() / (1024 * 1024))
            }
        } ?: "Unknown size"
    } catch (e: Exception) {
        "Unknown size"
    }
}
// פונקציית עזר לבדיקת סיומת הקובץ
fun isFileExtensionValid(context: android.content.Context, uri: Uri): Boolean {
    val contentResolver = context.contentResolver
    val type = contentResolver.getType(uri)
    // בדיקה לפי MIME Type
    if (type != null) {
        if (type == "text/csv" ||
            type == "application/vnd.ms-excel" ||
            type == "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") {
            return true
        }
    }
    // בדיקה נוספת לפי שם הקובץ ליתר ביטחון
    val name = getFileName(context, uri).lowercase()
    return name.endsWith(".csv") || name.endsWith(".xlsx") || name.endsWith(".xls")
}

// פונקציית עזר לקבלת שם הקובץ (לצורך הבדיקה למעלה)
fun getFileName(context: android.content.Context, uri: Uri): String {
    var name = ""
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1) name = cursor.getString(nameIndex)
        }
    }
    return name
}
@Composable
fun UploadSection(
    selectedFile: Uri?,
    isUploading: Boolean,
    ibmPlexSans: FontFamily,
    onFileSelected: (Uri?) -> Unit,
    onUploadClicked: () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { onFileSelected(it) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Upload Transactions", fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = ibmPlexSans)

        Box(
            modifier = Modifier
                .fillMaxWidth().height(140.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(colorScheme.primaryContainer.copy(alpha = 0.1f))
                .drawBehind {
                    drawRoundRect(
                        color = colorScheme.primary.copy(alpha = 0.3f),
                        style = Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)),
                        cornerRadius = CornerRadius(12.dp.toPx())
                    )
                }
                .clickable { launcher.launch("*/*") },
            contentAlignment = Alignment.Center
        ) {
            if (isUploading) {
                CircularProgressIndicator(color = colorScheme.primary)
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(painterResource(R.drawable.ic_upload_custom), null,
                        tint = colorScheme.primary, modifier = Modifier.size(30.dp))
                    Text(if (selectedFile != null) "Ready to Process" else "Tap to Select File",
                        fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = colorScheme.primary)
                    Text(
                        text = if (selectedFile != null) "File Size: ${getFileSize(context, selectedFile)}"
                        else "CSV or Excel file up to 5MB",
                        fontSize = 11.sp, color = Color.Gray
                    )
                }
            }
        }

        Button(
            onClick = {
                if (selectedFile == null) {
                    Toast.makeText(context, "Please select a file first", Toast.LENGTH_SHORT).show()
                } else if (!isFileExtensionValid(context, selectedFile)) {
                    // כאן אנחנו עוצרים את זה לפני שזה מגיע לשרת!
                    Toast.makeText(context, "Invalid file type. Please select CSV or Excel only.", Toast.LENGTH_LONG).show()
                } else {
                    onUploadClicked()
                }
            },

            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp),
            enabled = !isUploading,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (selectedFile != null) colorScheme.primary else Color.Gray,
                contentColor = Color.White
            )
        ) {
            Text("Process File", fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun FilterDropdown(
    selectedLimit: String,
    expanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    onLimitSelect: (String) -> Unit,
    ibmPlexSans: FontFamily
) {
    val colorScheme = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .width(110.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(colorScheme.primaryContainer.copy(alpha = 0.2f))
            .clickable { onExpandChange(!expanded) }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Limit: $selectedLimit",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = ibmPlexSans,
                    color = colorScheme.primary
                )
                Spacer(Modifier.weight(1f))
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = colorScheme.primary
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column {
                    listOf("5", "10", "15").forEach { limit ->
                        Text(
                            text = limit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onLimitSelect(limit) }
                                .padding(vertical = 4.dp),
                            fontSize = 12.sp,
                            color = colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionRowWithConfirm(
    transaction: com.cardify.app.ui.components.TransactionItem,
    onShareClick: () -> Unit,
    onStatusConfirmed: (Boolean) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
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
                    color = colorScheme.onSurface.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { showConfirm = false; onStatusConfirmed(pendingIrregular) },
                    colors  = ButtonDefaults.buttonColors(
                        containerColor = if (transaction.isIrregular) colorScheme.primary else colorScheme.error
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) { Text("Yes", color = Color.White, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showConfirm = false },
                    shape = RoundedCornerShape(10.dp)
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

@Composable
fun UpdateProfileBannerHome(
    count: Int,
    onUpdate: () -> Unit,
    onDismiss: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colorScheme.primary.copy(alpha = 0.08f))
            .border(1.dp, colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.AutoFixHigh, null, tint = colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Update AI Model", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = colorScheme.primary)
            Text("Based on $count corrections", fontSize = 11.sp, color = colorScheme.onBackground.copy(alpha = 0.7f))
        }
        Button(
            onClick = onUpdate,
            colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
            modifier = Modifier.height(32.dp)
        ) { Text("Update", fontSize = 12.sp) }
        IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, null, tint = colorScheme.primary, modifier = Modifier.size(16.dp))
        }
    }
}