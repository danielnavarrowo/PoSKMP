package com.dnavarro.poskmp.printer

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontFamily
import com.dnavarro.poskmp.domain.model.ReceiptPrinterOption
import java.awt.Font
import java.awt.GraphicsEnvironment
import java.awt.print.PrinterJob

@OptIn(ExperimentalTextApi::class)
private fun systemFontFamily(name: String): FontFamily = FontFamily(name)

actual fun getReceiptPrinterOptions(): List<ReceiptPrinterOption> {
    val services = PrinterJob.lookupPrintServices()
    val defaultName = runCatching { PrinterJob.getPrinterJob().printService?.name }.getOrNull()
    return services
        .map { service ->
            ReceiptPrinterOption(
                id = service.name,
                name = service.name,
                isDefault = service.name == defaultName
            )
        }
        .sortedBy { it.name.lowercase() }
}

actual fun getSystemReceiptFontFamilies(): List<String> =
    (GraphicsEnvironment.getLocalGraphicsEnvironment().availableFontFamilyNames.toList() +
            listOf(Font.MONOSPACED, Font.SANS_SERIF, Font.SERIF))
        .distinct()
        .sortedBy { it.lowercase() }

actual fun receiptFontFamily(name: String): FontFamily = systemFontFamily(name)
