package com.dnavarro.poskmp.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun PlatformBarcodeScanner(
    onScanResult: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier
) {
    // No-op on JVM/Desktop target
}

@Suppress("SameReturnValue")
actual fun isCameraScannerAvailable(): Boolean = false
