package com.dnavarro.poskmp.domain.usecase

import com.dnavarro.poskmp.data.ShiftRepository

class DeleteCashierUseCase(
    private val shiftRepository: ShiftRepository
) {
    suspend operator fun invoke(id: String): Result<Unit> {
        if (id.isBlank()) {
            return Result.failure(IllegalArgumentException("ID de cajero inválido."))
        }
        return shiftRepository.deleteCashier(id)
    }
}
