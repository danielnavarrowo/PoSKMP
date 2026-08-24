package com.dnavarro.poskmp.domain.model

data class ShiftSummary(
    val shift: CashierShift,
    val totalVentas: Double = 0.0,
    val ventasEfectivo: Double = 0.0,
    val ventasTarjeta: Double = 0.0,
    val ventasTransferencia: Double = 0.0,
    val ventasCredito: Double = 0.0,
    val ventasMixto: Double = 0.0,
    val totalEntradas: Double = 0.0,
    val totalSalidas: Double = 0.0,
    val efectivoEsperado: Double = 0.0,
    val totalTransacciones: Long = 0L,
    val movements: List<CashMovement> = emptyList()
)
