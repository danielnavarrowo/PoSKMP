package com.dnavarro.poskmp.domain.model

data class CashMovement(
    val id: String,
    val shiftId: String,
    val cashierId: String,
    val tipo: CashMovementType,
    val monto: Double,
    val motivo: String,
    val createdAt: Long
)
