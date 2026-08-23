package com.dnavarro.poskmp.printer

import com.dnavarro.poskmp.domain.model.ReceiptAlignment
import com.dnavarro.poskmp.domain.model.ReceiptDocument
import com.dnavarro.poskmp.domain.model.ReceiptFont

/**
 * Encodes a [ReceiptDocument] into native ESC/POS command bytes for direct thermal printing.
 */
object EscPosReceiptEncoder {

    // ESC/POS Command Constants
    private val CMD_INIT = byteArrayOf(0x1B, 0x40) // ESC @
    private val CMD_CODEPAGE_CP1252 = byteArrayOf(0x1B, 0x74, 0x10) // ESC t 16 (WPC1252 / Latin-1)
    private val CMD_FONT_A = byteArrayOf(0x1B, 0x4D, 0x00) // ESC M 0 (Standard 12x24 font)
    private val CMD_FONT_B = byteArrayOf(0x1B, 0x4D, 0x01) // ESC M 1 (Condensed / Sans 9x17 font)
    private val CMD_ALIGN_LEFT = byteArrayOf(0x1B, 0x61, 0x00) // ESC a 0
    private val CMD_ALIGN_CENTER = byteArrayOf(0x1B, 0x61, 0x01) // ESC a 1
    private val CMD_ALIGN_RIGHT = byteArrayOf(0x1B, 0x61, 0x02) // ESC a 2
    private val CMD_EMPHASIS_ON = byteArrayOf(0x1B, 0x45, 0x01) // ESC E 1
    private val CMD_EMPHASIS_OFF = byteArrayOf(0x1B, 0x45, 0x00) // ESC E 0
    private val CMD_RESET_SCALE = byteArrayOf(0x1D, 0x21, 0x00) // GS ! 0 (1x1 normal scale)
    private val CMD_CUT_PAPER = byteArrayOf(0x1D, 0x56, 0x41, 0x00) // GS V A 0 (Feed and Cut)
    private val CMD_DRAWER_KICK = byteArrayOf(0x1B, 0x70, 0x00, 0x19, 0xFA.toByte()) // ESC p 0 25 250

    /**
     * Converts a [ReceiptDocument] into a raw ESC/POS [ByteArray].
     *
     * @param document The receipt document containing formatted lines and settings.
     * @param openDrawer If true, prepends a cash drawer kick pulse command.
     * @param cutPaper If true, appends feed and cut commands at the end.
     */
    fun encode(
        document: ReceiptDocument,
        openDrawer: Boolean = false,
        cutPaper: Boolean = true
    ): ByteArray {
        val output = mutableListOf<Byte>()

        fun append(bytes: ByteArray) {
            for (b in bytes) output.add(b)
        }

        // 1. Initialize printer & set code page to CP1252 (Latin-1)
        append(CMD_INIT)
        append(CMD_CODEPAGE_CP1252)

        // 2. Select Font (Font A = Standard/Monospace/Serif, Font B = Condensed/Sans-Serif)
        val isSansSerif = document.fontFamily.contains("sans", ignoreCase = true) ||
                document.fontFamily.contains("arial", ignoreCase = true) ||
                document.fontFamily.contains("helvetica", ignoreCase = true) ||
                document.fontFamily.contains("roboto", ignoreCase = true) ||
                document.font == ReceiptFont.SANS_SERIF ||
                document.fontSize <= 9

        append(if (isSansSerif) CMD_FONT_B else CMD_FONT_A)

        // 3. Select Character Size / Scaling (GS ! n)
        // Bits 0-3: Width multiplier (0 = 1x, 1 = 2x, etc.)
        // Bits 4-7: Height multiplier (0 = 1x, 1 = 2x, etc.)
        val scaleByte: Byte = when {
            document.fontSize <= 13 -> 0x00.toByte() // 1x1 normal
            document.fontSize in 14..17 -> 0x01.toByte() // 1x width, 2x height (taller text)
            document.fontSize in 18..23 -> 0x11.toByte() // 2x width, 2x height
            document.fontSize in 24..29 -> 0x22.toByte() // 3x width, 3x height
            else -> 0x33.toByte() // 4x width, 4x height
        }
        append(byteArrayOf(0x1D, 0x21, scaleByte))

        // 4. Optional cash drawer kick
        if (openDrawer) {
            append(CMD_DRAWER_KICK)
        }

        // 5. Print lines
        var currentAlignment: ReceiptAlignment? = null
        var currentEmphasized: Boolean? = null

        for (line in document.lines) {
            // Apply alignment if changed
            if (line.alignment != currentAlignment) {
                currentAlignment = line.alignment
                when (line.alignment) {
                    ReceiptAlignment.LEFT -> append(CMD_ALIGN_LEFT)
                    ReceiptAlignment.CENTER -> append(CMD_ALIGN_CENTER)
                    ReceiptAlignment.RIGHT -> append(CMD_ALIGN_RIGHT)
                }
            }

            // Apply emphasis if changed
            if (line.emphasized != currentEmphasized) {
                currentEmphasized = line.emphasized
                if (line.emphasized) {
                    append(CMD_EMPHASIS_ON)
                } else {
                    append(CMD_EMPHASIS_OFF)
                }
            }

            // Write line text & newline
            if (line.text.isNotEmpty()) {
                append(encodeTextToCp1252(line.text))
            }
            output.add(0x0A.toByte()) // LF
        }

        // Reset formatting
        append(CMD_ALIGN_LEFT)
        append(CMD_EMPHASIS_OFF)
        append(CMD_RESET_SCALE)
        append(CMD_FONT_A)

        // 6. Feed lines
        val feedCount = document.feedLines.coerceIn(0, 15)
        if (feedCount > 0) {
            append(byteArrayOf(0x1B, 0x64, feedCount.toByte())) // ESC d n
        }

        // 7. Cut paper
        if (cutPaper) {
            append(CMD_CUT_PAPER)
        }

        return output.toByteArray()
    }

    /**
     * Maps characters to CodePage 1252 (Western European Latin-1) for thermal printer compatibility.
     */
    fun encodeTextToCp1252(text: String): ByteArray {
        val bytes = ByteArray(text.length)
        for (i in text.indices) {
            val c = text[i]
            bytes[i] = when (c) {
                in '\u0000'..'\u007F' -> c.code.toByte()
                'á' -> 0xE1.toByte()
                'é' -> 0xE9.toByte()
                'í' -> 0xED.toByte()
                'ó' -> 0xF3.toByte()
                'ú' -> 0xFA.toByte()
                'ñ' -> 0xF1.toByte()
                'Á' -> 0xC1.toByte()
                'É' -> 0xC9.toByte()
                'Í' -> 0xCD.toByte()
                'Ó' -> 0xD3.toByte()
                'Ú' -> 0xDA.toByte()
                'Ñ' -> 0xD1.toByte()
                '¿' -> 0xBF.toByte()
                '¡' -> 0xA1.toByte()
                'ü' -> 0xFC.toByte()
                'Ü' -> 0xDC.toByte()
                '°' -> 0xB0.toByte()
                '€' -> 0x80.toByte()
                else -> if (c.code in 0x80..0xFF) c.code.toByte() else '?'.code.toByte()
            }
        }
        return bytes
    }
}
