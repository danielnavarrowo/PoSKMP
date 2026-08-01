package com.dnavarro.poskmp.domain.usecase

import com.dnavarro.poskmp.data.ProductRepository
import com.dnavarro.poskmp.db.Products

/**
 * Use case to search for a product matching a specific barcode or product ID.
 * Reused across VentaViewModel, ChecadorDialog, and ProductosViewModel.
 */
class FindProductByBarcodeUseCase(
    private val repository: ProductRepository
) {
    suspend operator fun invoke(barcode: String): Products? {
        if (barcode.isBlank()) return null
        return repository.findProductByBarcode(barcode.trim())
    }
}
