package com.dnavarro.poskmp.printer

import androidx.compose.ui.text.font.FontFamily
import com.dnavarro.poskmp.domain.model.ReceiptPrinterOption

expect fun getReceiptPrinterOptions(): List<ReceiptPrinterOption>

expect fun getSystemReceiptFontFamilies(): List<String>

expect fun receiptFontFamily(name: String): FontFamily
