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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
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

// --- הגדרת צבעים גלובלית למסך זה ---
object CardifyColors {
    val DarkGreen = Color(0xFF004469) // הצבע החדש מהפיגמה
    val LightGreenText = Color(0xFF9FE88D) // ירוק בהיר לבוקר טוב
    val ButtonGreen = Color(0xFFA0FF9D) // כפתור Upload
    val GreyBackground = Color(0xFFF5F5F5)
    val TextPrimary = Color(0xFF1A1A1A)
    val IrregularRed = Color(0xFFD32F2F)
    val TurquoiseBox = Color(0xFFE6F7F7) // רקע לפילטר
    val DashedBorder = Color(0xFF006769)
}

@Composable
fun HomeScreen(
    onNavigate: (String) -> Unit = {},
    viewModel: HomeViewModel = viewModel()
) {
    // State Variables
    var expanded by remember { mutableStateOf(false) }
    var selectedLimit by remember { mutableStateOf("5") }
    var selectedFile by remember { mutableStateOf<Uri?>(null) }

    val currentUserName = remember { UserSession.username ?: "Guest" }

    // Data from ViewModel
    val transactions by viewModel.transactions.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isUploading by viewModel.isUploading.collectAsState()
    val uploadMessage by viewModel.uploadMessage.collectAsState()

    val context = LocalContext.current

    // Font Setup
    val ibmPlexSans = FontFamily(
        Font(R.font.ibm_plex_sans_regular, FontWeight.Normal),
        Font(R.font.ibm_plex_sans_semibold, FontWeight.SemiBold)
    )

    // Handle Toast Messages
    LaunchedEffect(uploadMessage) {
        uploadMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            if (it.contains("Success")) {
                selectedFile = null
            }
        }
    }

    AppScaffold(
        title = "Home",
        currentRoute = "home",
        onNavigate = onNavigate,
        useCustomTopBar = true,
        topBarContent = { CleanTopBar(onAccountClick = { onNavigate("account") }) }
    ) { padding ->

        // שימוש ב-LazyColumn מאפשר גלילה של כל המסך ביחד
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(padding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {

            // --- 1. Header Item: Greeting & Name ---
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Good Morning!",
                        color = CardifyColors.LightGreenText,
                        fontSize = 21.sp,
                        fontFamily = ibmPlexSans,
                        fontWeight = FontWeight.Normal,
                        lineHeight = 18.sp,
                        style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                    )

                    Text(
                        text = currentUserName,
                        modifier = Modifier.offset(y = (-17).dp), // התיקון האגרסיבי
                        color = CardifyColors.DarkGreen,
                        fontSize = 40.sp,
                        fontFamily = ibmPlexSans,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 40.sp,
                        style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                    )
                }
            }

            // --- 2. Item: Upload Section ---
            item {
                UploadSection(
                    selectedFile = selectedFile,
                    isUploading = isUploading,
                    ibmPlexSans = ibmPlexSans,
                    onFileSelected = { uri -> selectedFile = uri },
                    onUploadClicked = {
                        selectedFile?.let { uri -> viewModel.uploadFile(uri, context) }
                    }
                )
            }

            // --- 3. Item: Last Transactions Title & Filter ---
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 20.dp)) {
                    Text(
                        text = "Your Last Transactions",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = ibmPlexSans,
                        color = Color.Black
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // רכיב הפילטר (Dropdown)
                    Column(
                        modifier = Modifier
                            .width(118.dp)
                            .background(
                                color = CardifyColors.TurquoiseBox,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { expanded = !expanded }
                    ) {
                        // כותרת הפילטר
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "View limit: $selectedLimit",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = ibmPlexSans,
                                color = Color.Black
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(
                                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // רשימה נפתחת
                        AnimatedVisibility(visible = expanded) {
                            Column {
                                listOf("5", "10", "15").forEach { limit ->
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedLimit = limit
                                                expanded = false
                                                // כאן אפשר להוסיף קריאה ל-ViewModel לעדכן את הלימיט אם צריך
                                            }
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        HorizontalDivider(color = Color.Black.copy(alpha = 0.1f), thickness = 0.5.dp)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = limit,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = ibmPlexSans,
                                            color = Color.Black
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // --- 4. Items: רשימת הטרנזקציות עצמה ---
            // לוקחים את כמות הטרנזקציות לפי הלימיט שנבחר
            val limitInt = selectedLimit.toIntOrNull() ?: 5
            val displayList = transactions.take(limitInt)

            if (isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = CardifyColors.DarkGreen)
                    }
                }
            } else if (displayList.isEmpty()) {
                item {
                    Text(
                        text = "No transactions found.",
                        modifier = Modifier.padding(top = 16.dp),
                        color = Color.Gray,
                        fontFamily = ibmPlexSans
                    )
                }
            } else {
                items(displayList) { transaction ->
                    TransactionCard(transaction)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // מרווח תחתון לסיום
            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
}

// ----------------------------------------------------------------
// --- Composable Components (מופרדים לקריאות) ---
// ----------------------------------------------------------------

@Composable
fun CleanTopBar(onAccountClick: () -> Unit) {
    val kellySlabFont = FontFamily(Font(R.font.kelly_slab))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .background(CardifyColors.DarkGreen)
            .padding(horizontal = 24.dp)
            .padding(top = 40.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Cardify",
            color = Color.White,
            fontSize = 42.sp,
            fontFamily = kellySlabFont,
            fontWeight = FontWeight.Normal,
            letterSpacing = 1.sp
        )

        IconButton(onClick = onAccountClick) {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = "Account",
                tint = Color.White,
                modifier = Modifier.size(34.dp)
            )
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
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> onFileSelected(uri) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        Text(
            text = "Upload Your Latest Transactions",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = ibmPlexSans,
            color = Color.Black,
            modifier = Modifier.align(Alignment.Start)
        )

        // המלבן המקווקו
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFE0F2F1).copy(alpha = 0.5f))
                .drawBehind {
                    val stroke = Stroke(
                        width = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                    )
                    drawRoundRect(
                        color = CardifyColors.DashedBorder.copy(alpha = 0.4f),
                        style = stroke,
                        cornerRadius = CornerRadius(10.dp.toPx())
                    )
                }
                .clickable { launcher.launch("*/*") },
            contentAlignment = Alignment.Center
        ) {
            if (isUploading) {
                CircularProgressIndicator(color = CardifyColors.DashedBorder)
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(15.dp),
                    modifier = Modifier.padding(bottom = 36.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_upload_custom),
                        contentDescription = "Upload Icon",
                        tint = CardifyColors.DashedBorder,
                        modifier = Modifier.size(28.dp)
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (selectedFile != null) "File Selected!" else "Choose a file",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = ibmPlexSans,
                            color = CardifyColors.TextPrimary
                        )
                        Text(
                            text = if (selectedFile != null) selectedFile!!.lastPathSegment ?: "Unknown" else "CSV, XLS, and XLSL up to 10MB",
                            fontSize = 12.sp,
                            fontFamily = ibmPlexSans,
                            color = Color.Gray,
                            maxLines = 1
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(5.dp),
                        border = BorderStroke(1.dp, CardifyColors.DashedBorder.copy(alpha = 0.49f)),
                        color = Color.Transparent,
                        modifier = Modifier
                            .width(100.dp)
                            .height(30.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "Browse Files",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = ibmPlexSans,
                                color = CardifyColors.DashedBorder
                            )
                        }
                    }
                }
            }
        }

        Button(
            onClick = {
                if (selectedFile != null) {
                    onUploadClicked()
                } else {
                    Toast.makeText(context, "Please select a file first", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = CardifyColors.LightGreenText,
                contentColor = Color.Black
            )
        ) {
            Text(
                text = "Upload",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = ibmPlexSans,
                color = Color.Black
            )
        }
    }
}

@Composable
fun TransactionCard(transaction: Transaction) {
    val isIrregular = transaction.status == "IRREGULAR"
    val statusColor = if (isIrregular) CardifyColors.IrregularRed else CardifyColors.LightGreenText
    val statusText = if (isIrregular) "Irregular" else "Regular"

    // ניתן להוסיף כאן המרה בטוחה יותר לתאריך או שם עסק אם צריך

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp),
        border = BorderStroke(1.dp, Color(0xFFEEEEEE))
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = transaction.date ?: "",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.width(80.dp)
            )

            Text(
                text = transaction.businessName ?: "Unknown",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = CardifyColors.TextPrimary,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = "₪${transaction.amount ?: 0.0}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = CardifyColors.TextPrimary,
                modifier = Modifier.padding(end = 8.dp)
            )

            Text(
                text = statusText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = statusColor
            )
        }
    }
}