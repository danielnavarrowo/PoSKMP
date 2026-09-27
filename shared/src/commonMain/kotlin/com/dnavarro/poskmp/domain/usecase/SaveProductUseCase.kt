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

        val cleanName = product.nombre.trim()
        val existingByName = if (cleanName.isNotBlank()) {
            repository.getProductByName(cleanName)
        } else {
            null
        }

        // Validate that no OTHER product has the same name
        if (existingByName != null && (existingById == null || existingByName.id != existingById.id)) {
            throw IllegalArgumentException("Ya existe un producto con el nombre '$cleanName'")
        }

        // 2. Validate barcodes: ensure no OTHER product has any of these barcodes
        val excludeId = existingById?.id ?: existingByName?.id ?: product.id.ifBlank { null }
        val barcodeConflict = if (parsedCodes.isNotEmpty()) {
            repository.findConflictingProductForBarcodes(parsedCodes, excludeProductId = excludeId)
        } else {
            null
        }

        if (barcodeConflict != null) {
            throw IllegalArgumentException(
                "El código '${barcodeConflict.first}' ya pertenece al producto '${barcodeConflict.second.nombre}'"
            )
        }

        val existingByBarcode = if (existingById == null && parsedCodes.isNotEmpty()) {
            repository.findConflictingProductForBarcodes(parsedCodes)?.second
        } else {
            null
        }

        // 3. Resolve canonical ID
        val finalId = when {
            existingById != null -> existingById.id
            existingByName != null -> existingByName.id
            existingByBarcode != null -> existingByBarcode.id
            product.id.isNotBlank() -> product.id
            else -> generateUUID()
        }

        // 4. Determine if this is a brand new product
        val isBrandNew = existingById == null && existingByName == null && existingByBarcode == null

        // 5. Determine sync state: preserve PENDING_INSERT if it was never pushed to remote yet
        val syncState = if (isBrandNew) {
            "PENDING_INSERT"
        } else {
            val previousSyncState = existingById?.sync_state ?: existingByBarcode?.sync_state
            if (previousSyncState == "PENDING_INSERT") "PENDING_INSERT" else "PENDING_UPDATE"
        }

        val createdAt = when {
            isBrandNew -> if (product.created_at > 0L) product.created_at else now
            existingById?.created_at != null && existingById.created_at > 0L -> existingById.created_at
            existingByBarcode?.created_at != null && existingByBarcode.created_at > 0L -> existingByBarcode.created_at
            product.created_at > 0L -> product.created_at
            else -> existingById?.updated_at ?: existingByBarcode?.updated_at ?: now
        }

        val updatedProduct = product.copy(
            id = finalId,
            created_at = createdAt,
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
