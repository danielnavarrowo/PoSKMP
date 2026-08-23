package com.dnavarro.poskmp.printer

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontFamily
import com.dnavarro.poskmp.domain.model.ReceiptPrinterOption
import java.awt.Font
import java.awt.GraphicsEnvironment
import java.awt.print.PrinterJob
import java.io.File

@OptIn(ExperimentalTextApi::class)
private fun systemFontFamily(name: String): FontFamily = FontFamily(name)

actual fun getReceiptPrinterOptions(): List<ReceiptPrinterOption> {
    val options = mutableListOf<ReceiptPrinterOption>()

    // 1. Direct Linux/Unix Character Devices (/dev/usb/lp*, /dev/ttyUSB*, /dev/ttyACM*)
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
    }

    // 2. System Print Services (CUPS / Windows Spooler)
    val services = PrinterJob.lookupPrintServices()
    val defaultName = runCatching { PrinterJob.getPrinterJob().printService?.name }.getOrNull()
    val systemOptions = services
        .map { service ->
            ReceiptPrinterOption(
                id = service.name,
                name = service.name,
                isDefault = service.name == defaultName
            )
        }
        .sortedBy { it.name.lowercase() }

    options += systemOptions
    return options
}

actual fun getSystemReceiptFontFamilies(): List<String> =
    (GraphicsEnvironment.getLocalGraphicsEnvironment().availableFontFamilyNames.toList() +
            listOf(Font.MONOSPACED, Font.SANS_SERIF, Font.SERIF))
        .distinct()
        .sortedBy { it.lowercase() }

actual fun receiptFontFamily(name: String): FontFamily = systemFontFamily(name)
