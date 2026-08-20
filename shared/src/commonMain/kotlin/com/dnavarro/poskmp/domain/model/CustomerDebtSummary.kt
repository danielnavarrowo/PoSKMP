package com.dnavarro.poskmp.domain.model

data class CustomerDebtSummary(
    val totalClientes: Long = 0L,
    val totalDeudaAcumulada: Double = 0.0,
    val clientesConDeuda: Long = 0L
)
