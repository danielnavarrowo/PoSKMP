package com.dnavarro.poskmp.util

import kotlin.math.roundToLong

fun roundPrice(amount: Double): Double {
    if (amount <= 0.0) return 0.0
    val totalCents = (amount * 100.0).roundToLong()
    val integerPesos = totalCents / 100
    val cents = (totalCents % 100).toInt()
    return when {
        cents == 0 -> integerPesos.toDouble()
        cents in 1..50 -> integerPesos + 0.50
        else -> (integerPesos + 1).toDouble()
    }
}
