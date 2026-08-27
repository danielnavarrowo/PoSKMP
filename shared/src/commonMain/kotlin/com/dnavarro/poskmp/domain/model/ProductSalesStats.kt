package com.dnavarro.poskmp.domain.model

data class ProductSalesStats(
    val productId: String,
    val totalVentas: Double = 0.0,
    val ultimaVenta: Long? = null
)
