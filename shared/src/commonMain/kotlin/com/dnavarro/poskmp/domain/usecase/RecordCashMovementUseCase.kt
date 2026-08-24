package com.dnavarro.poskmp.domain.usecase

import com.dnavarro.poskmp.data.ShiftRepository
import com.dnavarro.poskmp.domain.model.CashMovement
import com.dnavarro.poskmp.domain.model.CashMovementType

class RecordCashMovementUseCase(
    private val shiftRepository: ShiftRepository
) {
    suspend operator fun invoke(
        shiftId: String,
        cashierId: String,
        type: CashMovementType,
        amount: Double,
        reason: String = ""
    ): Result<CashMovement> {
        if (amount <= 0.0) {
            return Result.failure(IllegalArgumentException("El monto debe ser mayor a 0."))
        }
        return shiftRepository.recordCashMovement(shiftId, cashierId, type, amount, reason.trim())
    }
}
