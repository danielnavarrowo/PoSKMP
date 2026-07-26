package com.dnavarro.poskmp.data

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.dnavarro.poskmp.util.isAndroid
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsRepository(
    private val dataStore: DataStore<Preferences> = getDataStore()
) {
    private object PreferenceKeys {
        val USE_DYNAMIC_COLOR = booleanPreferencesKey("use_dynamic_color")
        val SEED_COLOR = intPreferencesKey("seed_color_argb")
        val IS_AMOLED = booleanPreferencesKey("is_amoled")
    }

    val useDynamicColorFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferenceKeys.USE_DYNAMIC_COLOR] ?: isAndroid()
    }

    val seedColorFlow: Flow<Color> = dataStore.data.map { preferences ->
        val argb = preferences[PreferenceKeys.SEED_COLOR] ?: 0xFF0061A4.toInt()
        Color(argb)
    }

    val isAmoledFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferenceKeys.IS_AMOLED] ?: false
    }

    suspend fun setUseDynamicColor(useDynamic: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.USE_DYNAMIC_COLOR] = useDynamic
        }
    }

    suspend fun setSeedColor(color: Color) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.SEED_COLOR] = color.toArgb()
        }
    }

    suspend fun setIsAmoled(isAmoled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.IS_AMOLED] = isAmoled
        }
    }
}

