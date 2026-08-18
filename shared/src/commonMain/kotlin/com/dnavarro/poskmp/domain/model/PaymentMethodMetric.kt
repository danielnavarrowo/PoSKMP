package com.dnavarro.poskmp.domain.model

data class PaymentMethodMetric(
    val metodoPago: String,
    val totalRecaudado: Double,
    val transaccionesCount: Long,
    val porcentaje: Double = 0.0
)
