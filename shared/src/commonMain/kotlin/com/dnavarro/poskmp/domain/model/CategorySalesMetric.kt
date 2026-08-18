package com.dnavarro.poskmp.domain.model

data class CategorySalesMetric(
    val categoria: String,
    val totalRecaudado: Double,
    val totalUnidades: Double,
    val porcentaje: Double = 0.0
)
