package com.dnavarro.poskmp.ui.ajustes

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dnavarro.poskmp.data.SettingsRepository
import com.dnavarro.poskmp.theme.DarkModeConfig
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
        repository.useDynamicColorFlow,
        repository.seedColorFlow,
        repository.isAmoledFlow,
        repository.darkModeConfigFlow
    ) { useDynamicColor, seedColor, isAmoled, darkModeConfig ->
        AjustesUiState(
            useDynamicColor = useDynamicColor,
            seedColor = seedColor,
            isAmoled = isAmoled,
            darkModeConfig = darkModeConfig
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
}
