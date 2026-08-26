package com.dnavarro.poskmp.util

fun String.formatPrice(): String {
    return try {
        val parts = this.split(".")
        if (parts.size == 1) {
            "${parts[0]}.00"
        } else {
            val decimals = parts[1]
            if (decimals.length >= 2) {
                "${parts[0]}.${decimals.substring(0, 2)}"
            } else {
                "${parts[0]}.${decimals}0"
            }
        }
    } catch (_: Exception) {
        this
    }
}

fun Double.formatQuantity(isWeight: Boolean): String {
    return if (isWeight) {
        val parts = this.toString().split(".")
        if (parts.size == 1) {
            "${parts[0]}.000"
        } else {
            val decimals = parts[1]
            if (decimals.length >= 3) {
                "${parts[0]}.${decimals.substring(0, 3)}"
            } else {
                "${parts[0]}.${decimals.padEnd(3, '0')}"
            }
        }
    } else {
        this.toInt().toString()
    }
}

fun parseCsvLine(line: String): List<String> {
    val result = mutableListOf<String>()
    var curVal = StringBuilder()
    var inQuotes = false
    var i = 0
    while (i < line.length) {
        val ch = line[i]
        if (inQuotes) {
            if (ch == '\"') {
                if (i + 1 < line.length && line[i + 1] == '\"') {
                    curVal.append('\"')
                    i++
                } else {
                    inQuotes = false
                }
            } else {
                curVal.append(ch)
            }
        } else {
            when (ch) {
                '\"' -> {
                    inQuotes = true
                }
                ',' -> {
                    result.add(curVal.toString().trim())
                    curVal = StringBuilder()
                }
                else -> {
                    curVal.append(ch)
                }
            }
        }
        i++
    }
    result.add(curVal.toString().trim())
    return result
}

private val SPANISH_DAYS_OF_WEEK = arrayOf(
    "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo"
)

private val SPANISH_MONTHS = arrayOf(
    "enero", "febrero", "marzo", "abril", "mayo", "junio",
    "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"
)

fun getSpanishDayOfWeek(dayOfWeek: java.time.DayOfWeek): String {
    val idx = dayOfWeek.value - 1
    return if (idx in SPANISH_DAYS_OF_WEEK.indices) SPANISH_DAYS_OF_WEEK[idx] else dayOfWeek.name
}

fun getSpanishMonthName(monthValue: Int): String {
    val idx = monthValue - 1
    return if (idx in SPANISH_MONTHS.indices) SPANISH_MONTHS[idx] else monthValue.toString()
}

fun formatCurrentDate(dateTime: java.time.LocalDateTime = java.time.LocalDateTime.now()): String {
    val dayOfWeek = getSpanishDayOfWeek(dateTime.dayOfWeek)
    val dayOfMonth = dateTime.dayOfMonth
    val month = getSpanishMonthName(dateTime.monthValue)
    return "$dayOfWeek, $dayOfMonth de $month"
}

fun formatCurrentTime(dateTime: java.time.LocalDateTime = java.time.LocalDateTime.now()): String {
    val hour = if (dateTime.hour % 12 == 0) 12 else dateTime.hour % 12
    val minute = dateTime.minute.toString().padStart(2, '0')
    val amPm = if (dateTime.hour < 12) "a. m." else "p. m."
    return "$hour:$minute $amPm"
}

fun formatTimeOnly(epochMillis: Long): String {
    val instant = java.time.Instant.ofEpochMilli(epochMillis)
    val dateTime = java.time.LocalDateTime.ofInstant(instant, java.time.ZoneId.systemDefault())
    val hour = if (dateTime.hour % 12 == 0) 12 else dateTime.hour % 12
    val minute = dateTime.minute.toString().padStart(2, '0')
    val amPm = if (dateTime.hour < 12) "a. m." else "p. m."
    return "$hour:$minute $amPm"
}

fun formatEpochMillisToDateTime(epochMillis: Long): String {
    val instant = java.time.Instant.ofEpochMilli(epochMillis)
    val dateTime = java.time.LocalDateTime.ofInstant(instant, java.time.ZoneId.systemDefault())
    val day = dateTime.dayOfMonth.toString().padStart(2, '0')
    val month = dateTime.monthValue.toString().padStart(2, '0')
    val year = dateTime.year
    val hour = if (dateTime.hour % 12 == 0) 12 else dateTime.hour % 12
    val minute = dateTime.minute.toString().padStart(2, '0')
    val amPm = if (dateTime.hour < 12) "a. m." else "p. m."
    return "$day/$month/$year - $hour:$minute $amPm"
}

fun formatShiftInterval(startTime: Long, endTime: Long?, isClosed: Boolean, cashierName: String): String {
    val startInstant = java.time.Instant.ofEpochMilli(startTime)
    val startDt = java.time.LocalDateTime.ofInstant(startInstant, java.time.ZoneId.systemDefault())
    val startDay = startDt.dayOfMonth.toString().padStart(2, '0')
    val startMonth = startDt.monthValue.toString().padStart(2, '0')
    val startTimeStr = formatTimeOnly(startTime)

    val endTimeStr = if (isClosed && endTime != null) {
        formatTimeOnly(endTime)
    } else {
        "Abierto"
    }

    return "$cashierName • $startDay/$startMonth ($startTimeStr - $endTimeStr)"
}

/**
 * Parses a JSON array or comma-separated barcode string into a list of barcode strings.
 */
fun parseBarcodes(codigos: String?): List<String> {
    if (codigos.isNullOrBlank() || codigos == "[]") return emptyList()
    return try {
        codigos.replace("[", "")
            .replace("]", "")
            .replace("\"", "")
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    } catch (_: Exception) {
        emptyList()
    }
}

/**
 * Formats a raw barcode string (JSON array or raw) into a human-readable comma-separated string.
 *
 * @param emptyFallback String to return when no barcodes exist (e.g. "N/A" for display, "" for form fields).
 */
fun String?.formatBarcodesForDisplay(emptyFallback: String = "N/A"): String {
    val list = parseBarcodes(this)
    return if (list.isEmpty()) emptyFallback else list.joinToString(", ")
}

/**
 * Formats product barcodes into a human-readable comma-separated string.
 */
fun com.dnavarro.poskmp.db.Products.formatBarcodesForDisplay(emptyFallback: String = "N/A"): String {
    return this.codigos.formatBarcodesForDisplay(emptyFallback)
}

/**
 * Returns the parsed list of barcodes for this product.
 */
fun com.dnavarro.poskmp.db.Products.parseBarcodes(): List<String> {
    return parseBarcodes(this.codigos)
}

/**
 * Encodes a list of barcode strings into a JSON array string suitable for DB storage (e.g., `["123","456"]`).
 */
fun List<String>.encodeToJsonBarcodes(): String {
    val cleaned = this.map { it.trim().replace("\"", "") }.filter { it.isNotEmpty() }
    return if (cleaned.isEmpty()) "[]" else cleaned.joinToString(separator = "\",\"", prefix = "[\"", postfix = "\"]")
}

/**
 * Normalizes a barcode string by trimming leading zeroes while preserving single "0" barcodes.
 * Examples:
 * - "0752" -> "752"
 * - "00123" -> "123"
 * - "752" -> "752"
 * - "0" -> "0"
 * - "00" -> "0"
 */
fun normalizeBarcode(barcode: String?): String {
    if (barcode.isNullOrBlank()) return ""
    val trimmed = barcode.trim()
    val stripped = trimmed.trimStart('0')
    return stripped.ifEmpty { "0" }
}

/**
 * Checks whether two barcodes match, supporting both exact match and leading-zero normalized match.
 */
fun isBarcodeMatch(code1: String?, code2: String?): Boolean {
    if (code1.isNullOrBlank() || code2.isNullOrBlank()) return false
    val t1 = code1.trim()
    val t2 = code2.trim()
    if (t1.equals(t2, ignoreCase = true)) return true
    val n1 = normalizeBarcode(t1)
    val n2 = normalizeBarcode(t2)
    return n1.isNotEmpty() && n1.equals(n2, ignoreCase = true)
}

/**
 * Checks if a target barcode matches any barcode in a given list, using hybrid exact & normalized comparison.
 */
fun List<String>?.matchesBarcode(targetBarcode: String?): Boolean {
    return !(this.isNullOrEmpty() || targetBarcode.isNullOrBlank()) && this.any { code -> isBarcodeMatch(code, targetBarcode) }
}

