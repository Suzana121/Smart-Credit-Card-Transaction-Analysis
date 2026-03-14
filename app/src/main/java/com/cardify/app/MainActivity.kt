package com.cardify.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.cardify.app.data.UserSession
import com.cardify.app.ui.navigation.AppNavigation
import com.cardify.app.ui.theme.CardifyTheme
import com.cardify.app.ui.login.LoginActivity

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // בדיקה ב-auth_prefs
        val prefs = getSharedPreferences("auth_prefs", MODE_PRIVATE)
        val savedToken = prefs.getString("token", null)

        if (savedToken == null) {
            // אם אין טוקן - עוברים ללוגין וסוגרים את MainActivity
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        // אם יש טוקן - טוענים נתונים ומציגים את ה-Compose
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