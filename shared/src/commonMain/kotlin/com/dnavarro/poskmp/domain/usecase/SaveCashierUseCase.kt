package com.dnavarro.poskmp.domain.usecase

import com.dnavarro.poskmp.data.ShiftRepository
import com.dnavarro.poskmp.domain.model.Cashier

class SaveCashierUseCase(
    private val shiftRepository: ShiftRepository
) {
    suspend operator fun invoke(
        id: String?,
        nombre: String,
        pin: String
    ): Result<Cashier> {
        val trimmedName = nombre.trim()
        val trimmedPin = pin.trim()

        if (trimmedName.isBlank()) {
            return Result.failure(IllegalArgumentException("El nombre del cajero no puede estar vacío."))
        }
        if (trimmedPin.length != 4 || !trimmedPin.all { it.isDigit() }) {
            return Result.failure(IllegalArgumentException("El PIN debe ser exactamente de 4 dígitos numéricos."))
        }

        return shiftRepository.saveCashier(id, trimmedName, trimmedPin)
    }
}
