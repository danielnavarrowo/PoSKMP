package com.dnavarro.poskmp.theme

import androidx.compose.runtime.Composable

@Composable
actual fun SystemDynamicTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit
) {
    content()
}




