package com.dnavarro.poskmp.domain.usecase

import com.dnavarro.poskmp.data.ProductRepository
import com.dnavarro.poskmp.db.Products
import com.dnavarro.poskmp.ui.BulkProductModification
import com.dnavarro.poskmp.ui.applyBulkProductModification
import com.dnavarro.poskmp.util.currentTimeMillis

/**
 * Use case to apply price, cost, category, or status modifications across a set of selected product IDs.
 */
class ApplyBulkModificationUseCase(
    private val repository: ProductRepository
) {
    suspend operator fun invoke(
        selectedIds: Set<String>,
        modification: BulkProductModification,
        roundProductPrices: Boolean = false,
        onProgress: ((current: Int, total: Int) -> Unit)? = null
    ) {
        if (selectedIds.isEmpty()) return
        val allProducts = repository.getAllProductsList()
        val targetProducts = allProducts.filter { it.id in selectedIds }
        val total = targetProducts.size
        if (total == 0) return
        val now = currentTimeMillis()

        onProgress?.invoke(0, total)

        val toDeleteIds = mutableListOf<String>()
        val toUpdate = ArrayList<Products>(total)
        val updateInterval = maxOf(1, total / 20)

        targetProducts.forEachIndexed { index, product ->
            val updated = applyBulkProductModification(
                product = product,
                modification = modification,
                roundProductPrices = roundProductPrices
            )
            if (updated == null) {
                toDeleteIds.add(product.id)
            } else {
                toUpdate.add(
                    updated.copy(
                        updated_at = now,
                        sync_state = "PENDING_UPDATE"
                    )
                )
            }
            if ((index + 1) % updateInterval == 0 || index == total - 1) {
                onProgress?.invoke(index + 1, total)
            }
        }

        if (toDeleteIds.isNotEmpty()) {
            repository.deleteProductsHard(toDeleteIds)
        }
        if (toUpdate.isNotEmpty()) {
            repository.updateProducts(toUpdate)
        }

        onProgress?.invoke(total, total)
    }
}
