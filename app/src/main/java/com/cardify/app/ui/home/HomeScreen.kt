package com.cardify.app.ui.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// יש לוודא שה-import הזה תואם לחבילה שבה שמרת את השלד:
import com.cardify.app.ui.components.AppScaffold
import com.cardify.app.ui.components.NavigationItem

// ========================================
// צבעי Cardify
// ========================================
object CardifyColors {
    val Primary = Color(0xFF0D7377)          // ירוק כהה
    val PrimaryLight = Color(0xFF14FFEC)     // ירוק זורח
    val ScanButton = Color(0xFFA0FF9D)       // כפתור ירוק בהיר
    val Grey = Color(0xFFE8E8E8)             // אפור
    val TextPrimary = Color(0xFF1A1A1A)      // טקסט שחור
    val TextSecondary = Color(0xFF666666)    // טקסט אפור
}

// ========================================
// מסך ראשי
// ========================================
@Composable
fun HomeScreen(
    onNavigate: (String) -> Unit // הוספנו קולבק לניווט
) {
    var selectedFile by remember { mutableStateOf<Uri?>(null) }

    // שימוש בשלד החדש
    AppScaffold(
        title = "Home",
        currentRoute = NavigationItem.Home.route, // מגדיר שאנחנו בבית
        onNavigate = onNavigate,
        useCustomTopBar = true, // אומר לשלד להשתמש בטופ בר שלנו
        topBarContent = { TopBar() } // מעביר את הטופ בר המעוצב
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(padding) // שימוש בפדינג שה-Scaffold נותן
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            // סקשן העלאת CSV
            UploadSection(
                selectedFile = selectedFile,
                onFileSelected = { selectedFile = it }
            )

            // סקשן שליחה מהירה
            QuickSendSection()

            // סקשן היסטוריה
            HistorySection()
        }
    }
}

// ========================================
// כותרת עליונה (מותאמת אישית)
// ========================================
@Composable
fun TopBar() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardifyColors.Primary)
            .padding(horizontal = 24.dp, vertical = 20.dp)
            // הוספת סטטוס בר פדינג אם צריך (תלוי ב-WindowInsets של האפליקציה)
            .statusBarsPadding()
    ) {
        // משתמש - צד שמאל
        Row(
            modifier = Modifier.align(Alignment.CenterStart),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // תמונת פרופיל
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                CardifyColors.PrimaryLight,
                                CardifyColors.Primary
                            )
                        )
                    )
            )

            // ברכה ושם
            Column {
                Text(
                    text = "Good Morning!",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Light
                )
                Text(
                    text = "Hailey David",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // אייקונים - צד ימין
        Row(
            modifier = Modifier.align(Alignment.CenterEnd),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconButton(onClick = { /* התראות */ }) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Notifications",
                    tint = CardifyColors.PrimaryLight,
                    modifier = Modifier.size(24.dp)
                )
            }
            IconButton(onClick = { /* הודעות */ }) {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = "Email",
                    tint = CardifyColors.PrimaryLight,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

// ========================================
// סקשן העלאת קובץ
// ========================================
@Composable
fun UploadSection(
    selectedFile: Uri?,
    onFileSelected: (Uri?) -> Unit
) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> onFileSelected(uri) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            // כותרת
            Text(
                text = "Upload Your Transaction CSV",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = CardifyColors.TextPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // אזור העלאה
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardifyColors.Grey)
                    .clickable { launcher.launch("*/*") }
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
                        text = if (selectedFile != null)
                            "File selected!"
                        else
                            "Drag & drop CSV file here",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = CardifyColors.TextPrimary
                    )

                    Text(
                        text = "or click to browse",
                        fontSize = 13.sp,
                        color = CardifyColors.TextSecondary
                    )

                    Text(
                        text = "Supported: .csv, .xlsx up to 10MB",
                        fontSize = 11.sp,
                        color = CardifyColors.TextSecondary.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // קישור לדוגמה
            Text(
                text = "Need a sample? (Download Sample CSV)",
                fontSize = 12.sp,
                color = CardifyColors.Primary,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { /* הורדת דוגמה */ }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // כפתור סריקה
            Button(
                onClick = { /* סריקה */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CardifyColors.ScanButton
                )
            ) {
                Text(
                    text = "SCAN",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A5C1A)
                )
            }
        }
    }
}

// ========================================
// סקשן שליחה מהירה
// ========================================
@Composable
fun QuickSendSection() {
    Column {
        Text(
            text = "Quick Send",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = CardifyColors.TextPrimary
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(mockContacts) { contact ->
                ContactItem(contact)
            }
        }
    }
}

@Composable
fun ContactItem(contact: Contact) {
    Column(
        modifier = Modifier
            .width(80.dp)
            .clickable { /* פתח דיאלוג שליחה */ },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // אווטאר
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(CardifyColors.Grey),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = contact.name.first().toString(),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = CardifyColors.TextSecondary
            )
        }

        // שם
        Text(
            text = "${contact.name}\n${contact.surname}",
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = CardifyColors.TextPrimary,
            textAlign = TextAlign.Center,
            lineHeight = 14.sp
        )
    }
}

// ========================================
// סקשן היסטוריה
// ========================================
@Composable
fun HistorySection() {
    Column {
        Text(
            text = "Your Upload History",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = CardifyColors.TextPrimary
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 3 ריבועים אפורים (skeleton)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            repeat(3) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(120.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardifyColors.Grey)
                        .clickable { /* פתח היסטוריה */ }
                )
            }
        }
    }
}

// ========================================
// נתונים לדוגמה
// ========================================
data class Contact(val name: String, val surname: String)

val mockContacts = listOf(
    Contact("Daniyal", "Mcknight"),
    Contact("Mattie", "Osborne"),
    Contact("Aleeza", "Hensley"),
    Contact("Jordanne", "Cohen"),
    Contact("Doug", "Horn")
)

// הסרנו את BottomNav ואת navItems מכאן כי הם מנוהלים עכשיו ב-AppScaffold