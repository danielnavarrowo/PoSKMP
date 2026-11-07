package com.dnavarro.poskmp.ui.venta

import com.dnavarro.poskmp.data.SaleRepository
import com.dnavarro.poskmp.db.Products
import com.dnavarro.poskmp.domain.model.Sale
import com.dnavarro.poskmp.domain.model.SaleItem
import com.dnavarro.poskmp.domain.receipt.ReceiptFormatter
import com.dnavarro.poskmp.domain.usecase.RecordSaleUseCase
import com.dnavarro.poskmp.ui.CartItem
import com.dnavarro.poskmp.util.formatPrice
import com.dnavarro.poskmp.util.formatQuantity
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BulkProductPricingPrecisionTest {

    private fun createBulkProduct(id: String, name: String, price: Double): Products {
        return Products(
            id = id,
            codigos = "[]",
            nombre = name,
            precio = price,
            costo = price * 0.7,
            categoria = "Granel",
            activo = 1L,
            por_peso = 1L,
            precio_mayoreo = 0.0,
            es_favorito = 0L,
            piezas = 100.0,
            precio_delivery = 0.0,
            created_at = 0L,
            updated_at = 0L,
            sync_state = "SYNCED"
        )
    }

    @Test
    fun testFormatQuantity_roundsToThreeDecimals() {
        // 200 pesos of bolsa at $76/kg = 2.631578947... kg -> must round to 2.632 (not truncate to 2.631)
        val bolsaQty = 200.0 / 76.0
        assertEquals("2.632", bolsaQty.formatQuantity(isWeight = true))

        // 5 pesos of bolsa at $76/kg = 0.065789... kg -> must round to 0.066
        val fivePesosQty = 5.0 / 76.0
        assertEquals("0.066", fivePesosQty.formatQuantity(isWeight = true))

        // 10 pesos at $45/kg = 0.222222... kg -> 0.222
        val tenPesosQty = 10.0 / 45.0
        assertEquals("0.222", tenPesosQty.formatQuantity(isWeight = true))

        // Integer weight formats with 3 trailing zeros
        assertEquals("2.000", 2.0.formatQuantity(isWeight = true))
        assertEquals("0.500", 0.5.formatQuantity(isWeight = true))

        // Unit items format without decimals
        assertEquals("1", 1.0.formatQuantity(isWeight = false))
        assertEquals("5", 5.0.formatQuantity(isWeight = false))
    }

    @Test
    fun testBulkProductSaleByPrice_producesExactSubtotalAndTotal() {
        val bolsa = createBulkProduct("prod-1", "BOLSA NATURAL 60X90", 76.0)
        val enteredPesos = 200.0
        val quantity = enteredPesos / bolsa.precio

        val item = CartItem(
            product = bolsa,
            quantity = quantity,
            originalPrice = bolsa.precio
        )

        val uiState = VentaUiState(
            cartItems = listOf(item),
            roundTicketTotal = false
        )

        // Raw total and total must match exact $200.00 (not $200.03)
        assertEquals(200.00, uiState.rawTotal, 0.0001)
        assertEquals(200.00, uiState.total, 0.0001)

        // Formatted line subtotal on ticket must be "$200.00"
        val lineSubtotal = (item.product.precio * item.quantity).toString().formatPrice()
        assertEquals("200.00", lineSubtotal)

        // Formatted quantity on ticket must be "2.632"
        val formattedQty = item.quantity.formatQuantity(item.product.por_peso == 1L)
        assertEquals("2.632", formattedQty)
    }

    @Test
    fun testBulkProductPricePresets_produceExactSubtotals() {
        val bolsa = createBulkProduct("prod-1", "BOLSA NATURAL 60X90", 76.0)
        val presets = listOf(5.0, 10.0, 15.0, 20.0, 50.0, 100.0, 200.0)

        for (preset in presets) {
            val qty = preset / bolsa.precio
            val item = CartItem(product = bolsa, quantity = qty, originalPrice = bolsa.precio)
            val uiState = VentaUiState(cartItems = listOf(item), roundTicketTotal = false)

            assertEquals(preset, uiState.rawTotal, 0.0001, "Raw total for preset \$$preset must be exact")
            assertEquals(preset, uiState.total, 0.0001, "Total for preset \$$preset must be exact")

            val subtotalText = (item.product.precio * item.quantity).toString().formatPrice()
            assertEquals(preset.formatPrice(), subtotalText, "Line subtotal formatted text must match preset")
        }
    }

    private class FakeSaleRepository : SaleRepository {
        var recordedSale: Sale? = null
        var recordedItems: List<SaleItem> = emptyList()
        private var nextFolio = 1001L

        override suspend fun recordSale(sale: Sale, items: List<SaleItem>): Long {
            recordedSale = sale
            recordedItems = items
            return 1L
        }
        override suspend fun getNextFolio(): Long = nextFolio++
        override suspend fun getSalesSummaryBetween(startTime: Long, endTime: Long, shiftId: String?): com.dnavarro.poskmp.domain.model.SalesSummary =
            com.dnavarro.poskmp.domain.model.SalesSummary(0.0, 0.0, 0.0, 0.0, 0.0, 0L, 0.0)
        override suspend fun getDeliveryComparisonBetween(startTime: Long, endTime: Long, shiftId: String?): com.dnavarro.poskmp.domain.model.DeliveryComparisonMetric =
            com.dnavarro.poskmp.domain.model.DeliveryComparisonMetric()
        override suspend fun getSoldProductsBetween(startTime: Long, endTime: Long, shiftId: String?): List<com.dnavarro.poskmp.domain.model.ProductSalesMetric> = emptyList()
        override suspend fun getTopSellingProductsBetween(startTime: Long, endTime: Long, limit: Long, shiftId: String?): List<com.dnavarro.poskmp.domain.model.ProductSalesMetric> = emptyList()
        override suspend fun getLeastSellingProductsBetween(startTime: Long, endTime: Long, limit: Long, shiftId: String?): List<com.dnavarro.poskmp.domain.model.ProductSalesMetric> = emptyList()
        override suspend fun getRecentSales(limit: Long, offset: Long): List<Sale> = emptyList()
        override suspend fun getSalesBetween(startTime: Long, endTime: Long, limit: Long, offset: Long, shiftId: String?): List<Sale> = emptyList()
        override suspend fun getPaymentMethodSalesBetween(startTime: Long, endTime: Long, shiftId: String?): List<com.dnavarro.poskmp.domain.model.PaymentMethodMetric> = emptyList()
        override suspend fun getCategorySalesBetween(startTime: Long, endTime: Long, shiftId: String?): List<com.dnavarro.poskmp.domain.model.CategorySalesMetric> = emptyList()
        override suspend fun getDailySalesBetween(startTime: Long, endTime: Long, shiftId: String?): List<com.dnavarro.poskmp.domain.model.DailySalesMetric> = emptyList()
        override suspend fun getSaleById(id: String): Sale? = recordedSale
        override suspend fun getItemsBySaleId(saleId: String): List<SaleItem> = recordedItems
        override suspend fun getTotalSalesCount(): Long = if (recordedSale != null) 1L else 0L
        override suspend fun cancelSale(saleId: String) {}
        override fun getLastSale(): kotlinx.coroutines.flow.Flow<Sale?> = kotlinx.coroutines.flow.flowOf(recordedSale)
    }

    @Test
    fun testRecordSaleUseCase_persistsExactSubtotalForBulkProductEnteredByPrice() = runBlocking {
        val fakeSaleRepo = FakeSaleRepository()
        val recordSaleUseCase = RecordSaleUseCase(saleRepository = fakeSaleRepo)

        val bolsa = createBulkProduct("prod-1", "BOLSA NATURAL 60X90", 76.0)
        val enteredPesos = 200.0
        val quantity = enteredPesos / bolsa.precio

        val cartItem = CartItem(
            product = bolsa,
            quantity = quantity,
            originalPrice = bolsa.precio
        )

        recordSaleUseCase(
            cartItems = listOf(cartItem),
            pagoCon = 200.0,
            cambio = 0.0,
            metodoPago = "EFECTIVO"
        )

        assertEquals(1, fakeSaleRepo.recordedItems.size)
        val saleItem = fakeSaleRepo.recordedItems.first()

        // Subtotal in sale item must be exactly 200.00
        assertEquals(200.00, saleItem.subtotal, 0.0001)

        // Sale total must be exactly 200.00
        assertEquals(200.00, fakeSaleRepo.recordedSale?.total ?: 0.0, 0.0001)
    }

    @Test
    fun testReceiptFormatter_formatsBulkProductReceiptLineAccurately() {
        val bolsa = createBulkProduct("prod-1", "BOLSA NATURAL 60X90", 76.0)
        val enteredPesos = 200.0
        val quantity = enteredPesos / bolsa.precio

        val receiptItem = com.dnavarro.poskmp.domain.model.ReceiptItem(
            name = bolsa.nombre,
            quantity = quantity,
            unitPrice = bolsa.precio,
            subtotal = 200.0,
            originalUnitPrice = bolsa.precio,
            isWeightBased = true
        )

        val receiptDoc = ReceiptFormatter.create(
            folio = 100L,
            createdAt = 1700000000000L,
            items = listOf(receiptItem),
            total = 200.0,
            paid = 200.0,
            change = 0.0,
            paymentMethod = "EFECTIVO",
            customerName = null,
            settings = com.dnavarro.poskmp.domain.model.ReceiptSettings(storeName = "Tienda")
        )

        val renderedLines = receiptDoc.lines.map { it.text }

        // Must display "2.632 kg BOLSA NATURAL 60X90" and "$200.00"
        val itemLine = renderedLines.find { it.contains("BOLSA NATURAL 60X90") }
        assertTrue(itemLine != null, "Receipt must contain product line")
        assertTrue(itemLine.contains("2.632 kg"), "Receipt line must display 2.632 kg")
        assertTrue(itemLine.contains("$200.00"), "Receipt line must display $200.00 subtotal")
    }
}
