package com.dnavarro.poskmp.domain.model

data class DailySalesMetric(
    val fecha: String,
    val diaLabel: String,
    val totalVentas: Double,
    val totalGanancia: Double,
    val transaccionesCount: Long
)
