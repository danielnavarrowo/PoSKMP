package com.dnavarro.poskmp.domain.model

data class ProductSalesMetric(
    val productId: String?,
    val productNombre: String,
    val totalUnidades: Double,
    val totalRecaudado: Double,
    val gananciaGenerada: Double
)
