package com.dnavarro.poskmp.data.source.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProductDto(
    val id: String,
    val codigos: String = "[]",
    val nombre: String,
    val precio: Double = 0.0,
    val costo: Double = 0.0,
    val categoria: String? = "Sin categoría",
    val activo: Boolean = true,
    @SerialName("por_peso")
    val porPeso: Boolean = false,
    @SerialName("precio_mayoreo")
    val precioMayoreo: Double = 0.0,
    @SerialName("es_favorito")
    val esFavorito: Boolean = false,
    val piezas: Double = 1.0,
    @SerialName("updated_at")
    val updatedAt: Long
)

@Serializable
data class CustomerDto(
    val id: String,
    val nombre: String,
    val telefono: String = "",
    val direccion: String = "",
    val notas: String = "",
    @SerialName("limite_credito")
    val limiteCredito: Double = 0.0,
    @SerialName("siempre_mayoreo")
    val siempreMayoreo: Boolean = false,
    val activo: Boolean = true,
    @SerialName("created_at")
    val createdAt: Long,
    @SerialName("updated_at")
    val updatedAt: Long
)

@Serializable
data class CustomerPaymentDto(
    val id: String,
    @SerialName("customer_id")
    val customerId: String,
    val monto: Double,
    @SerialName("metodo_pago")
    val metodoPago: String = "EFECTIVO",
    val notas: String = "",
    @SerialName("created_at")
    val createdAt: Long
)

@Serializable
data class SaleDto(
    val id: String,
    val folio: Long,
    val total: Double,
    @SerialName("total_original")
    val totalOriginal: Double,
    @SerialName("total_costo")
    val totalCosto: Double,
    val ganancia: Double,
    @SerialName("pago_con")
    val pagoCon: Double,
    val cambio: Double,
    @SerialName("metodo_pago")
    val metodoPago: String = "EFECTIVO",
    @SerialName("total_items")
    val totalItems: Double = 0.0,
    @SerialName("customer_id")
    val customerId: String? = null,
    @SerialName("created_at")
    val createdAt: Long
)

@Serializable
data class SaleItemDto(
    val id: String,
    @SerialName("sale_id")
    val saleId: String,
    @SerialName("product_id")
    val productId: String? = null,
    @SerialName("product_nombre")
    val productNombre: String,
    val cantidad: Double,
    @SerialName("precio_unitario")
    val precioUnitario: Double,
    @SerialName("costo_unitario")
    val costoUnitario: Double,
    val subtotal: Double,
    val ganancia: Double,
    @SerialName("es_mayoreo")
    val esMayoreo: Boolean = false,
    @SerialName("created_at")
    val createdAt: Long
)

@Serializable
data class StoreSettingsDto(
    val id: String = "default",
    @SerialName("store_name")
    val storeName: String = "",
    @SerialName("store_address")
    val storeAddress: String = "",
    @SerialName("store_phone")
    val storePhone: String = "",
    @SerialName("receipt_footer")
    val receiptFooter: String = "",
    @SerialName("default_retail_margin")
    val defaultRetailMargin: Double = 0.0,
    @SerialName("default_wholesale_margin")
    val defaultWholesaleMargin: Double = 0.0,
    @SerialName("is_rounding_enabled")
    val isRoundingEnabled: Boolean = false,
    @SerialName("round_retail_price")
    val roundRetailPrice: Boolean = false,
    @SerialName("round_wholesale_price")
    val roundWholesalePrice: Boolean = false,
    @SerialName("round_ticket_total")
    val roundTicketTotal: Boolean = false,
    @SerialName("disallow_card_payment_on_wholesale")
    val disallowCardPaymentOnWholesale: Boolean = false,
    @SerialName("updated_at")
    val updatedAt: Long
)

@Serializable
data class DeletedRecordDto(
    val id: String,
    @SerialName("entity_type")
    val entityType: String,
    @SerialName("deleted_at")
    val deletedAt: Long
)

