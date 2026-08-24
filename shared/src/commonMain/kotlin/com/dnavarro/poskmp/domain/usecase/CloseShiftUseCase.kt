package com.dnavarro.poskmp.domain.usecase

import com.dnavarro.poskmp.data.ShiftRepository
import com.dnavarro.poskmp.domain.model.CashierShift

class CloseShiftUseCase(
    private val shiftRepository: ShiftRepository
) {
    suspend operator fun invoke(shiftId: String, countedCash: Double, notes: String? = null): Result<CashierShift> {
        if (countedCash < 0.0) {
            return Result.failure(IllegalArgumentException("El efectivo contado no puede ser negativo."))
        }
        return shiftRepository.closeShift(shiftId, countedCash, notes)
    }
}
