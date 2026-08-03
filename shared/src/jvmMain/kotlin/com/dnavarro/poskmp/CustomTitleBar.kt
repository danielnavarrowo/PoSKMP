package com.dnavarro.poskmp

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowState
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import poskmp.shared.generated.resources.Res
import poskmp.shared.generated.resources.app_window_title
import poskmp.shared.generated.resources.close
import poskmp.shared.generated.resources.maximize
import poskmp.shared.generated.resources.minimize
import poskmp.shared.generated.resources.point_of_sale
import poskmp.shared.generated.resources.restore
import java.awt.MouseInfo
import java.awt.Point

@Composable
fun FrameWindowScope.CustomTitleBar(
    state: WindowState,
    onCloseRequest: () -> Unit
) {
    var initialMouseLoc by remember { mutableStateOf<Point?>(null) }
    var initialWindowLoc by remember { mutableStateOf<Point?>(null) }

    Surface(
        modifier = Modifier.fillMaxWidth().height(36.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(state.placement) {
                    detectTapGestures(
                        onDoubleTap = {
                            state.placement = if (state.placement == WindowPlacement.Maximized) {
                                WindowPlacement.Floating
                            } else {
                                WindowPlacement.Maximized
                            }
                        }
                    )
                }
                .pointerInput(state.placement) {
                    var isUnmaximizing = false
                    detectDragGestures(
                        onDragStart = {
                            try {
                                initialMouseLoc = MouseInfo.getPointerInfo().location
                                initialWindowLoc = window.location
                                isUnmaximizing = false
                            } catch (_: Exception) {
                            }
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            try {
                                val currentMouseLoc = MouseInfo.getPointerInfo().location
                                val startMouseLoc = initialMouseLoc
                                val startWinLoc = initialWindowLoc

                                if (currentMouseLoc != null) {
                                    val screenBounds = window.graphicsConfiguration?.bounds
                                        ?: java.awt.Rectangle(0, 0, 1920, 1080)

                                    if (state.placement == WindowPlacement.Maximized && !isUnmaximizing) {
                                        isUnmaximizing = true
                                        val mouseXInScreen = currentMouseLoc.x - screenBounds.x
                                        val ratio = mouseXInScreen.toDouble() / screenBounds.width.coerceAtLeast(1)

                                        state.placement = WindowPlacement.Floating

                                        val newWidth = window.width
                                        val newX = (currentMouseLoc.x - (newWidth * ratio)).toInt()
                                        val newY = currentMouseLoc.y - 18
                                        window.setLocation(newX, newY)

                                        initialMouseLoc = currentMouseLoc
                                        initialWindowLoc = Point(newX, newY)
                                    } else if (startMouseLoc != null && startWinLoc != null) {
                                        val dx = currentMouseLoc.x - startMouseLoc.x
                                        val dy = currentMouseLoc.y - startMouseLoc.y
                                        window.setLocation(startWinLoc.x + dx, startWinLoc.y + dy)
                                    }
                                }
                            } catch (_: Exception) {
                            }
                        },
                        onDragEnd = {
                            try {
                                val currentMouseLoc = MouseInfo.getPointerInfo()?.location
                                val screenBounds = window.graphicsConfiguration?.bounds
                                    ?: java.awt.Rectangle(0, 0, 1920, 1080)

                                if (currentMouseLoc != null && currentMouseLoc.y <= screenBounds.y + 10) {
                                    state.placement = WindowPlacement.Maximized
                                }
                            } catch (_: Exception) {
                            }
                        }
                    )
                }
        ) {
            // Left: Logo & App Title
            Row(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 12.dp)
                    .fillMaxHeight(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    painter = painterResource(Res.drawable.point_of_sale),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )

                Text(
                    text = stringResource(Res.string.app_window_title),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Right: Window Control Buttons (Minimize, Maximize/Restore, Close)
            Row(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Minimize Button
                WindowControlButton(
                    onClick = { state.isMinimized = true },
                    hoverColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.minimize),
                        contentDescription = "Minimizar",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Maximize / Restore Button
                val isMaximized = state.placement == WindowPlacement.Maximized
                WindowControlButton(
                    onClick = {
                        state.placement =
                            if (isMaximized) WindowPlacement.Floating else WindowPlacement.Maximized
                    },
                    hoverColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                ) {
                    Icon(
                        painterResource(if (isMaximized) Res.drawable.restore else Res.drawable.maximize),
                        contentDescription = if (isMaximized) "Restaurar" else "Maximizar",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Close Button
                WindowControlButton(
                    onClick = onCloseRequest,
                    hoverColor = MaterialTheme.colorScheme.error,
                    hoverContentColor = MaterialTheme.colorScheme.onError
                ) { isHovered ->
                    Icon(
                        painter = painterResource(Res.drawable.close),
                        contentDescription = "Cerrar",
                        tint = if (isHovered) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun WindowControlButton(
    onClick: () -> Unit,
    hoverColor: Color,
    hoverContentColor: Color? = null,
    content: @Composable (isHovered: Boolean) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Box(
        modifier = Modifier
            .width(44.dp)
            .fillMaxHeight()
            .background(if (isHovered) hoverColor else Color.Transparent)
            .hoverable(interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        content(isHovered)
    }
}
