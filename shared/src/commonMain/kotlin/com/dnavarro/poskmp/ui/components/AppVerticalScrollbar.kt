package com.dnavarro.poskmp.ui.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun AppVerticalScrollbar(
    state: LazyListState,
    modifier: Modifier = Modifier
)

@Composable
expect fun AppVerticalScrollbar(
    gridState: LazyGridState,
    modifier: Modifier = Modifier
)
