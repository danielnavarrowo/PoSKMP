package com.dnavarro.poskmp.util

/**
 * Formats an integer digit string with comma thousands separators (e.g. "1234567" -> "1,234,567").
 * Preserves leading negative sign if present.
 */
fun formatWithThousandsSeparator(intPart: String): String {
    val isNegative = intPart.startsWith('-')
    val digits = if (isNegative) intPart.substring(1) else intPart
    if (digits.isEmpty() || !digits.all { it.isDigit() }) return intPart
    if (digits.length <= 3) return intPart

    val sb = StringBuilder()
    if (isNegative) sb.append('-')
    val firstGroupLen = if (digits.length % 3 == 0) 3 else digits.length % 3
    sb.append(digits, 0, firstGroupLen)
    var i = firstGroupLen
    while (i < digits.length) {
        sb.append(',')
        sb.append(digits, i, i + 3)
        i += 3
    }
    return sb.toString()
}

/**
 * Formats a Double into a price string with comma thousands separators and 2 decimal places
 * according to the Spanish/Mexican format (e.g. 123456.78 -> "123,456.78").
 */
fun Double.formatPrice(): String {
    if (this.isNaN() || this.isInfinite()) return "0.00"
    val isNegative = this < 0.0
    val absVal = kotlin.math.abs(this)
    val totalCents = kotlin.math.round(absVal * 100.0).toLong()
    val longPart = totalCents / 100L
    val decPart = totalCents % 100L
    val formattedInt = formatWithThousandsSeparator(longPart.toString())
    val formattedDec = decPart.toString().padStart(2, '0')
    val sign = if (isNegative && totalCents > 0L) "-" else ""
    return "$sign$formattedInt.$formattedDec"
}

fun Float.formatPrice(): String = this.toDouble().formatPrice()
fun Long.formatPrice(): String = this.toDouble().formatPrice()
fun Int.formatPrice(): String = this.toDouble().formatPrice()

/**
 * Formats a numeric price string with comma thousands separators and 2 decimal places
 * according to the Spanish/Mexican format (e.g. "123456.78" -> "123,456.78").
 */
fun String.formatPrice(): String {
    return try {
        val trimmed = this.trim()
        if (trimmed.isEmpty()) return this
        val normalized = trimmed.replace(",", "")
        val doubleVal = normalized.toDoubleOrNull()
        if (doubleVal != null) {
            doubleVal.formatPrice()
        } else {
            val parts = trimmed.split(".")
            val formattedInt = formatWithThousandsSeparator(parts[0])
            if (parts.size == 1) {
                "$formattedInt.00"
            } else {
                val decimals = parts[1]
                if (decimals.length >= 2) {
                    "$formattedInt.${decimals.substring(0, 2)}"
                } else {
                    "$formattedInt.${decimals}0"
                }
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

/**
 * Normalizes a string for diacritics/accent-insensitive search (e.g. "Piñata" -> "pinata", "Café" -> "cafe").
 * Maps accented vowels to their plain versions, and 'ñ' to 'n' for fuzzy matching.
 */
fun String.normalizeForSearch(): String {
    val sb = StringBuilder(this.length)
    for (ch in this.lowercase()) {
        val replacement = when (ch) {
            'á', 'à', 'ä', 'â' -> 'a'
            'é', 'è', 'ë', 'ê' -> 'e'
            'í', 'ì', 'ï', 'î' -> 'i'
            'ó', 'ò', 'ö', 'ô' -> 'o'
            'ú', 'ù', 'ü', 'û' -> 'u'
            'ñ' -> 'n'
            else -> ch
        }
        sb.append(replacement)
    }
    return sb.toString()
}

/**
 * Checks if this string contains [query], ignoring case (including Unicode like ñ/Ñ) and optional accents/diacritics.
 */
fun String?.matchesSearchQuery(query: String): Boolean {
    if (this.isNullOrBlank() || query.isBlank()) return false
    val trimmedQuery = query.trim()
    if (trimmedQuery.isEmpty()) return false
    // 1. Direct Unicode case-insensitive match (e.g., "PIÑATA" matches "Piñata")
    if (this.contains(trimmedQuery, ignoreCase = true)) return true
    // 2. Diacritics-insensitive match (e.g., "pinata" matches "Piñata", "cafe" matches "Café")
    return this.normalizeForSearch().contains(trimmedQuery.normalizeForSearch())
}

/**
 * Evaluates whether a product matches a search query across its name, category, barcodes, and ID.
 * Fully case-insensitive (including Spanish ñ/Ñ) and accent-tolerant.
 */
fun com.dnavarro.poskmp.db.Products.matchesSearch(query: String, normalizedBarcode: String = ""): Boolean {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) return true

    // 1. Direct match on full query
    if (this.nombre.matchesSearchQuery(trimmed)) return true
    if (!this.categoria.isNullOrBlank() && this.categoria.matchesSearchQuery(trimmed)) return true
    if (this.codigos.contains(trimmed, ignoreCase = true)) return true
    if (normalizedBarcode.isNotEmpty() && this.codigos.contains(normalizedBarcode, ignoreCase = true)) return true
    if (this.parseBarcodes().matchesBarcode(trimmed)) return true
    if (this.id.equals(trimmed, ignoreCase = true)) return true

    // 2. Multi-word token match (e.g. "piñata chica" or "chica piñata")
    val tokens = trimmed.split("\\s+".toRegex()).filter { it.isNotEmpty() }
    if (tokens.size > 1) {
        val matchesAllTokens = tokens.all { token ->
            this.nombre.matchesSearchQuery(token) ||
            (!this.categoria.isNullOrBlank() && this.categoria.matchesSearchQuery(token)) ||
            this.codigos.contains(token, ignoreCase = true) ||
            this.parseBarcodes().matchesBarcode(token)
        }
        if (matchesAllTokens) return true
    }

    return false
}

val DUPLICATE_SUFFIX_REGEX = """\s*\(Duplicado(?:\s+\d+)?\)$""".toRegex(RegexOption.IGNORE_CASE)

/**
 * Resolves product name collisions by appending " (Duplicado)" or " (Duplicado N)".
 */
fun disambiguateProductName(
    baseName: String,
    isNameTaken: (String) -> Boolean
): String {
    val cleanBase = baseName.replace(DUPLICATE_SUFFIX_REGEX, "").trim().ifBlank { "Producto" }
    val candidate = "$cleanBase (Duplicado)"
    if (!isNameTaken(candidate)) return candidate
    var counter = 2
    while (true) {
        val nextCandidate = "$cleanBase (Duplicado $counter)"
        if (!isNameTaken(nextCandidate)) return nextCandidate
        counter++
    }
}

/**
 * Capitalizes the first letter of each word in the string, preserving existing formatting,
 * digits, punctuation, and uppercase letters elsewhere in the words.
 */
fun String.capitalizeWords(): String {
    if (isEmpty()) return this
    val sb = StringBuilder(length)
    for (i in indices) {
        val c = this[i]
        if (c.isLetter()) {
            val prev = if (i > 0) this[i - 1] else null
            val isWordStart = prev == null || (!prev.isLetterOrDigit() && !(prev == '\'' && i > 1 && this[i - 2].isLetter()))
            if (isWordStart) {
                sb.append(if (c.isLowerCase()) c.titlecase() else c.toString())
            } else {
                sb.append(c)
            }
        } else {
            sb.append(c)
        }
    }
    return sb.toString()
}


