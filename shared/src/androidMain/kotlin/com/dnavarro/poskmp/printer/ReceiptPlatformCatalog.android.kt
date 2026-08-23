package com.dnavarro.poskmp.printer

import android.content.ComponentName
import android.content.Intent
import android.printservice.PrintService
import com.dnavarro.poskmp.db.DatabaseDriverFactory
import com.dnavarro.poskmp.domain.model.ReceiptPrinterOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual suspend fun getReceiptPrinterOptions(): List<ReceiptPrinterOption> = withContext(Dispatchers.IO) {
    val context = DatabaseDriverFactory.appContext ?: return@withContext emptyList()
    val services = runCatching {
        context.packageManager.queryIntentServices(
            Intent(PrintService.SERVICE_INTERFACE),
            0
        )
    }.getOrDefault(emptyList())
    services.map { resolveInfo ->
        val serviceInfo = resolveInfo.serviceInfo
        ReceiptPrinterOption(
            id = ComponentName(serviceInfo.packageName, serviceInfo.name).flattenToString(),
            name = resolveInfo.loadLabel(context.packageManager).toString()
        )
    }.distinctBy { it.id }.sortedBy { it.name.lowercase() }
}
