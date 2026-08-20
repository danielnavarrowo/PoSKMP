package com.dnavarro.poskmp.domain.model

data class Customer(
    val id: String,
    val nombre: String,
    val telefono: String = "",
    val direccion: String = "",
    val notas: String = "",
    val limiteCredito: Double = 0.0,
    val activo: Boolean = true,
    val saldoDeudor: Double = 0.0,
    val totalCompras: Long = 0L,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val syncState: String = "PENDING_INSERT"
)
