package com.dnavarro.poskmp.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

import com.dnavarro.poskmp.db.Products

@Composable
actual fun PlatformBarcodeScanner(
    onScanResult: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier,
    statusMessage: String?,
    lastScannedProduct: Products?,
    lastScannedQuantity: Double,
    onUndo: (() -> Unit)?,
    onQuantityChange: ((Double) -> Unit)?
) {
    // No-op on JVM/Desktop target
}

@Suppress("SameReturnValue")
actual fun isCameraScannerAvailable(): Boolean = false
