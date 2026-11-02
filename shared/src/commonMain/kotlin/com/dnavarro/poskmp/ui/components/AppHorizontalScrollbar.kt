package com.dnavarro.poskmp.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun AppHorizontalScrollbar(
    state: ScrollState,
    modifier: Modifier = Modifier
)
