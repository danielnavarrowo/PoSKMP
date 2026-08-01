package com.dnavarro.poskmp.domain.usecase

import com.dnavarro.poskmp.data.ProductRepository
import com.dnavarro.poskmp.ui.BulkProductModification
import com.dnavarro.poskmp.ui.applyBulkProductModification
import com.dnavarro.poskmp.util.currentTimeMillis

/**
 * Use case to apply price, cost, category, or status modifications across a set of selected product IDs.
 */
class ApplyBulkModificationUseCase(
    private val repository: ProductRepository
) {
    suspend operator fun invoke(selectedIds: Set<String>, modification: BulkProductModification) {
        if (selectedIds.isEmpty()) return
        val allProducts = repository.getAllProductsList()
        val now = currentTimeMillis()

        for (product in allProducts) {
            if (product.id in selectedIds) {
                val updated = applyBulkProductModification(product, modification)
                if (updated == null) {
                    repository.deleteProductHard(product.id)
                } else {
                    repository.updateProduct(
                        updated.copy(
                            updated_at = now,
                            sync_state = "PENDING_UPDATE"
                        )
                    )
                }
            }
        }
    }
}
