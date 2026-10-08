package com.dnavarro.poskmp.util

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Standard Material 3 / Android Adaptive Window Width Size Classes:
 * - [COMPACT]: Screen width < 600.dp (Phones in portrait)
 * - [MEDIUM]: Screen width between 600.dp and 840.dp (Tablets in portrait, foldables)
 * - [EXPANDED]: Screen width >= 840.dp (Tablets in landscape, desktop displays)
 */
enum class WindowWidthSizeClass {
    COMPACT,
    MEDIUM,
    EXPANDED
}

/**
 * Centralized adaptive breakpoints used throughout the application to eliminate
 * arbitrary hardcoded dp thresholds.
 */
object AdaptiveBreakpoints {
    val CompactMaxWidth: Dp = 600.dp
    val MediumMaxWidth: Dp = 840.dp
    val ExpandedMinWidth: Dp = 840.dp

    // Navigation Rail specific breakpoints
    val NavRailMinWidth: Dp = 800.dp
    val NavRailExpandedMinWidth: Dp = 1200.dp
}

/**
 * Calculates the [WindowWidthSizeClass] for a given [width] in Dp according to Material 3 guidelines.
 */
fun calculateWindowWidthSizeClass(width: Dp): WindowWidthSizeClass = when {
    width < AdaptiveBreakpoints.CompactMaxWidth -> WindowWidthSizeClass.COMPACT
    width < AdaptiveBreakpoints.MediumMaxWidth -> WindowWidthSizeClass.MEDIUM
    else -> WindowWidthSizeClass.EXPANDED
}

/**
 * Helper to check whether the available width is in the compact class (< 600.dp).
 */
fun isCompactWidth(width: Dp): Boolean = width < AdaptiveBreakpoints.CompactMaxWidth

/**
 * Helper to check whether the available width is in the medium class (600.dp ..< 840.dp).
 */
fun isMediumWidth(width: Dp): Boolean =
    width >= AdaptiveBreakpoints.CompactMaxWidth && width < AdaptiveBreakpoints.MediumMaxWidth

/**
 * Helper to check whether the available width is expanded (>= 840.dp).
 */
fun isExpandedWidth(width: Dp): Boolean = width >= AdaptiveBreakpoints.ExpandedMinWidth

/**
 * Helper to check whether the available width can accommodate medium or expanded layouts (>= 600.dp).
 */
fun isMediumOrExpandedWidth(width: Dp): Boolean = width >= AdaptiveBreakpoints.CompactMaxWidth
