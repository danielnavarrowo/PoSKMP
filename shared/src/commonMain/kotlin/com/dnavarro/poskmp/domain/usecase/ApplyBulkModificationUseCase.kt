package com.dnavarro.poskmp.domain.usecase

import com.dnavarro.poskmp.data.ProductRepository
import com.dnavarro.poskmp.ui.BulkProductModification
import com.dnavarro.poskmp.ui.applyBulkProductModification
import com.dnavarro.poskmp.util.currentTimeMillis
import kotlinx.coroutines.delay
import kotlinx.coroutines.yield
import kotlin.time.Duration.Companion.milliseconds

/**
 * Use case to apply price, cost, category, or status modifications across a set of selected product IDs.
 */
class ApplyBulkModificationUseCase(
    private val repository: ProductRepository
) {
    suspend operator fun invoke(
        selectedIds: Set<String>,
        modification: BulkProductModification,
        roundRetailPrice: Boolean = false,
        roundWholesalePrice: Boolean = false,
        roundDeliveryPrice: Boolean = false,
        onProgress: ((current: Int, total: Int) -> Unit)? = null
    ) {
        if (selectedIds.isEmpty()) return
        val allProducts = repository.getAllProductsList()
        val targetProducts = allProducts.filter { it.id in selectedIds }
        val total = targetProducts.size
        if (total == 0) return
        val now = currentTimeMillis()

        onProgress?.invoke(0, total)

        targetProducts.forEachIndexed { index, product ->
            val updated = applyBulkProductModification(
                product = product,
                modification = modification,
                roundRetailPrice = roundRetailPrice,
                roundWholesalePrice = roundWholesalePrice,
                roundDeliveryPrice = roundDeliveryPrice
            )
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
            onProgress?.invoke(index + 1, total)
            yield()
            if (total <= 30) {
                delay(15.milliseconds)
            }
        }
        if (total > 0) {
            delay(200.milliseconds)
        }
    }
}
