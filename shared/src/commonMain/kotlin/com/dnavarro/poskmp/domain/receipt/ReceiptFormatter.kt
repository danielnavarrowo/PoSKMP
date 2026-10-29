package com.dnavarro.poskmp.domain.receipt

import com.dnavarro.poskmp.domain.model.ReceiptAlignment
import com.dnavarro.poskmp.domain.model.ReceiptDocument
import com.dnavarro.poskmp.domain.model.ReceiptItem
import com.dnavarro.poskmp.domain.model.ReceiptLine
import com.dnavarro.poskmp.domain.model.ReceiptSettings
import com.dnavarro.poskmp.domain.model.Sale
import com.dnavarro.poskmp.domain.model.SaleItem
import com.dnavarro.poskmp.util.formatEpochMillisToDateTime
import com.dnavarro.poskmp.util.formatPrice
import kotlin.math.roundToInt

object ReceiptFormatter {
    fun createFromSale(
        sale: Sale,
        items: List<SaleItem>,
        customerName: String? = null,
        settings: ReceiptSettings
    ): ReceiptDocument {
        val receiptItems = items.map { item ->
            ReceiptItem(
                name = item.productNombre,
                quantity = item.cantidad,
                unitPrice = item.precioUnitario,
                subtotal = item.subtotal,
                isWeightBased = item.cantidad % 1.0 != 0.0,
                originalUnitPrice = item.precioUnitario,
                isWholesale = item.esMayoreo,
                isDelivery = item.esDelivery
            )
        }
        return create(
            folio = sale.folio,
            createdAt = sale.createdAt,
            items = receiptItems,
            total = sale.total,
            paid = sale.pagoCon,
            change = sale.cambio,
            paymentMethod = sale.metodoPago,
            customerName = customerName,
            settings = settings,
            overrideTotalPieces = sale.totalItems,
            overrideTotalOriginal = sale.totalOriginal,
            montoEfectivo = sale.montoEfectivo,
            montoTarjeta = sale.montoTarjeta,
            montoTransferencia = sale.montoTransferencia,
            montoCredito = sale.montoCredito
        )
    }

    fun create(
        folio: Long,
        createdAt: Long,
        items: List<ReceiptItem>,
        total: Double,
        paid: Double,
        change: Double,
        paymentMethod: String,
        customerName: String?,
        settings: ReceiptSettings,
        overrideTotalPieces: Double? = null,
        overrideTotalOriginal: Double? = null,
        montoEfectivo: Double = 0.0,
        montoTarjeta: Double = 0.0,
        montoTransferencia: Double = 0.0,
        montoCredito: Double = 0.0
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
                text = left + " ".repeat(space) + right,
                alignment = ReceiptAlignment.LEFT,
                emphasized = emphasized,
                leftText = left,
                rightText = right
            )
        }

        fun separator() {
            lines += ReceiptLine(
                text = "-".repeat(width),
                alignment = ReceiptAlignment.LEFT,
                isSeparator = true
            )
        }

        // 1. Store Header
        settings.storeName.trim().takeIf { it.isNotEmpty() }?.let { centered(it, emphasized = true) }
        settings.storeAddress.trim().takeIf { it.isNotEmpty() }?.let { centered(it) }
        settings.storePhone.trim().takeIf { it.isNotEmpty() }?.let { centered("Tel. $it") }
        if (lines.isNotEmpty()) lines += ReceiptLine("")

        // 2. Receipt metadata
        centered("TICKET DE COMPRA", emphasized = true)
        val folioDisplay = if (settings.terminalPrefix.isNotBlank()) "${settings.terminalPrefix}-$folio" else folio.toString()
        keyValue("Folio", folioDisplay, emphasized = true)
        keyValue("Fecha", formatEpochMillisToDateTime(createdAt))
        customerName?.trim()?.takeIf { it.isNotEmpty() }?.let { keyValue("Cliente", it) }
        separator()

        // 3. Items List
        items.forEach { item ->
            val displayName = item.name.trim().ifEmpty { "Producto" }
            val quantity = formatQuantity(item.quantity, item.isWeightBased)
            val fullLeftText = "$quantity $displayName"
            val priceText = "$${item.subtotal.toString().formatPrice()}"

            val maxLeftWidth = (width - priceText.length - 1).coerceAtLeast(1)
            val leftText = fullLeftText.take(maxLeftWidth)
            val space = (width - leftText.length - priceText.length).coerceAtLeast(1)

            lines += ReceiptLine(
                text = leftText + " ".repeat(space) + priceText,
                alignment = ReceiptAlignment.LEFT,
                leftText = fullLeftText,
                rightText = priceText
            )

            // Show unit price detail when quantity != 1 or weight-based or when discounted
            if (item.quantity != 1.0 || item.isWeightBased || item.originalUnitPrice > item.unitPrice + 0.001) {
                val unitPriceFormatted = "$${item.unitPrice.toString().formatPrice()}"
                val detailText = if (item.originalUnitPrice > item.unitPrice + 0.001) {
                    "  ($unitPriceFormatted c/u - desc.)"
                } else {
                    "  ($unitPriceFormatted c/u)"
                }
                lines += ReceiptLine(text = detailText, alignment = ReceiptAlignment.LEFT)
            }
        }

        separator()

        // 4. Products and Pieces count
        val totalProducts = items.size
        val totalPieces = overrideTotalPieces ?: items.sumOf { if (it.isWeightBased) 1.0 else it.quantity }
        val formattedPieces = if (totalPieces % 1.0 == 0.0) {
            totalPieces.toInt().toString()
        } else {
            ((totalPieces * 1000.0).roundToInt() / 1000.0).toString()
        }
        twoColumn("Artículos: $totalProducts", "Piezas: $formattedPieces")

        // 5. Wholesale / Discount Summary (if any)
        val totalWithoutDiscount = overrideTotalOriginal ?: items.sumOf { it.originalUnitPrice * it.quantity }
        val hasDiscount = items.any { item -> item.isWholesale || item.originalUnitPrice > item.unitPrice + 0.001 } ||
                (totalWithoutDiscount > total + 0.001)

        if (hasDiscount) {
            separator()
            twoColumn("Total sin desc:", money(totalWithoutDiscount))
            val savings = (totalWithoutDiscount - total).coerceAtLeast(0.0)
            if (savings > 0.001) {
                twoColumn("Ahorro:", money(savings))
            }
        }

        // 6. Total
        separator()
        twoColumn("TOTAL", totalText(total), emphasized = true)

        // 7. Payment breakdown
        twoColumn("Pago", paymentMethod)
        if (paymentMethod == "MIXTO") {
            if (montoEfectivo > 0.0) twoColumn("  Efectivo", money(montoEfectivo))
            if (montoTarjeta > 0.0) twoColumn("  Tarjeta", money(montoTarjeta))
            if (montoTransferencia > 0.0) twoColumn("  Transferencia", money(montoTransferencia))
            if (montoCredito > 0.0) twoColumn("  Crédito", money(montoCredito))
        }
        if (paid > 0.0) twoColumn("Recibido", money(paid))
        if (change > 0.0) twoColumn("Cambio", money(change), emphasized = true)

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
            fontFamily = settings.fontFamily,
            fontSize = settings.fontSize.coerceIn(8, 32),
            feedLines = settings.feedLines.coerceIn(0, 10),
            openCashDrawer = settings.openCashDrawerOnCashSale
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

        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = StringBuilder()

        for (word in words) {
            if (word.isEmpty()) continue
            if (currentLine.isEmpty()) {
                if (word.length <= width) {
                    currentLine.append(word)
                } else {
                    word.chunked(width).forEach { chunk ->
                        if (currentLine.isEmpty()) {
                            currentLine.append(chunk)
                        } else {
                            lines.add(currentLine.toString())
                            currentLine = StringBuilder(chunk)
                        }
                    }
                }
            } else if (currentLine.length + 1 + word.length <= width) {
                currentLine.append(" ").append(word)
            } else {
                lines.add(currentLine.toString())
                if (word.length <= width) {
                    currentLine = StringBuilder(word)
                } else {
                    currentLine = StringBuilder()
                    word.chunked(width).forEach { chunk ->
                        if (currentLine.isEmpty()) {
                            currentLine.append(chunk)
                        } else {
                            lines.add(currentLine.toString())
                            currentLine = StringBuilder(chunk)
                        }
                    }
                }
            }
        }
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine.toString())
        }
        return lines
    }
}
