package com.dnavarro.poskmp.domain.usecase

import com.dnavarro.poskmp.data.ProductRepository
import com.dnavarro.poskmp.db.Products
import com.dnavarro.poskmp.util.currentTimeMillis
import com.dnavarro.poskmp.util.generateUUID
import com.dnavarro.poskmp.util.parseBarcodes

/**
 * Use case to validate and persist new or updated products.
 * Encapsulates timestamp generation, ID creation, and sync state initialization.
 */
class SaveProductUseCase(
    private val repository: ProductRepository
) {
    suspend operator fun invoke(product: Products) {
        val now = currentTimeMillis()
        val parsedCodes = parseBarcodes(product.codigos)
        val existingProductByBarcode = if (product.id.isBlank() && parsedCodes.isNotEmpty()) {
            repository.findConflictingProductForBarcodes(parsedCodes)?.second
        } else {
            null
        }

        val formattedId = when {
            product.id.isNotBlank() -> product.id
            existingProductByBarcode != null -> existingProductByBarcode.id
            else -> generateUUID()
        }

        val updatedProduct = product.copy(
            id = formattedId,
            updated_at = now,
            sync_state = if (product.id.isBlank() && existingProductByBarcode == null) "PENDING_INSERT" else "PENDING_UPDATE"
        )

        if (product.id.isBlank() && existingProductByBarcode == null && repository.getProductById(formattedId) == null) {
            repository.insertProduct(updatedProduct)
        } else {
            repository.updateProduct(updatedProduct)
        }
    }
}
