package com.dnavarro.poskmp.domain.usecase

import com.dnavarro.poskmp.domain.model.ReceiptDocument
import com.dnavarro.poskmp.printer.ReceiptPrinter

class PrintReceiptUseCase(
    private val receiptPrinter: ReceiptPrinter
) {
    suspend operator fun invoke(document: ReceiptDocument): Result<Unit> = receiptPrinter.print(document)
}
