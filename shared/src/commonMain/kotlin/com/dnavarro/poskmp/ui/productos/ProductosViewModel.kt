package com.dnavarro.poskmp.ui.productos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dnavarro.poskmp.data.ProductRepository
import com.dnavarro.poskmp.db.Products
import com.dnavarro.poskmp.ui.BulkProductModification
import com.dnavarro.poskmp.ui.ProductSortField
import com.dnavarro.poskmp.ui.ProductSortOrder
import com.dnavarro.poskmp.ui.applyBulkProductModification
import com.dnavarro.poskmp.util.currentTimeMillis
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private data class DisplayState(
    val sortField: ProductSortField = ProductSortField.NOMBRE,
    val sortOrder: ProductSortOrder = ProductSortOrder.ASC,
    val showProductDialogFor: Products? = null,
    val showImportDialog: Boolean = false,
    val showBulkModificationDialog: Boolean = false,
    val selectedProductIds: Set<String> = emptySet()
)

/**
 * ViewModel for Productos screen, hosting state and handling UI events according to Google UI Layer architecture.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ProductosViewModel(
    private val repository: ProductRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _displayState = MutableStateFlow(DisplayState())

    private val _productsFlow = _searchQuery.flatMapLatest { query ->
        if (query.isBlank()) {
            repository.getAllProducts()
        } else {
            repository.searchProducts(query)
        }
    }

    val uiState: StateFlow<ProductosUiState> = combine(
        _searchQuery,
        _productsFlow,
        _displayState
    ) { query, products, display ->
        ProductosUiState(
            searchQuery = query,
            rawProducts = products,
            sortField = display.sortField,
            sortOrder = display.sortOrder,
            showProductDialogFor = display.showProductDialogFor,
            showImportDialog = display.showImportDialog,
            showBulkModificationDialog = display.showBulkModificationDialog,
            selectedProductIds = display.selectedProductIds
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ProductosUiState()
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onSortFieldChanged(field: ProductSortField) {
        _displayState.update { it.copy(sortField = field) }
    }

    fun onSortOrderChanged(order: ProductSortOrder) {
        _displayState.update { it.copy(sortOrder = order) }
    }

    fun onShowProductDialog(product: Products?) {
        _displayState.update { it.copy(showProductDialogFor = product) }
    }

    fun onDismissProductDialog() {
        _displayState.update { it.copy(showProductDialogFor = null) }
    }

    fun onShowImportDialog(show: Boolean) {
        _displayState.update { it.copy(showImportDialog = show) }
    }

    fun onShowBulkModificationDialog(show: Boolean) {
        _displayState.update { it.copy(showBulkModificationDialog = show) }
    }

    fun onToggleSelectProduct(productId: String) {
        _displayState.update { state ->
            val set = state.selectedProductIds
            val updated = if (set.contains(productId)) set - productId else set + productId
            state.copy(selectedProductIds = updated)
        }
    }

    fun onSelectAllProducts(productIds: List<String>) {
        _displayState.update { state ->
            val updated = if (state.selectedProductIds.containsAll(productIds)) emptySet() else productIds.toSet()
            state.copy(selectedProductIds = updated)
        }
    }

    fun onClearSelectedProducts() {
        _displayState.update { it.copy(selectedProductIds = emptySet()) }
    }

    fun saveProduct(product: Products) {
        viewModelScope.launch {
            if (_displayState.value.showProductDialogFor?.id?.isEmpty() == true) {
                repository.insertProduct(product)
            } else {
                repository.updateProduct(product)
            }
            _displayState.update { it.copy(showProductDialogFor = null) }
        }
    }

    fun deleteProductSoft(productId: String) {
        viewModelScope.launch {
            repository.deleteProductSoft(productId, currentTimeMillis())
        }
    }

    fun deleteProductHard(productId: String) {
        viewModelScope.launch {
            repository.deleteProductHard(productId)
        }
    }

    fun applyBulkModification(modification: BulkProductModification) {
        viewModelScope.launch {
            val selectedIds = _displayState.value.selectedProductIds
            val allProducts = repository.getAllProductsList()
            allProducts.filter { it.id in selectedIds }.forEach { product ->
                val updated = applyBulkProductModification(product, modification)
                if (updated == null) {
                    repository.deleteProductHard(product.id)
                } else {
                    repository.updateProduct(
                        updated.copy(
                            updated_at = currentTimeMillis(),
                            sync_state = "PENDING_UPDATE"
                        )
                    )
                }
            }
            _displayState.update {
                it.copy(
                    selectedProductIds = emptySet(),
                    showBulkModificationDialog = false
                )
            }
        }
    }
}
