package com.dnavarro.poskmp.printer

import android.content.ComponentName
import android.content.Intent
import android.graphics.Typeface
import android.printservice.PrintService
import androidx.compose.ui.text.font.FontFamily
import com.dnavarro.poskmp.db.DatabaseDriverFactory
import com.dnavarro.poskmp.domain.model.ReceiptPrinterOption

actual fun getReceiptPrinterOptions(): List<ReceiptPrinterOption> {
    val context = DatabaseDriverFactory.appContext ?: return emptyList()
    val services = runCatching {
        context.packageManager.queryIntentServices(
            Intent(PrintService.SERVICE_INTERFACE),
            0
        )
    }.getOrDefault(emptyList())
    return services.map { resolveInfo ->
        val serviceInfo = resolveInfo.serviceInfo
        ReceiptPrinterOption(
            id = ComponentName(serviceInfo.packageName, serviceInfo.name).flattenToString(),
            name = resolveInfo.loadLabel(context.packageManager).toString()
        )
    }.distinctBy { it.id }.sortedBy { it.name.lowercase() }
}

actual fun getSystemReceiptFontFamilies(): List<String> =
    listOf(
        "sans-serif",
        "serif",
        "monospace",
        "sans-serif-light",
        "sans-serif-medium",
        "sans-serif-condensed",
        "sans-serif-black",
        "sans-serif-thin",
        "sans-serif-smallcaps"
    )
        .filter { Typeface.create(it, Typeface.NORMAL) != null }
        .distinct()
        .sortedBy { it.lowercase() }

actual fun receiptFontFamily(name: String): FontFamily =
    FontFamily(Typeface.create(name, Typeface.NORMAL))
