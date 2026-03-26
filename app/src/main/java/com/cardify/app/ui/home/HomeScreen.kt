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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cardify.app.R
import com.cardify.app.data.UserSession
import com.cardify.app.data.model.Transaction
import com.cardify.app.ui.components.AppScaffold

// --- ניהול צבעים נקי ---
object CardifyColors {
    val DarkGreen = Color(0xFF006769)
    val LightGreenText = Color(0xFF9FE88D)
    val TurquoiseBox = Color(0xFFE6F7F7)
    val DashedBorder = Color(0xFF006769)
    val IrregularRed = Color(0xFFE23125)
    val RegularGreen = Color(0xFF38D325)
    val TextPrimary = Color(0xFF1A1A1A)
}

@Composable
fun HomeScreen(
    onNavigate: (String) -> Unit = {},
    onShareClick: (Transaction) -> Unit = {},
    viewModel: HomeViewModel = viewModel()
) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    var selectedLimit by remember { mutableStateOf("5") }
    var selectedFile by remember { mutableStateOf<Uri?>(null) }

    val transactions by viewModel.transactions.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isUploading by viewModel.isUploading.collectAsState()
    val uploadMessage by viewModel.uploadMessage.collectAsState()

    val ibmPlexSans = FontFamily(
        Font(R.font.ibm_plex_sans_regular, FontWeight.Normal),
        Font(R.font.ibm_plex_sans_semibold, FontWeight.SemiBold)
    )

    // טעינה ראשונית
    LaunchedEffect(Unit) {
        viewModel.fetchTransactions()
    }

    // ניהול הודעות הצלחה/שגיאה
    LaunchedEffect(uploadMessage) {
        uploadMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            if (it.contains("Success", ignoreCase = true)) {
                selectedFile = null
            }
        }
    }

    AppScaffold(currentRoute = "home", onNavigate = onNavigate) { padding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(padding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 1. Header
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Good Morning!", color = CardifyColors.LightGreenText, fontSize = 21.sp, fontFamily = ibmPlexSans)
                    Text(
                        text = UserSession.username ?: "Guest",
                        modifier = Modifier.offset(y = (-17).dp),
                        color = CardifyColors.DarkGreen,
                        fontSize = 40.sp,
                        fontFamily = ibmPlexSans,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // 2. Upload Section
            item {
                UploadSection(
                    selectedFile = selectedFile,
                    isUploading = isUploading,
                    ibmPlexSans = ibmPlexSans,
                    onFileSelected = { selectedFile = it },
                    onUploadClicked = { selectedFile?.let { viewModel.uploadFile(it, context) } }
                )
            }

            // 3. Transactions Title & Filter
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Your Last Transactions", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, fontFamily = ibmPlexSans)
                    Spacer(modifier = Modifier.height(10.dp))

                    FilterDropdown(
                        selectedLimit = selectedLimit,
                        expanded = expanded,
                        onExpandChange = { expanded = it },
                        onLimitSelect = { selectedLimit = it; expanded = false },
                        ibmPlexSans = ibmPlexSans
                    )
                }
            }

            // 4. Transactions List
            val limitInt = selectedLimit.toIntOrNull() ?: 5
            val displayList = transactions.take(limitInt)

            if (isLoading) {
                item { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = CardifyColors.DarkGreen) } }
            } else if (displayList.isEmpty()) {
                item { Text("No transactions found.", color = Color.Gray, fontFamily = ibmPlexSans) }
            } else {
                items(displayList) { transaction ->
                    TransactionCard(transaction = transaction, viewModel = viewModel, onShareClick = onShareClick)
                }
            }
            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
}

@Composable
fun TransactionCard(transaction: Transaction, viewModel: HomeViewModel, onShareClick: (Transaction) -> Unit = {}) {
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(false) }
    val currentStatus = transaction.status ?: "REGULAR"
    val isIrregular = currentStatus == "IRREGULAR"

    val ibmPlexSans = FontFamily(Font(R.font.ibm_plex_sans_regular))

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { isExpanded = !isExpanded },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, if (isIrregular) CardifyColors.IrregularRed else Color(0xFFEEEEEE))
    ) {
        Column {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(transaction.date ?: "", fontSize = 12.sp, modifier = Modifier.width(80.dp), fontWeight = FontWeight.Bold)
                Text(transaction.businessName ?: "Unknown", fontSize = 14.sp, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                Text("₪${transaction.amount}", fontSize = 14.sp, modifier = Modifier.padding(end = 16.dp), fontWeight = FontWeight.Bold)
                Text(
                    text = if (isIrregular) "Irregular" else "Regular",
                    color = if (isIrregular) CardifyColors.IrregularRed else CardifyColors.RegularGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    HorizontalDivider(color = Color(0xFFEEEEEE))
                    if (isIrregular) {
                        Text(
                            "Please Notice: Unrecognizable Transaction!",
                            color = Color.Red,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp).align(Alignment.CenterHorizontally)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        // Share
                        Row(
                            modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(Color(0xFFE8FCE8))
                                .clickable { onShareClick(transaction) }.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Share, null, tint = Color(0xFF2E7D32), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Share", fontSize = 11.sp)
                        }

                        // Report Button
                        Button(
                            onClick = {
                                val nextStatus = if (isIrregular) "REGULAR" else "IRREGULAR"
                                transaction.id?.let { viewModel.updateTransactionStatus( it, nextStatus) }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CardifyColors.DarkGreen),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(38.dp)
                        ) {
                            Icon(Icons.Default.Warning, null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isIrregular) "Mark Regular" else "Mark Irregular", fontSize = 11.sp)
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
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { onFileSelected(it) }

    Column(verticalArrangement = Arrangement.spacedBy(15.dp)) {
        Text("Upload Your Latest Transactions", fontSize = 17.sp, fontWeight = FontWeight.Bold, fontFamily = ibmPlexSans)

        Box(
            modifier = Modifier
                .fillMaxWidth().height(200.dp).clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFE0F2F1).copy(alpha = 0.5f))
                .drawBehind {
                    drawRoundRect(
                        color = CardifyColors.DashedBorder.copy(alpha = 0.4f),
                        style = Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)),
                        cornerRadius = CornerRadius(10.dp.toPx())
                    )
                }.clickable { launcher.launch("*/*") },
            contentAlignment = Alignment.Center
        ) {
            if (isUploading) CircularProgressIndicator(color = CardifyColors.DarkGreen)
            else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(painterResource(R.drawable.ic_upload_custom), null, tint = CardifyColors.DarkGreen, modifier = Modifier.size(30.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(if (selectedFile != null) "File Ready" else "Tap to choose file", fontWeight = FontWeight.SemiBold)
                    Text(selectedFile?.lastPathSegment ?: "CSV, XLS up to 10MB", fontSize = 12.sp, color = Color.Gray)
                }
            }
        }

        Button(
            onClick = onUploadClicked,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = CardifyColors.LightGreenText, contentColor = Color.Black)
        ) {
            Text("Upload", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun FilterDropdown(selectedLimit: String, expanded: Boolean, onExpandChange: (Boolean) -> Unit, onLimitSelect: (String) -> Unit, ibmPlexSans: FontFamily) {
    Box(
        modifier = Modifier.width(130.dp).background(CardifyColors.TurquoiseBox, RoundedCornerShape(12.dp))
            .clickable { onExpandChange(!expanded) }.padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Limit: $selectedLimit", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = ibmPlexSans)
                Spacer(Modifier.weight(1f))
                Icon(if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, null, Modifier.size(16.dp))
            }
            AnimatedVisibility(visible = expanded) {
                Column {
                    listOf("5", "10", "15").forEach {
                        Text(it, modifier = Modifier.fillMaxWidth().clickable { onLimitSelect(it) }.padding(vertical = 4.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun CleanTopBar(onAccountClick: () -> Unit) {
    val context = LocalContext.current
    Row(
        modifier = Modifier.fillMaxWidth().height(100.dp).background(CardifyColors.DarkGreen).padding(horizontal = 24.dp).padding(top = 40.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Cardify", color = Color.White, fontSize = 42.sp, fontFamily = FontFamily(Font(R.font.kelly_slab)))
        IconButton(onClick = {
            com.cardify.app.utils.PreferencesManager.getInstance(context).clearAll()
            UserSession.id = null
            UserSession.username = null
            onAccountClick()
        }) {
            Icon(Icons.Default.AccountCircle, "Logout", tint = Color.White, modifier = Modifier.size(34.dp))
        }
    }
}