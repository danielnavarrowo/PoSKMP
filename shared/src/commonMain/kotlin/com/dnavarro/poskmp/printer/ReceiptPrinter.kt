package com.dnavarro.poskmp.printer

import com.dnavarro.poskmp.domain.model.ReceiptDocument

interface ReceiptPrinter {
    suspend fun print(document: ReceiptDocument): Result<Unit>
}

expect fun createReceiptPrinter(): ReceiptPrinter
