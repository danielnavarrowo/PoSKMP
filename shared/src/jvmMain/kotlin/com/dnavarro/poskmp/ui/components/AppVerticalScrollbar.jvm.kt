package com.dnavarro.poskmp.ui.components

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.defaultScrollbarStyle
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
actual fun AppVerticalScrollbar(
    state: LazyListState,
    modifier: Modifier
) {
    VerticalScrollbar(
        adapter = rememberScrollbarAdapter(scrollState = state),
        modifier = modifier,
        style = defaultScrollbarStyle().copy(
            unhoverColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f),
            hoverColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
            thickness = 8.dp,
            shape = RoundedCornerShape(4.dp)
        )
    )
}
