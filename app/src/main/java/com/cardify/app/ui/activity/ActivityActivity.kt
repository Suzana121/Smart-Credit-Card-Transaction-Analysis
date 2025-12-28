package com.cardify.app.ui.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.cardify.app.ui.theme.CardifyTheme

class ActivityActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CardifyTheme {
                ActivityScreen()
            }
        }
    }
}