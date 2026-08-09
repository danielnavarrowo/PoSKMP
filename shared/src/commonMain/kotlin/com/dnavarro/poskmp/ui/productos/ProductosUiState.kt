package com.dnavarro.poskmp.ui.productos

import com.dnavarro.poskmp.db.Products
import com.dnavarro.poskmp.ui.BulkProductOperation
import com.dnavarro.poskmp.ui.ProductSortField
import com.dnavarro.poskmp.ui.ProductSortOrder

/**
 * Immutable UI state for the Productos screen following Google Android UI Layer guidelines.
 */
data class ProductosUiState(
    val searchQuery: String = "",
    val rawProducts: List<Products> = emptyList(),
    val sortField: ProductSortField = ProductSortField.NOMBRE,
    val sortOrder: ProductSortOrder = ProductSortOrder.ASC,
    val showProductDialogFor: Products? = null,
    val showBulkModificationFor: BulkProductOperation? = null,
    val selectedProductIds: Set<String> = emptySet(),
    val isLoading: Boolean = false
) {
    val sortedProducts: List<Products>
        get() {
            return rawProducts.sortedWith { p1, p2 ->
                val f1 = p1.es_favorito == 1L
                val f2 = p2.es_favorito == 1L
                if (f1 != f2) return@sortedWith if (f1) -1 else 1

                val primaryComp = when (sortField) {
                    ProductSortField.NOMBRE -> p1.nombre.lowercase().compareTo(p2.nombre.lowercase())
                    ProductSortField.CODIGO -> {
                        val c1 = p1.codigos.replace("[", "").replace("]", "").replace("\"", "").trim()
                        val c2 = p2.codigos.replace("[", "").replace("]", "").replace("\"", "").trim()
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
