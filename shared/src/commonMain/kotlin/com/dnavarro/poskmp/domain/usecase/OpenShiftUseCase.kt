package com.dnavarro.poskmp.domain.usecase

import com.dnavarro.poskmp.data.SettingsRepository
import com.dnavarro.poskmp.data.ShiftRepository
import com.dnavarro.poskmp.domain.model.CashierShift

class OpenShiftUseCase(
    private val shiftRepository: ShiftRepository,
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(cashierId: String, pin: String, initialCash: Double): Result<CashierShift> {
        val cashier = shiftRepository.getCashierById(cashierId)
            ?: return Result.failure(IllegalArgumentException("Cajero no encontrado."))

        val currentDeviceId = settingsRepository.getOrCreateDeviceId()
        if (cashier.deviceId != null && cashier.deviceId != currentDeviceId) {
            return Result.failure(IllegalStateException("Este cajero pertenece a otro dispositivo."))
        }

        if (cashier.pin != pin.trim()) {
            return Result.failure(IllegalArgumentException("PIN de cajero incorrecto."))
        }
        if (initialCash < 0.0) {
            return Result.failure(IllegalArgumentException("El fondo de caja no puede ser negativo."))
        }
        return shiftRepository.openShift(cashierId, initialCash)
    }
}
