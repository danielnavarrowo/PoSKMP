package com.dnavarro.poskmp.ui.ajustes

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dnavarro.poskmp.data.SettingsRepository
import com.dnavarro.poskmp.data.sync.SyncRepository
import com.dnavarro.poskmp.data.backup.BackupRepository
import com.dnavarro.poskmp.data.updater.ReleaseAsset
import com.dnavarro.poskmp.data.updater.UpdateCheckResult
import com.dnavarro.poskmp.data.updater.UpdateDownloadState
import com.dnavarro.poskmp.data.updater.UpdateRepository
import com.dnavarro.poskmp.domain.model.ReceiptSettings
import com.dnavarro.poskmp.domain.usecase.GetCashiersUseCase
import com.dnavarro.poskmp.domain.usecase.SaveCashierUseCase
import com.dnavarro.poskmp.domain.usecase.DeleteCashierUseCase
import com.dnavarro.poskmp.domain.usecase.ResetAppToFactoryDefaultsUseCase
import com.dnavarro.poskmp.theme.DarkModeConfig
import com.dnavarro.poskmp.ui.Screen
import com.materialkolor.PaletteStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private data class UpdateInternalState(
    val isCheckingUpdates: Boolean = false,
    val updateCheckResult: UpdateCheckResult? = null,
    val downloadState: UpdateDownloadState = UpdateDownloadState.Idle,
    val isTestingConnection: Boolean = false,
    val connectionTestResult: String? = null,
    val syncMessage: String? = null,
    val isBackingUp: Boolean = false,
    val backupMessage: String? = null,
    val isSavingCashier: Boolean = false,
    val isDeletingCashier: Boolean = false,
    val cashierActionError: String? = null,
    val cashierActionSuccess: String? = null,
    val isResettingApp: Boolean = false,
    val resetAppError: String? = null,
    val resetAppSuccess: String? = null
)

private data class Tuple4<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
private data class Tuple5<A, B, C, D, E>(val a: A, val b: B, val c: C, val d: D, val e: E)
private data class Tuple6<A, B, C, D, E, F>(val a: A, val b: B, val c: C, val d: D, val e: E, val f: F)
private data class Tuple7<A, B, C, D, E, F, G>(val a: A, val b: B, val c: C, val d: D, val e: E, val f: F, val g: G)

/**
 * ViewModel for Settings screen according to Google UI Layer architecture.
 */
class AjustesViewModel(
    private val repository: SettingsRepository,
    private val updateRepository: UpdateRepository,
    private val syncRepository: SyncRepository,
    private val backupRepository: BackupRepository,
    getCashiersUseCase: GetCashiersUseCase,
    private val saveCashierUseCase: SaveCashierUseCase,
    private val deleteCashierUseCase: DeleteCashierUseCase,
    private val resetAppToFactoryDefaultsUseCase: ResetAppToFactoryDefaultsUseCase
) : ViewModel() {

    private val _updateState = MutableStateFlow(UpdateInternalState())

    private val _themeFlow = combine(
        combine(
            repository.useDynamicColorFlow,
            repository.seedColorFlow,
            repository.isAmoledFlow
        ) { useDynamicColor, seedColor, isAmoled ->
            Triple(useDynamicColor, seedColor, isAmoled)
        },
        combine(
            repository.darkModeConfigFlow,
            repository.appScaleFlow,
            repository.paletteStyleFlow
        ) { darkModeConfig, appScale, paletteStyle ->
            Triple(darkModeConfig, appScale, paletteStyle)
        }
    ) { (useDynamicColor, seedColor, isAmoled), (darkModeConfig, appScale, paletteStyle) ->
        AjustesUiState(
            useDynamicColor = useDynamicColor,
            seedColor = seedColor,
            isAmoled = isAmoled,
            darkModeConfig = darkModeConfig,
            appScale = appScale,
            paletteStyle = paletteStyle
        )
    }

    private val _behaviorFlow = combine(
        combine(
            combine(
                repository.defaultScreenFlow,
                repository.isChecadorDialogFlow,
                repository.showExtraPricesChecadorFlow
            ) { defaultScreen, isChecadorDialog, showExtraPricesChecador ->
                Triple(defaultScreen, isChecadorDialog, showExtraPricesChecador)
            },
            combine(
                repository.useProductTableInCatalogFlow,
                repository.defaultRetailMarginFlow,
                repository.defaultWholesaleMarginFlow
            ) { useProductTableInCatalog, defaultRetailMargin, defaultWholesaleMargin ->
                Triple(useProductTableInCatalog, defaultRetailMargin, defaultWholesaleMargin)
            }
        ) { (defaultScreen, isChecadorDialog, showExtraPricesChecador), (useProductTableInCatalog, defaultRetailMargin, defaultWholesaleMargin) ->
            Tuple6(defaultScreen, isChecadorDialog, showExtraPricesChecador, useProductTableInCatalog, defaultRetailMargin, defaultWholesaleMargin)
        },
        combine(
            repository.isRoundingEnabledFlow,
            repository.roundRetailPriceFlow,
            repository.roundWholesalePriceFlow,
            repository.roundTicketTotalFlow,
            repository.disallowCardPaymentOnWholesaleFlow
        ) { isRoundingEnabled, roundRetailPrice, roundWholesalePrice, roundTicketTotal, disallowCardPaymentOnWholesale ->
            Tuple5(isRoundingEnabled, roundRetailPrice, roundWholesalePrice, roundTicketTotal, disallowCardPaymentOnWholesale)
        },
        combine(
            combine(
                repository.supabaseUrlFlow,
                repository.supabaseKeyFlow,
                repository.lastSyncTimestampFlow,
                repository.autoSyncEnabledFlow
            ) { supabaseUrl, supabaseKey, lastSyncTimestamp, autoSyncEnabled ->
                Tuple4(supabaseUrl, supabaseKey, lastSyncTimestamp, autoSyncEnabled)
            },
            combine(
                repository.autoBackupEnabledFlow,
                repository.lastBackupTimestampFlow,
                backupRepository.backupDirectoryPathFlow
            ) { autoBackupEnabled, lastBackupTimestamp, backupDirectoryPath ->
                Triple(autoBackupEnabled, lastBackupTimestamp, backupDirectoryPath)
            }
        ) { (supabaseUrl, supabaseKey, lastSyncTimestamp, autoSyncEnabled), (autoBackupEnabled, lastBackupTimestamp, backupDirectoryPath) ->
            Tuple7(supabaseUrl, supabaseKey, lastSyncTimestamp, autoSyncEnabled, autoBackupEnabled, lastBackupTimestamp, backupDirectoryPath)
        }
    ) { (defaultScreen, isChecadorDialog, showExtraPricesChecador, useProductTableInCatalog, defaultRetailMargin, defaultWholesaleMargin),
        (isRoundingEnabled, roundRetailPrice, roundWholesalePrice, roundTicketTotal, disallowCardPaymentOnWholesale),
        (supabaseUrl, supabaseKey, lastSyncTimestamp, autoSyncEnabled, autoBackupEnabled, lastBackupTimestamp, backupDirectoryPath) ->
        AjustesUiState(
            defaultScreen = defaultScreen,
            isChecadorDialog = isChecadorDialog,
            showExtraPricesChecador = showExtraPricesChecador,
            useProductTableInCatalog = useProductTableInCatalog,
            defaultRetailMargin = defaultRetailMargin,
            defaultWholesaleMargin = defaultWholesaleMargin,
            isRoundingEnabled = isRoundingEnabled,
            roundRetailPrice = roundRetailPrice,
            roundWholesalePrice = roundWholesalePrice,
            roundTicketTotal = roundTicketTotal,
            disallowCardPaymentOnWholesale = disallowCardPaymentOnWholesale,
            supabaseUrl = supabaseUrl,
            supabaseKey = supabaseKey,
            lastSyncTimestamp = lastSyncTimestamp,
            autoSyncEnabled = autoSyncEnabled,
            autoBackupEnabled = autoBackupEnabled,
            lastBackupTimestamp = lastBackupTimestamp,
            backupDirectoryPath = backupDirectoryPath
        )
    }

    private val _receiptFlow = repository.receiptSettingsFlow

    private val _baseUiState = combine(
        _themeFlow,
        _behaviorFlow,
        _receiptFlow
    ) { themeState, behaviorState, receiptSettings ->
        themeState.copy(
            defaultScreen = behaviorState.defaultScreen,
            isChecadorDialog = behaviorState.isChecadorDialog,
            showExtraPricesChecador = behaviorState.showExtraPricesChecador,
            useProductTableInCatalog = behaviorState.useProductTableInCatalog,
            defaultRetailMargin = behaviorState.defaultRetailMargin,
            defaultWholesaleMargin = behaviorState.defaultWholesaleMargin,
            isRoundingEnabled = behaviorState.isRoundingEnabled,
            roundRetailPrice = behaviorState.roundRetailPrice,
            roundWholesalePrice = behaviorState.roundWholesalePrice,
            roundTicketTotal = behaviorState.roundTicketTotal,
            disallowCardPaymentOnWholesale = behaviorState.disallowCardPaymentOnWholesale,
            supabaseUrl = behaviorState.supabaseUrl,
            supabaseKey = behaviorState.supabaseKey,
            lastSyncTimestamp = behaviorState.lastSyncTimestamp,
            autoSyncEnabled = behaviorState.autoSyncEnabled,
            autoBackupEnabled = behaviorState.autoBackupEnabled,
            lastBackupTimestamp = behaviorState.lastBackupTimestamp,
            backupDirectoryPath = behaviorState.backupDirectoryPath,
            receiptSettings = receiptSettings
        )
    }

    val uiState: StateFlow<AjustesUiState> = combine(
        _baseUiState,
        _updateState,
        syncRepository.syncState,
        getCashiersUseCase()
    ) { baseState, updateState, syncState, cashiers ->
        baseState.copy(
            syncState = syncState,
            currentVersion = updateRepository.getCurrentVersion(),
            isCheckingUpdates = updateState.isCheckingUpdates,
            updateCheckResult = updateState.updateCheckResult,
            downloadState = updateState.downloadState,
            isTestingConnection = updateState.isTestingConnection,
            connectionTestResult = updateState.connectionTestResult,
            syncMessage = updateState.syncMessage,
            isBackingUp = updateState.isBackingUp,
            backupMessage = updateState.backupMessage,
            cashiers = cashiers,
            isSavingCashier = updateState.isSavingCashier,
            isDeletingCashier = updateState.isDeletingCashier,
            cashierActionError = updateState.cashierActionError,
            cashierActionSuccess = updateState.cashierActionSuccess,
            isResettingApp = updateState.isResettingApp,
            resetAppError = updateState.resetAppError,
            resetAppSuccess = updateState.resetAppSuccess
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AjustesUiState(
            currentVersion = updateRepository.getCurrentVersion(),
            backupDirectoryPath = backupRepository.getDefaultBackupDirectoryPath()
        )
    )

    fun setUseDynamicColor(useDynamic: Boolean) {
        viewModelScope.launch {
            repository.setUseDynamicColor(useDynamic)
        }
    }

    fun setSeedColor(color: Color) {
        viewModelScope.launch {
            repository.setSeedColor(color)
        }
    }

    fun setIsAmoled(isAmoled: Boolean) {
        viewModelScope.launch {
            repository.setIsAmoled(isAmoled)
        }
    }

    fun setDarkModeConfig(config: DarkModeConfig) {
        viewModelScope.launch {
            repository.setDarkModeConfig(config)
        }
    }

    fun setPaletteStyle(style: PaletteStyle) {
        viewModelScope.launch {
            repository.setPaletteStyle(style)
        }
    }

    fun setAppScale(scale: Float) {
        viewModelScope.launch {
            repository.setAppScale(scale)
        }
    }

    fun setDefaultScreen(screen: Screen) {
        viewModelScope.launch {
            repository.setDefaultScreen(screen)
        }
    }

    fun setIsChecadorDialog(isDialog: Boolean) {
        viewModelScope.launch {
            repository.setIsChecadorDialog(isDialog)
        }
    }

    fun setShowExtraPricesChecador(show: Boolean) {
        viewModelScope.launch {
            repository.setShowExtraPricesChecador(show)
        }
    }

    fun setUseProductTableInCatalog(enabled: Boolean) {
        viewModelScope.launch {
            repository.setUseProductTableInCatalog(enabled)
        }
    }

    fun setDefaultRetailMargin(margin: Double) {
        viewModelScope.launch {
            repository.setDefaultRetailMargin(margin)
            launch(Dispatchers.IO) {
                syncRepository.syncAll()
            }
        }
    }

    fun setDefaultWholesaleMargin(margin: Double) {
        viewModelScope.launch {
            repository.setDefaultWholesaleMargin(margin)
            launch(Dispatchers.IO) {
                syncRepository.syncAll()
            }
        }
    }

    fun setIsRoundingEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setIsRoundingEnabled(enabled)
            launch(Dispatchers.IO) {
                syncRepository.syncAll()
            }
        }
    }

    fun setRoundRetailPrice(enabled: Boolean) {
        viewModelScope.launch {
            repository.setRoundRetailPrice(enabled)
            launch(Dispatchers.IO) {
                syncRepository.syncAll()
            }
        }
    }

    fun setRoundWholesalePrice(enabled: Boolean) {
        viewModelScope.launch {
            repository.setRoundWholesalePrice(enabled)
            launch(Dispatchers.IO) {
                syncRepository.syncAll()
            }
        }
    }

    fun setRoundTicketTotal(enabled: Boolean) {
        viewModelScope.launch {
            repository.setRoundTicketTotal(enabled)
            launch(Dispatchers.IO) {
                syncRepository.syncAll()
            }
        }
    }

    fun setDisallowCardPaymentOnWholesale(enabled: Boolean) {
        viewModelScope.launch {
            repository.setDisallowCardPaymentOnWholesale(enabled)
            launch(Dispatchers.IO) {
                syncRepository.syncAll()
            }
        }
    }

    fun checkForUpdates() {
        if (_updateState.value.isCheckingUpdates) return
        viewModelScope.launch {
            _updateState.value = _updateState.value.copy(
                isCheckingUpdates = true,
                updateCheckResult = null,
                downloadState = UpdateDownloadState.Idle
            )
            val result = updateRepository.checkForUpdates()
            _updateState.value = _updateState.value.copy(
                isCheckingUpdates = false,
                updateCheckResult = result
            )
        }
    }

    fun downloadAndInstallUpdate(asset: ReleaseAsset) {
        viewModelScope.launch {
            _updateState.value = _updateState.value.copy(
                downloadState = UpdateDownloadState.Downloading(
                    progress = 0f,
                    downloadedBytes = 0L,
                    totalBytes = asset.sizeBytes
                )
            )

            val result = updateRepository.downloadAndInstall(
                asset = asset,
                onProgress = { progress, downloaded, total ->
                    _updateState.value = _updateState.value.copy(
                        downloadState = UpdateDownloadState.Downloading(
                            progress = progress,
                            downloadedBytes = downloaded,
                            totalBytes = total
                        )
                    )
                }
            )

            if (result.isSuccess) {
                _updateState.value = _updateState.value.copy(
                    downloadState = UpdateDownloadState.Installing
                )
            } else {
                _updateState.value = _updateState.value.copy(
                    downloadState = UpdateDownloadState.Error(
                        result.exceptionOrNull()?.message ?: "Error al descargar la actualización"
                    )
                )
            }
        }
    }

    fun dismissUpdateResult() {
        _updateState.value = _updateState.value.copy(
            updateCheckResult = null,
            downloadState = UpdateDownloadState.Idle
        )
    }

    fun setAutoSyncEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setAutoSyncEnabled(enabled)
        }
    }

    fun setReceiptSettings(settings: ReceiptSettings) {
        viewModelScope.launch {
            repository.setReceiptSettings(settings)
            launch(Dispatchers.IO) {
                syncRepository.syncAll()
            }
        }
    }

    fun testAndSaveConnection(url: String, key: String) {
        viewModelScope.launch {
            val cleanUrl = url.trim()
            val cleanKey = key.trim()
            repository.setSupabaseUrl(cleanUrl)
            repository.setSupabaseKey(cleanKey)

            if (cleanUrl.isBlank() || cleanKey.isBlank()) {
                _updateState.value = _updateState.value.copy(
                    connectionTestResult = "Ingresa la URL y la API Key",
                    isTestingConnection = false
                )
                return@launch
            }

            _updateState.value = _updateState.value.copy(
                isTestingConnection = true,
                connectionTestResult = null
            )

            val result = syncRepository.testConnection(cleanUrl, cleanKey)
            _updateState.value = _updateState.value.copy(
                isTestingConnection = false,
                connectionTestResult = if (result.isSuccess) {
                    "SUCCESS"
                } else {
                    result.exceptionOrNull()?.message ?: "Error al conectar"
                }
            )
        }
    }

    fun syncNow(forceFullSync: Boolean = false) {
        viewModelScope.launch {
            _updateState.value = _updateState.value.copy(syncMessage = null)
            val result = syncRepository.syncAll(forceFullSync = forceFullSync, isManual = true)
            if (result.isSuccess) {
                val report = result.getOrNull()
                _updateState.value = _updateState.value.copy(
                    syncMessage = "SYNC_SUCCESS:${report?.itemsPushed ?: 0}:${report?.itemsPulled ?: 0}"
                )
            } else {
                _updateState.value = _updateState.value.copy(
                    syncMessage = "SYNC_ERROR:${result.exceptionOrNull()?.message ?: "Error"}"
                )
            }
        }
    }

    fun setAutoBackupEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setAutoBackupEnabled(enabled)
        }
    }

    fun performManualBackup() {
        if (_updateState.value.isBackingUp) return
        viewModelScope.launch {
            _updateState.value = _updateState.value.copy(
                isBackingUp = true,
                backupMessage = null
            )
            val result = backupRepository.performBackup()
            if (result.isSuccess) {
                val path = result.getOrNull() ?: ""
                _updateState.value = _updateState.value.copy(
                    isBackingUp = false,
                    backupMessage = "BACKUP_SUCCESS:$path"
                )
            } else {
                val error = result.exceptionOrNull()?.message ?: "Error desconocido"
                _updateState.value = _updateState.value.copy(
                    isBackingUp = false,
                    backupMessage = "BACKUP_ERROR:$error"
                )
            }
        }
    }

    fun dismissBackupMessage() {
        _updateState.value = _updateState.value.copy(backupMessage = null)
    }

    fun setBackupDirectoryPath(path: String) {
        viewModelScope.launch {
            backupRepository.setBackupDirectoryPath(path)
        }
    }

    fun resetBackupDirectoryPathToDefault() {
        viewModelScope.launch {
            backupRepository.resetBackupDirectoryPathToDefault()
        }
    }

    fun saveCashier(id: String?, nombre: String, pin: String) {
        viewModelScope.launch {
            _updateState.value = _updateState.value.copy(
                isSavingCashier = true,
                cashierActionError = null,
                cashierActionSuccess = null
            )
            val result = saveCashierUseCase(id, nombre, pin)
            if (result.isSuccess) {
                _updateState.value = _updateState.value.copy(
                    isSavingCashier = false,
                    cashierActionSuccess = if (id == null) "Cajero agregado exitosamente" else "Cajero actualizado exitosamente"
                )
                launch(Dispatchers.IO) {
                    syncRepository.syncAll()
                }
            } else {
                _updateState.value = _updateState.value.copy(
                    isSavingCashier = false,
                    cashierActionError = result.exceptionOrNull()?.message ?: "Error al guardar cajero"
                )
            }
        }
    }

    fun deleteCashier(id: String) {
        viewModelScope.launch {
            _updateState.value = _updateState.value.copy(
                isDeletingCashier = true,
                cashierActionError = null,
                cashierActionSuccess = null
            )
            val result = deleteCashierUseCase(id)
            if (result.isSuccess) {
                _updateState.value = _updateState.value.copy(
                    isDeletingCashier = false,
                    cashierActionSuccess = "Cajero eliminado exitosamente"
                )
                launch(Dispatchers.IO) {
                    syncRepository.syncAll()
                }
            } else {
                _updateState.value = _updateState.value.copy(
                    isDeletingCashier = false,
                    cashierActionError = result.exceptionOrNull()?.message ?: "Error al eliminar cajero"
                )
            }
        }
    }

    fun clearCashierActionMessage() {
        _updateState.value = _updateState.value.copy(
            cashierActionError = null,
            cashierActionSuccess = null
        )
    }

    fun resetAppToFactoryDefaults() {
        viewModelScope.launch {
            _updateState.value = _updateState.value.copy(
                isResettingApp = true,
                resetAppError = null,
                resetAppSuccess = null
            )
            val result = resetAppToFactoryDefaultsUseCase()
            if (result.isSuccess) {
                _updateState.value = _updateState.value.copy(
                    isResettingApp = false,
                    resetAppSuccess = "La aplicación se ha restablecido a los valores de fábrica exitosamente."
                )
            } else {
                _updateState.value = _updateState.value.copy(
                    isResettingApp = false,
                    resetAppError = result.exceptionOrNull()?.message ?: "Error al restablecer la aplicación"
                )
            }
        }
    }

    fun clearResetAppMessage() {
        _updateState.value = _updateState.value.copy(
            resetAppError = null,
            resetAppSuccess = null
        )
    }
}
