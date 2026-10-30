package com.dnavarro.poskmp.util

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Centralized adaptive breakpoints used throughout the application to eliminate
 * arbitrary hardcoded dp thresholds.
 */
object AdaptiveBreakpoints {
    val CompactMaxWidth: Dp = 600.dp

    // Navigation Rail specific breakpoints
    val NavRailMinWidth: Dp = 800.dp
    val NavRailExpandedMinWidth: Dp = 1200.dp
}

/**
 * Helper to check whether the available width is in the compact class (< 600.dp).
 */
fun isCompactWidth(width: Dp): Boolean = width < AdaptiveBreakpoints.CompactMaxWidth
