package com.dnavarro.poskmp.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

import com.dnavarro.poskmp.db.Products

@Suppress("EmptyMethod")
@Composable
actual fun PlatformBarcodeScanner(
    onScanResult: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier,
    statusMessage: String?,
    lastScannedProduct: Products?,
    lastScannedQuantity: Double,
    onUndo: (() -> Unit)?,
    onQuantityChange: ((Double) -> Unit)?,
    isChecadorMode: Boolean,
    prioritizeDeliveryPrice: Boolean,
    onEditProduct: ((Products) -> Unit)?
) {
    // No-op on JVM/Desktop target
}

@Suppress("SameReturnValue")
actual fun isCameraScannerAvailable(): Boolean = false
