package com.dnavarro.poskmp.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun PlatformBarcodeScanner(
    onScanResult: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
)

expect fun isCameraScannerAvailable(): Boolean
