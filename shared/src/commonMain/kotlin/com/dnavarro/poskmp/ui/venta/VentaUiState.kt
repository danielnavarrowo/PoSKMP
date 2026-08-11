package com.dnavarro.poskmp.ui.venta

import com.dnavarro.poskmp.db.Products
import com.dnavarro.poskmp.ui.CartItem
import com.dnavarro.poskmp.util.currentTimeMillis
import com.dnavarro.poskmp.util.generateUUID

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
    val isLoading: Boolean = false
) {
    val total: Double
        get() = cartItems.sumOf { it.product.precio * it.quantity }
}
