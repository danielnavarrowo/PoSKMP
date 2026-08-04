package com.dnavarro.poskmp.ui.ajustes

import androidx.compose.ui.graphics.Color
import com.dnavarro.poskmp.theme.DarkModeConfig
import com.dnavarro.poskmp.ui.Screen
import com.dnavarro.poskmp.util.isAndroid

/**
 * UI State for Settings screen.
 */
data class AjustesUiState(
    val useDynamicColor: Boolean = isAndroid(),
    val seedColor: Color = Color(0xFF0061A4),
    val isAmoled: Boolean = false,
    val darkModeConfig: DarkModeConfig = DarkModeConfig.SYSTEM,
    val appScale: Float = 1.0f,
    val defaultScreen: Screen = Screen.VENTA,
    val isChecadorDialog: Boolean = true
)
