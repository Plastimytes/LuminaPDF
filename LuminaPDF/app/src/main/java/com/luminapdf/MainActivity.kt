package com.luminapdf

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.luminapdf.ui.LuminaNavGraph
import com.luminapdf.ui.Routes
import com.luminapdf.ui.theme.LuminaPDFTheme
import dagger.hilt.android.AndroidEntryPoint
import java.net.URLEncoder

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle VIEW intent (opening PDF from file manager)
        val externalUri: Uri? = if (intent?.action == android.content.Intent.ACTION_VIEW) {
            intent.data
        } else null

        setContent {
            // Theme is driven by DataStore; start with system default,
            // the ViewModel will apply the saved preference via StateFlow.
            LuminaPDFTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()

                    LuminaNavGraph(navController = navController)

                    // If opened via external intent, navigate straight to reader
                    if (externalUri != null) {
                        val encoded = URLEncoder.encode(externalUri.toString(), "UTF-8")
                        navController.navigate("reader/$encoded") {
                            popUpTo(Routes.LIBRARY) { inclusive = false }
                        }
                    }
                }
            }
        }
    }
}
