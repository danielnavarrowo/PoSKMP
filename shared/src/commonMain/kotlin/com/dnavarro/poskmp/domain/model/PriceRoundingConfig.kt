package com.dnavarro.poskmp.domain.model

/**
 * Independent rounding switches for each product price calculated from a cost and a margin.
 * (The ticket-total rounding is handled separately at checkout.)
 */
data class PriceRoundingConfig(
    val retail: Boolean = false,
    val wholesale: Boolean = false,
    val delivery: Boolean = false
)
