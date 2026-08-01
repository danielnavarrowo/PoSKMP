package com.dnavarro.poskmp.theme

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
actual fun SystemDynamicTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        motionScheme = MotionScheme.expressive(),
        content = content
    )
}

