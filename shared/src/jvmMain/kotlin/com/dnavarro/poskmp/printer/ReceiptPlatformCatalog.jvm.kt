package com.dnavarro.poskmp.printer

import androidx.compose.runtime.Composable
import com.dnavarro.poskmp.domain.model.ReceiptPrinterOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.print.PrinterJob
import java.io.File
import javax.print.DocFlavor

actual fun hasBluetoothPrinterPermission(): Boolean = true

actual fun openBluetoothSettings() {}

@Composable
actual fun rememberBluetoothPermissionRequester(onPermissionResult: (Boolean) -> Unit): () -> Unit = {
    onPermissionResult(true)
}

actual suspend fun getReceiptPrinterOptions(): List<ReceiptPrinterOption> = withContext(Dispatchers.IO) {
    val options = mutableListOf<ReceiptPrinterOption>()

    // 1. Direct Linux/Unix Character Devices (/dev/usb/lp*, /dev/ttyUSB*, /dev/ttyACM*, /dev/rfcomm*)
    val osName = System.getProperty("os.name")?.lowercase() ?: ""
    if (osName.contains("linux") || osName.contains("unix") || osName.contains("mac")) {
        val usbDevices = (0..9).map { File("/dev/usb/lp$it") }.filter { it.exists() }
        usbDevices.forEach { dev ->
            options += ReceiptPrinterOption(
                id = "direct:${dev.absolutePath}",
                name = "ESC/POS USB Directo (${dev.name})",
                isDefault = false
            )
        }

        val serialDevices = ((0..3).map { File("/dev/ttyUSB$it") } + (0..3).map { File("/dev/ttyACM$it") })
            .filter { it.exists() }
        serialDevices.forEach { dev ->
            options += ReceiptPrinterOption(
                id = "direct:${dev.absolutePath}",
                name = "ESC/POS Serial Directo (${dev.name})",
                isDefault = false
            )
        }

        val btSerialDevices = (0..7).map { File("/dev/rfcomm$it") }.filter { it.exists() }
        btSerialDevices.forEach { dev ->
            options += ReceiptPrinterOption(
                id = "direct:${dev.absolutePath}",
                name = "ESC/POS Bluetooth Serial (${dev.name})",
                isDefault = false
            )
        }
    }

    // 2. System Print Services (CUPS / Windows Spooler)
    val services = runCatching { PrinterJob.lookupPrintServices() }.getOrDefault(emptyArray())
    val defaultName = runCatching { PrinterJob.getPrinterJob().printService?.name }.getOrNull()
    val sortedServices = services.sortedBy { it.name.lowercase() }

    sortedServices.forEach { service ->
        options += ReceiptPrinterOption(
            id = service.name,
            name = service.name,
            isDefault = service.name == defaultName
        )
        val supportsRaw = runCatching {
            service.isDocFlavorSupported(DocFlavor.BYTE_ARRAY.AUTOSENSE)
        }.getOrDefault(false)
        if (supportsRaw) {
            options += ReceiptPrinterOption(
                id = "raw:${service.name}",
                name = "${service.name} (ESC/POS RAW)",
                isDefault = false
            )
        }
    }

    options
}
