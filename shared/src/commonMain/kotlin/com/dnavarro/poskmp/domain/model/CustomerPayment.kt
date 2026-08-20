package com.dnavarro.poskmp.domain.model

data class CustomerPayment(
    val id: String,
    val customerId: String,
    val monto: Double,
    val metodoPago: String = "EFECTIVO",
    val notas: String = "",
    val createdAt: Long = 0L,
    val syncState: String = "PENDING_INSERT"
)
