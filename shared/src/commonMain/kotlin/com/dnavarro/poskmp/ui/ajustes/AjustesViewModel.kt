package com.dnavarro.poskmp.ui.ajustes

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dnavarro.poskmp.data.SettingsRepository
import com.dnavarro.poskmp.data.updater.ReleaseAsset
import com.dnavarro.poskmp.data.updater.UpdateCheckResult
import com.dnavarro.poskmp.data.updater.UpdateDownloadState
import com.dnavarro.poskmp.data.updater.UpdateRepository
import com.dnavarro.poskmp.theme.DarkModeConfig
import com.dnavarro.poskmp.ui.Screen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private data class UpdateInternalState(
    val isCheckingUpdates: Boolean = false,
    val updateCheckResult: UpdateCheckResult? = null,
    val downloadState: UpdateDownloadState = UpdateDownloadState.Idle
)

/**
 * ViewModel for Settings screen according to Google UI Layer architecture.
 */
class AjustesViewModel(
    private val repository: SettingsRepository,
    private val updateRepository: UpdateRepository
) : ViewModel() {

    private val _updateState = MutableStateFlow(UpdateInternalState())

    private val _themeFlow = combine(
        repository.useDynamicColorFlow,
        repository.seedColorFlow,
        repository.isAmoledFlow,
        repository.darkModeConfigFlow,
        repository.appScaleFlow
    ) { useDynamicColor, seedColor, isAmoled, darkModeConfig, appScale ->
        AjustesUiState(
            useDynamicColor = useDynamicColor,
            seedColor = seedColor,
            isAmoled = isAmoled,
            darkModeConfig = darkModeConfig,
            appScale = appScale
        )
    }

    private val _behaviorFlow = combine(
        repository.defaultScreenFlow,
        repository.isChecadorDialogFlow,
        repository.showExtraPricesChecadorFlow,
        repository.defaultRetailMarginFlow,
        repository.defaultWholesaleMarginFlow
    ) { defaultScreen, isChecadorDialog, showExtraPricesChecador, defaultRetailMargin, defaultWholesaleMargin ->
        AjustesUiState(
            defaultScreen = defaultScreen,
            isChecadorDialog = isChecadorDialog,
            showExtraPricesChecador = showExtraPricesChecador,
            defaultRetailMargin = defaultRetailMargin,
            defaultWholesaleMargin = defaultWholesaleMargin
        )
    }

    val uiState: StateFlow<AjustesUiState> = combine(
        _themeFlow,
        _behaviorFlow,
        _updateState
    ) { themeState, behaviorState, updateState ->
        themeState.copy(
            defaultScreen = behaviorState.defaultScreen,
            isChecadorDialog = behaviorState.isChecadorDialog,
            showExtraPricesChecador = behaviorState.showExtraPricesChecador,
            defaultRetailMargin = behaviorState.defaultRetailMargin,
            defaultWholesaleMargin = behaviorState.defaultWholesaleMargin,
            currentVersion = updateRepository.getCurrentVersion(),
            isCheckingUpdates = updateState.isCheckingUpdates,
            updateCheckResult = updateState.updateCheckResult,
            downloadState = updateState.downloadState
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AjustesUiState(currentVersion = updateRepository.getCurrentVersion())
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

    fun setDefaultRetailMargin(margin: Double) {
        viewModelScope.launch {
            repository.setDefaultRetailMargin(margin)
        }
    }

    fun setDefaultWholesaleMargin(margin: Double) {
        viewModelScope.launch {
            repository.setDefaultWholesaleMargin(margin)
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
}
