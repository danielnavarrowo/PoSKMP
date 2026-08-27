package com.dnavarro.poskmp.ui.productos

import com.dnavarro.poskmp.db.Products
import com.dnavarro.poskmp.ui.BulkProductOperation
import com.dnavarro.poskmp.ui.ProductSortField
import com.dnavarro.poskmp.ui.ProductSortOrder
import com.dnavarro.poskmp.util.formatBarcodesForDisplay
import org.jetbrains.compose.resources.StringResource
import poskmp.shared.generated.resources.Res
import poskmp.shared.generated.resources.header_category
import poskmp.shared.generated.resources.header_codes
import poskmp.shared.generated.resources.header_cost
import poskmp.shared.generated.resources.header_product_name
import poskmp.shared.generated.resources.header_retail_price
import poskmp.shared.generated.resources.wholesale

enum class ProductTableColumn(
    val titleRes: StringResource,
    val sortField: ProductSortField,
    val defaultWeight: Float
) {
    CODIGO(Res.string.header_codes, ProductSortField.CODIGO, 0.18f),
    NOMBRE(Res.string.header_product_name, ProductSortField.NOMBRE, 0.28f),
    CATEGORIA(Res.string.header_category, ProductSortField.CATEGORIA, 0.14f),
    PRECIO(Res.string.header_retail_price, ProductSortField.PRECIO, 0.10f),
    COSTO(Res.string.header_cost, ProductSortField.COSTO, 0.10f),
    MAYOREO(Res.string.wholesale, ProductSortField.MAYOREO, 0.10f)
}

enum class FavoriteFilterOption {
    ALL, ONLY_FAVORITES, ONLY_NON_FAVORITES
}

enum class StatusFilterOption {
    ALL, ONLY_ACTIVE, ONLY_INACTIVE
}

/**
 * Immutable UI state for the Productos screen following Google Android UI Layer guidelines.
 */
data class ProductosUiState(
    val searchQuery: String = "",
    val rawProducts: List<Products> = emptyList(),
    val sortField: ProductSortField = ProductSortField.NOMBRE,
    val sortOrder: ProductSortOrder = ProductSortOrder.ASC,
    val selectedCategory: String? = null,
    val favoriteFilter: FavoriteFilterOption = FavoriteFilterOption.ALL,
    val statusFilter: StatusFilterOption = StatusFilterOption.ALL,
    val visibleColumns: Set<ProductTableColumn> = ProductTableColumn.entries.toSet(),
    val showProductDialogFor: Products? = null,
    val showBulkModificationFor: BulkProductOperation? = null,
    val selectedProductIds: Set<String> = emptySet(),
    val defaultRetailMargin: Double = 0.0,
    val defaultWholesaleMargin: Double = 0.0,
    val roundRetailPrice: Boolean = false,
    val roundWholesalePrice: Boolean = false,
    val isLoading: Boolean = false,
    val isSyncing: Boolean = false
) {
    val availableCategories: List<String>
        get() = rawProducts.mapNotNull { it.categoria }.filter { it.isNotBlank() }.distinct().sorted()

    val hasActiveFilters: Boolean
        get() = selectedCategory != null || favoriteFilter != FavoriteFilterOption.ALL || statusFilter != StatusFilterOption.ALL

    val sortedProducts: List<Products>
        get() {
            return rawProducts
                .filter { product ->
                    val matchesCategory = when (selectedCategory) {
                        null -> true
                        "NO_CATEGORY" -> product.categoria.isNullOrBlank()
                        else -> product.categoria == selectedCategory
                    }
                    val matchesFavorite = when (favoriteFilter) {
                        FavoriteFilterOption.ALL -> true
                        FavoriteFilterOption.ONLY_FAVORITES -> product.es_favorito == 1L
                        FavoriteFilterOption.ONLY_NON_FAVORITES -> product.es_favorito == 0L
                    }
                    val matchesStatus = when (statusFilter) {
                        StatusFilterOption.ALL -> true
                        StatusFilterOption.ONLY_ACTIVE -> product.activo == 1L
                        StatusFilterOption.ONLY_INACTIVE -> product.activo == 0L
                    }
                    matchesCategory && matchesFavorite && matchesStatus
                }
                .sortedWith { p1, p2 ->
                    val f1 = p1.es_favorito == 1L
                    val f2 = p2.es_favorito == 1L
                    if (f1 != f2) return@sortedWith if (f1) -1 else 1

                    val primaryComp = when (sortField) {
                        ProductSortField.NOMBRE -> p1.nombre.lowercase().compareTo(p2.nombre.lowercase())
                        ProductSortField.CODIGO -> {
                            val c1 = p1.formatBarcodesForDisplay(emptyFallback = "")
                            val c2 = p2.formatBarcodesForDisplay(emptyFallback = "")
                            c1.lowercase().compareTo(c2.lowercase())
                        }
                        ProductSortField.CATEGORIA -> (p1.categoria ?: "").lowercase().compareTo((p2.categoria ?: "").lowercase())
                        ProductSortField.PRECIO -> p1.precio.compareTo(p2.precio)
                        ProductSortField.COSTO -> p1.costo.compareTo(p2.costo)
                        ProductSortField.MAYOREO -> p1.precio_mayoreo.compareTo(p2.precio_mayoreo)
                    }

                    if (sortOrder == ProductSortOrder.ASC) primaryComp else -primaryComp
                }
        }
}
