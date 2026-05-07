package com.cardify.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.cardify.app.data.UserSession
import com.cardify.app.ui.navigation.AppNavigation
import com.cardify.app.ui.theme.CardifyTheme
import com.cardify.app.ui.login.LoginActivity

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ── Status Bar — צבע כהה עם אייקונים בהירים ──
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // בדיקה ב-auth_prefs
        val prefs = getSharedPreferences("auth_prefs", MODE_PRIVATE)
        val savedToken = prefs.getString("token", null)

        if (savedToken == null) {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        UserSession.token = savedToken
        UserSession.username = prefs.getString("username", "User")
        UserSession.id = prefs.getString("user_id", null)

        setContent {
            CardifyTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation(startDestination = "home")
                }
            }
        }
    }
}