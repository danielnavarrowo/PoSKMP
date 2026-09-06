package com.dnavarro.poskmp.domain.model

data class DeliveryComparisonMetric(
    val localVentas: Double = 0.0,
    val localGanancia: Double = 0.0,
    val localTickets: Long = 0L,
    val localPromedio: Double = 0.0,
    val deliveryVentas: Double = 0.0,
    val deliveryGanancia: Double = 0.0,
    val deliveryTickets: Long = 0L,
    val deliveryPromedio: Double = 0.0
) {
    val totalVentas: Double get() = localVentas + deliveryVentas
    val totalTickets: Long get() = localTickets + deliveryTickets
    val localPorcentaje: Double get() = if (totalVentas > 0) (localVentas / totalVentas) * 100.0 else 0.0
    val deliveryPorcentaje: Double get() = if (totalVentas > 0) (deliveryVentas / totalVentas) * 100.0 else 0.0
    val hasData: Boolean get() = totalTickets > 0L
}
