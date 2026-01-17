package com.cardify.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.cardify.app.ui.navigation.AppNavigation
import com.cardify.app.ui.theme.CardifyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // מחקנו את כל הבדיקות והקפיצות.
        // עכשיו האפליקציה פשוט תפתח את מסך הבית וזהו.
        // זה יעצור את הלולאה האינסופית.

        setContent {
            CardifyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // מתחילים תמיד בבית באופן נקי
                    AppNavigation(startDestination = "home")
                }
            }
        }
    }
}