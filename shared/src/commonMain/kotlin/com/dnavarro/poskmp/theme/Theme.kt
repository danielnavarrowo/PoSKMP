package com.dnavarro.poskmp.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.materialkolor.DynamicMaterialExpressiveTheme

expect fun isDynamicColorSupported(): Boolean

@Composable
expect fun SystemDynamicTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppTheme(
    seedColor: Color = Color(0xFF0061A4),
    useDynamicColor: Boolean = isDynamicColorSupported(),
    isAmoled: Boolean = false,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    if (useDynamicColor && isDynamicColorSupported()) {
        SystemDynamicTheme(
            darkTheme = darkTheme,
            content = content
        )
    } else {
        DynamicMaterialExpressiveTheme(
            seedColor = seedColor,
            motionScheme = MotionScheme.expressive(),
            isAmoled = isAmoled,
            isDark = darkTheme,
            animate = true,
            content = content,
        )
    }
}

