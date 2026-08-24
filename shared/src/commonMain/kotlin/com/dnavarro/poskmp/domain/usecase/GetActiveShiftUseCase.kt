package com.dnavarro.poskmp.domain.usecase

import com.dnavarro.poskmp.data.ShiftRepository
import com.dnavarro.poskmp.domain.model.CashierShift
import kotlinx.coroutines.flow.Flow

class GetActiveShiftUseCase(
    private val shiftRepository: ShiftRepository
) {
    operator fun invoke(): Flow<CashierShift?> = shiftRepository.activeShiftFlow
}
