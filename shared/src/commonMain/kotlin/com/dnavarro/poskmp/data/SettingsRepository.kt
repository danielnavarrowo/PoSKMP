package com.dnavarro.poskmp.data

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.dnavarro.poskmp.theme.DarkModeConfig
import com.dnavarro.poskmp.ui.Screen
import com.dnavarro.poskmp.util.isAndroid
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

import androidx.datastore.preferences.core.floatPreferencesKey

/**
 * Interface defining settings data operations.
 */
interface SettingsRepository {
    val useDynamicColorFlow: Flow<Boolean>
    val seedColorFlow: Flow<Color>
    val isAmoledFlow: Flow<Boolean>
    val darkModeConfigFlow: Flow<DarkModeConfig>
    val appScaleFlow: Flow<Float>
    val defaultScreenFlow: Flow<Screen>
    val isChecadorDialogFlow: Flow<Boolean>

    suspend fun setUseDynamicColor(useDynamic: Boolean)
    suspend fun setSeedColor(color: Color)
    suspend fun setIsAmoled(isAmoled: Boolean)
    suspend fun setDarkModeConfig(config: DarkModeConfig)
    suspend fun setAppScale(scale: Float)
    suspend fun setDefaultScreen(screen: Screen)
    suspend fun setIsChecadorDialog(isDialog: Boolean)
}

/**
 * Concrete DataStore implementation of [SettingsRepository].
 */
class SettingsRepositoryImpl(
    private val dataStore: DataStore<Preferences> = getDataStore()
) : SettingsRepository {

    private object PreferenceKeys {
        val USE_DYNAMIC_COLOR = booleanPreferencesKey("use_dynamic_color")
        val SEED_COLOR = intPreferencesKey("seed_color_argb")
        val IS_AMOLED = booleanPreferencesKey("is_amoled")
        val DARK_MODE_CONFIG = stringPreferencesKey("dark_mode_config")
        val APP_SCALE = floatPreferencesKey("app_scale")
        val DEFAULT_SCREEN = stringPreferencesKey("default_screen")
        val IS_CHECADOR_DIALOG = booleanPreferencesKey("is_checador_dialog")
    }

    override val useDynamicColorFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferenceKeys.USE_DYNAMIC_COLOR] ?: isAndroid()
    }

    override val seedColorFlow: Flow<Color> = dataStore.data.map { preferences ->
        val argb = preferences[PreferenceKeys.SEED_COLOR] ?: 0xFF0061A4.toInt()
        Color(argb)
    }

    override val isAmoledFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferenceKeys.IS_AMOLED] ?: false
    }

    override val darkModeConfigFlow: Flow<DarkModeConfig> = dataStore.data.map { preferences ->
        val configName = preferences[PreferenceKeys.DARK_MODE_CONFIG] ?: DarkModeConfig.SYSTEM.name
        try {
            DarkModeConfig.valueOf(configName)
        } catch (_: Exception) {
            DarkModeConfig.SYSTEM
        }
    }

    override val appScaleFlow: Flow<Float> = dataStore.data.map { preferences ->
        preferences[PreferenceKeys.APP_SCALE] ?: 1.0f
    }

    override val defaultScreenFlow: Flow<Screen> = dataStore.data.map { preferences ->
        val screenName = preferences[PreferenceKeys.DEFAULT_SCREEN] ?: Screen.VENTA.name
        try {
            val screen = Screen.valueOf(screenName)
            if (screen == Screen.AJUSTES) Screen.VENTA else screen
        } catch (_: Exception) {
            Screen.VENTA
        }
    }

    override val isChecadorDialogFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferenceKeys.IS_CHECADOR_DIALOG] ?: true
    }

    override suspend fun setUseDynamicColor(useDynamic: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.USE_DYNAMIC_COLOR] = useDynamic
        }
    }

    override suspend fun setSeedColor(color: Color) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.SEED_COLOR] = color.toArgb()
        }
    }

    override suspend fun setIsAmoled(isAmoled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.IS_AMOLED] = isAmoled
        }
    }

    override suspend fun setDarkModeConfig(config: DarkModeConfig) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.DARK_MODE_CONFIG] = config.name
        }
    }

    override suspend fun setAppScale(scale: Float) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.APP_SCALE] = scale
        }
    }

    override suspend fun setDefaultScreen(screen: Screen) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.DEFAULT_SCREEN] = screen.name
        }
    }

    override suspend fun setIsChecadorDialog(isDialog: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.IS_CHECADOR_DIALOG] = isDialog
        }
    }
}
