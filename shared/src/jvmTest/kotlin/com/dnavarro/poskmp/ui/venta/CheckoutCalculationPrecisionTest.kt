package com.dnavarro.poskmp.ui.venta

import com.dnavarro.poskmp.db.Products
import com.dnavarro.poskmp.ui.CartItem
import com.dnavarro.poskmp.util.formatPrice
import kotlin.math.roundToLong
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CheckoutCalculationPrecisionTest {

    private fun createProduct(id: String, name: String, price: Double, wholesale: Double = 0.0): Products {
        return Products(
            id = id,
            codigos = "[]",
            nombre = name,
            precio = price,
            costo = 0.0,
            categoria = "Test",
            activo = 1L,
            por_peso = 0L,
            precio_mayoreo = wholesale,
            es_favorito = 0L,
            piezas = 100.0,
            precio_delivery = 0.0,
            created_at = 0L,
            updated_at = 0L,
            sync_state = "SYNCED"
        )
    }

    @Test
    fun testVentaUiStateTotalRoundsToTwoDecimals() {
        // Create items that would naturally produce floating-point precision noise in double addition
        val p1 = createProduct("1", "Product 1", 131.75)
        val p2 = createProduct("2", "Product 2", 131.76)
        val p3 = createProduct("3", "Product 3", 131.76)

        val cartItems = listOf(
            CartItem(product = p1, quantity = 1.0, originalPrice = 140.50),
            CartItem(product = p2, quantity = 1.0, originalPrice = 140.50),
            CartItem(product = p3, quantity = 1.0, originalPrice = 140.50)
        )

        val uiState = VentaUiState(
            cartItems = cartItems,
            roundTicketTotal = false
        )

        // Expected total is 395.27
        assertEquals(395.27, uiState.total, 0.000000001)
        assertEquals(395.27, uiState.rawTotal, 0.000000001)
    }

    @Test
    fun testExactPaymentAmountDoesNotTriggerInsufficientAmountError() {
        val total = 395.27
        val paymentText = "395.27"
        val paymentAmount = paymentText.toDoubleOrNull() ?: 0.0

        val totalCents = (total * 100.0).roundToLong()
        val paymentCents = (paymentAmount * 100.0).roundToLong()

        val missingCents = totalCents - paymentCents
        val changeCents = (paymentCents - totalCents).coerceAtLeast(0L)
        val change = changeCents / 100.0

        val isCheckoutValid = paymentCents >= totalCents || paymentText.isEmpty()

        assertTrue(isCheckoutValid, "Checkout should be valid when entering exact amount")
        assertEquals(0L, missingCents, "Missing cents should be 0")
        assertEquals(0.0, change, "Change should be 0.0")
        assertFalse(missingCents > 0L, "Should NOT trigger insufficient amount error")
    }

    @Test
    fun testFloatingPointEpsilonDoesNotBlockCheckout() {
        // Even if total double has floating-point noise like 395.27000000000004
        val totalWithNoise = 395.27 + 4e-14
        val paymentText = "395.27"
        val paymentAmount = paymentText.toDoubleOrNull() ?: 0.0

        val totalCents = (totalWithNoise * 100.0).roundToLong()
        val paymentCents = (paymentAmount * 100.0).roundToLong()

        val missingCents = totalCents - paymentCents

        assertEquals(39527L, totalCents)
        assertEquals(39527L, paymentCents)
        assertEquals(0L, missingCents)
        assertTrue(paymentCents >= totalCents)
        assertFalse(missingCents > 0L, "Must not display missing amount error of $0.00")
    }

    @Test
    fun testMixedPaymentFloatingPointPrecision() {
        val total = 395.27
        val totalCents = (total * 100.0).roundToLong()

        val mCash = 195.27
        val mCard = 200.00

        val mCashCents = (mCash * 100.0).roundToLong()
        val mCardCents = (mCard * 100.0).roundToLong()

        val totalReceivedMixtoCents = mCashCents + mCardCents
        val missingToCoverCents = (totalCents - totalReceivedMixtoCents).coerceAtLeast(0L)

        assertEquals(39527L, totalReceivedMixtoCents)
        assertEquals(0L, missingToCoverCents)
        assertFalse(missingToCoverCents > 0L)
    }
}
