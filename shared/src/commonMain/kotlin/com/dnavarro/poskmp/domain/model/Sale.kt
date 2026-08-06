package com.dnavarro.poskmp.domain.model

data class Sale(
    val id: String,
    val folio: Long,
    val total: Double,
    val totalOriginal: Double,
    val totalCosto: Double,
    val ganancia: Double,
    val pagoCon: Double,
    val cambio: Double,
    val metodoPago: String = "EFECTIVO",
    val totalItems: Double,
    val createdAt: Long,
    val syncState: String = "PENDING_INSERT"
)
