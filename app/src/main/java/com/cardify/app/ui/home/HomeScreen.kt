package com.cardify.app.ui.home

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.border
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cardify.app.R
import com.cardify.app.data.UserSession
import com.cardify.app.data.model.Transaction
import com.cardify.app.ui.components.AppScaffold

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

    val transactions  by viewModel.transactions.collectAsState()
    val isLoading     by viewModel.isLoading.collectAsState()
    val isUploading   by viewModel.isUploading.collectAsState()
    val uploadMessage by viewModel.uploadMessage.collectAsState()
    var manualOverrides by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    val ibmPlexSans = FontFamily(
        Font(R.font.ibm_plex_sans_regular, FontWeight.Normal),
        Font(R.font.ibm_plex_sans_semibold, FontWeight.SemiBold)
    )

    LaunchedEffect(Unit) { viewModel.fetchTransactions() }
    LaunchedEffect(uploadMessage) {
        uploadMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            if (it.contains("Success", ignoreCase = true)) selectedFile = null
        }
    }

    AppScaffold(currentRoute = "home", onNavigate = onNavigate) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            item {
                Spacer(Modifier.height(10.dp))
                Column(Modifier.fillMaxWidth()) {
                    Text("Good Morning!", color = CardifyColors.LightGreenText,
                        fontSize = 18.sp, fontFamily = ibmPlexSans)
                    Text(
                        text     = UserSession.username ?: "Guest",
                        modifier = Modifier.offset(y = (-12).dp),
                        color    = CardifyColors.DarkGreen,
                        fontSize = 34.sp,
                        fontFamily  = ibmPlexSans,
                        fontWeight  = FontWeight.SemiBold
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
                    Text("Your Last Transactions",
                        fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                        fontFamily = ibmPlexSans)
                    FilterDropdown(
                        selectedLimit  = selectedLimit,
                        expanded       = expanded,
                        onExpandChange = { expanded = it },
                        onLimitSelect  = { selectedLimit = it; expanded = false },
                        ibmPlexSans    = ibmPlexSans
                    )
                }
            }

            val limitInt    = selectedLimit.toIntOrNull() ?: 5
            val displayList = transactions.take(limitInt)

            if (isLoading) {
                item {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = CardifyColors.DarkGreen)
                    }
                }
            } else if (displayList.isEmpty()) {
                item {
                    Text("No transactions found.", color = Color.Gray,
                        fontFamily = ibmPlexSans,
                        modifier = Modifier.padding(vertical = 16.dp))
                }
            } else {
                items(displayList) { transaction ->
                    TransactionCard(
                        transaction    = transaction,
                        viewModel      = viewModel,
                        onShareClick   = onShareClick,
                        onStatusChanged = { id, status ->
                            manualOverrides = manualOverrides + (id to status)
                        }
                    )
                }
            }
            // Update Profile Banner
            if (manualOverrides.isNotEmpty()) {
                item {
                    UpdateProfileBannerHome(
                        count = manualOverrides.size,
                        onUpdate = {
                            viewModel.updateProfile(manualOverrides) {
                                manualOverrides = emptyMap()
                                Toast.makeText(context,
                                    "Profile updated successfully!",
                                    Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

// ─────────────────────────────────────────────
// Transaction Card - fixed layout
// ─────────────────────────────────────────────
@Composable
fun TransactionCard(
    transaction: Transaction,
    viewModel: HomeViewModel,
    onShareClick: (Transaction) -> Unit = {},
    onStatusChanged: (String, String) -> Unit = { _, _ -> }
) {
    var isExpanded  by remember { mutableStateOf(false) }
    var isIrregular by remember(transaction.id) {
        mutableStateOf(transaction.status == "IRREGULAR")
    }
    LaunchedEffect(transaction.status) {
        isIrregular = transaction.status == "IRREGULAR"
    }

    val amountStr = "%.2f".format(transaction.amount)

    Card(
        modifier  = Modifier.fillMaxWidth().clickable { isExpanded = !isExpanded },
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        border    = BorderStroke(1.dp,
            if (isIrregular) CardifyColors.IrregularRed else Color(0xFFEEEEEE)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon
                Icon(
                    imageVector = when {
                        isIrregular -> Icons.Default.Warning
                        transaction.category.contains("Food", ignoreCase = true) ||
                                transaction.category.contains("Restaurant", ignoreCase = true) ||
                                transaction.category.contains("Grocery", ignoreCase = true) -> Icons.Default.Restaurant
                        transaction.category.contains("Transport", ignoreCase = true) ||
                                transaction.category.contains("Auto", ignoreCase = true) -> Icons.Default.DirectionsCar
                        transaction.category.contains("Health", ignoreCase = true) -> Icons.Default.LocalHospital
                        transaction.category.contains("Education", ignoreCase = true) -> Icons.Default.School
                        transaction.category.contains("Entertainment", ignoreCase = true) -> Icons.Default.Movie
                        transaction.category.contains("Travel", ignoreCase = true) -> Icons.Default.Flight
                        transaction.category.contains("Finance", ignoreCase = true) ||
                                transaction.category.contains("Bank", ignoreCase = true) -> Icons.Default.AccountBalance
                        transaction.category.contains("Shopping", ignoreCase = true) ||
                                transaction.category.contains("Fashion", ignoreCase = true) -> Icons.Default.ShoppingBag
                        transaction.category.contains("Telecom", ignoreCase = true) -> Icons.Default.PhoneAndroid
                        else -> Icons.Default.Receipt
                    },
                    contentDescription = null,
                    tint     = if (isIrregular) CardifyColors.IrregularRed
                    else CardifyColors.DarkGreen,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(10.dp))

                // Business name - single line
                Text(
                    text      = transaction.businessName,
                    fontSize  = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines  = 1,
                    overflow  = TextOverflow.Ellipsis,
                    modifier  = Modifier.weight(1f)
                )

                Spacer(Modifier.width(8.dp))

                // Amount + status
                Column(horizontalAlignment = Alignment.End) {
                    Text("₪$amountStr",
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines   = 1)
                    Text(
                        if (isIrregular) "Irregular" else "Regular",
                        color      = if (isIrregular) CardifyColors.IrregularRed
                        else CardifyColors.RegularGreen,
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.width(4.dp))
                Icon(
                    if (isExpanded) Icons.Default.KeyboardArrowUp
                    else Icons.Default.KeyboardArrowDown,
                    null, tint = Color.Gray, modifier = Modifier.size(16.dp)
                )
            }

            // (date shown only when expanded)
            Spacer(Modifier.height(4.dp))

            // Expanded content
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFAFAFA))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    HorizontalDivider(color = Color(0xFFEEEEEE))
                    Spacer(Modifier.height(8.dp))

                    Row {
                        Text("Date: ", fontSize = 11.sp, color = Color.Gray)
                        Text(transaction.date, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(4.dp))

                    val displayCategory = mapOf(
                        "מזון וצריכה" to "Food & Grocery", "מסעדות, קפה וברים" to "Restaurants & Cafes",
                        "מסעדות" to "Restaurants", "אופנה" to "Fashion", "בריאות" to "Health",
                        "תחבורה" to "Transport", "חינוך" to "Education",
                        "שירותי תקשורת" to "Telecommunications", "עירייה וממשלה" to "Government",
                        "שונות" to "Other", "כללי" to "General", "בידור" to "Entertainment",
                        "קניות" to "Shopping"
                    ).getOrDefault(transaction.category, transaction.category)
                    Row {
                        Text("Category: ", fontSize = 11.sp, color = Color.Gray)
                        Text(displayCategory, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }

                    if (isIrregular) {
                        val explText = if (!transaction.explanation.isNullOrBlank() &&
                            transaction.explanation != "Unusual transaction pattern detected")
                            transaction.explanation
                        else "Transaction pattern deviates from your usual spending behavior"
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(CardifyColors.IrregularRed.copy(alpha = 0.08f))
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, null,
                                tint = CardifyColors.IrregularRed,
                                modifier = Modifier.size(13.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(explText,
                                color      = CardifyColors.IrregularRed,
                                fontSize   = 11.sp,
                                fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onShareClick(transaction) },
                            shape   = RoundedCornerShape(10.dp),
                            border  = BorderStroke(1.dp, CardifyColors.DarkGreen),
                            colors  = ButtonDefaults.outlinedButtonColors(
                                contentColor = CardifyColors.DarkGreen),
                            modifier = Modifier.weight(1f).height(36.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Icon(Icons.Default.Share, null, modifier = Modifier.size(13.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Share", fontSize = 11.sp)
                        }
                        var showConfirm by remember { mutableStateOf(false) }
                        if (showConfirm) {
                            AlertDialog(
                                onDismissRequest = { showConfirm = false },
                                title = { Text("Are you sure?", fontWeight = FontWeight.Bold) },
                                text  = {
                                    Text(
                                        if (isIrregular) "Mark this transaction as Regular?"
                                        else "Mark this transaction as Irregular (suspicious)?",
                                        color = Color.Gray, fontSize = 14.sp
                                    )
                                },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            showConfirm = false
                                            val next = if (isIrregular) "REGULAR" else "IRREGULAR"
                                            isIrregular = !isIrregular
                                            viewModel.updateTransactionStatus(transaction.id, next)
                                            onStatusChanged(transaction.id, next)
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isIrregular) Color(0xFF2E7D32)
                                            else CardifyColors.IrregularRed),
                                        shape  = RoundedCornerShape(10.dp)
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
                        Button(
                            onClick = { showConfirm = true },
                            colors  = ButtonDefaults.buttonColors(
                                containerColor = if (isIrregular) Color(0xFF2E7D32)
                                else CardifyColors.IrregularRed),
                            shape   = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).height(36.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Icon(
                                if (isIrregular) Icons.Default.CheckCircle
                                else Icons.Default.Warning,
                                null, modifier = Modifier.size(13.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                if (isIrregular) "Mark Regular" else "Mark Irregular",
                                fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

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
        Text("Upload Your Latest Transactions",
            fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = ibmPlexSans)
        Box(
            modifier = Modifier
                .fillMaxWidth().height(150.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFE0F2F1).copy(alpha = 0.5f))
                .drawBehind {
                    drawRoundRect(
                        color  = CardifyColors.DashedBorder.copy(alpha = 0.4f),
                        style  = Stroke(width = 2f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)),
                        cornerRadius = CornerRadius(10.dp.toPx())
                    )
                }
                .clickable { launcher.launch("*/*") },
            contentAlignment = Alignment.Center
        ) {
            if (isUploading) CircularProgressIndicator(color = CardifyColors.DarkGreen)
            else Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(painterResource(R.drawable.ic_upload_custom), null,
                    tint = CardifyColors.DarkGreen, modifier = Modifier.size(26.dp))
                Spacer(Modifier.height(6.dp))
                Text(if (selectedFile != null) "File Ready" else "Tap to choose file",
                    fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text(selectedFile?.lastPathSegment ?: "CSV, XLS up to 10MB",
                    fontSize = 11.sp, color = Color.Gray)
            }
        }
        Button(
            onClick  = onUploadClicked,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape    = RoundedCornerShape(12.dp),
            colors   = ButtonDefaults.buttonColors(
                containerColor = CardifyColors.LightGreenText,
                contentColor   = Color.Black)
        ) { Text("Upload", fontSize = 15.sp, fontWeight = FontWeight.Bold) }
    }
}

@Composable
fun UpdateProfileBannerHome(count: Int, onUpdate: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardifyColors.DarkGreen.copy(alpha = 0.1f))
            .border(1.dp, CardifyColors.DarkGreen, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.AutoFixHigh, null,
            tint = CardifyColors.DarkGreen, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Profile Update Available",
                fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                color = CardifyColors.DarkGreen)
            Text("You corrected $count transaction(s). Update your profile to improve future detection.",
                fontSize = 11.sp, color = Color.Gray)
        }
        Spacer(Modifier.width(8.dp))
        Button(
            onClick = onUpdate,
            colors  = ButtonDefaults.buttonColors(containerColor = CardifyColors.DarkGreen),
            shape   = RoundedCornerShape(10.dp),
            modifier = Modifier.height(34.dp),
            contentPadding = PaddingValues(horizontal = 12.dp)
        ) { Text("Update", fontSize = 12.sp) }
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
    Box(
        modifier = Modifier
            .width(110.dp)
            .background(CardifyColors.TurquoiseBox, RoundedCornerShape(10.dp))
            .clickable { onExpandChange(!expanded) }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Limit: $selectedLimit", fontSize = 11.sp,
                    fontWeight = FontWeight.Bold, fontFamily = ibmPlexSans)
                Spacer(Modifier.weight(1f))
                Icon(if (expanded) Icons.Default.KeyboardArrowUp
                else Icons.Default.KeyboardArrowDown,
                    null, Modifier.size(14.dp))
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