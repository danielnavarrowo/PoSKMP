package com.dnavarro.poskmp.util

import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ToggleButtonShapes
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Window width size classes based on Material 3 adaptive design guidelines.
 */
enum class WindowWidthSizeClass {
    COMPACT,
    MEDIUM,
    EXPANDED,
    LARGE
}

/**
 * Centralized adaptive breakpoints used throughout the application to eliminate
 * arbitrary hardcoded dp thresholds.
 */
object AdaptiveBreakpoints {
    // Standard window size class thresholds
    val CompactMaxWidth: Dp = 600.dp
    val MediumMaxWidth: Dp = 840.dp
    val ExpandedMaxWidth: Dp = 1200.dp

    // Navigation Rail specific breakpoints
    val NavRailMinWidth: Dp = 800.dp
    val NavRailExpandedMinWidth: Dp = 1200.dp

    // Component-level responsive thresholds
    val FilterBarSingleRowMinWidth: Dp = 840.dp
    val TripleColumnKpiMinWidth: Dp = 840.dp
    val DualColumnKpiMinWidth: Dp = 420.dp
    val MultiColumnContentMinWidth: Dp = 720.dp
    val PaymentAndCategoryRowMinWidth: Dp = 760.dp
    val ShiftActionsRowMinWidth: Dp = 520.dp
    val NavButtonsRowMinWidth: Dp = 600.dp
    val SegmentedGroupWrapWidth: Dp = 440.dp
    val SegmentedGroupCompactWidth: Dp = 340.dp
    val ConnectedButtonsRowMinWidth: Dp = 420.dp
    val DetailPaneTwoPaneThreshold: Dp = 600.dp

    // Venta screen multi-pane thresholds
    val VentaMinCatalogWidth: Dp = 320.dp
    val VentaMinTicketWidth: Dp = 280.dp
    val VentaSplitterWidth: Dp = 10.dp
    val VentaTwoPaneMinWidth: Dp = VentaMinCatalogWidth + VentaMinTicketWidth + VentaSplitterWidth
}

/**
 * Returns the [WindowWidthSizeClass] corresponding to the given [width].
 */
fun getWindowWidthSizeClass(width: Dp): WindowWidthSizeClass = when {
    width < AdaptiveBreakpoints.CompactMaxWidth -> WindowWidthSizeClass.COMPACT
    width < AdaptiveBreakpoints.MediumMaxWidth -> WindowWidthSizeClass.MEDIUM
    width < AdaptiveBreakpoints.ExpandedMaxWidth -> WindowWidthSizeClass.EXPANDED
    else -> WindowWidthSizeClass.LARGE
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
 * Helper to check whether the available width is in the expanded class or wider (>= 840.dp).
 */
fun isExpandedWidth(width: Dp): Boolean = width >= AdaptiveBreakpoints.MediumMaxWidth

/**
 * Helper to check whether the available width is in the large class (>= 1200.dp).
 */
fun isLargeWidth(width: Dp): Boolean = width >= AdaptiveBreakpoints.ExpandedMaxWidth

/**
 * Computes appropriate [ToggleButtonShapes] for a connected button in a segmented group,
 * correctly falling back to standalone shapes if only a single item is in the row.
 */
@Composable
fun connectedToggleButtonShapes(index: Int, totalCount: Int): ToggleButtonShapes = when {
    totalCount <= 1 -> ToggleButtonShapes(
        MaterialTheme.shapes.small,
        MaterialTheme.shapes.small,
        MaterialTheme.shapes.small
    )
    index == 0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
    index == totalCount - 1 -> ButtonGroupDefaults.connectedTrailingButtonShapes()
    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
}

