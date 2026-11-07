package com.dnavarro.poskmp.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
expect fun ChecadorAnimatedBackground(
    onForegroundColorChanged: (Color) -> Unit = {},
    modifier: Modifier = Modifier
)

