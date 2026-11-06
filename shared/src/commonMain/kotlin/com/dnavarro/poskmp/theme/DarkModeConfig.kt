package com.dnavarro.poskmp.theme

enum class DarkModeConfig {
    SYSTEM,
    LIGHT,
    DARK,
    SCHEDULED
}

const val DEFAULT_LIGHT_MODE_START_MINUTES = 7 * 60   // 07:00 AM
const val DEFAULT_DARK_MODE_START_MINUTES = 19 * 60   // 07:00 PM

/**
 * Determines whether dark mode should be active at [currentMinutesOfDay] (0..1439)
 * given [lightStartMinutes] and [darkStartMinutes].
 */
fun isScheduledDarkTime(
    currentMinutesOfDay: Int,
    lightStartMinutes: Int,
    darkStartMinutes: Int
): Boolean {
    val now = currentMinutesOfDay.mod(24 * 60)
    val lightStart = lightStartMinutes.mod(24 * 60)
    val darkStart = darkStartMinutes.mod(24 * 60)
    return lightStart != darkStart && if (darkStart > lightStart) {
        // E.g. Light starts at 07:00 (420), Dark starts at 19:00 (1140)
        // Dark is active before 07:00 OR at/after 19:00
        now !in lightStart..<darkStart
    } else {
        // E.g. Dark starts at 01:00 (60), Light starts at 08:00 (480)
        now in darkStart until lightStart
    }
}

/**
 * Formats minutes from midnight (0..1439) into a 12-hour string (e.g., "07:00 a. m.").
 */
fun formatMinutesOfDay12h(minutesOfDay: Int): String {
    val normalized = minutesOfDay.mod(24 * 60)
    val hour24 = normalized / 60
    val minute = normalized % 60
    val hour12 = if (hour24 % 12 == 0) 12 else hour24 % 12
    val hourStr = hour12.toString().padStart(2, '0')
    val minStr = minute.toString().padStart(2, '0')
    val amPm = if (hour24 < 12) "a. m." else "p. m."
    return "$hourStr:$minStr $amPm"
}
