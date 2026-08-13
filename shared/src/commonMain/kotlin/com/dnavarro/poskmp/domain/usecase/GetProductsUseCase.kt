package com.dnavarro.poskmp.domain.usecase

import com.dnavarro.poskmp.data.ProductRepository
import com.dnavarro.poskmp.db.Products
import kotlinx.coroutines.flow.Flow

/**
 * Use case to observe products, supporting search query filtering and active-only filtering.
 */
class GetProductsUseCase(
    private val repository: ProductRepository
) {
    operator fun invoke(query: String = "", activeOnly: Boolean = false): Flow<List<Products>> {
        return if (query.isBlank()) {
            if (activeOnly) repository.getActiveProducts() else repository.getAllProducts()
        } else {
            repository.searchProducts(query.trim(), activeOnly = activeOnly)
        }
    }
}
