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

/**
 * Application entry point after the user has authenticated.
 *
 * On creation, [MainActivity] checks [PreferencesManager] for a persisted JWT token.
 * - If no token is found the user is redirected to [LoginActivity] and this activity finishes.
 * - If a token exists, session data is hydrated into [UserSession] and the Compose navigation
 *   host ([AppNavigation]) is displayed, starting on the home screen.
 */
class MainActivity : ComponentActivity() {

    /**
     * Bootstraps the authenticated session and sets up the Compose UI.
     * Redirects unauthenticated users to [LoginActivity].
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = PreferencesManager.getInstance(this)
        val savedToken = prefs.getToken()

        if (savedToken == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // Hydrate the in-memory session from persistent storage.
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
