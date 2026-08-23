package com.dnavarro.poskmp.printer

import com.dnavarro.poskmp.domain.model.ReceiptPrinterOption

expect suspend fun getReceiptPrinterOptions(): List<ReceiptPrinterOption>
