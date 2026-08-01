package com.dnavarro.poskmp.ui.venta

import com.dnavarro.poskmp.db.Products

/**
 * UI state for the Venta screen.
 */
data class VentaUiState(
    val searchQuery: String = "",
    val activeProducts: List<Products> = emptyList(),
    val selectedCategory: String? = null,
    val isLoading: Boolean = false
)
