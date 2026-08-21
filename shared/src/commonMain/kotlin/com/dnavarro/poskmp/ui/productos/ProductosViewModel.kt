package com.dnavarro.poskmp.ui.productos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dnavarro.poskmp.data.ProductRepository
import com.dnavarro.poskmp.data.SettingsRepository
import com.dnavarro.poskmp.db.Products
import com.dnavarro.poskmp.domain.usecase.ApplyBulkModificationUseCase
import com.dnavarro.poskmp.domain.usecase.GetProductsUseCase
import com.dnavarro.poskmp.domain.usecase.SaveProductUseCase
import com.dnavarro.poskmp.ui.BulkProductModification
import com.dnavarro.poskmp.ui.BulkProductOperation
import com.dnavarro.poskmp.ui.ProductSortField
import com.dnavarro.poskmp.ui.ProductSortOrder
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

import com.dnavarro.poskmp.data.sync.SyncRepository
import com.dnavarro.poskmp.data.sync.SyncStateEnum
import kotlinx.coroutines.Dispatchers

private data class DisplayState(
    val sortField: ProductSortField = ProductSortField.NOMBRE,
    val sortOrder: ProductSortOrder = ProductSortOrder.ASC,
    val selectedCategory: String? = null,
    val favoriteFilter: FavoriteFilterOption = FavoriteFilterOption.ALL,
    val statusFilter: StatusFilterOption = StatusFilterOption.ALL,
    val showProductDialogFor: Products? = null,
    val showBulkModificationFor: BulkProductOperation? = null,
    val selectedProductIds: Set<String> = emptySet()
)

private data class Tuple5<A, B, C, D, E>(val a: A, val b: B, val c: C, val d: D, val e: E)

/**
 * ViewModel for Productos screen, hosting state and handling UI events according to Google UI & Domain Layer architecture.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ProductosViewModel(
    private val repository: ProductRepository,
    settingsRepository: SettingsRepository,
    private val getProductsUseCase: GetProductsUseCase = GetProductsUseCase(repository),
    private val saveProductUseCase: SaveProductUseCase = SaveProductUseCase(repository),
    private val applyBulkModificationUseCase: ApplyBulkModificationUseCase = ApplyBulkModificationUseCase(repository),
    private val syncRepository: SyncRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _displayState = MutableStateFlow(DisplayState())

    private val _productsFlow = _searchQuery.flatMapLatest { query ->
        getProductsUseCase(query = query, activeOnly = false)
    }

    val uiState: StateFlow<ProductosUiState> = combine(
        combine(
            _searchQuery,
            _productsFlow,
            _displayState,
            settingsRepository.defaultRetailMarginFlow,
            settingsRepository.defaultWholesaleMarginFlow
        ) { query, products, display, defaultRetailMargin, defaultWholesaleMargin ->
            Tuple5(query, products, display, defaultRetailMargin, defaultWholesaleMargin)
        },
        combine(
            settingsRepository.isRoundingEnabledFlow,
            settingsRepository.roundRetailPriceFlow,
            settingsRepository.roundWholesalePriceFlow
        ) { isRoundingEnabled, roundRetailPrice, roundWholesalePrice ->
            Triple(isRoundingEnabled, roundRetailPrice, roundWholesalePrice)
        },
        syncRepository.syncState
    ) { (query, products, display, defaultRetailMargin, defaultWholesaleMargin),
        (isRoundingEnabled, roundRetailPrice, roundWholesalePrice),
        syncState ->
        ProductosUiState(
            searchQuery = query,
            rawProducts = products,
            sortField = display.sortField,
            sortOrder = display.sortOrder,
            selectedCategory = display.selectedCategory,
            favoriteFilter = display.favoriteFilter,
            statusFilter = display.statusFilter,
            showProductDialogFor = display.showProductDialogFor,
            showBulkModificationFor = display.showBulkModificationFor,
            selectedProductIds = display.selectedProductIds,
            defaultRetailMargin = defaultRetailMargin,
            defaultWholesaleMargin = defaultWholesaleMargin,
            roundRetailPrice = isRoundingEnabled && roundRetailPrice,
            roundWholesalePrice = isRoundingEnabled && roundWholesalePrice,
            isSyncing = syncState == SyncStateEnum.SYNCING
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

    fun onCategoryFilterChanged(category: String?) {
        _displayState.update { it.copy(selectedCategory = category) }
    }

    fun onFavoriteFilterChanged(filter: FavoriteFilterOption) {
        _displayState.update { it.copy(favoriteFilter = filter) }
    }

    fun onStatusFilterChanged(filter: StatusFilterOption) {
        _displayState.update { it.copy(statusFilter = filter) }
    }

    fun onResetFilters() {
        _displayState.update {
            it.copy(
                selectedCategory = null,
                favoriteFilter = FavoriteFilterOption.ALL,
                statusFilter = StatusFilterOption.ALL,
                sortField = ProductSortField.NOMBRE,
                sortOrder = ProductSortOrder.ASC
            )
        }
    }

    fun onShowProductDialog(product: Products?) {
        _displayState.update { it.copy(showProductDialogFor = product) }
    }

    fun onDismissProductDialog() {
        _displayState.update { it.copy(showProductDialogFor = null) }
    }

    fun onShowBulkModificationDialog(op: BulkProductOperation?) {
        _displayState.update { it.copy(showBulkModificationFor = op) }
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
            if (productIds.isEmpty()) return@update state
            val containsAll = state.selectedProductIds.containsAll(productIds)
            val updated = if (containsAll) {
                state.selectedProductIds - productIds.toSet()
            } else {
                state.selectedProductIds + productIds.toSet()
            }
            state.copy(selectedProductIds = updated)
        }
    }

    fun saveProduct(product: Products) {
        viewModelScope.launch {
            saveProductUseCase(product)
            _displayState.update { it.copy(showProductDialogFor = null) }
            launch(Dispatchers.IO) {
                syncRepository.syncAll()
            }
        }
    }

    fun deleteProductSoft(productId: String) {
        viewModelScope.launch {
            repository.deleteProductSoft(productId, currentTimeMillis())
            launch(Dispatchers.IO) {
                syncRepository.syncAll()
            }
        }
    }

     fun applyBulkModification(modification: BulkProductModification) {
        viewModelScope.launch {
            applyBulkModificationUseCase(_displayState.value.selectedProductIds, modification)
            _displayState.update {
                it.copy(
                    selectedProductIds = emptySet(),
                    showBulkModificationFor = null
                )
            }
            launch(Dispatchers.IO) {
                syncRepository.syncAll()
            }
        }
    }

    fun refreshSync() {
        viewModelScope.launch(Dispatchers.IO) {
            syncRepository.syncAll(isManual = true)
        }
    }

    suspend fun validateBarcodes(codes: List<String>, excludeProductId: String?): Pair<String, Products>? {
        return repository.findConflictingProductForBarcodes(codes, excludeProductId)
    }
}
