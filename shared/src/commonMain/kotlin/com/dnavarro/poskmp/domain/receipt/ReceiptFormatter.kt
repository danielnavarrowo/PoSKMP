package com.dnavarro.poskmp.domain.receipt

import com.dnavarro.poskmp.domain.model.ReceiptAlignment
import com.dnavarro.poskmp.domain.model.ReceiptDocument
import com.dnavarro.poskmp.domain.model.ReceiptItem
import com.dnavarro.poskmp.domain.model.ReceiptLine
import com.dnavarro.poskmp.domain.model.ReceiptSettings
import com.dnavarro.poskmp.util.formatEpochMillisToDateTime
import com.dnavarro.poskmp.util.formatPrice

object ReceiptFormatter {
    fun create(
        folio: Long,
        createdAt: Long,
        items: List<ReceiptItem>,
        total: Double,
        paid: Double,
        change: Double,
        paymentMethod: String,
        customerName: String?,
        settings: ReceiptSettings
    ): ReceiptDocument {
        val width = ((settings.paperWidthMm * 48) / 80).coerceIn(24, 80)
        val lines = mutableListOf<ReceiptLine>()

        fun centered(text: String, emphasized: Boolean = false) {
            wrap(text, width).forEach { lines += ReceiptLine(it, ReceiptAlignment.CENTER, emphasized) }
        }

        fun keyValue(label: String, value: String, emphasized: Boolean = false) {
            val separator = " $label: "
            val available = (width - separator.length).coerceAtLeast(1)
            val valueText = value.take(available)
            lines += ReceiptLine(
                separator.trimStart() + valueText,
                ReceiptAlignment.LEFT,
                emphasized
            )
        }

        settings.storeName.trim().takeIf { it.isNotEmpty() }?.let { centered(it, emphasized = true) }
        settings.storeAddress.trim().takeIf { it.isNotEmpty() }?.let { centered(it) }
        settings.storePhone.trim().takeIf { it.isNotEmpty() }?.let { centered("Tel. $it") }
        if (lines.isNotEmpty()) lines += ReceiptLine("")

        centered("TICKET DE COMPRA", emphasized = true)
        keyValue("Folio", folio.toString(), emphasized = true)
        keyValue("Fecha", formatEpochMillisToDateTime(createdAt))
        customerName?.trim()?.takeIf { it.isNotEmpty() }?.let { keyValue("Cliente", it) }
        lines += ReceiptLine("-".repeat(width))

        items.forEach { item ->
            wrap(item.name.trim().ifEmpty { "Producto" }, width).forEach { nameLine ->
                lines += ReceiptLine(nameLine)
            }
            val quantity = formatQuantity(item.quantity, item.isWeightBased)
            val detail = "$quantity x $${item.unitPrice.toString().formatPrice()}"
            val subtotal = "$${item.subtotal.toString().formatPrice()}"
            val detailWidth = (width - detail.length - subtotal.length).coerceAtLeast(1)
            lines += ReceiptLine(
                detail.take(detailWidth).padEnd(detailWidth) + subtotal.padStart(subtotal.length),
                ReceiptAlignment.LEFT
            )
        }

        lines += ReceiptLine("-".repeat(width))
        lines += ReceiptLine(
            "TOTAL".padEnd((width - totalText(total).length).coerceAtLeast(1)) + totalText(total),
            ReceiptAlignment.LEFT,
            emphasized = true
        )
        keyValue("Pago", paymentMethod)
        if (paid > 0.0) keyValue("Recibido", money(paid))
        if (change > 0.0) keyValue("Cambio", money(change), emphasized = true)

        settings.footerMessage.trim().takeIf { it.isNotEmpty() }?.let {
            lines += ReceiptLine("")
            centered(it)
        }
        repeat(settings.feedLines.coerceIn(0, 10)) { lines += ReceiptLine("") }

        return ReceiptDocument(
            folio = folio,
            createdAt = createdAt,
            lines = lines,
            paperWidthMm = settings.paperWidthMm.coerceIn(55, 105),
            printerType = settings.printerType,
            printerId = settings.printerId,
            font = settings.font,
            fontFamily = settings.fontFamily.ifBlank { settings.font.defaultFamilyName },
            fontSize = settings.fontSize.coerceIn(8, 32),
            feedLines = settings.feedLines.coerceIn(0, 10)
        )
    }

    private fun money(value: Double): String = "$${value.toString().formatPrice()}"

    private fun totalText(value: Double): String = money(value)

    private fun formatQuantity(quantity: Double, isWeightBased: Boolean): String {
        return if (isWeightBased) {
            val text = quantity.toString()
            val decimals = text.substringAfter('.', "").padEnd(3, '0').take(3)
            "${text.substringBefore('.')}.${decimals} kg"
        } else {
            quantity.toInt().toString()
        }
    }

    private fun wrap(value: String, width: Int): List<String> {
        val text = value.trim()
        if (text.isEmpty()) return listOf("")
        if (text.length <= width) return listOf(text)
        return text.chunked(width)
    }
}
