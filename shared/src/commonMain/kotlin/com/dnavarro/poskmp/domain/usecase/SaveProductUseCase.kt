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

        // 1. Check if this product already exists in the local database by its ID
        val existingById = if (product.id.isNotBlank()) {
            repository.getProductById(product.id)
        } else {
            null
        }

        // 2. If not found by ID and barcodes are provided, check if a product with this barcode already exists
        val existingByBarcode = if (existingById == null && parsedCodes.isNotEmpty()) {
            repository.findConflictingProductForBarcodes(parsedCodes)?.second
        } else {
            null
        }

        // 3. Resolve canonical ID
        val finalId = when {
            existingById != null -> existingById.id
            existingByBarcode != null -> existingByBarcode.id
            product.id.isNotBlank() -> product.id
            else -> generateUUID()
        }

        // 4. Determine if this is a brand new product
        val isBrandNew = existingById == null && existingByBarcode == null

        // 5. Determine sync state: preserve PENDING_INSERT if it was never pushed to remote yet
        val syncState = if (isBrandNew) {
            "PENDING_INSERT"
        } else {
            val previousSyncState = existingById?.sync_state ?: existingByBarcode?.sync_state
            if (previousSyncState == "PENDING_INSERT") "PENDING_INSERT" else "PENDING_UPDATE"
        }

        val updatedProduct = product.copy(
            id = finalId,
            updated_at = now,
            sync_state = syncState
        )

        // 6. Insert new product or update existing
        if (isBrandNew || repository.getProductById(finalId) == null) {
            repository.insertProduct(updatedProduct)
        } else {
            repository.updateProduct(updatedProduct)
        }
    }
}
