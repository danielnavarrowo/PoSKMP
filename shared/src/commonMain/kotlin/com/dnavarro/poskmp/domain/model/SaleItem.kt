package com.dnavarro.poskmp.domain.model

data class SaleItem(
    val id: String,
    val saleId: String,
    val productId: String?,
    val productNombre: String,
    val cantidad: Double,
    val precioUnitario: Double,
    val costoUnitario: Double,
    val subtotal: Double,
    val ganancia: Double,
    val esMayoreo: Boolean,
    val esDelivery: Boolean = false,
    val createdAt: Long
)
