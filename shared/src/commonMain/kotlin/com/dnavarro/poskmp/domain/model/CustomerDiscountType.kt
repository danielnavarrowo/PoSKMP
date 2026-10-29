package com.dnavarro.poskmp.domain.model

enum class CustomerDiscountType {
    NONE,
    WHOLESALE,
    COST_PLUS_PERCENTAGE,
    RETAIL_MINUS_PERCENTAGE;

    companion object {
        fun fromString(value: String?): CustomerDiscountType {
            if (value == null) return NONE
            return entries.firstOrNull { it.name.equals(value.trim(), ignoreCase = true) } ?: NONE
        }
    }
}
