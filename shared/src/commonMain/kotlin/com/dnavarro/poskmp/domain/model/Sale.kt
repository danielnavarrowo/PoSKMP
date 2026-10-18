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
    val customerId: String? = null,
    val createdAt: Long,
    val syncState: String = "PENDING_INSERT",
    val shiftId: String? = null,
    val cashierId: String? = null,
    val cashierName: String? = null,
    val estado: String = "COMPLETADA",
    val esForanea: Boolean = false,
    val montoEfectivo: Double = 0.0,
    val montoTarjeta: Double = 0.0,
    val montoTransferencia: Double = 0.0,
    val montoCredito: Double = 0.0
) {
    val isCancelled: Boolean get() = estado == "CANCELADA"
}
