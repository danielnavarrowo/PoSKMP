package com.dnavarro.poskmp.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

@Composable
actual fun AppVerticalScrollbar(
    state: LazyListState,
    modifier: Modifier
) {
    // Usamos derivedStateOf para no leer @FrequentlyChangingValue directamente en composición
    val canScroll by remember(state) {
        derivedStateOf {
            val info = state.layoutInfo
            info.totalItemsCount > 0 && info.visibleItemsInfo.size < info.totalItemsCount
        }
    }

    if (!canScroll) return

    val coroutineScope = rememberCoroutineScope()
    var isDragging by remember { mutableStateOf(false) }
    var isVisible by remember { mutableStateOf(false) }
    var dragPositionPx by remember { mutableFloatStateOf(0f) }

    val isScrollInProgress = state.isScrollInProgress
    LaunchedEffect(isScrollInProgress, isDragging) {
        if (isScrollInProgress || isDragging) {
            isVisible = true
        } else {
            delay(1500.milliseconds)
            isVisible = false
        }
    }

    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 300)
    )

    if (alpha <= 0f) return

    val density = LocalDensity.current
    val minThumbHeightPx = with(density) { 40.dp.toPx() }

    var lastTargetIndex by remember { mutableIntStateOf(-1) }
    var lastTargetOffsetPx by remember { mutableIntStateOf(-1) }
    var scrollJob by remember { mutableStateOf<Job?>(null) }

    Box(
        modifier = modifier
            .width(32.dp) // Área táctil accesible de 32.dp
            .fillMaxHeight()
            .alpha(alpha)
    ) {
        // Capa de interacción táctil en toda el área accesible
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            val trackH = size.height.toFloat()
                            val layout = state.layoutInfo
                            val total = layout.totalItemsCount
                            val visible = layout.visibleItemsInfo.size
                            val avgSize = if (visible > 0) {
                                layout.visibleItemsInfo.sumOf { it.size }.toFloat() / visible
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
                                dragPositionPx = (offset.y - thumbH / 2f).coerceIn(0f, scrollable)
                                val progress = (dragPositionPx / scrollable).coerceIn(0f, 1f)
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
                                dragPositionPx = currentThumbTop
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
                            val visible = layout.visibleItemsInfo.size
                            val avgSize = if (visible > 0) {
                                layout.visibleItemsInfo.sumOf { it.size }.toFloat() / visible
                            } else 1f
                            val ratio = (trackH / (avgSize * total)).coerceIn(0.08f, 0.75f)
                            val thumbH = (trackH * ratio).coerceAtLeast(minThumbHeightPx)
                            val scrollable = (trackH - thumbH).coerceAtLeast(1f)

                            dragPositionPx = (dragPositionPx + dragAmount).coerceIn(0f, scrollable)
                            val progress = (dragPositionPx / scrollable).coerceIn(0f, 1f)
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
        )

        // Pulgar visual: medido y posicionado en la fase de Layout (evita recomposiciones en cada pixel)
        val thumbWidth = if (isDragging) 8.dp else 6.dp
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 2.dp)
                .layout { measurable, constraints ->
                    val trackH = if (constraints.hasBoundedHeight) constraints.maxHeight.toFloat() else 0f
                    if (trackH <= 0f) {
                        return@layout layout(0, 0) {}
                    }

                    val layout = state.layoutInfo
                    val total = layout.totalItemsCount
                    val visible = layout.visibleItemsInfo.size
                    val avgSize = if (visible > 0) {
                        layout.visibleItemsInfo.sumOf { it.size }.toFloat() / visible
                    } else 1f
                    val ratio = (trackH / (avgSize * total)).coerceIn(0.08f, 0.75f)
                    val thumbH = (trackH * ratio).coerceAtLeast(minThumbHeightPx)
                    val scrollable = (trackH - thumbH).coerceAtLeast(1f)

                    val offsetPx = if (isDragging) {
                        dragPositionPx.coerceIn(0f, scrollable)
                    } else {
                        val firstIdx = state.firstVisibleItemIndex
                        val firstOffset = state.firstVisibleItemScrollOffset
                        val progress = if (total > visible) {
                            (firstIdx.toFloat() + (firstOffset / avgSize).coerceIn(0f, 1f)) /
                                (total - visible).toFloat()
                        } else 0f
                        scrollable * progress.coerceIn(0f, 1f)
                    }

                    val placeable = measurable.measure(
                        constraints.copy(
                            minHeight = thumbH.roundToInt(),
                            maxHeight = thumbH.roundToInt()
                        )
                    )

                    layout(placeable.width, constraints.maxHeight) {
                        placeable.place(x = 0, y = offsetPx.roundToInt())
                    }
                }
                .width(thumbWidth)
                .clip(RoundedCornerShape(4.dp))
                .background(
                    if (isDragging) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.40f)
                )
        )
    }
}
