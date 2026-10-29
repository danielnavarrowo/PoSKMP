package com.dnavarro.poskmp.domain.pricing

import com.dnavarro.poskmp.domain.model.Customer
import com.dnavarro.poskmp.domain.model.CustomerDiscountType
import com.dnavarro.poskmp.domain.model.CustomerPriceCalculator
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CustomerPriceCalculatorTest {

    // 1. COST_PLUS_PERCENTAGE: cost > 0, markup calculation, rounding to 2 decimals
    @Test
    fun testCostPlusPercentage_normalMarkupAndRounding() {
        // Simple markup
        val price1 = CustomerPriceCalculator.calculatePriceForType(
            discountType = CustomerDiscountType.COST_PLUS_PERCENTAGE,
            discountPercentage = 15.0,
            retailPrice = 20.0,
            costPrice = 10.0,
            wholesalePrice = 18.0
        )
        // 10.0 * 1.15 = 11.50
        assertEquals(11.50, price1)

        // Fractional calculation requiring rounding: 33.33 * 1.3333 = 44.438889 -> 44.44
        val price2 = CustomerPriceCalculator.calculatePriceForType(
            discountType = CustomerDiscountType.COST_PLUS_PERCENTAGE,
            discountPercentage = 33.33,
            retailPrice = 60.0,
            costPrice = 33.33,
            wholesalePrice = 50.0
        )
        assertEquals(44.44, price2)

        // Rounding half-up check: 10.05 * 1.10 = 11.055 -> 11.06
        val price3 = CustomerPriceCalculator.calculatePriceForType(
            discountType = CustomerDiscountType.COST_PLUS_PERCENTAGE,
            discountPercentage = 10.0,
            retailPrice = 20.0,
            costPrice = 10.05,
            wholesalePrice = 15.0
        )
        assertEquals(11.06, price3)

        // Zero markup: 50.0 * 1.0 = 50.00
        val priceZero = CustomerPriceCalculator.calculatePriceForType(
            discountType = CustomerDiscountType.COST_PLUS_PERCENTAGE,
            discountPercentage = 0.0,
            retailPrice = 80.0,
            costPrice = 50.0,
            wholesalePrice = 70.0
        )
        assertEquals(50.00, priceZero)
    }

    // 2. COST_PLUS_PERCENTAGE: cost <= 0 fallback to wholesale price (if wholesale > 0)
    @Test
    fun testCostPlusPercentage_costZeroOrNegative_fallbackToWholesale() {
        // Cost = 0.0, wholesale > 0 -> should use wholesale
        val priceZeroCost = CustomerPriceCalculator.calculatePriceForType(
            discountType = CustomerDiscountType.COST_PLUS_PERCENTAGE,
            discountPercentage = 25.0,
            retailPrice = 100.0,
            costPrice = 0.0,
            wholesalePrice = 85.0
        )
        assertEquals(85.00, priceZeroCost)

        // Cost < 0.0, wholesale > 0 -> should use wholesale
        val priceNegativeCost = CustomerPriceCalculator.calculatePriceForType(
            discountType = CustomerDiscountType.COST_PLUS_PERCENTAGE,
            discountPercentage = 25.0,
            retailPrice = 100.0,
            costPrice = -15.0,
            wholesalePrice = 85.0
        )
        assertEquals(85.00, priceNegativeCost)
    }

    // 3. COST_PLUS_PERCENTAGE: cost <= 0 fallback to retail price (if wholesale <= 0)
    @Test
    fun testCostPlusPercentage_costZeroOrNegative_fallbackToRetailWhenWholesaleZeroOrNegative() {
        // Cost = 0.0, wholesale = 0.0 -> retail
        val priceZeroWholesale = CustomerPriceCalculator.calculatePriceForType(
            discountType = CustomerDiscountType.COST_PLUS_PERCENTAGE,
            discountPercentage = 20.0,
            retailPrice = 45.0,
            costPrice = 0.0,
            wholesalePrice = 0.0
        )
        assertEquals(45.00, priceZeroWholesale)

        // Cost = 0.0, wholesale < 0.0 -> retail
        val priceNegativeWholesale = CustomerPriceCalculator.calculatePriceForType(
            discountType = CustomerDiscountType.COST_PLUS_PERCENTAGE,
            discountPercentage = 20.0,
            retailPrice = 45.0,
            costPrice = 0.0,
            wholesalePrice = -10.0
        )
        assertEquals(45.00, priceNegativeWholesale)

        // Cost < 0.0, wholesale <= 0.0 -> retail
        val priceBothNegative = CustomerPriceCalculator.calculatePriceForType(
            discountType = CustomerDiscountType.COST_PLUS_PERCENTAGE,
            discountPercentage = 20.0,
            retailPrice = 45.0,
            costPrice = -5.0,
            wholesalePrice = 0.0
        )
        assertEquals(45.00, priceBothNegative)
    }

    // 4. COST_PLUS_PERCENTAGE: negative cost fallback
    @Test
    fun testCostPlusPercentage_negativeCostFallback() {
        val priceWithWholesale = CustomerPriceCalculator.calculatePriceForType(
            discountType = CustomerDiscountType.COST_PLUS_PERCENTAGE,
            discountPercentage = 50.0,
            retailPrice = 120.0,
            costPrice = -0.01,
            wholesalePrice = 90.0
        )
        assertEquals(90.00, priceWithWholesale)

        val priceWithoutWholesale = CustomerPriceCalculator.calculatePriceForType(
            discountType = CustomerDiscountType.COST_PLUS_PERCENTAGE,
            discountPercentage = 50.0,
            retailPrice = 120.0,
            costPrice = -100.0,
            wholesalePrice = 0.0
        )
        assertEquals(120.00, priceWithoutWholesale)
    }

    // 5. COST_PLUS_PERCENTAGE: clamping to max(calculated, cost) and negative percentage handling
    @Test
    fun testCostPlusPercentage_clampingAndNegativePercentage() {
        // Negative percentage: should be coerced to 0% markup, resulting in price == cost
        val priceNegativePercentage = CustomerPriceCalculator.calculatePriceForType(
            discountType = CustomerDiscountType.COST_PLUS_PERCENTAGE,
            discountPercentage = -20.0,
            retailPrice = 100.0,
            costPrice = 60.0,
            wholesalePrice = 80.0
        )
        assertEquals(60.00, priceNegativePercentage)

        // Verification that calculated price is never below costPrice
        assertTrue(priceNegativePercentage >= 60.0)
    }

    // 6. RETAIL_MINUS_PERCENTAGE: normal discount
    @Test
    fun testRetailMinusPercentage_normalDiscount() {
        // 100.0 with 10% discount -> 90.00
        val price1 = CustomerPriceCalculator.calculatePriceForType(
            discountType = CustomerDiscountType.RETAIL_MINUS_PERCENTAGE,
            discountPercentage = 10.0,
            retailPrice = 100.0,
            costPrice = 50.0,
            wholesalePrice = 80.0
        )
        assertEquals(90.00, price1)

        // 85.50 with 15% discount -> 85.50 * 0.85 = 72.675 -> 72.68
        val price2 = CustomerPriceCalculator.calculatePriceForType(
            discountType = CustomerDiscountType.RETAIL_MINUS_PERCENTAGE,
            discountPercentage = 15.0,
            retailPrice = 85.50,
            costPrice = 40.0,
            wholesalePrice = 75.0
        )
        assertEquals(72.68, price2)

        // Zero percent discount -> retailPrice
        val priceZero = CustomerPriceCalculator.calculatePriceForType(
            discountType = CustomerDiscountType.RETAIL_MINUS_PERCENTAGE,
            discountPercentage = 0.0,
            retailPrice = 75.0,
            costPrice = 30.0,
            wholesalePrice = 60.0
        )
        assertEquals(75.00, priceZero)
    }

    // 7. RETAIL_MINUS_PERCENTAGE: clamping to cost when cost > 0 (discounted price must NEVER be lower than cost)
    @Test
    fun testRetailMinusPercentage_clampingToCostWhenCostPositive() {
        // retail = 100, cost = 50, discount = 60% -> raw = 40, clamped to cost = 50
        val price1 = CustomerPriceCalculator.calculatePriceForType(
            discountType = CustomerDiscountType.RETAIL_MINUS_PERCENTAGE,
            discountPercentage = 60.0,
            retailPrice = 100.0,
            costPrice = 50.0,
            wholesalePrice = 70.0
        )
        assertEquals(50.00, price1)

        // retail = 100, cost = 50, discount = 100% -> raw = 0, clamped to cost = 50
        val price2 = CustomerPriceCalculator.calculatePriceForType(
            discountType = CustomerDiscountType.RETAIL_MINUS_PERCENTAGE,
            discountPercentage = 100.0,
            retailPrice = 100.0,
            costPrice = 50.0,
            wholesalePrice = 70.0
        )
        assertEquals(50.00, price2)

        // retail = 100, cost = 50, discount = 150% -> raw = -50, clamped to cost = 50
        val price3 = CustomerPriceCalculator.calculatePriceForType(
            discountType = CustomerDiscountType.RETAIL_MINUS_PERCENTAGE,
            discountPercentage = 150.0,
            retailPrice = 100.0,
            costPrice = 50.0,
            wholesalePrice = 70.0
        )
        assertEquals(50.00, price3)

        // retail < cost already (loss-leader): retail = 40, cost = 50, discount = 10% -> raw = 36, clamped to 50
        val priceLossLeader = CustomerPriceCalculator.calculatePriceForType(
            discountType = CustomerDiscountType.RETAIL_MINUS_PERCENTAGE,
            discountPercentage = 10.0,
            retailPrice = 40.0,
            costPrice = 50.0,
            wholesalePrice = 35.0
        )
        assertEquals(50.00, priceLossLeader)
    }

    // 8. RETAIL_MINUS_PERCENTAGE: cost <= 0 clamping to max(calculated, 0.0)
    @Test
    fun testRetailMinusPercentage_costZeroOrNegative_clampingToZero() {
        // retail = 100, cost = 0.0, discount = 100% -> 0.00
        val priceZero = CustomerPriceCalculator.calculatePriceForType(
            discountType = CustomerDiscountType.RETAIL_MINUS_PERCENTAGE,
            discountPercentage = 100.0,
            retailPrice = 100.0,
            costPrice = 0.0,
            wholesalePrice = 80.0
        )
        assertEquals(0.00, priceZero)

        // retail = 100, cost = 0.0, discount = 120% -> raw = -20, clamped to 0.00
        val priceExcessDiscountZeroCost = CustomerPriceCalculator.calculatePriceForType(
            discountType = CustomerDiscountType.RETAIL_MINUS_PERCENTAGE,
            discountPercentage = 120.0,
            retailPrice = 100.0,
            costPrice = 0.0,
            wholesalePrice = 80.0
        )
        assertEquals(0.00, priceExcessDiscountZeroCost)

        // retail = 100, cost = -10.0, discount = 150% -> raw = -50, clamped to 0.00
        val priceNegativeCost = CustomerPriceCalculator.calculatePriceForType(
            discountType = CustomerDiscountType.RETAIL_MINUS_PERCENTAGE,
            discountPercentage = 150.0,
            retailPrice = 100.0,
            costPrice = -10.0,
            wholesalePrice = 80.0
        )
        assertEquals(0.00, priceNegativeCost)
    }

    // 9. WHOLESALE: wholesale price > 0 vs wholesale price <= 0 fallback to retail
    @Test
    fun testWholesale_positiveVsFallbackToRetail() {
        // wholesale > 0 -> wholesalePrice
        val priceWholesale = CustomerPriceCalculator.calculatePriceForType(
            discountType = CustomerDiscountType.WHOLESALE,
            discountPercentage = 0.0,
            retailPrice = 25.0,
            costPrice = 10.0,
            wholesalePrice = 18.0
        )
        assertEquals(18.00, priceWholesale)

        // wholesale == 0.0 -> retailPrice
        val priceZeroWholesale = CustomerPriceCalculator.calculatePriceForType(
            discountType = CustomerDiscountType.WHOLESALE,
            discountPercentage = 0.0,
            retailPrice = 25.0,
            costPrice = 10.0,
            wholesalePrice = 0.0
        )
        assertEquals(25.00, priceZeroWholesale)

        // wholesale < 0.0 -> retailPrice
        val priceNegativeWholesale = CustomerPriceCalculator.calculatePriceForType(
            discountType = CustomerDiscountType.WHOLESALE,
            discountPercentage = 0.0,
            retailPrice = 25.0,
            costPrice = 10.0,
            wholesalePrice = -5.0
        )
        assertEquals(25.00, priceNegativeWholesale)
    }

    // 10. NONE: always retail price
    @Test
    fun testNone_alwaysRetailPrice() {
        val priceNone = CustomerPriceCalculator.calculatePriceForType(
            discountType = CustomerDiscountType.NONE,
            discountPercentage = 50.0, // Should be ignored
            retailPrice = 30.0,
            costPrice = 10.0,
            wholesalePrice = 20.0
        )
        assertEquals(30.00, priceNone)

        // calculateEffectivePrice with null customer -> retail
        val priceNullCustomer = CustomerPriceCalculator.calculateEffectivePrice(
            customer = null,
            retailPrice = 30.0,
            costPrice = 10.0,
            wholesalePrice = 20.0
        )
        assertEquals(30.00, priceNullCustomer)
    }

    // 11. Customer model integration & backward compatibility
    @Test
    fun testCustomerModelIntegration_backwardCompatibility() {
        // Legacy customer with siempreMayoreo = true, tipoDescuento = NONE
        val legacyWholesaleCustomer = Customer(
            id = "c1",
            nombre = "Legacy Mayoreo",
            siempreMayoreo = true,
            tipoDescuento = CustomerDiscountType.NONE,
            porcentajeDescuento = 0.0
        )
        assertEquals(CustomerDiscountType.WHOLESALE, legacyWholesaleCustomer.effectiveDiscountType)
        assertTrue(legacyWholesaleCustomer.hasSpecialPricing)

        val priceLegacy = CustomerPriceCalculator.calculateEffectivePrice(
            customer = legacyWholesaleCustomer,
            retailPrice = 50.0,
            costPrice = 20.0,
            wholesalePrice = 35.0
        )
        assertEquals(35.00, priceLegacy)

        // Modern customer with Cost + 10%
        val costPlusCustomer = Customer(
            id = "c2",
            nombre = "Cost Plus",
            siempreMayoreo = false,
            tipoDescuento = CustomerDiscountType.COST_PLUS_PERCENTAGE,
            porcentajeDescuento = 10.0
        )
        assertEquals(CustomerDiscountType.COST_PLUS_PERCENTAGE, costPlusCustomer.effectiveDiscountType)
        val priceCostPlus = CustomerPriceCalculator.calculateEffectivePrice(
            customer = costPlusCustomer,
            retailPrice = 100.0,
            costPrice = 50.0,
            wholesalePrice = 75.0
        )
        assertEquals(55.00, priceCostPlus)

        // Customer with both siempreMayoreo = true AND tipoDescuento = RETAIL_MINUS_PERCENTAGE
        // tipoDescuento != NONE takes precedence
        val conflictCustomer = Customer(
            id = "c3",
            nombre = "Retail Minus Priority",
            siempreMayoreo = true,
            tipoDescuento = CustomerDiscountType.RETAIL_MINUS_PERCENTAGE,
            porcentajeDescuento = 20.0
        )
        assertEquals(CustomerDiscountType.RETAIL_MINUS_PERCENTAGE, conflictCustomer.effectiveDiscountType)
        val priceConflict = CustomerPriceCalculator.calculateEffectivePrice(
            customer = conflictCustomer,
            retailPrice = 100.0,
            costPrice = 50.0,
            wholesalePrice = 70.0
        )
        assertEquals(80.00, priceConflict)
    }

    // 12. Adversarial Property / Stress Harness
    @Test
    fun testAdversarialStressHarness() {
        val random = Random(42)

        for (i in 0 until 2000) {
            val retail = random.nextDouble(-100.0, 1000.0)
            val cost = random.nextDouble(-100.0, 1000.0)
            val wholesale = random.nextDouble(-100.0, 1000.0)
            val percentage = random.nextDouble(-50.0, 200.0)

            for (type in CustomerDiscountType.entries) {
                val effectivePrice = CustomerPriceCalculator.calculatePriceForType(
                    discountType = type,
                    discountPercentage = percentage,
                    retailPrice = retail,
                    costPrice = cost,
                    wholesalePrice = wholesale
                )

                // Property 1: Decimal precision is always at most 2 places
                val roundedTwice = CustomerPriceCalculator.roundPrice(effectivePrice)
                assertEquals(roundedTwice, effectivePrice, "Price should already be rounded to 2 decimals: $effectivePrice")

                // Property 2: Cost safety floor invariant
                when (type) {
                    CustomerDiscountType.COST_PLUS_PERCENTAGE -> {
                        if (cost > 0.0) {
                            // Rounded effective price must be >= rounded cost
                            val roundedCost = CustomerPriceCalculator.roundPrice(cost)
                            assertTrue(
                                effectivePrice >= roundedCost - 0.01,
                                "COST_PLUS_PERCENTAGE price $effectivePrice must be >= cost $cost"
                            )
                        }
                    }
                    CustomerDiscountType.RETAIL_MINUS_PERCENTAGE -> {
                        if (cost > 0.0) {
                            val roundedCost = CustomerPriceCalculator.roundPrice(cost)
                            assertTrue(
                                effectivePrice >= roundedCost - 0.01,
                                "RETAIL_MINUS_PERCENTAGE price $effectivePrice must be >= cost $cost"
                            )
                        } else {
                            assertTrue(
                                effectivePrice >= 0.0,
                                "RETAIL_MINUS_PERCENTAGE with cost <= 0 must be non-negative: $effectivePrice"
                            )
                        }
                    }
                    CustomerDiscountType.WHOLESALE -> {
                        if (wholesale > 0.0) {
                            assertEquals(CustomerPriceCalculator.roundPrice(wholesale), effectivePrice)
                        } else {
                            assertEquals(CustomerPriceCalculator.roundPrice(retail), effectivePrice)
                        }
                    }
                    CustomerDiscountType.NONE -> {
                        assertEquals(CustomerPriceCalculator.roundPrice(retail), effectivePrice)
                    }
                }
            }
        }
    }
}
