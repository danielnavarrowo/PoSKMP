package com.dnavarro.poskmp.domain.model

enum class PrinterType(
    val widthMillimeters: Int,
    val charactersPerLine: Int
) {
    THERMAL_80MM(widthMillimeters = 80, charactersPerLine = 48),
    A4(widthMillimeters = 210, charactersPerLine = 80),
    LETTER(widthMillimeters = 216, charactersPerLine = 82)
}

enum class ReceiptFont {
    MONOSPACE,
    SANS_SERIF,
    SERIF;

    val defaultFamilyName: String
        get() = when (this) {
            MONOSPACE -> "Monospaced"
            SANS_SERIF -> "SansSerif"
            SERIF -> "Serif"
        }
}

const val DEFAULT_RECEIPT_FONT_FAMILY = "Monospaced"

data class ReceiptPrinterOption(
    val id: String,
    val name: String,
    val isDefault: Boolean = false
)

data class ReceiptSettings(
    val storeName: String = "",
    val storeAddress: String = "",
    val storePhone: String = "",
    val printerType: PrinterType = PrinterType.THERMAL_80MM,
    val printerId: String? = null,
    val font: ReceiptFont = ReceiptFont.MONOSPACE,
    val fontFamily: String = DEFAULT_RECEIPT_FONT_FAMILY,
    val fontSize: Int = 12,
    val feedLines: Int = 3,
    val footerMessage: String = ""
)

data class ReceiptItem(
    val name: String,
    val quantity: Double,
    val unitPrice: Double,
    val subtotal: Double,
    val isWeightBased: Boolean = false
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
    val printerType: PrinterType,
    val printerId: String? = null,
    val font: ReceiptFont,
    val fontFamily: String = DEFAULT_RECEIPT_FONT_FAMILY,
    val fontSize: Int,
    val feedLines: Int
)
