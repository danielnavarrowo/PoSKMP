package com.dnavarro.poskmp.ui.ajustes

import androidx.compose.ui.graphics.Color
import com.dnavarro.poskmp.domain.model.Cashier
import com.dnavarro.poskmp.domain.model.DeviceRole
import com.dnavarro.poskmp.domain.model.ReceiptSettings
import com.dnavarro.poskmp.theme.DarkModeConfig
import com.dnavarro.poskmp.ui.Screen
import com.dnavarro.poskmp.util.isAndroid
import com.materialkolor.PaletteStyle

import com.dnavarro.poskmp.data.source.remote.dto.RemoteAuditLogDto
import com.dnavarro.poskmp.data.updater.UpdateCheckResult
import com.dnavarro.poskmp.data.updater.UpdateDownloadState

import com.dnavarro.poskmp.util.AppConstants

/**
 * UI State for Settings screen.
 */
data class AjustesUiState(
    val deviceRole: DeviceRole = DeviceRole.ADMIN,
    val terminalPrefix: String = "",
    val useDynamicColor: Boolean = isAndroid(),
    val seedColor: Color = Color(0xFF0061A4),
    val isAmoled: Boolean = false,
    val darkModeConfig: DarkModeConfig = DarkModeConfig.SYSTEM,
    val paletteStyle: PaletteStyle = PaletteStyle.Fidelity,
    val appScale: Float = 1.0f,
    val defaultScreen: Screen = Screen.VENTA,
    val isChecadorDialog: Boolean = true,
    val showExtraPricesChecador: Boolean = false,
    val useProductTableInCatalog: Boolean = false,
    val swapVentaLayoutOrder: Boolean = false,
    val defaultRetailMargin: Double = 0.0,
    val defaultWholesaleMargin: Double = 0.0,
    val defaultDeliveryMargin: Double = 0.0,
    val isRoundingEnabled: Boolean = false,
    val roundProductPrices: Boolean = false,
    val roundTicketTotal: Boolean = false,
    val disallowCardPaymentOnWholesale: Boolean = false,
    val prioritizeDeliveryPrice: Boolean = false,
    val autoWholesaleByQuantity: Boolean = false,
    val autoWholesaleQuantityThreshold: Int = 3,
    val autoWholesaleByTicketTotal: Boolean = false,
    val autoWholesaleTicketTotalThreshold: Double = 0.0,
    val currentVersion: String = AppConstants.APP_VERSION,
    val isCheckingUpdates: Boolean = false,
    val updateCheckResult: UpdateCheckResult? = null,
    val downloadState: UpdateDownloadState = UpdateDownloadState.Idle,
    val supabaseUrl: String = "",
    val supabaseKey: String = "",
    val lastSyncTimestamp: Long = 0L,
    val autoSyncEnabled: Boolean = true,
    val autoBackupEnabled: Boolean = true,
    val lastBackupTimestamp: Long = 0L,
    val backupDirectoryPath: String = "",
    val isBackingUp: Boolean = false,
    val backupMessage: String? = null,
    val syncState: com.dnavarro.poskmp.data.sync.SyncStateEnum = com.dnavarro.poskmp.data.sync.SyncStateEnum.IDLE,
    val isTestingConnection: Boolean = false,
    val connectionTestResult: String? = null,
    val syncMessage: String? = null,
    val receiptSettings: ReceiptSettings = ReceiptSettings(),
    val cashiers: List<Cashier> = emptyList(),
    val isSavingCashier: Boolean = false,
    val isDeletingCashier: Boolean = false,
    val cashierActionError: String? = null,
    val cashierActionSuccess: String? = null,
    val isResettingApp: Boolean = false,
    val resetAppError: String? = null,
    val resetAppSuccess: String? = null,
    val remoteAuditLogs: List<RemoteAuditLogDto> = emptyList(),
    val isLoadingAuditLogs: Boolean = false,
    val auditLogsError: String? = null
)
