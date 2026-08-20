package com.dnavarro.poskmp.domain.model

enum class AccountStatementType {
    CARGO_CREDITO,
    ABONO
}

data class AccountStatementItem(
    val id: String,
    val fecha: Long,
    val tipo: AccountStatementType,
    val folio: Long? = null,
    val monto: Double,
    val saldoResultante: Double = 0.0,
    val metodoPago: String? = null,
    val descripcion: String = "",
    val notas: String = ""
)
