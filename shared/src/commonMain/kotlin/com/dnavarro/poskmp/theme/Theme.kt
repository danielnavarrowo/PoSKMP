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
import com.dnavarro.poskmp.util.isAndroid
import com.materialkolor.DynamicMaterialExpressiveTheme
import com.materialkolor.PaletteStyle

@Composable
expect fun SystemDynamicTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppTheme(
    seedColor: Color = Color(0xFF0061A4),
    useDynamicColor: Boolean = isAndroid(),
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

    if (useDynamicColor && isAndroid()) {
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

