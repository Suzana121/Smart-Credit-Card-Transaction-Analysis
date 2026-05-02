package com.cardify.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.*
import com.cardify.app.R

@Composable
fun ProcessingOverlay(isSuccess: Boolean) {
    val turquoise = Color(0xFF006769)
    val gradientStart = Color(0xFF55784B)
    val gradientEnd = Color(0xFF9DE88B)

    var startAnim by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { startAnim = true }

    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.wired_outline_24_approved_checked_hover_loading)
    )
    val lottieProgress by animateLottieCompositionAsState(
        composition = composition,
        isPlaying = isSuccess,
        iterations = 1
    )

    val barProgress by animateFloatAsState(
        targetValue = if (isSuccess) 1f else if (startAnim) 0.9f else 0f,
        animationSpec = tween(durationMillis = 5000, easing = LinearOutSlowInEasing),
        label = "BarProgress"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = turquoise)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 48.dp, horizontal = 24.dp), // פדינג אנכי נדיב למרכוז
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (isSuccess) {
                    // --- תיקון מרכוז למסך סיום ---

                    // עיגול לבן מסביב ל-V כמו ב-Property 1=4.png
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .background(Color.Transparent, shape = RoundedCornerShape(50.dp))
                            .padding(4.dp), // מרווח קטן מהשוליים של העיגול
                        contentAlignment = Alignment.Center
                    ) {
                        LottieAnimation(
                            composition = composition,
                            progress = { lottieProgress },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "All Done!",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Your transactions were processed.",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center
                    )
                } else {
                    // --- מסך טעינה (השארתי ללא שינוי בגודל) ---
                    Text(
                        text = "We Are Processing\nYour Transactions",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        lineHeight = 32.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "It may take a few minutes...",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 15.sp
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(18.dp)
                            .padding(horizontal = 8.dp)
                    ) {
                        val radius = CornerRadius(size.height / 2, size.height / 2)
                        drawRoundRect(
                            color = Color.White.copy(alpha = 0.2f),
                            size = size,
                            cornerRadius = radius
                        )
                        drawRoundRect(
                            brush = Brush.horizontalGradient(listOf(gradientStart, gradientEnd)),
                            size = Size(size.width * barProgress, size.height),
                            cornerRadius = radius
                        )
                    }
                }
            }
        }
    }
}