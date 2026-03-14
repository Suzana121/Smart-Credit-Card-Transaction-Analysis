package com.cardify.app.ui.stats

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.cardify.app.ui.theme.CardifyTheme

class StatsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            CardifyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    StatsScreen(
                        currentRoute = "stats",
                        onNavigate = { route ->
                            // ניווט בין מסכים
                            println("Navigate to: $route")
                        },
                        onBackClick = {
                            finish()
                        }
                    )
                }
            }
        }
    }
}