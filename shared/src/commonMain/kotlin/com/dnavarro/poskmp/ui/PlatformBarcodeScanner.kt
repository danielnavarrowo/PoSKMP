package com.dnavarro.poskmp.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.dnavarro.poskmp.db.Products

@Composable
expect fun PlatformBarcodeScanner(
    onScanResult: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    statusMessage: String? = null,
    lastScannedProduct: Products? = null,
    lastScannedQuantity: Double = 1.0,
    onUndo: (() -> Unit)? = null,
    onQuantityChange: ((Double) -> Unit)? = null
)

expect fun isCameraScannerAvailable(): Boolean
