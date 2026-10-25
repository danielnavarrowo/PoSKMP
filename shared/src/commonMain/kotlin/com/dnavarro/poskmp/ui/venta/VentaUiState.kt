package com.dnavarro.poskmp.ui.venta

import com.dnavarro.poskmp.db.Products
import com.dnavarro.poskmp.domain.model.Customer
import com.dnavarro.poskmp.domain.model.ReceiptDocument
import com.dnavarro.poskmp.domain.model.ReceiptSettings
import com.dnavarro.poskmp.ui.CartItem
import com.dnavarro.poskmp.ui.ProductSortField
import com.dnavarro.poskmp.ui.ProductSortOrder
import com.dnavarro.poskmp.util.currentTimeMillis
import com.dnavarro.poskmp.util.formatBarcodesForDisplay
import com.dnavarro.poskmp.util.generateUUID
import com.dnavarro.poskmp.util.roundPrice

data class HeldTicket(
    val id: String = generateUUID(),
    val items: List<CartItem>,
    val timestamp: Long = currentTimeMillis()
) {
    val total: Double
        get() = items.sumOf { it.product.precio * it.quantity }
    val totalItemsCount: Double
        get() = items.sumOf { if (it.product.por_peso == 1L) 1.0 else it.quantity }
}

/**
 * UI state for the Venta screen.
 */
data class VentaUiState(
    val searchQuery: String = "",
    val activeProducts: List<Products> = emptyList(),
    val displayedCatalogProducts: List<Products> = emptyList(),
    val catalogSortField: ProductSortField = ProductSortField.NOMBRE,
    val catalogSortOrder: ProductSortOrder = ProductSortOrder.ASC,
    val allActiveProducts: List<Products> = emptyList(),
    val availableCategories: List<String> = emptyList(),
    val selectedCategory: String? = null,
    val cartItems: List<CartItem> = emptyList(),
    val heldTickets: List<HeldTicket> = emptyList(),
    val canUndo: Boolean = false,
    val defaultRetailMargin: Double = 0.0,
    val defaultWholesaleMargin: Double = 0.0,
    val defaultDeliveryMargin: Double = 0.0,
    val isRoundingEnabled: Boolean = false,
    val roundProductPrices: Boolean = false,
    val roundTicketTotal: Boolean = false,
    val disallowCardPaymentOnWholesale: Boolean = false,
    val prioritizeDeliveryPrice: Boolean = false,
    val autoWholesaleByQuantity: Boolean = false,
    val autoWholesaleQuantityThreshold: Int = 3,
    val autoWholesaleByTicketTotal: Boolean = false,
    val autoWholesaleTicketTotalThreshold: Double = 0.0,
    val autoLookupBarcodeProducts: Boolean = true,
    val geminiGroundingEnabled: Boolean = false,
    val geminiApiKey: String = "",
    val googleSearchEngineId: String = "",
    val googleSearchApiKey: String = "",
    val useProductTableInCatalog: Boolean = false,
    val swapVentaLayoutOrder: Boolean = false,
    val beepOnBarcodeScan: Boolean = false,
    val ventaSplitRatio: Float = 0.6f,
    val isLoading: Boolean = false,
    val customers: List<Customer> = emptyList(),
    val filteredCustomers: List<Customer> = emptyList(),
    val selectedCustomer: Customer? = null,
    val customerSearchQuery: String = "",
    val showCustomerDialog: Boolean = false,
    val isSyncing: Boolean = false,
    val receiptSettings: ReceiptSettings = ReceiptSettings(),
    val lastReceipt: ReceiptDocument? = null,
    val isPrintingReceipt: Boolean = false,
    val receiptPrintError: Boolean = false,
    val receiptPrintErrorMessage: String? = null,
    val receiptPrintSuccessful: Boolean = false,
    val activeShift: com.dnavarro.poskmp.domain.model.CashierShift? = null,
    val cashiers: List<com.dnavarro.poskmp.domain.model.Cashier> = emptyList(),
    val isOpeningShift: Boolean = false,
    val openShiftError: String? = null,
    val showInflowDialog: Boolean = false,
    val showOutflowDialog: Boolean = false,
    val isRecordingMovement: Boolean = false,
    val shiftActionError: String? = null,
    val shiftActionSuccess: String? = null,
    val activeShiftMovements: List<com.dnavarro.poskmp.domain.model.CashMovement> = emptyList()
) {
    val rawTotal: Double
        get() = cartItems.sumOf { it.product.precio * it.quantity }

    val total: Double
        get() = if (roundTicketTotal) roundPrice(rawTotal) else rawTotal
}

fun computeDisplayedCatalogProducts(
    productsList: List<Products>,
    sortField: ProductSortField,
    sortOrder: ProductSortOrder
): List<Products> {
    if (productsList.isEmpty()) return emptyList()
    val formattedBarcodesById = if (sortField == ProductSortField.CODIGO) {
        productsList.associate { it.id to it.formatBarcodesForDisplay(emptyFallback = "") }
    } else {
        emptyMap()
    }
    return productsList.sortedWith { p1, p2 ->
        val f1 = p1.es_favorito == 1L
        val f2 = p2.es_favorito == 1L
        if (f1 != f2) return@sortedWith if (f1) -1 else 1

        val primaryComp = when (sortField) {
            ProductSortField.NOMBRE -> p1.nombre.compareTo(p2.nombre, ignoreCase = true)
            ProductSortField.CODIGO -> {
                val c1 = formattedBarcodesById[p1.id] ?: ""
                val c2 = formattedBarcodesById[p2.id] ?: ""
                c1.compareTo(c2, ignoreCase = true)
            }
            ProductSortField.CATEGORIA -> (p1.categoria ?: "")
                .compareTo(p2.categoria ?: "", ignoreCase = true)
            ProductSortField.PIEZAS -> p1.piezas.compareTo(p2.piezas)
            ProductSortField.PRECIO -> p1.precio.compareTo(p2.precio)
            ProductSortField.COSTO -> p1.costo.compareTo(p2.costo)
            ProductSortField.MAYOREO -> p1.precio_mayoreo.compareTo(p2.precio_mayoreo)
            ProductSortField.DOMICILIO -> p1.precio_delivery.compareTo(p2.precio_delivery)
            ProductSortField.FECHA_CREACION -> {
                val c1 = if (p1.created_at > 0L) p1.created_at else p1.updated_at
                val c2 = if (p2.created_at > 0L) p2.created_at else p2.updated_at
                c1.compareTo(c2)
            }
            ProductSortField.FECHA_ACTUALIZACION -> p1.updated_at.compareTo(p2.updated_at)
            else -> p1.nombre.compareTo(p2.nombre, ignoreCase = true)
        }

        if (sortOrder == ProductSortOrder.ASC) primaryComp else -primaryComp
    }.take(50)
}
