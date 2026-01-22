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
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cardify.app.data.UserSession
import com.cardify.app.data.model.Transaction
import com.cardify.app.ui.components.AppScaffold

// צבעים
object CardifyColors {
    val Primary = Color(0xFF0D7377)
    val PrimaryLight = Color(0xFF14FFEC)
    val ScanButton = Color(0xFFA0FF9D)
    val Grey = Color(0xFFE8E8E8)
    val TextPrimary = Color(0xFF1A1A1A)
    val TextSecondary = Color(0xFF666666)
    val IrregularRed = Color(0xFFD32F2F)
}

@Composable
fun HomeScreen(
    onNavigate: (String) -> Unit = {},
    viewModel: HomeViewModel = viewModel()
) {
    // משתנים לשמירת המצב
    var selectedFile by remember { mutableStateOf<Uri?>(null) }
    val currentUserName = remember { UserSession.username ?: "Guest" }

    // הקשבה לנתונים מה-ViewModel
    val transactions by viewModel.transactions.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isUploading by viewModel.isUploading.collectAsState()
    val uploadMessage by viewModel.uploadMessage.collectAsState()

    // הקשר (Context) נדרש כדי לגשת לקבצים ולהציג הודעות
    val context = LocalContext.current

    // הצגת הודעה קופצת (Toast) כשיש תשובה מהשרת על ההעלאה
    LaunchedEffect(uploadMessage) {
        uploadMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            // אם ההעלאה הצליחה, מנקים את הקובץ שנבחר
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
        topBarContent = { TopBar(userName = currentUserName) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // --- חלק 1: אזור ההעלאה ---
            UploadSection(
                selectedFile = selectedFile,
                isUploading = isUploading,
                onFileSelected = { uri -> selectedFile = uri },
                onUploadClicked = {
                    // כאן הקסם קורה! שליחה ל-ViewModel
                    selectedFile?.let { uri ->
                        viewModel.uploadFile(uri, context)
                    }
                }
            )

            // --- חלק 2: רשימת הקניות ---
            Text(
                text = "Your Last Purchases",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = CardifyColors.TextPrimary
            )

            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = CardifyColors.Primary)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
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
fun UploadSection(
    selectedFile: Uri?,
    isUploading: Boolean,
    onFileSelected: (Uri?) -> Unit,
    onUploadClicked: () -> Unit
) {
    // המשגר שפותח את גלריית הקבצים
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> onFileSelected(uri) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Upload Your Transaction CSV",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = CardifyColors.TextPrimary,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (isUploading) {
                // מצב טעינה (כמו בפיגמה)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = CardifyColors.Primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Processing your file...", color = CardifyColors.Primary)
                }
            } else {
                // מצב רגיל - בחירת קובץ
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardifyColors.Grey)
                        .border(1.dp, CardifyColors.Primary, RoundedCornerShape(16.dp)) // מסגרת ירוקה
                        .clickable { launcher.launch("*/*") } // לחיצה פותחת את הקבצים
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = "Upload",
                            tint = CardifyColors.Primary,
                            modifier = Modifier.size(40.dp)
                        )

                        Text(
                            text = if (selectedFile != null) "File Selected!" else "Click to Browse Files",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = CardifyColors.TextPrimary
                        )

                        // הצגת שם הקובץ שנבחר (אם יש)
                        if (selectedFile != null) {
                            Text(
                                text = "Ready to upload",
                                fontSize = 12.sp,
                                color = CardifyColors.Primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // כפתור ההעלאה (מופיע רק אם נבחר קובץ)
                Button(
                    onClick = onUploadClicked,
                    enabled = selectedFile != null, // הכפתור פעיל רק אם נבחר קובץ
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedFile != null) CardifyColors.ScanButton else Color.Gray
                    )
                ) {
                    Text(
                        text = "UPLOAD NOW",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A5C1A)
                    )
                }
            }
        }
    }
}

// --- שאר הרכיבים (TopBar, TransactionCard) נשארים אותו דבר ---

@Composable
fun TopBar(userName: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardifyColors.Primary)
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        Row(
            modifier = Modifier.align(Alignment.CenterStart),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(CardifyColors.PrimaryLight),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = userName.take(1).uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }
            Column {
                Text(
                    text = "Good Morning!",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 13.sp
                )
                Text(
                    text = userName,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Row(
            modifier = Modifier.align(Alignment.CenterEnd),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconButton(onClick = {}) { Icon(Icons.Default.Notifications, "", tint = CardifyColors.PrimaryLight) }
            IconButton(onClick = {}) { Icon(Icons.Default.Email, "", tint = CardifyColors.PrimaryLight) }
        }
    }
}

@Composable
fun TransactionCard(transaction: Transaction) {
    // בודקים אם הסטטוס הוא IRREGULAR. אם הסטטוס ריק, נניח שזה רגיל.
    val isIrregular = transaction.status == "IRREGULAR"
    val statusColor = if (isIrregular) CardifyColors.IrregularRed else CardifyColors.Primary
    val statusText = if (isIrregular) "Irregular" else "Regular"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        border = if (isIrregular) BorderStroke(1.dp, Color(0xFFFFCDD2)) else null
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // תיקון 1: אם אין תאריך, נציג טקסט ריק
            Text(
                text = transaction.date ?: "",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.width(70.dp)
            )

            // תיקון 2: אם אין שם עסק, נכתוב Unknown
            Text(
                text = transaction.businessName ?: "Unknown",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = CardifyColors.TextPrimary,
                modifier = Modifier.weight(1f)
            )

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // תיקון 3: אם אין סכום, נציג 0.0
                Text(
                    text = "₪${transaction.amount ?: 0.0}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = CardifyColors.TextPrimary
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
}