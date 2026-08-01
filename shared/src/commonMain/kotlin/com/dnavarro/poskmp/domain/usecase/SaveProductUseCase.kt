package com.dnavarro.poskmp.domain.usecase

import com.dnavarro.poskmp.data.ProductRepository
import com.dnavarro.poskmp.db.Products
import com.dnavarro.poskmp.util.currentTimeMillis
import com.dnavarro.poskmp.util.generateUUID

/**
 * Use case to validate and persist new or updated products.
 * Encapsulates timestamp generation, ID creation, and sync state initialization.
 */
class SaveProductUseCase(
    private val repository: ProductRepository
) {
    suspend operator fun invoke(product: Products) {
        val now = currentTimeMillis()
        val formattedId = product.id.ifBlank { generateUUID() }
        val updatedProduct = product.copy(
            id = formattedId,
            updated_at = now,
            sync_state = "PENDING"
        )
        if (product.id.isBlank() || repository.getProductById(formattedId) == null) {
            repository.insertProduct(updatedProduct)
        } else {
            repository.updateProduct(updatedProduct)
        }
    }
}
