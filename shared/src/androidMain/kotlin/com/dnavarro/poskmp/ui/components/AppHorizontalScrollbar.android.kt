package com.dnavarro.poskmp.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
actual fun AppHorizontalScrollbar(
    state: ScrollState,
    modifier: Modifier
) {
    if (state.maxValue <= 0) return

    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val thumbColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
    val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)

    BoxWithConstraints(
        modifier = modifier
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(trackColor)
    ) {
        val totalWidthPx = constraints.maxWidth.toFloat()
        if (totalWidthPx <= 0f) return@BoxWithConstraints

        val totalContentWidth = (state.maxValue + state.viewportSize).toFloat()
        val thumbWidthPx = ((state.viewportSize / totalContentWidth) * totalWidthPx).coerceIn(
            with(density) { 32.dp.toPx() },
            totalWidthPx
        )
        val maxThumbOffset = totalWidthPx - thumbWidthPx
        val scrollFraction = (state.value.toFloat() / state.maxValue.toFloat()).coerceIn(0f, 1f)
        val thumbOffsetPx = scrollFraction * maxThumbOffset

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(with(density) { thumbWidthPx.toDp() })
                .graphicsLayer { translationX = thumbOffsetPx }
                .background(thumbColor, RoundedCornerShape(4.dp))
                .pointerInput(state, totalWidthPx, thumbWidthPx, maxThumbOffset) {
                    detectHorizontalDragGestures { change, dragAmount ->
                        change.consume()
                        if (maxThumbOffset > 0f) {
                            val scrollDelta = (dragAmount / maxThumbOffset) * state.maxValue
                            coroutineScope.launch {
                                state.scrollBy(scrollDelta)
                            }
                        }
                    }
                }
        )
    }
}
