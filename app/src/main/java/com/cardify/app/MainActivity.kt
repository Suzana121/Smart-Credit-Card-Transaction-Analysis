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
import com.cardify.app.utils.PreferencesManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // עכשיו קוראים מ-PreferencesManager במקום auth_prefs ישירות
        val prefs = PreferencesManager.getInstance(this)
        val savedToken = prefs.getToken()

        if (savedToken == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // טוענים את כל הנתונים מ-PreferencesManager
        UserSession.token = savedToken
        UserSession.username = prefs.getUserName()
        UserSession.id = prefs.getUserId()
        UserSession.email = prefs.getUserEmail()

        setContent {
            CardifyTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation(startDestination = "home")
                }
            }
        }
    }
}