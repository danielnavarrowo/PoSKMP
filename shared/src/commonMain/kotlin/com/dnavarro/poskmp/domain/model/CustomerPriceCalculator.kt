package com.dnavarro.poskmp.domain.model

import kotlin.math.round

object CustomerPriceCalculator {

    /**
     * Rounds a monetary amount to 2 decimal places.
     */
    fun roundPrice(value: Double): Double {
        return round(value * 100.0) / 100.0
    }

    /**
     * Calculates the unit price for a customer given standard retail price, wholesale price, and product cost.
     * Enforces all safety rules from R1:
     * - COST_PLUS_PERCENTAGE: cost * (1 + % / 100). If cost <= 0, falls back to wholesale (if > 0) or retail.
     * - Clamps calculated price to never be below cost when cost > 0.
     * - RETAIL_MINUS_PERCENTAGE: retail * (1 - % / 100). Never below cost when cost > 0.
     * - Applies 2-decimal currency rounding to the final price.
     */
    fun calculateEffectivePrice(
        customer: Customer?,
        retailPrice: Double,
        costPrice: Double,
        wholesalePrice: Double
    ): Double {
        if (customer == null) {
            return roundPrice(retailPrice)
        }
        return calculatePriceForType(
            discountType = customer.effectiveDiscountType,
            discountPercentage = customer.porcentajeDescuento,
            retailPrice = retailPrice,
            costPrice = costPrice,
            wholesalePrice = wholesalePrice
        )
    }

    /**
     * Calculates the unit price directly given a discount type and percentage.
     */
    fun calculatePriceForType(
        discountType: CustomerDiscountType,
        discountPercentage: Double,
        retailPrice: Double,
        costPrice: Double,
        wholesalePrice: Double
    ): Double {
        val calculated = when (discountType) {
            CustomerDiscountType.NONE -> retailPrice

            CustomerDiscountType.WHOLESALE -> {
                if (wholesalePrice > 0.0) wholesalePrice else retailPrice
            }

            CustomerDiscountType.COST_PLUS_PERCENTAGE -> {
                val percentage = discountPercentage.coerceAtLeast(0.0)
                if (costPrice <= 0.0) {
                    if (wholesalePrice > 0.0) wholesalePrice else retailPrice
                } else {
                    val rawPrice = costPrice * (1.0 + (percentage / 100.0))
                    maxOf(rawPrice, costPrice)
                }
            }

            CustomerDiscountType.RETAIL_MINUS_PERCENTAGE -> {
                val percentage = discountPercentage.coerceAtLeast(0.0)
                val rawPrice = retailPrice * (1.0 - (percentage / 100.0))
                if (costPrice > 0.0) {
                    maxOf(rawPrice, costPrice)
                } else {
                    maxOf(rawPrice, 0.0)
                }
            }
        }
        return roundPrice(calculated)
    }
}
