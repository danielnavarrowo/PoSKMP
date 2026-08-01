package com.dnavarro.poskmp.ui.venta

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dnavarro.poskmp.data.ProductRepository
import com.dnavarro.poskmp.db.Products
import com.dnavarro.poskmp.util.currentTimeMillis
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.dnavarro.poskmp.domain.usecase.FindProductByBarcodeUseCase
import com.dnavarro.poskmp.domain.usecase.GetProductsUseCase

/**
 * ViewModel for Venta screen managing product flow, barcode search, and cart updates.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class VentaViewModel(
    private val repository: ProductRepository,
    private val getProductsUseCase: GetProductsUseCase = GetProductsUseCase(repository),
    private val findProductByBarcodeUseCase: FindProductByBarcodeUseCase = FindProductByBarcodeUseCase(repository)
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategory = MutableStateFlow<String?>(null)

    private val _productsFlow = _searchQuery.flatMapLatest { query ->
        getProductsUseCase(query = query, activeOnly = true)
    }

    val uiState: StateFlow<VentaUiState> = combine(
        _searchQuery,
        _productsFlow,
        _selectedCategory
    ) { query, products, category ->
        VentaUiState(
            searchQuery = query,
            activeProducts = products,
            selectedCategory = category
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = VentaUiState()
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onCategorySelected(category: String?) {
        _selectedCategory.value = category
    }

    fun toggleProductFavorite(product: Products) {
        viewModelScope.launch {
            val updated = product.copy(
                es_favorito = if (product.es_favorito == 1L) 0L else 1L,
                updated_at = currentTimeMillis(),
                sync_state = "PENDING_UPDATE"
            )
            repository.updateProduct(updated)
        }
    }

    fun updateProduct(product: Products) {
        viewModelScope.launch {
            repository.updateProduct(product)
        }
    }

    suspend fun findProductByBarcode(barcode: String): Products? {
        return findProductByBarcodeUseCase(barcode)
    }
}
