package com.dnavarro.poskmp.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.FloatState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.computedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastSumBy
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

private data class ListScrollbarDrawElement(
    val state: LazyListState,
    val alphaState: State<Float>,
    val isDragging: Boolean,
    val dragPositionState: FloatState,
    val minThumbHeightPx: Float,
    val activeColor: Color,
    val idleColor: Color
) : ModifierNodeElement<ListScrollbarDrawNode>() {
    override fun create(): ListScrollbarDrawNode = ListScrollbarDrawNode(
        state = state,
        alphaState = alphaState,
        isDragging = isDragging,
        dragPositionState = dragPositionState,
        minThumbHeightPx = minThumbHeightPx,
        activeColor = activeColor,
        idleColor = idleColor
    )

    override fun update(node: ListScrollbarDrawNode) {
        node.state = state
        node.alphaState = alphaState
        node.isDragging = isDragging
        node.dragPositionState = dragPositionState
        node.minThumbHeightPx = minThumbHeightPx
        node.activeColor = activeColor
        node.idleColor = idleColor
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "appVerticalListScrollbar"
        properties["isDragging"] = isDragging
    }
}

private class ListScrollbarDrawNode(
    var state: LazyListState,
    var alphaState: State<Float>,
    var isDragging: Boolean,
    var dragPositionState: FloatState,
    var minThumbHeightPx: Float,
    var activeColor: Color,
    var idleColor: Color
) : Modifier.Node(), DrawModifierNode {
    override fun ContentDrawScope.draw() {
        drawContent()
        val alpha = alphaState.value
        val trackH = size.height
        if (alpha <= 0.001f || trackH <= 0f) return

        val layout = state.layoutInfo
        val total = layout.totalItemsCount
        val visibleItems = layout.visibleItemsInfo
        val visible = visibleItems.size
        if (total <= 0 || visible >= total) return

        val avgSize = if (visible > 0) {
            visibleItems.fastSumBy { it.size }.toFloat() / visible
        } else 1f
        val ratio = (trackH / (avgSize * total)).coerceIn(0.08f, 0.75f)
        val thumbH = (trackH * ratio).coerceAtLeast(minThumbHeightPx)
        val scrollable = (trackH - thumbH).coerceAtLeast(1f)

        val offsetPx = if (isDragging) {
            dragPositionState.floatValue.coerceIn(0f, scrollable)
        } else {
            val firstIdx = state.firstVisibleItemIndex
            val firstOffset = state.firstVisibleItemScrollOffset
            val progress = (firstIdx.toFloat() + (firstOffset / avgSize).coerceIn(0f, 1f)) /
                (total - visible).toFloat()
            scrollable * progress.coerceIn(0f, 1f)
        }

        val thumbWidthPx = (if (isDragging) 8.dp else 6.dp).toPx()
        val endPaddingPx = 2.dp.toPx()
        val cornerRadiusPx = 4.dp.toPx()
        val baseColor = if (isDragging) activeColor else idleColor

        drawRoundRect(
            color = baseColor.copy(alpha = baseColor.alpha * alpha),
            topLeft = Offset(x = size.width - endPaddingPx - thumbWidthPx, y = offsetPx),
            size = Size(width = thumbWidthPx, height = thumbH),
            cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)
        )
    }
}

private data class GridScrollbarDrawElement(
    val gridState: LazyGridState,
    val alphaState: State<Float>,
    val isDragging: Boolean,
    val dragPositionState: FloatState,
    val minThumbHeightPx: Float,
    val activeColor: Color,
    val idleColor: Color
) : ModifierNodeElement<GridScrollbarDrawNode>() {
    override fun create(): GridScrollbarDrawNode = GridScrollbarDrawNode(
        gridState = gridState,
        alphaState = alphaState,
        isDragging = isDragging,
        dragPositionState = dragPositionState,
        minThumbHeightPx = minThumbHeightPx,
        activeColor = activeColor,
        idleColor = idleColor
    )

    override fun update(node: GridScrollbarDrawNode) {
        node.gridState = gridState
        node.alphaState = alphaState
        node.isDragging = isDragging
        node.dragPositionState = dragPositionState
        node.minThumbHeightPx = minThumbHeightPx
        node.activeColor = activeColor
        node.idleColor = idleColor
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "appVerticalGridScrollbar"
        properties["isDragging"] = isDragging
    }
}

private class GridScrollbarDrawNode(
    var gridState: LazyGridState,
    var alphaState: State<Float>,
    var isDragging: Boolean,
    var dragPositionState: FloatState,
    var minThumbHeightPx: Float,
    var activeColor: Color,
    var idleColor: Color
) : Modifier.Node(), DrawModifierNode {
    override fun ContentDrawScope.draw() {
        drawContent()
        val alpha = alphaState.value
        val trackH = size.height
        if (alpha <= 0.001f || trackH <= 0f) return

        val layout = gridState.layoutInfo
        val total = layout.totalItemsCount
        val visibleItems = layout.visibleItemsInfo
        val visible = visibleItems.size
        if (total <= 0 || visible >= total) return

        val avgSize = if (visible > 0) {
            visibleItems.fastSumBy { it.size.height }.toFloat() / visible
        } else 1f
        val ratio = (trackH / (avgSize * total)).coerceIn(0.08f, 0.75f)
        val thumbH = (trackH * ratio).coerceAtLeast(minThumbHeightPx)
        val scrollable = (trackH - thumbH).coerceAtLeast(1f)

        val offsetPx = if (isDragging) {
            dragPositionState.floatValue.coerceIn(0f, scrollable)
        } else {
            val firstIdx = gridState.firstVisibleItemIndex
            val firstOffset = gridState.firstVisibleItemScrollOffset
            val progress = (firstIdx.toFloat() + (firstOffset / avgSize).coerceIn(0f, 1f)) /
                (total - visible).toFloat()
            scrollable * progress.coerceIn(0f, 1f)
        }

        val thumbWidthPx = (if (isDragging) 8.dp else 6.dp).toPx()
        val endPaddingPx = 2.dp.toPx()
        val cornerRadiusPx = 4.dp.toPx()
        val baseColor = if (isDragging) activeColor else idleColor

        drawRoundRect(
            color = baseColor.copy(alpha = baseColor.alpha * alpha),
            topLeft = Offset(x = size.width - endPaddingPx - thumbWidthPx, y = offsetPx),
            size = Size(width = thumbWidthPx, height = thumbH),
            cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)
        )
    }
}

@Composable
actual fun AppVerticalScrollbar(
    state: LazyListState,
    modifier: Modifier
) {
    // Usamos computedStateOf para no leer @FrequentlyChangingValue directamente en composición
    val canScroll by remember(state) {
        computedStateOf {
            val info = state.layoutInfo
            info.totalItemsCount > 0 && info.visibleItemsInfo.size < info.totalItemsCount
        }
    }

    if (!canScroll) return

    val coroutineScope = rememberCoroutineScope()
    var isDragging by remember { mutableStateOf(false) }
    var isVisible by remember { mutableStateOf(false) }
    val dragPositionState = remember { mutableFloatStateOf(0f) }

    val isScrollInProgress = state.isScrollInProgress
    LaunchedEffect(isScrollInProgress, isDragging) {
        if (isScrollInProgress || isDragging) {
            isVisible = true
        } else {
            delay(1500.milliseconds)
            isVisible = false
        }
    }

    val alphaState = animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "ScrollbarAlpha"
    )

    val density = LocalDensity.current
    val minThumbHeightPx = with(density) { 40.dp.toPx() }
    val activeColor = MaterialTheme.colorScheme.primary
    val idleColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.40f)

    var lastTargetIndex by remember { mutableIntStateOf(-1) }
    var lastTargetOffsetPx by remember { mutableIntStateOf(-1) }
    var scrollJob by remember { mutableStateOf<Job?>(null) }

    Spacer(
        modifier = modifier
            .width(32.dp) // Área táctil accesible de 32.dp
            .fillMaxHeight()
            .pointerInput(isVisible, state) {
                if (!isVisible) return@pointerInput
                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        val trackH = size.height.toFloat()
                        val layout = state.layoutInfo
                        val total = layout.totalItemsCount
                        val visibleItems = layout.visibleItemsInfo
                        val visible = visibleItems.size
                        val avgSize = if (visible > 0) {
                            visibleItems.fastSumBy { it.size }.toFloat() / visible
                        } else 1f
                        val ratio = (trackH / (avgSize * total)).coerceIn(0.08f, 0.75f)
                        val thumbH = (trackH * ratio).coerceAtLeast(minThumbHeightPx)
                        val scrollable = (trackH - thumbH).coerceAtLeast(1f)

                        val firstIdx = state.firstVisibleItemIndex
                        val firstOffset = state.firstVisibleItemScrollOffset
                        val idleProgress = if (total > visible) {
                            (firstIdx.toFloat() + (firstOffset / avgSize).coerceIn(0f, 1f)) /
                                (total - visible).toFloat()
                        } else 0f
                        val currentThumbTop = scrollable * idleProgress.coerceIn(0f, 1f)
                        val currentThumbBottom = currentThumbTop + thumbH

                        if (offset.y !in currentThumbTop..currentThumbBottom) {
                            val newDragPos = (offset.y - thumbH / 2f).coerceIn(0f, scrollable)
                            dragPositionState.floatValue = newDragPos
                            val progress = (newDragPos / scrollable).coerceIn(0f, 1f)
                            val remaining = (total - visible).coerceAtLeast(1)
                            val exactItem = remaining * progress
                            val targetIndex = exactItem.toInt().coerceIn(0, total - 1)
                            val offsetFraction = exactItem - targetIndex
                            val targetOffsetPx = (offsetFraction * avgSize).roundToInt()
                            lastTargetIndex = targetIndex
                            lastTargetOffsetPx = targetOffsetPx
                            scrollJob?.cancel()
                            scrollJob = coroutineScope.launch {
                                state.scrollToItem(targetIndex, targetOffsetPx)
                            }
                        } else {
                            dragPositionState.floatValue = currentThumbTop
                        }
                    },
                    onDragEnd = {
                        isDragging = false
                    },
                    onDragCancel = {
                        isDragging = false
                    },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        val trackH = size.height.toFloat()
                        val layout = state.layoutInfo
                        val total = layout.totalItemsCount
                        val visibleItems = layout.visibleItemsInfo
                        val visible = visibleItems.size
                        val avgSize = if (visible > 0) {
                            visibleItems.fastSumBy { it.size }.toFloat() / visible
                        } else 1f
                        val ratio = (trackH / (avgSize * total)).coerceIn(0.08f, 0.75f)
                        val thumbH = (trackH * ratio).coerceAtLeast(minThumbHeightPx)
                        val scrollable = (trackH - thumbH).coerceAtLeast(1f)

                        val newDragPos = (dragPositionState.floatValue + dragAmount).coerceIn(0f, scrollable)
                        dragPositionState.floatValue = newDragPos
                        val progress = (newDragPos / scrollable).coerceIn(0f, 1f)
                        val remaining = (total - visible).coerceAtLeast(1)
                        val exactItem = remaining * progress
                        val targetIndex = exactItem.toInt().coerceIn(0, total - 1)
                        val offsetFraction = exactItem - targetIndex
                        val targetOffsetPx = (offsetFraction * avgSize).roundToInt()

                        if (targetIndex != lastTargetIndex || abs(targetOffsetPx - lastTargetOffsetPx) >= 4) {
                            lastTargetIndex = targetIndex
                            lastTargetOffsetPx = targetOffsetPx
                            scrollJob?.cancel()
                            scrollJob = coroutineScope.launch {
                                state.scrollToItem(targetIndex, targetOffsetPx)
                            }
                        }
                    }
                )
            }
            .then(
                ListScrollbarDrawElement(
                    state = state,
                    alphaState = alphaState,
                    isDragging = isDragging,
                    dragPositionState = dragPositionState,
                    minThumbHeightPx = minThumbHeightPx,
                    activeColor = activeColor,
                    idleColor = idleColor
                )
            )
    )
}

@Composable
actual fun AppVerticalScrollbar(
    gridState: LazyGridState,
    modifier: Modifier
) {
    val canScroll by remember(gridState) {
        computedStateOf {
            val info = gridState.layoutInfo
            info.totalItemsCount > 0 && info.visibleItemsInfo.size < info.totalItemsCount
        }
    }

    if (!canScroll) return

    val coroutineScope = rememberCoroutineScope()
    var isDragging by remember { mutableStateOf(false) }
    var isVisible by remember { mutableStateOf(false) }
    val dragPositionState = remember { mutableFloatStateOf(0f) }

    val isScrollInProgress = gridState.isScrollInProgress
    LaunchedEffect(isScrollInProgress, isDragging) {
        if (isScrollInProgress || isDragging) {
            isVisible = true
        } else {
            delay(1500.milliseconds)
            isVisible = false
        }
    }

    val alphaState = animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "ScrollbarGridAlpha"
    )

    val density = LocalDensity.current
    val minThumbHeightPx = with(density) { 40.dp.toPx() }
    val activeColor = MaterialTheme.colorScheme.primary
    val idleColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.40f)

    var lastTargetIndex by remember { mutableIntStateOf(-1) }
    var lastTargetOffsetPx by remember { mutableIntStateOf(-1) }
    var scrollJob by remember { mutableStateOf<Job?>(null) }

    Spacer(
        modifier = modifier
            .width(32.dp)
            .fillMaxHeight()
            .pointerInput(isVisible, gridState) {
                if (!isVisible) return@pointerInput
                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        val trackH = size.height.toFloat()
                        val layout = gridState.layoutInfo
                        val total = layout.totalItemsCount
                        val visibleItems = layout.visibleItemsInfo
                        val visible = visibleItems.size
                        val avgSize = if (visible > 0) {
                            visibleItems.fastSumBy { it.size.height }.toFloat() / visible
                        } else 1f
                        val ratio = (trackH / (avgSize * total)).coerceIn(0.08f, 0.75f)
                        val thumbH = (trackH * ratio).coerceAtLeast(minThumbHeightPx)
                        val scrollable = (trackH - thumbH).coerceAtLeast(1f)

                        val firstIdx = gridState.firstVisibleItemIndex
                        val firstOffset = gridState.firstVisibleItemScrollOffset
                        val idleProgress = if (total > visible) {
                            (firstIdx.toFloat() + (firstOffset / avgSize).coerceIn(0f, 1f)) /
                                (total - visible).toFloat()
                        } else 0f
                        val currentThumbTop = scrollable * idleProgress.coerceIn(0f, 1f)
                        val currentThumbBottom = currentThumbTop + thumbH

                        if (offset.y !in currentThumbTop..currentThumbBottom) {
                            val newDragPos = (offset.y - thumbH / 2f).coerceIn(0f, scrollable)
                            dragPositionState.floatValue = newDragPos
                            val progress = (newDragPos / scrollable).coerceIn(0f, 1f)
                            val remaining = (total - visible).coerceAtLeast(1)
                            val exactItem = remaining * progress
                            val targetIndex = exactItem.toInt().coerceIn(0, total - 1)
                            val offsetFraction = exactItem - targetIndex
                            val targetOffsetPx = (offsetFraction * avgSize).roundToInt()
                            lastTargetIndex = targetIndex
                            lastTargetOffsetPx = targetOffsetPx
                            scrollJob?.cancel()
                            scrollJob = coroutineScope.launch {
                                gridState.scrollToItem(targetIndex, targetOffsetPx)
                            }
                        } else {
                            dragPositionState.floatValue = currentThumbTop
                        }
                    },
                    onDragEnd = {
                        isDragging = false
                    },
                    onDragCancel = {
                        isDragging = false
                    },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        val trackH = size.height.toFloat()
                        val layout = gridState.layoutInfo
                        val total = layout.totalItemsCount
                        val visibleItems = layout.visibleItemsInfo
                        val visible = visibleItems.size
                        val avgSize = if (visible > 0) {
                            visibleItems.fastSumBy { it.size.height }.toFloat() / visible
                        } else 1f
                        val ratio = (trackH / (avgSize * total)).coerceIn(0.08f, 0.75f)
                        val thumbH = (trackH * ratio).coerceAtLeast(minThumbHeightPx)
                        val scrollable = (trackH - thumbH).coerceAtLeast(1f)

                        val newDragPos = (dragPositionState.floatValue + dragAmount).coerceIn(0f, scrollable)
                        dragPositionState.floatValue = newDragPos
                        val progress = (newDragPos / scrollable).coerceIn(0f, 1f)
                        val remaining = (total - visible).coerceAtLeast(1)
                        val exactItem = remaining * progress
                        val targetIndex = exactItem.toInt().coerceIn(0, total - 1)
                        val offsetFraction = exactItem - targetIndex
                        val targetOffsetPx = (offsetFraction * avgSize).roundToInt()

                        if (targetIndex != lastTargetIndex || abs(targetOffsetPx - lastTargetOffsetPx) >= 4) {
                            lastTargetIndex = targetIndex
                            lastTargetOffsetPx = targetOffsetPx
                            scrollJob?.cancel()
                            scrollJob = coroutineScope.launch {
                                gridState.scrollToItem(targetIndex, targetOffsetPx)
                            }
                        }
                    }
                )
            }
            .then(
                GridScrollbarDrawElement(
                    gridState = gridState,
                    alphaState = alphaState,
                    isDragging = isDragging,
                    dragPositionState = dragPositionState,
                    minThumbHeightPx = minThumbHeightPx,
                    activeColor = activeColor,
                    idleColor = idleColor
                )
            )
    )
}
