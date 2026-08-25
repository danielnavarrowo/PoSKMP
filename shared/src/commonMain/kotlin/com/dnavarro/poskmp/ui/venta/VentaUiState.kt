package com.dnavarro.poskmp.ui.venta

import com.dnavarro.poskmp.db.Products
import com.dnavarro.poskmp.domain.model.Customer
import com.dnavarro.poskmp.domain.model.ReceiptDocument
import com.dnavarro.poskmp.domain.model.ReceiptSettings
import com.dnavarro.poskmp.ui.CartItem
import com.dnavarro.poskmp.util.currentTimeMillis
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
        get() = items.sumOf { it.quantity }
}

/**
 * UI state for the Venta screen.
 */
data class VentaUiState(
    val searchQuery: String = "",
    val activeProducts: List<Products> = emptyList(),
    val selectedCategory: String? = null,
    val cartItems: List<CartItem> = emptyList(),
    val heldTickets: List<HeldTicket> = emptyList(),
    val canUndo: Boolean = false,
    val defaultRetailMargin: Double = 0.0,
    val defaultWholesaleMargin: Double = 0.0,
    val isRoundingEnabled: Boolean = false,
    val roundRetailPrice: Boolean = false,
    val roundWholesalePrice: Boolean = false,
    val roundTicketTotal: Boolean = false,
    val disallowCardPaymentOnWholesale: Boolean = false,
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
    val receiptPrintSuccessful: Boolean = false,
    val activeShift: com.dnavarro.poskmp.domain.model.CashierShift? = null,
    val cashiers: List<com.dnavarro.poskmp.domain.model.Cashier> = emptyList(),
    val isOpeningShift: Boolean = false,
    val openShiftError: String? = null
) {
    val rawTotal: Double
        get() = cartItems.sumOf { it.product.precio * it.quantity }

    val total: Double
        get() = if (roundTicketTotal) roundPrice(rawTotal) else rawTotal
}
