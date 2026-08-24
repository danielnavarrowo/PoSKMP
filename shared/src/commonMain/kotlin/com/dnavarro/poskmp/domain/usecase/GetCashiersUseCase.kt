package com.dnavarro.poskmp.domain.usecase

import com.dnavarro.poskmp.data.ShiftRepository
import com.dnavarro.poskmp.domain.model.Cashier
import kotlinx.coroutines.flow.Flow

class GetCashiersUseCase(
    private val shiftRepository: ShiftRepository
) {
    operator fun invoke(): Flow<List<Cashier>> = shiftRepository.activeCashiersFlow
}
