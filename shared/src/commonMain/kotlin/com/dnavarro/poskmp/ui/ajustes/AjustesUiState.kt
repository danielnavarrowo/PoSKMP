package com.dnavarro.poskmp.ui.ajustes

import androidx.compose.ui.graphics.Color
import com.dnavarro.poskmp.theme.DarkModeConfig
import com.dnavarro.poskmp.ui.Screen
import com.dnavarro.poskmp.util.isAndroid

import com.dnavarro.poskmp.data.updater.UpdateCheckResult
import com.dnavarro.poskmp.data.updater.UpdateDownloadState

import com.dnavarro.poskmp.util.AppConstants

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
    val isChecadorDialog: Boolean = true,
    val showExtraPricesChecador: Boolean = false,
    val defaultRetailMargin: Double = 0.0,
    val defaultWholesaleMargin: Double = 0.0,
    val currentVersion: String = AppConstants.APP_VERSION,
    val isCheckingUpdates: Boolean = false,
    val updateCheckResult: UpdateCheckResult? = null,
    val downloadState: UpdateDownloadState = UpdateDownloadState.Idle
)
