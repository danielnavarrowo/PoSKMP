package com.dnavarro.poskmp.printer

import androidx.compose.runtime.Composable
import com.dnavarro.poskmp.domain.model.ReceiptPrinterOption

expect suspend fun getReceiptPrinterOptions(): List<ReceiptPrinterOption>

expect fun hasBluetoothPrinterPermission(): Boolean

expect fun openBluetoothSettings()

@Composable
expect fun rememberBluetoothPermissionRequester(onPermissionResult: (Boolean) -> Unit): () -> Unit
