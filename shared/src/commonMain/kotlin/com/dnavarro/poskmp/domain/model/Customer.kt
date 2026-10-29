package com.dnavarro.poskmp.domain.model

data class Customer(
    val id: String,
    val nombre: String,
    val telefono: String = "",
    val direccion: String = "",
    val notas: String = "",
    val limiteCredito: Double = 0.0,
    val siempreMayoreo: Boolean = false,
    val activo: Boolean = true,
    val saldoDeudor: Double = 0.0,
    val totalCompras: Long = 0L,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val syncState: String = "PENDING_INSERT",
    val tipoDescuento: CustomerDiscountType = if (siempreMayoreo) CustomerDiscountType.WHOLESALE else CustomerDiscountType.NONE,
    val porcentajeDescuento: Double = 0.0
) {
    val discountType: CustomerDiscountType get() = tipoDescuento
    val discountPercentage: Double get() = porcentajeDescuento

    val effectiveDiscountType: CustomerDiscountType
        get() = when {
            tipoDescuento != CustomerDiscountType.NONE -> tipoDescuento
            siempreMayoreo -> CustomerDiscountType.WHOLESALE
            else -> CustomerDiscountType.NONE
        }

    val hasSpecialPricing: Boolean
        get() = effectiveDiscountType != CustomerDiscountType.NONE
}
