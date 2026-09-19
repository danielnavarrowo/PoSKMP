package com.dnavarro.poskmp.util

import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Resets the scroll position of a [LazyListState] immediately to 0,
 * and launches a coroutine to ensure it stays at index 0 after layout/recomposition.
 */
fun LazyListState.resetScroll(scope: CoroutineScope) {
    try {
        requestScrollToItem(0)
    } catch (_: Exception) {
    }
    scope.launch {
        try {
            scrollToItem(0)
        } catch (_: Exception) {
        }
    }
}

/**
 * Resets the scroll position of a [LazyGridState] immediately to 0,
 * and launches a coroutine to ensure it stays at index 0 after layout/recomposition.
 */
fun LazyGridState.resetScroll(scope: CoroutineScope) {
    try {
        requestScrollToItem(0)
    } catch (_: Exception) {
    }
    scope.launch {
        try {
            scrollToItem(0)
        } catch (_: Exception) {
        }
    }
}

/**
 * Smoothly scrolls [targetIndex] into view only if it is outside or partially visible,
 * avoiding jumping the item to the top of the viewport when navigating downwards.
 */
suspend fun LazyListState.scrollItemIntoView(targetIndex: Int) {
    val items = layoutInfo.visibleItemsInfo
    if (items.isEmpty()) {
        animateScrollToItem(targetIndex)
        return
    }
    val firstVisible = items.first()
    val lastVisible = items.last()

    if (targetIndex < firstVisible.index) {
        animateScrollToItem(targetIndex)
        return
    }

    val visibleBottom = layoutInfo.viewportEndOffset - layoutInfo.afterContentPadding
    val existingItem = items.firstOrNull { it.index == targetIndex }

    if (existingItem != null) {
        if (existingItem.offset < layoutInfo.viewportStartOffset) {
            animateScrollToItem(targetIndex)
        } else if (existingItem.offset + existingItem.size > visibleBottom) {
            val delta = (existingItem.offset + existingItem.size) - visibleBottom
            animateScrollBy(delta.toFloat() + 4f)
        }
        return
    }

    if (targetIndex > lastVisible.index) {
        if (targetIndex - lastVisible.index >= items.size) {
            val targetFirstVisibleIndex = (targetIndex - items.size + 2).coerceAtLeast(0)
            animateScrollToItem(targetFirstVisibleIndex)
        } else {
            val itemHeight = lastVisible.size.takeIf { it > 0 } ?: 48
            val delta = (targetIndex - lastVisible.index) * itemHeight
            animateScrollBy(delta.toFloat() + 4f)
        }
    }
}

/**
 * Smoothly scrolls [targetIndex] into view in a [LazyGridState] only if it is outside or partially visible,
 * avoiding jumping the item to the top of the viewport when navigating downwards.
 */
suspend fun LazyGridState.scrollItemIntoView(targetIndex: Int) {
    val items = layoutInfo.visibleItemsInfo
    if (items.isEmpty()) {
        animateScrollToItem(targetIndex)
        return
    }
    val firstVisible = items.first()
    val lastVisible = items.last()

    if (targetIndex < firstVisible.index) {
        animateScrollToItem(targetIndex)
        return
    }

    val visibleBottom = layoutInfo.viewportEndOffset - layoutInfo.afterContentPadding
    val existingItem = items.firstOrNull { it.index == targetIndex }

    if (existingItem != null) {
        if (existingItem.offset.y < layoutInfo.viewportStartOffset) {
            animateScrollToItem(targetIndex)
        } else if (existingItem.offset.y + existingItem.size.height > visibleBottom) {
            val delta = (existingItem.offset.y + existingItem.size.height) - visibleBottom
            animateScrollBy(delta.toFloat() + 4f)
        }
        return
    }

    if (targetIndex > lastVisible.index) {
        if (targetIndex - lastVisible.index >= items.size) {
            val targetFirstVisibleIndex = (targetIndex - items.size + 2).coerceAtLeast(0)
            animateScrollToItem(targetFirstVisibleIndex)
        } else {
            val itemHeight = lastVisible.size.height.takeIf { it > 0 } ?: 140
            val delta = (targetIndex - lastVisible.index) * itemHeight
            animateScrollBy(delta.toFloat() + 4f)
        }
    }
}
