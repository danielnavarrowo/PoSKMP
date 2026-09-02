package com.dnavarro.poskmp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@Composable
actual fun ChecadorAnimatedBackground(
    modifier: Modifier
) {
    val backgroundGradient = remember {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFFFACD8A), // Warm Peach
                Color(0xFF13ADC4), // Vibrant Turquoise
                Color(0xFFFD820B), // Burning Orange
                Color(0xFFFD2945), // Neon Coral Red
                Color(0xFF010203)  // Midnight Void
            )
        )
    }
    Box(modifier = modifier.background(backgroundGradient))
}
