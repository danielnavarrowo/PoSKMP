package com.dnavarro.poskmp.domain.receipt

import com.dnavarro.poskmp.domain.model.PrinterType
import com.dnavarro.poskmp.domain.model.ReceiptFont
import com.dnavarro.poskmp.domain.model.ReceiptItem
import com.dnavarro.poskmp.domain.model.ReceiptSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReceiptFormatterTest {
    @Test
    fun createsReceiptWithStorePurchaseAndPaymentDetails() {
        val receipt = ReceiptFormatter.create(
            folio = 42L,
            createdAt = 0L,
            items = listOf(
                ReceiptItem(
                    name = "Café de olla",
                    quantity = 2.0,
                    unitPrice = 18.5,
                    subtotal = 37.0
                )
            ),
            total = 37.0,
            paid = 50.0,
            change = 13.0,
            paymentMethod = "EFECTIVO",
            customerName = "María López",
            settings = ReceiptSettings(
                storeName = "La Tienda",
                storeAddress = "Calle Principal 10",
                storePhone = "5551234567",
                footerMessage = "Gracias por tu compra"
            )
        )

        val text = receipt.lines.joinToString("\n") { it.text }
        assertTrue(text.contains("La Tienda"))
        assertTrue(text.contains("Café de olla"))
        assertTrue(text.contains("Folio: 42"))
        assertTrue(text.contains("María López"))
        assertTrue(text.contains("$37.00"))
        assertTrue(text.contains("EFECTIVO"))
        assertTrue(text.contains("Gracias por tu compra"))
    }

    @Test
    fun thermalReceiptKeepsLinesWithinConfiguredWidthAndFeedsRequestedLines() {
        val receipt = ReceiptFormatter.create(
            folio = 7L,
            createdAt = 0L,
            items = listOf(
                ReceiptItem(
                    name = "Producto con un nombre suficientemente largo para envolverlo",
                    quantity = 1.0,
                    unitPrice = 10.0,
                    subtotal = 10.0
                )
            ),
            total = 10.0,
            paid = 10.0,
            change = 0.0,
            paymentMethod = "TARJETA",
            customerName = null,
            settings = ReceiptSettings(
                printerType = PrinterType.THERMAL_80MM,
                font = ReceiptFont.SANS_SERIF,
                printerId = "desktop-printer",
                fontFamily = "Arial",
                fontSize = 16,
                feedLines = 4
            )
        )

        assertEquals(PrinterType.THERMAL_80MM, receipt.printerType)
        assertEquals("desktop-printer", receipt.printerId)
        assertEquals(ReceiptFont.SANS_SERIF, receipt.font)
        assertEquals("Arial", receipt.fontFamily)
        assertEquals(16, receipt.fontSize)
        assertTrue(receipt.lines.all { it.text.length <= PrinterType.THERMAL_80MM.charactersPerLine })
        assertEquals(4, receipt.lines.takeLast(4).count { it.text.isEmpty() })
    }
}
