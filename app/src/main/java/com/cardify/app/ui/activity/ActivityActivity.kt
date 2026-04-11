package com.cardify.app.ui.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.cardify.app.ui.home.HomeActivity

/**
 * Legacy activity shell that hosts the [ActivityScreen] Compose content.
 *
 * Navigation to other destinations is currently handled through explicit [Intent] launches
 * rather than the main Compose navigation graph. Only the `"home"` route is wired up;
 * other routes (`"wallet"`, `"stats"`, `"account"`) are stubs for future implementation.
 */
class ActivityActivity : ComponentActivity() {
    /** Sets the Compose content and wires basic navigation intents. */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // כאן אנחנו קוראים למסך ActivityScreen שנמצא בקובץ השני
            ActivityScreen(
                onNavigate = { route ->
                    when (route) {
                        "home" -> {
                            val intent = Intent(this, HomeActivity::class.java)
                            startActivity(intent)
                            overridePendingTransition(0, 0)
                            finish()
                        }
                        "wallet" -> {
                            // בקרוב...
                        }
                        "stats" -> {
                            // בקרוב...
                        }
                        "account" -> {
                            // בקרוב...
                        }
                    }
                }
            )
        }
    }
}