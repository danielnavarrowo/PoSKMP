package com.dnavarro.poskmp.domain.model

enum class PrinterType(
) {
    THERMAL_80MM
}

const val PRINTER_SYSTEM_DIALOG_ID = "system_dialog"
const val DEFAULT_PAPER_WIDTH_MM = 80
const val MIN_PAPER_WIDTH_MM = 55
const val MAX_PAPER_WIDTH_MM = 105

data class ReceiptPrinterOption(
    val id: String,
    val name: String,
    val isDefault: Boolean = false
)

data class ReceiptSettings(
    val storeName: String = "",
    val storeAddress: String = "",
    val storePhone: String = "",
    val transferClabe: String = "",
    val transferBeneficiary: String = "",
    val paperWidthMm: Int = DEFAULT_PAPER_WIDTH_MM,
    val printerType: PrinterType = PrinterType.THERMAL_80MM,
    val printerId: String? = null,
    val fontSize: Int = 12,
    val feedLines: Int = 3,
    val footerMessage: String = ""
)

data class ReceiptItem(
    val name: String,
    val quantity: Double,
    val unitPrice: Double,
    val subtotal: Double,
    val isWeightBased: Boolean = false,
    val originalUnitPrice: Double = unitPrice,
    val isWholesale: Boolean = false
)

enum class ReceiptAlignment {
    LEFT,
    CENTER,
    RIGHT
}

data class ReceiptLine(
    val text: String,
    val alignment: ReceiptAlignment = ReceiptAlignment.LEFT,
    val emphasized: Boolean = false
)

data class ReceiptDocument(
    val folio: Long,
    val createdAt: Long,
    val lines: List<ReceiptLine>,
    val paperWidthMm: Int = DEFAULT_PAPER_WIDTH_MM,
    val printerType: PrinterType = PrinterType.THERMAL_80MM,
    val printerId: String? = null,
    val fontSize: Int,
    val feedLines: Int
)
