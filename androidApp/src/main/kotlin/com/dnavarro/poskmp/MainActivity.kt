package com.dnavarro.poskmp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.core.view.WindowCompat
import com.dnavarro.poskmp.db.DatabaseDriverFactory
import com.dnavarro.poskmp.di.initKoin
import com.dnavarro.poskmp.ui.preview.FormFactorPreviews

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.enableEdgeToEdge(window)
        window.isNavigationBarContrastEnforced = false
        volumeControlStream = android.media.AudioManager.STREAM_MUSIC
        super.onCreate(savedInstanceState)

        // Initialize local database context for Android
        DatabaseDriverFactory.appContext = applicationContext

        initKoin()

        setContent {
            App(
                onDarkThemeChanged = { darkTheme ->
                    val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                    insetsController.isAppearanceLightStatusBars = !darkTheme
                    insetsController.isAppearanceLightNavigationBars = !darkTheme
                }
            )
        }
    }
}

@FormFactorPreviews
@Composable
fun AppAndroidPreview() {
    App()
}