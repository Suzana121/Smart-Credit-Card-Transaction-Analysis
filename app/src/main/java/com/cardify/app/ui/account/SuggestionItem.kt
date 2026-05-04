package com.cardify.app.ui.account
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cardify.app.data.model.Friend
@Composable
fun SuggestionItem(
    suggestion: Friend,
    onAddClick: () -> Unit,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    // שימוש ב-Surface כדי ליצור את המראה של התיבה המעוגלת מה-Sheet
    Surface(
        modifier = Modifier
            .width(100.dp) // רוחב מעט גדול יותר כדי להכיל את הטקסט והכפתור בנוחות
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFF7F8F9), // הצבע האפור הבהיר מה-Sheet שלך
        border = BorderStroke(1.dp, Color(0xFFE7E8E9)) // המסגרת העדינה
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // תמונת פרופיל מעוגלת
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(Color.White), // רקע לבן לתמונה בתוך התיבה האפורה
                contentAlignment = Alignment.Center
            ) {
                if (!suggestion.photoUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(suggestion.photoUrl).crossfade(true).build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(Icons.Default.Person, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(24.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // שם המשתמש בפורמט של ה-Sheet
            Text(
                text = suggestion.name,
                color = Color.Black,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold, // מודגש כמו ב-Sheet[cite: 2]
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            // כפתור ה-Add בעיצוב של כפתור ה-Confirm
            Button(
                onClick = onAddClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp),
                contentPadding = PaddingValues(0.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = teal) // שימוש בצבע ה-teal שלך[cite: 1, 2]
            ) {
                Text(
                    text = "Add",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}