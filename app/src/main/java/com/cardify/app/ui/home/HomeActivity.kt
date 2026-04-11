package com.cardify.app.ui.home

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.cardify.app.ui.theme.CardifyTheme

/**
 * Legacy Activity shell that embeds [HomeScreen] using Jetpack Compose.
 *
 * This activity is retained for backwards compatibility with older navigation paths
 * but is largely superseded by [com.cardify.app.MainActivity] which hosts the full
 * Compose navigation graph. Navigation callbacks from [HomeScreen] currently only log
 * the destination route.
 */
class HomeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            CardifyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    HomeScreen(
                        onNavigate = { route ->
                            println("User clicked to navigate to: $route")
                        }
                    )
                }
            }
        }
    }
}
