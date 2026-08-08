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

fun formatCurrentDateTime(dateTime: java.time.LocalDateTime = java.time.LocalDateTime.now()): String {
    val date = formatCurrentDate(dateTime)
    val time = formatCurrentTime(dateTime)
    return "$date\n$time"
}

fun formatCurrentDate(dateTime: java.time.LocalDateTime = java.time.LocalDateTime.now()): String {
    val locale = java.util.Locale.forLanguageTag("es-MX")
    val dayOfWeek = dateTime.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, locale)
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
    val dayOfMonth = dateTime.dayOfMonth
    val month = dateTime.month.getDisplayName(java.time.format.TextStyle.FULL, locale)

    return "$dayOfWeek, $dayOfMonth de $month"
}

fun formatCurrentTime(dateTime: java.time.LocalDateTime = java.time.LocalDateTime.now()): String {
    val locale = java.util.Locale.forLanguageTag("es-MX")
    val hour12 = dateTime.format(java.time.format.DateTimeFormatter.ofPattern("h:mm", locale))

    return "$hour12"
}
