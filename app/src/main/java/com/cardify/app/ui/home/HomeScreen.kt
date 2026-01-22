package com.cardify.app.ui.home

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CloudUpload
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cardify.app.data.UserSession
import com.cardify.app.data.model.Transaction
import com.cardify.app.ui.components.AppScaffold
import androidx.compose.ui.text.font.Font
import com.cardify.app.R
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items

// --- עדכון צבעים לפי הפיגמה ---
object CardifyColors {
    val DarkGreen = Color(0xFF004469) // הצבע החדש מהפיגמה!
    val LightGreenText = Color(0xFF9FE88D) // צבע משוער ל-Good Morning (ירוק בהיר)
    val ButtonGreen = Color(0xFFA0FF9D) // כפתור Upload
    val GreyBackground = Color(0xFFF5F5F5)
    val TextPrimary = Color(0xFF1A1A1A)
    val IrregularRed = Color(0xFFD32F2F)
}

@Composable
fun HomeScreen(
    onNavigate: (String) -> Unit = {},
    viewModel: HomeViewModel = viewModel()
) {
    var selectedFile by remember { mutableStateOf<Uri?>(null) }
    val currentUserName = remember { UserSession.username ?: "Guest" }

    val transactions by viewModel.transactions.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isUploading by viewModel.isUploading.collectAsState()
    val uploadMessage by viewModel.uploadMessage.collectAsState()

    val context = LocalContext.current

    val ibmPlexSans = FontFamily(
        Font(R.font.ibm_plex_sans_regular, FontWeight.Normal),
        Font(R.font.ibm_plex_sans_semibold, FontWeight.SemiBold)
    )

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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(padding)
                .padding(horizontal = 24.dp), // מרווח צדדי כמו בעיצוב
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // --- אזור הברכה המעודכן ---
            // --- את החלק הזה תחליפי בקוד הבא ---
            // --- הבלוק עם התיקון האגרסיבי (Offset) ---

            val ibmPlexSans = FontFamily(
                Font(R.font.ibm_plex_sans_regular, FontWeight.Normal),
                Font(R.font.ibm_plex_sans_semibold, FontWeight.SemiBold)
            )

            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Good Morning!",
                    color = CardifyColors.LightGreenText,
                    fontSize = 21.sp,
                    fontFamily = ibmPlexSans,
                    fontWeight = FontWeight.Normal,
                    lineHeight = 18.sp, // גובה שורה צפוף
                    style = TextStyle(
                        platformStyle = PlatformTextStyle(
                            includeFontPadding = false
                        )
                    )
                )

                Text(
                    text = currentUserName,
                    modifier = Modifier.offset(y = (-17).dp), // <--- הנה הפטיש: דוחף את הטקסט 10 פיקסלים למעלה
                    color = CardifyColors.DarkGreen,
                    fontSize = 40.sp,
                    fontFamily = ibmPlexSans,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 40.sp,
                    style = TextStyle(
                        platformStyle = PlatformTextStyle(
                            includeFontPadding = false
                        )
                    )
                )
            }
            // --- אזור ההעלאה ---
            UploadSection(
                selectedFile = selectedFile,
                isUploading = isUploading,
                onFileSelected = { uri -> selectedFile = uri },
                onUploadClicked = {
                    selectedFile?.let { uri -> viewModel.uploadFile(uri, context) }
                }
            )

            // --- רשימת הקניות ---
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Your Last Purchases",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = CardifyColors.TextPrimary
                )

                // כפתור View limit קטן
                Surface(
                    color = Color(0xFFE0F2F1),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "View limit >",
                        fontSize = 10.sp,
                        color = CardifyColors.DarkGreen,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = CardifyColors.DarkGreen)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 20.dp)
                ) {
                    items(transactions) { transaction ->
                        TransactionCard(transaction)
                    }
                }
            }
        }
    }
}

@Composable
fun CleanTopBar(onAccountClick: () -> Unit) {
    // כאן אנחנו טוענים את הפונט מהקובץ ששמת בתיקייה
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
        // הלוגו עם הפונט החדש!
        Text(
            text = "Cardify",
            color = Color.White,
            fontSize = 42.sp, // גודל גדול כמו בפיגמה
            fontFamily = kellySlabFont, // <--- הנה השינוי הקסום
            fontWeight = FontWeight.Normal,
            letterSpacing = 1.sp // ריווח קטן בין האותיות למראה יוקרתי
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
    onFileSelected: (Uri?) -> Unit,
    onUploadClicked: () -> Unit
) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> onFileSelected(uri) }

    val ibmPlexSans = FontFamily(
        Font(R.font.ibm_plex_sans_regular, FontWeight.Normal),
        Font(R.font.ibm_plex_sans_semibold, FontWeight.SemiBold),
        Font(R.font.ibm_plex_sans_bold, FontWeight.Bold)
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(15.dp) // הרווח בין הכותרת למלבן ובין המלבן לכפתור התחתון
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
                .height(250.dp) // גובה שמתאים לטאבלט ולנייד
                .clip(RoundedCornerShape(10.dp)) //
                .background(Color(0xFFE0F2F1).copy(alpha = 0.5f))
                .drawBehind {
                    val stroke = Stroke(
                        width = 1.dp.toPx(), //
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                    )
                    drawRoundRect(
                        color = Color(0xFF006769).copy(alpha = 0.4f),
                        style = stroke,
                        cornerRadius = CornerRadius(10.dp.toPx())
                    )
                }
                .clickable { launcher.launch("*/*") },
            contentAlignment = Alignment.Center
        ) {
            if (isUploading) {
                CircularProgressIndicator(color = Color(0xFF006769))
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(15.dp), // ה-Gap הפנימי
                    modifier = Modifier.padding(bottom = 36.dp) // Padding תחתון
                ) {
                    // האייקון
                    Icon(
                        imageVector = Icons.Default.CloudUpload, // <--- זה מה שיש לך עכשיו
                        contentDescription = null,
                        tint = Color(0xFF006769),
                        modifier = Modifier.size(38.dp)
                    )

                    // טקסטים מרכזיים
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Choose a file",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = ibmPlexSans,
                            color = Color(0xFF1A1A1A)
                        )
                        Text(
                            text = "CSV, XLS, and XLSL up to 10MB",
                            fontSize = 12.sp,
                            fontFamily = ibmPlexSans,
                            color = Color.Gray
                        )
                    }

                    // כפתור Browse Files המדויק מהפיגמה
                    Surface(
                        shape = RoundedCornerShape(5.dp), //
                        border = BorderStroke(1.dp, Color(0xFF006769).copy(alpha = 0.49f)), //
                        color = Color.Transparent, // הרקע שקוף או לבן עדין
                        modifier = Modifier
                            .width(100.dp) // מעט רחב יותר מה-86 כדי שיכיל טקסט בנוחות בכל מסך
                            .height(30.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "Browse Files",
                                fontSize = 11.sp, // גודל קטן כפי שרואים בעיצוב
                                fontWeight = FontWeight.Bold,
                                fontFamily = ibmPlexSans,
                                color = Color(0xFF006769) //
                            )
                        }
                    }
                }
            }
        }

        // כפתור ה-Upload הגדול למטה
        Button(
            onClick = onUploadClicked,
            enabled = selectedFile != null,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF9FE88D), // הירוק הבהיר מהפיגמה
                disabledContainerColor = Color(0xFFE0E0E0)
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
            // תאריך
            Text(
                text = transaction.date ?: "",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.width(80.dp)
            )

            // שם העסק
            Text(
                text = transaction.businessName ?: "Unknown",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = CardifyColors.TextPrimary,
                modifier = Modifier.weight(1f)
            )

            // סכום
            Text(
                text = "₪${transaction.amount ?: 0.0}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = CardifyColors.TextPrimary,
                modifier = Modifier.padding(end = 8.dp)
            )

            // סטטוס
            Text(
                text = statusText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = statusColor
            )
        }
    }
}