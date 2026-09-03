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
import com.dnavarro.poskmp.ui.BulkProgressState
import com.dnavarro.poskmp.ui.ProductSortField
import com.dnavarro.poskmp.ui.ProductSortOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import com.dnavarro.poskmp.data.sync.SyncRepository
import com.dnavarro.poskmp.data.sync.SyncStateEnum
import com.dnavarro.poskmp.domain.model.ProductSalesStats

private data class DisplayState(
    val sortField: ProductSortField = ProductSortField.NOMBRE,
    val sortOrder: ProductSortOrder = ProductSortOrder.ASC,
    val selectedCategory: String? = null,
    val favoriteFilter: FavoriteFilterOption = FavoriteFilterOption.ALL,
    val statusFilter: StatusFilterOption = StatusFilterOption.ALL,
    val showProductDialogFor: Products? = null,
    val showBulkModificationFor: BulkProductOperation? = null,
    val selectedProductIds: Set<String> = emptySet(),
    val bulkModificationProgress: BulkProgressState? = null
)

private data class Tuple4<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

private data class ProductSettingsConfig(
    val defaultRetailMargin: Double,
    val defaultWholesaleMargin: Double,
    val defaultDeliveryMargin: Double,
    val isRoundingEnabled: Boolean,
    val roundRetailPrice: Boolean,
    val roundWholesalePrice: Boolean,
    val roundDeliveryPrice: Boolean
)

private data class ProductExtraState(
    val visibleColumns: Set<ProductTableColumn>,
    val salesStats: Map<String, ProductSalesStats>,
    val syncState: SyncStateEnum
)

/**
 * ViewModel for Productos screen, hosting state and handling UI events according to Google UI & Domain Layer architecture.
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class ProductosViewModel(
    private val repository: ProductRepository,
    private val settingsRepository: SettingsRepository,
    private val getProductsUseCase: GetProductsUseCase = GetProductsUseCase(repository),
    private val saveProductUseCase: SaveProductUseCase = SaveProductUseCase(repository),
    private val applyBulkModificationUseCase: ApplyBulkModificationUseCase = ApplyBulkModificationUseCase(repository),
    private val syncRepository: SyncRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _displayState = MutableStateFlow(DisplayState())

    private val _visibleColumnsFlow: Flow<Set<ProductTableColumn>> =
        settingsRepository.productTableVisibleColumnsFlow.map { savedNames ->
            val parsed = savedNames.mapNotNull { name ->
                try {
                    ProductTableColumn.valueOf(name)
                } catch (_: Exception) {
                    null
                }
            }.toSet()
            parsed.ifEmpty { DEFAULT_PRODUCT_TABLE_COLUMNS }
        }.distinctUntilChanged()

    private val _debouncedSearchQuery = _searchQuery.debounce { query ->
        if (query.isEmpty()) 0L else 300L
    }

    private val _productsFlow = _debouncedSearchQuery.flatMapLatest { query ->
        getProductsUseCase(query = query, activeOnly = false)
    }

    private val _needsSalesStats = combine(_displayState, _visibleColumnsFlow) { display, visibleCols ->
        visibleCols.contains(ProductTableColumn.VENTAS_TOTALES) ||
            visibleCols.contains(ProductTableColumn.ULTIMA_VENTA) ||
            display.sortField == ProductSortField.VENTAS_TOTALES ||
            display.sortField == ProductSortField.ULTIMA_VENTA
    }.distinctUntilChanged()

    private val _salesStatsFlow = _needsSalesStats.flatMapLatest { needed ->
        if (needed) {
            repository.getProductSalesStats()
        } else {
            flowOf(emptyMap())
        }
    }

    val uiState: StateFlow<ProductosUiState> = combine(
        combine(
            _searchQuery,
            _productsFlow,
            _displayState
        ) { query, products, display ->
            Triple(query, products, display)
        },
        combine(
            _visibleColumnsFlow,
            _salesStatsFlow,
            syncRepository.syncState
        ) { visibleColumns, salesStats, syncState ->
            ProductExtraState(visibleColumns, salesStats, syncState)
        },
        combine(
            combine(
                settingsRepository.defaultRetailMarginFlow,
                settingsRepository.defaultWholesaleMarginFlow,
                settingsRepository.defaultDeliveryMarginFlow
            ) { retailMargin, wholesaleMargin, deliveryMargin ->
                Triple(retailMargin, wholesaleMargin, deliveryMargin)
            },
            combine(
                settingsRepository.isRoundingEnabledFlow,
                settingsRepository.roundRetailPriceFlow,
                settingsRepository.roundWholesalePriceFlow,
                settingsRepository.roundDeliveryPriceFlow
            ) { isRounding, roundRetail, roundWholesale, roundDelivery ->
                Tuple4(isRounding, roundRetail, roundWholesale, roundDelivery)
            }
        ) { (retailMargin, wholesaleMargin, deliveryMargin), (isRounding, roundRetail, roundWholesale, roundDelivery) ->
            ProductSettingsConfig(
                defaultRetailMargin = retailMargin,
                defaultWholesaleMargin = wholesaleMargin,
                defaultDeliveryMargin = deliveryMargin,
                isRoundingEnabled = isRounding,
                roundRetailPrice = roundRetail,
                roundWholesalePrice = roundWholesale,
                roundDeliveryPrice = roundDelivery
            )
        }
    ) { (query, products, display), extra, settings ->
        ProductosUiState(
            searchQuery = query,
            rawProducts = products,
            salesStats = extra.salesStats,
            sortField = display.sortField,
            sortOrder = display.sortOrder,
            selectedCategory = display.selectedCategory,
            favoriteFilter = display.favoriteFilter,
            statusFilter = display.statusFilter,
            visibleColumns = extra.visibleColumns,
            showProductDialogFor = display.showProductDialogFor,
            showBulkModificationFor = display.showBulkModificationFor,
            selectedProductIds = display.selectedProductIds,
            bulkModificationProgress = display.bulkModificationProgress,
            defaultRetailMargin = settings.defaultRetailMargin,
            defaultWholesaleMargin = settings.defaultWholesaleMargin,
            defaultDeliveryMargin = settings.defaultDeliveryMargin,
            roundRetailPrice = settings.isRoundingEnabled && settings.roundRetailPrice,
            roundWholesalePrice = settings.isRoundingEnabled && settings.roundWholesalePrice,
            roundDeliveryPrice = settings.isRoundingEnabled && settings.roundDeliveryPrice,
            isSyncing = extra.syncState == SyncStateEnum.SYNCING
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

    fun onToggleColumn(column: ProductTableColumn) {
        viewModelScope.launch {
            settingsRepository.toggleProductTableColumn(
                columnName = column.name,
                defaultColumns = DEFAULT_PRODUCT_TABLE_COLUMNS.map { it.name }.toSet()
            )
        }
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
        _displayState.update {
            it.copy(
                showBulkModificationFor = op,
                bulkModificationProgress = if (op == null) null else it.bulkModificationProgress
            )
        }
    }

    fun onToggleSelectProduct(productId: String) {
        _displayState.update { state ->
            val set = state.selectedProductIds
            val updated = if (set.contains(productId)) set - productId else set + productId
            state.copy(selectedProductIds = updated)
        }
    }

    fun onSetSelectedProductIds(productIds: Set<String>) {
        _displayState.update { state ->
            state.copy(selectedProductIds = productIds)
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

    fun applyBulkModification(modification: BulkProductModification) {
        val state = uiState.value
        val selectedIds = _displayState.value.selectedProductIds
        val total = selectedIds.size
        viewModelScope.launch {
            _displayState.update {
                it.copy(
                    bulkModificationProgress = BulkProgressState(
                        operation = modification.operation,
                        current = 0,
                        total = total
                    )
                )
            }
            withContext(Dispatchers.IO) {
                applyBulkModificationUseCase(
                    selectedIds = selectedIds,
                    modification = modification,
                    roundRetailPrice = state.roundRetailPrice,
                    roundWholesalePrice = state.roundWholesalePrice,
                    roundDeliveryPrice = state.roundDeliveryPrice,
                    onProgress = { current, count ->
                        _displayState.update {
                            it.copy(
                                bulkModificationProgress = BulkProgressState(
                                    operation = modification.operation,
                                    current = current,
                                    total = count
                                )
                            )
                        }
                    }
                )
            }
            _displayState.update {
                it.copy(
                    selectedProductIds = emptySet(),
                    showBulkModificationFor = null,
                    bulkModificationProgress = null
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
