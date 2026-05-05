package com.cardify.app.ui.account

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cardify.app.data.model.Friend

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuggestionItem(
    suggestion: Friend,
    onAddClick: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // תמונת הפרופיל של ההצעה
            Image(
                painter = painterResource(id = suggestion.photoResource),
                contentDescription = suggestion.name,
                modifier = Modifier.size(80.dp).padding(bottom = 8.dp)
            )

            Text(text = "Suggested Friend", fontSize = 20.sp, fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.height(16.dp))

            // תיבת הפרטים האפורה מה-Sheet המקורי
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFF7F8F9),
                border = BorderStroke(1.dp, Color(0xFFE7E8E9))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Name", color = teal, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(suggestion.name, color = Color.Black, fontSize = 16.sp)

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFE7E8E9))

                    Text("Phone number", color = teal, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(suggestion.phone, color = Color.Black, fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // כפתור הוספה (במקום Confirm)
            Button(
                onClick = { onAddClick(); onDismiss() },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = teal)
            ) {
                Text("Add", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // כפתור מחיקת הצעה (Delete)
            Text(
                text = "Delete suggestion",
                fontSize = 14.sp,
                color = Color(0xFFD32F2F),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onDelete(); onDismiss() }
            )
        }
    }
}