package com.dnavarro.poskmp.domain.model

data class CashierShift(
    val id: String,
    val cashierId: String,
    val cashierName: String,
    val startTime: Long,
    val endTime: Long? = null,
    val initialCash: Double = 0.0,
    val finalCashExpected: Double? = null,
    val finalCashCounted: Double? = null,
    val difference: Double? = null,
    val notes: String? = null,
    val isClosed: Boolean = false
)
