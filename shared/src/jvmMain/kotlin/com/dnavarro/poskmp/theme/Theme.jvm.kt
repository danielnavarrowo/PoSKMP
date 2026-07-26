package com.dnavarro.poskmp.theme

import androidx.compose.runtime.Composable

actual fun isDynamicColorSupported(): Boolean {
    return false
}

@Composable
actual fun SystemDynamicTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit
) {
    content()
}



