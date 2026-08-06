package com.dnavarro.poskmp.domain.model

data class SalesSummary(
    val totalVentas: Double,
    val totalSinDescuento: Double,
    val totalCosto: Double,
    val totalGanancia: Double,
    val porcentajeGanancia: Double,
    val totalTicketCount: Long,
    val promedioTicket: Double
)
