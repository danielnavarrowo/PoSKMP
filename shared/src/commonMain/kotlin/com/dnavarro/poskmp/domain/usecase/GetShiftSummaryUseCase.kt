package com.dnavarro.poskmp.domain.usecase

import com.dnavarro.poskmp.data.ShiftRepository
import com.dnavarro.poskmp.domain.model.ShiftSummary

class GetShiftSummaryUseCase(
    private val shiftRepository: ShiftRepository
) {
    suspend operator fun invoke(shiftId: String): Result<ShiftSummary> =
        shiftRepository.getShiftSummary(shiftId)
}
