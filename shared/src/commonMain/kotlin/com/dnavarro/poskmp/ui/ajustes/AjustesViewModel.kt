package com.dnavarro.poskmp.ui.ajustes

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dnavarro.poskmp.data.SettingsRepository
import com.dnavarro.poskmp.theme.DarkModeConfig
import com.dnavarro.poskmp.ui.Screen
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for Settings screen according to Google UI Layer architecture.
 */
class AjustesViewModel(
    private val repository: SettingsRepository
) : ViewModel() {

    val uiState: StateFlow<AjustesUiState> = combine(
        combine(
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
        },
        repository.defaultScreenFlow,
        repository.isChecadorDialogFlow,
        repository.showExtraPricesChecadorFlow,
        combine(
            repository.defaultRetailMarginFlow,
            repository.defaultWholesaleMarginFlow
        ) { retailMargin, wholesaleMargin -> retailMargin to wholesaleMargin }
    ) { state, defaultScreen, isChecadorDialog, showExtraPricesChecador, (defaultRetailMargin, defaultWholesaleMargin) ->
        state.copy(
            defaultScreen = defaultScreen,
            isChecadorDialog = isChecadorDialog,
            showExtraPricesChecador = showExtraPricesChecador,
            defaultRetailMargin = defaultRetailMargin,
            defaultWholesaleMargin = defaultWholesaleMargin
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AjustesUiState()
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
}
