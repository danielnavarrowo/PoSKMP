package com.dnavarro.poskmp.domain.receipt

import com.dnavarro.poskmp.domain.model.ReceiptAlignment
import com.dnavarro.poskmp.domain.model.ReceiptDocument
import com.dnavarro.poskmp.domain.model.ReceiptItem
import com.dnavarro.poskmp.domain.model.ReceiptLine
import com.dnavarro.poskmp.domain.model.ReceiptSettings
import com.dnavarro.poskmp.util.formatEpochMillisToDateTime
import com.dnavarro.poskmp.util.formatPrice
import kotlin.math.roundToInt

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
        val baseCharsPer80mm = when {
            settings.fontSize >= 25 -> 16
            settings.fontSize >= 19 -> 24
            settings.fontSize in 14..18 -> 32
            settings.fontSize <= 9 -> 64
            else -> 48
        }
        val width = ((settings.paperWidthMm * baseCharsPer80mm) / 80).coerceIn(12, 80)
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

        fun twoColumn(left: String, right: String, emphasized: Boolean = false) {
            val space = (width - left.length - right.length).coerceAtLeast(1)
            lines += ReceiptLine(
                left + " ".repeat(space) + right,
                ReceiptAlignment.LEFT,
                emphasized
            )
        }

        // 1. Store Header
        settings.storeName.trim().takeIf { it.isNotEmpty() }?.let { centered(it, emphasized = true) }
        settings.storeAddress.trim().takeIf { it.isNotEmpty() }?.let { centered(it) }
        settings.storePhone.trim().takeIf { it.isNotEmpty() }?.let { centered("Tel. $it") }
        if (lines.isNotEmpty()) lines += ReceiptLine("")

        // 2. Receipt metadata
        centered("TICKET DE COMPRA", emphasized = true)
        keyValue("Folio", folio.toString(), emphasized = true)
        keyValue("Fecha", formatEpochMillisToDateTime(createdAt))
        customerName?.trim()?.takeIf { it.isNotEmpty() }?.let { keyValue("Cliente", it) }
        lines += ReceiptLine("-".repeat(width))

        // 3. Items List
        items.forEach { item ->
            val hasWholesale = item.isWholesale || (item.originalUnitPrice > item.unitPrice + 0.001)
            val displayName = if (hasWholesale) {
                "${item.name.trim().ifEmpty { "Producto" }} (MAY)"
            } else {
                item.name.trim().ifEmpty { "Producto" }
            }

            wrap(displayName, width).forEach { nameLine ->
                lines += ReceiptLine(nameLine)
            }

            val quantity = formatQuantity(item.quantity, item.isWeightBased)
            val unitPriceFormatted = "$${item.unitPrice.toString().formatPrice()}"
            val detail = if (hasWholesale && item.originalUnitPrice > item.unitPrice + 0.001 && width >= 36) {
                "$quantity x $unitPriceFormatted (Reg. $${item.originalUnitPrice.toString().formatPrice()})"
            } else {
                "$quantity x $unitPriceFormatted"
            }
            val subtotal = "$${item.subtotal.toString().formatPrice()}"
            val detailWidth = (width - subtotal.length - 1).coerceAtLeast(1)
            val truncatedDetail = detail.take(detailWidth)
            val space = (width - truncatedDetail.length - subtotal.length).coerceAtLeast(1)
            lines += ReceiptLine(
                truncatedDetail + " ".repeat(space) + subtotal,
                ReceiptAlignment.LEFT
            )
        }

        lines += ReceiptLine("-".repeat(width))

        // 4. Products and Pieces count
        val totalProducts = items.size
        val totalPieces = items.sumOf { it.quantity }
        val formattedPieces = if (totalPieces % 1.0 == 0.0) {
            totalPieces.toInt().toString()
        } else {
            ((totalPieces * 1000.0).roundToInt() / 1000.0).toString()
        }
        twoColumn("Artículos: $totalProducts", "Piezas: $formattedPieces")

        // 5. Wholesale / Discount Summary (if any)
        val totalWithoutDiscount = items.sumOf { it.originalUnitPrice * it.quantity }
        val hasDiscount = items.any { item -> item.isWholesale || item.originalUnitPrice > item.unitPrice + 0.001 } ||
                (totalWithoutDiscount > total + 0.001)

        if (hasDiscount) {
            lines += ReceiptLine("-".repeat(width))
            twoColumn("Total sin desc:", money(totalWithoutDiscount))
            val savings = (totalWithoutDiscount - total).coerceAtLeast(0.0)
            if (savings > 0.001) {
                twoColumn("Ahorro:", money(savings))
            }
        }

        // 6. Total
        lines += ReceiptLine("-".repeat(width))
        twoColumn("TOTAL", totalText(total), emphasized = true)

        // 7. Payment breakdown
        keyValue("Pago", paymentMethod)
        if (paid > 0.0) keyValue("Recibido", money(paid))
        if (change > 0.0) keyValue("Cambio", money(change), emphasized = true)

        // 8. Footer
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
            fontSize = settings.fontSize.coerceIn(8, 32),
            feedLines = settings.feedLines.coerceIn(0, 10),
            openCashDrawer = settings.openCashDrawerOnReceipt
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
            if (quantity % 1.0 == 0.0) quantity.toInt().toString() else quantity.toString()
        }
    }

    private fun wrap(value: String, width: Int): List<String> {
        val text = value.trim()
        if (text.isEmpty()) return listOf("")
        if (text.length <= width) return listOf(text)
        return text.chunked(width)
    }
}
