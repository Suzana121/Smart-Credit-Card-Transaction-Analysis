/*
package com.cardify.app.ui.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.cardify.app.ui.home.HomeActivity

// ==========================================
// ה-Activity: ה"אבא" שמפעיל את המסך
// ==========================================
class ActivityActivity : ComponentActivity() {
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
}*/
