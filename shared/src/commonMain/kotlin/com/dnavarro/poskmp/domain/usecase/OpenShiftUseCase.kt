package com.dnavarro.poskmp.domain.usecase

import com.dnavarro.poskmp.data.ShiftRepository
import com.dnavarro.poskmp.domain.model.CashierShift

class OpenShiftUseCase(
    private val shiftRepository: ShiftRepository
) {
    suspend operator fun invoke(cashierId: String, pin: String, initialCash: Double): Result<CashierShift> {
        val cashier = shiftRepository.getCashierById(cashierId)
            ?: return Result.failure(IllegalArgumentException("Cajero no encontrado."))

        if (cashier.pin != pin.trim()) {
            return Result.failure(IllegalArgumentException("PIN de cajero incorrecto."))
        }
        if (initialCash < 0.0) {
            return Result.failure(IllegalArgumentException("El fondo de caja no puede ser negativo."))
        }
        return shiftRepository.openShift(cashierId, initialCash)
    }
}
