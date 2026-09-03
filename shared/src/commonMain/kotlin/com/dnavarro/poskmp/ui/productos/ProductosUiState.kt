package com.dnavarro.poskmp.ui.productos

import com.dnavarro.poskmp.db.Products
import com.dnavarro.poskmp.domain.model.ProductSalesStats
import com.dnavarro.poskmp.ui.BulkProductOperation
import com.dnavarro.poskmp.ui.BulkProgressState
import com.dnavarro.poskmp.ui.ProductSortField
import com.dnavarro.poskmp.ui.ProductSortOrder
import com.dnavarro.poskmp.util.formatBarcodesForDisplay
import org.jetbrains.compose.resources.StringResource
import poskmp.shared.generated.resources.Res
import poskmp.shared.generated.resources.header_category
import poskmp.shared.generated.resources.header_codes
import poskmp.shared.generated.resources.header_cost
import poskmp.shared.generated.resources.header_last_sale
import poskmp.shared.generated.resources.header_pieces
import poskmp.shared.generated.resources.header_product_name
import poskmp.shared.generated.resources.header_retail_margin
import poskmp.shared.generated.resources.header_retail_price
import poskmp.shared.generated.resources.header_total_sales
import poskmp.shared.generated.resources.header_delivery_margin
import poskmp.shared.generated.resources.header_delivery_price
import poskmp.shared.generated.resources.header_wholesale_margin
import poskmp.shared.generated.resources.wholesale

enum class ProductTableColumn(
    val titleRes: StringResource,
    val sortField: ProductSortField,
    val defaultWeight: Float
) {
    CODIGO(Res.string.header_codes, ProductSortField.CODIGO, 0.12f),
    NOMBRE(Res.string.header_product_name, ProductSortField.NOMBRE, 0.28f),
    CATEGORIA(Res.string.header_category, ProductSortField.CATEGORIA, 0.16f),
    PIEZAS(Res.string.header_pieces, ProductSortField.PIEZAS, 0.10f),
    PRECIO(Res.string.header_retail_price, ProductSortField.PRECIO, 0.12f),
    COSTO(Res.string.header_cost, ProductSortField.COSTO, 0.12f),
    MAYOREO(Res.string.wholesale, ProductSortField.MAYOREO, 0.12f),
    DOMICILIO(Res.string.header_delivery_price, ProductSortField.DOMICILIO, 0.12f),
    MARGEN_VENTA(Res.string.header_retail_margin, ProductSortField.MARGEN_VENTA, 0.10f),
    MARGEN_MAYOREO(Res.string.header_wholesale_margin, ProductSortField.MARGEN_MAYOREO, 0.10f),
    MARGEN_DOMICILIO(Res.string.header_delivery_margin, ProductSortField.MARGEN_DOMICILIO, 0.10f),
    VENTAS_TOTALES(Res.string.header_total_sales, ProductSortField.VENTAS_TOTALES, 0.10f),
    ULTIMA_VENTA(Res.string.header_last_sale, ProductSortField.ULTIMA_VENTA, 0.14f)
}

val DEFAULT_PRODUCT_TABLE_COLUMNS: Set<ProductTableColumn> = setOf(
    ProductTableColumn.NOMBRE,
    ProductTableColumn.CATEGORIA,
    ProductTableColumn.PRECIO,
    ProductTableColumn.COSTO,
    ProductTableColumn.MAYOREO
)

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
    val salesStats: Map<String, ProductSalesStats> = emptyMap(),
    val sortField: ProductSortField = ProductSortField.NOMBRE,
    val sortOrder: ProductSortOrder = ProductSortOrder.ASC,
    val selectedCategory: String? = null,
    val favoriteFilter: FavoriteFilterOption = FavoriteFilterOption.ALL,
    val statusFilter: StatusFilterOption = StatusFilterOption.ALL,
    val visibleColumns: Set<ProductTableColumn> = DEFAULT_PRODUCT_TABLE_COLUMNS,
    val showProductDialogFor: Products? = null,
    val showBulkModificationFor: BulkProductOperation? = null,
    val selectedProductIds: Set<String> = emptySet(),
    val bulkModificationProgress: BulkProgressState? = null,
    val defaultRetailMargin: Double = 0.0,
    val defaultWholesaleMargin: Double = 0.0,
    val defaultDeliveryMargin: Double = 0.0,
    val roundRetailPrice: Boolean = false,
    val roundWholesalePrice: Boolean = false,
    val roundDeliveryPrice: Boolean = false,
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
                        ProductSortField.PIEZAS -> p1.piezas.compareTo(p2.piezas)
                        ProductSortField.PRECIO -> p1.precio.compareTo(p2.precio)
                        ProductSortField.COSTO -> p1.costo.compareTo(p2.costo)
                        ProductSortField.MAYOREO -> p1.precio_mayoreo.compareTo(p2.precio_mayoreo)
                        ProductSortField.DOMICILIO -> p1.precio_delivery.compareTo(p2.precio_delivery)
                        ProductSortField.MARGEN_VENTA -> {
                            val m1 = if (p1.costo > 0.0 && p1.precio > 0.0) ((p1.precio - p1.costo) / p1.costo) * 100.0 else -Double.MAX_VALUE
                            val m2 = if (p2.costo > 0.0 && p2.precio > 0.0) ((p2.precio - p2.costo) / p2.costo) * 100.0 else -Double.MAX_VALUE
                            m1.compareTo(m2)
                        }
                        ProductSortField.MARGEN_MAYOREO -> {
                            val m1 = if (p1.costo > 0.0 && p1.precio_mayoreo > 0.0) ((p1.precio_mayoreo - p1.costo) / p1.costo) * 100.0 else -Double.MAX_VALUE
                            val m2 = if (p2.costo > 0.0 && p2.precio_mayoreo > 0.0) ((p2.precio_mayoreo - p2.costo) / p2.costo) * 100.0 else -Double.MAX_VALUE
                            m1.compareTo(m2)
                        }
                        ProductSortField.MARGEN_DOMICILIO -> {
                            val m1 = if (p1.costo > 0.0 && p1.precio_delivery > 0.0) ((p1.precio_delivery - p1.costo) / p1.costo) * 100.0 else -Double.MAX_VALUE
                            val m2 = if (p2.costo > 0.0 && p2.precio_delivery > 0.0) ((p2.precio_delivery - p2.costo) / p2.costo) * 100.0 else -Double.MAX_VALUE
                            m1.compareTo(m2)
                        }
                        ProductSortField.VENTAS_TOTALES -> {
                            val v1 = salesStats[p1.id]?.totalVentas ?: 0.0
                            val v2 = salesStats[p2.id]?.totalVentas ?: 0.0
                            v1.compareTo(v2)
                        }
                        ProductSortField.ULTIMA_VENTA -> {
                            val d1 = salesStats[p1.id]?.ultimaVenta ?: 0L
                            val d2 = salesStats[p2.id]?.ultimaVenta ?: 0L
                            d1.compareTo(d2)
                        }
                    }

                    if (sortOrder == ProductSortOrder.ASC) primaryComp else -primaryComp
                }
        }
}
