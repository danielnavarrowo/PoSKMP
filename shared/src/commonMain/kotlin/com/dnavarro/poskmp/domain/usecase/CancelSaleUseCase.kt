package com.dnavarro.poskmp.domain.usecase

import com.dnavarro.poskmp.data.SaleRepository

class CancelSaleUseCase(
    private val saleRepository: SaleRepository
) {
    suspend operator fun invoke(saleId: String): Result<Unit> {
        return try {
            saleRepository.cancelSale(saleId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
