package com.dnavarro.poskmp.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.Color
import com.dnavarro.poskmp.util.isDynamicColorSupported
import com.materialkolor.DynamicMaterialExpressiveTheme
import com.materialkolor.PaletteStyle

val PRESET_SEED_COLORS = listOf(
    Color(0xFFEB4CFF),
    Color(0xFF2FC991),
    Color(0xFF6750A4),
    Color(0xFFEC7E00),
    Color(0xFFF7FF00),
    Color(0xFFFF88B4),
    Color(0xFF00ACC1),
    Color(0xFFFA8072),
    Color(0xFF664F49)
)

val DEFAULT_SEED_COLOR = PRESET_SEED_COLORS.first()

@Composable
expect fun SystemDynamicTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppTheme(
    seedColor: Color = DEFAULT_SEED_COLOR,
    useDynamicColor: Boolean = isDynamicColorSupported(),
    isAmoled: Boolean = false,
    paletteStyle: PaletteStyle = PaletteStyle.TonalSpot,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val currentContent by rememberUpdatedState(content)
    val movableContent = remember {
        movableContentOf {
            currentContent()
        }
    }

    if (useDynamicColor && isDynamicColorSupported()) {
        SystemDynamicTheme(
            darkTheme = darkTheme,
            content = movableContent
        )
    } else {
        DynamicMaterialExpressiveTheme(
            seedColor = seedColor,
            typography = AppTypography,
            motionScheme = MotionScheme.expressive(),
            isAmoled = isAmoled,
            isDark = darkTheme,
            animate = true,
            style = paletteStyle,
            content = movableContent
        )
    }
}

