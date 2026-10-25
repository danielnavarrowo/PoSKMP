package com.dnavarro.poskmp.printer

import com.dnavarro.poskmp.domain.model.ReceiptAlignment
import com.dnavarro.poskmp.domain.model.ReceiptDocument

/**
 * Encodes a [ReceiptDocument] into native ESC/POS command bytes for direct thermal printing.
 */
object EscPosReceiptEncoder {

    // ESC/POS Command Constants
    private val CMD_INIT = byteArrayOf(0x1B, 0x40) // ESC @
    private val CMD_CANCEL_CHINESE_MODE = byteArrayOf(0x1C, 0x2E) // FS . (Disable 2-byte GBK Chinese mode)
    private val CMD_CODEPAGE_CP1252 = byteArrayOf(0x1B, 0x74, 0x10) // ESC t 16 (WPC1252 / Latin-1)
    private val CMD_FONT_A = byteArrayOf(0x1B, 0x4D, 0x00) // ESC M 0 (Standard 12x24 font)
    private val CMD_FONT_B = byteArrayOf(0x1B, 0x4D, 0x01) // ESC M 1 (Condensed 9x17 font)
    private val CMD_ALIGN_LEFT = byteArrayOf(0x1B, 0x61, 0x00) // ESC a 0
    private val CMD_ALIGN_CENTER = byteArrayOf(0x1B, 0x61, 0x01) // ESC a 1
    private val CMD_ALIGN_RIGHT = byteArrayOf(0x1B, 0x61, 0x02) // ESC a 2
    private val CMD_EMPHASIS_ON = byteArrayOf(0x1B, 0x45, 0x01) // ESC E 1
    private val CMD_EMPHASIS_OFF = byteArrayOf(0x1B, 0x45, 0x00) // ESC E 0
    private val CMD_RESET_SCALE = byteArrayOf(0x1D, 0x21, 0x00) // GS ! 0 (1x1 normal scale)
    private val CMD_DEFAULT_LINE_SPACING = byteArrayOf(0x1B, 0x32) // ESC 2 (Default line spacing)
    private val CMD_CUT_PAPER = byteArrayOf(0x1D, 0x56, 0x41, 0x00) // GS V A 0 (Feed and Cut)
    private val CMD_DRAWER_KICK = byteArrayOf(
        0x1B, 0x70, 0x00, 0x19, 0xFA.toByte() // ESC p 0 25 250 (Pin 2 standard drawer kick)
    )

    /**
     * Returns raw ESC/POS byte commands to kick open the cash drawer.
     */
    fun encodeDrawerKick(): ByteArray = CMD_DRAWER_KICK.copyOf()

    /**
     * Converts a [ReceiptDocument] into a raw ESC/POS [ByteArray].
     *
     * @param document The receipt document containing formatted lines and settings.
     * @param openDrawer If true, prepends a cash drawer kick pulse command. Defaults to document.openCashDrawer.
     * @param cutPaper If true, appends feed and cut commands at the end.
     */
    fun encode(
        document: ReceiptDocument,
        openDrawer: Boolean = document.openCashDrawer,
        cutPaper: Boolean = document.printerId?.trim()?.startsWith("bt://", ignoreCase = true) != true
    ): ByteArray {
        val output = mutableListOf<Byte>()

        fun append(bytes: ByteArray) {
            for (b in bytes) output.add(b)
        }

        // 1. Initialize printer, disable Chinese GBK mode, & set code page
        append(CMD_INIT)
        append(CMD_CANCEL_CHINESE_MODE)
        append(CMD_CODEPAGE_CP1252)

        // 2. Select Font, Scaling (GS ! n), and Line Spacing (ESC 3 n)
        // Font A base: 12x24 dots (48 cols on 80mm)
        // Font B base: 9x17 dots (64 cols on 80mm)
        // Font B @ 2x2: 18x34 dots (32 cols on 80mm)
        // Font A @ 2x2: 24x48 dots (24 cols on 80mm)
        // Note: ESC @ sets default line spacing to ~30 dots, which is too tight for 24-dot and 34-dot tall fonts.
        // We set explicit line spacing (ESC 3 n) with ~40% extra vertical breathing room per line.
        val (fontCmd, scaleByte, lineSpacingDots) = when {
            document.fontSize <= 9 -> Triple(CMD_FONT_B, 0x00.toByte(), 28) // 17-dot tall font -> 28-dot line height
            document.fontSize in 10..13 -> Triple(CMD_FONT_A, 0x00.toByte(), 38) // 24-dot tall font -> 38-dot line height
            document.fontSize in 14..18 -> Triple(CMD_FONT_B, 0x11.toByte(), 50) // 34-dot tall font -> 50-dot line height
            document.fontSize in 19..24 -> Triple(CMD_FONT_A, 0x11.toByte(), 66) // 48-dot tall font -> 66-dot line height
            document.fontSize in 25..29 -> Triple(CMD_FONT_A, 0x22.toByte(), 96) // 72-dot tall font -> 96-dot line height
            else -> Triple(CMD_FONT_A, 0x33.toByte(), 124)
        }

        append(fontCmd)
        append(byteArrayOf(0x1D, 0x21, scaleByte))
        append(byteArrayOf(0x1B, 0x20, 0x00)) // ESC SP 0 (normal character spacing)
        append(byteArrayOf(0x1B, 0x33, lineSpacingDots.toByte())) // ESC 3 n (line spacing in dots)

        // 4. Optional cash drawer kick
        if (openDrawer) {
            append(CMD_DRAWER_KICK)
        }

        // 5. Print lines
        var currentAlignment: ReceiptAlignment? = null
        var currentEmphasized: Boolean? = null

        for ((text, alignment, emphasized) in document.lines) {
            // Apply alignment if changed
            if (alignment != currentAlignment) {
                currentAlignment = alignment
                when (alignment) {
                    ReceiptAlignment.LEFT -> append(CMD_ALIGN_LEFT)
                    ReceiptAlignment.CENTER -> append(CMD_ALIGN_CENTER)
                    ReceiptAlignment.RIGHT -> append(CMD_ALIGN_RIGHT)
                }
            }

            // Apply emphasis if changed
            if (emphasized != currentEmphasized) {
                currentEmphasized = emphasized
                if (emphasized) {
                    append(CMD_EMPHASIS_ON)
                } else {
                    append(CMD_EMPHASIS_OFF)
                }
            }

            // Write line text & newline
            if (text.isNotEmpty()) {
                append(encodeTextToSafeAscii(text))
            }
            output.add(0x0A.toByte()) // LF
        }

        // Reset formatting
        append(CMD_ALIGN_LEFT)
        append(CMD_EMPHASIS_OFF)
        append(CMD_RESET_SCALE)
        append(CMD_FONT_A)
        append(CMD_DEFAULT_LINE_SPACING)
        append(byteArrayOf(0x1B, 0x20, 0x00)) // ESC SP 0

        // 6. Feed lines for comfortable tear-off margin
        val feedCount = document.feedLines.coerceIn(0, 15)
        if (feedCount > 0) {
            append(byteArrayOf(0x1B, 0x64, feedCount.toByte())) // ESC d n
        }

        // 7. Cut paper (skipped on portable Bluetooth printers without an auto-cutter blade)
        if (cutPaper) {
            append(CMD_CUT_PAPER)
        }

        return output.toByteArray()
    }

    /**
     * Returns the standard 203 DPI (8 dots/mm) printable width in dots (always a multiple of 8)
     * for a given paper width in millimeters.
     * - 58mm paper -> 384 dots (48 bytes/row)
     * - 80mm paper -> 576 dots (72 bytes/row)
     */
    fun printableWidthDots(paperWidthMm: Int): Int {
        if (paperWidthMm <= 60) return 384
        val printableMm = (paperWidthMm - 8).coerceIn(48, 72)
        return printableMm * 8
    }

    /**
     * Encodes a rendered 1-bit monochrome bitmap into chunked ESC/POS `GS v 0` raster bit-image bands.
     * Slicing into 24-dot horizontal bands ensures compatibility with portable Bluetooth and USB printers
     * that have small internal receive buffers.
     */
    fun encodeRasterBitmap(
        widthDots: Int,
        heightDots: Int,
        openDrawer: Boolean,
        feedLines: Int,
        cutPaper: Boolean,
        isPixelBlack: (x: Int, y: Int) -> Boolean
    ): ByteArray {
        val bytesPerRow = (widthDots + 7) / 8
        val bandHeight = 24
        val output = ArrayList<Byte>(heightDots * bytesPerRow + 128)

        fun append(bytes: ByteArray) {
            for (b in bytes) output.add(b)
        }

        // 1. Initialize printer & center alignment
        append(CMD_INIT)
        append(CMD_ALIGN_LEFT)

        // 2. Optional cash drawer kick
        if (openDrawer) {
            append(CMD_DRAWER_KICK)
        }

        // 3. Emit horizontal bands using GS v 0 (0x1D 0x76 0x30 0x00 xL xH yL yH d1...dk)
        var yStart = 0
        while (yStart < heightDots) {
            val currentBandHeight = minOf(bandHeight, heightDots - yStart)
            output.add(0x1D.toByte())
            output.add(0x76.toByte())
            output.add(0x30.toByte())
            output.add(0x00.toByte())
            output.add((bytesPerRow and 0xFF).toByte())
            output.add(((bytesPerRow shr 8) and 0xFF).toByte())
            output.add((currentBandHeight and 0xFF).toByte())
            output.add(((currentBandHeight shr 8) and 0xFF).toByte())

            for (row in 0 until currentBandHeight) {
                val y = yStart + row
                for (byteIndex in 0 until bytesPerRow) {
                    var slice = 0
                    val baseX = byteIndex * 8
                    for (bit in 0..7) {
                        val x = baseX + bit
                        if (x < widthDots && isPixelBlack(x, y)) {
                            slice = slice or (1 shl (7 - bit))
                        }
                    }
                    output.add(slice.toByte())
                }
            }
            yStart += currentBandHeight
        }

        // 4. Feed lines for comfortable tear-off margin
        val feedCount = feedLines.coerceIn(0, 15)
        if (feedCount > 0) {
            append(byteArrayOf(0x1B, 0x64, feedCount.toByte())) // ESC d n
        }

        // 5. Cut paper if supported
        if (cutPaper) {
            append(CMD_CUT_PAPER)
        }

        return output.toByteArray()
    }

    /**
     * Normalizes Spanish accented characters and symbols into clean 7-bit ASCII (0x20..0x7E)
     * so Chinese thermal printers with hardware-locked GBK ROMs never interpret high bytes (0x80..0xFF)
     * as 2-byte Chinese characters or shift column alignment.
     */
    fun encodeTextToSafeAscii(text: String): ByteArray {
        val sb = StringBuilder(text.length)
        for (i in text.indices) {
            when (val c = text[i]) {
                in '\u0020'..'\u007E' -> sb.append(c)
                'á', 'à', 'ä', 'â', 'ã' -> sb.append('a')
                'é', 'è', 'ë', 'ê' -> sb.append('e')
                'í', 'ì', 'ï', 'î' -> sb.append('i')
                'ó', 'ò', 'ö', 'ô', 'õ' -> sb.append('o')
                'ú', 'ù', 'ü', 'û' -> sb.append('u')
                'ñ' -> sb.append('n')
                'Á', 'À', 'Ä', 'Â', 'Ã' -> sb.append('A')
                'É', 'È', 'Ë', 'Ê' -> sb.append('E')
                'Í', 'Ì', 'Ï', 'Î' -> sb.append('I')
                'Ó', 'Ò', 'Ö', 'Ô', 'Õ' -> sb.append('O')
                'Ú', 'Ù', 'Ü', 'Û' -> sb.append('U')
                'Ñ' -> sb.append('N')
                '¿', '¡' -> {} // Omit inverted punctuation so no stray symbols print
                '°' -> sb.append('.')
                '€' -> sb.append("EUR")
                '\t' -> sb.append(' ')
                else -> if (c.code < 0x20) sb.append(' ') else sb.append('?')
            }
        }
        val ascii = sb.toString()
        return ByteArray(ascii.length) { idx -> ascii[idx].code.toByte() }
    }
}
