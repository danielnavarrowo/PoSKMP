package com.dnavarro.poskmp.domain.usecase

import com.dnavarro.poskmp.data.SettingsRepository
import com.dnavarro.poskmp.printer.ReceiptPrinter
import kotlinx.coroutines.flow.first

class OpenCashDrawerUseCase(
    private val receiptPrinter: ReceiptPrinter,
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        val settings = settingsRepository.receiptSettingsFlow.first()
        return receiptPrinter.openCashDrawer(settings.printerId)
    }
}
