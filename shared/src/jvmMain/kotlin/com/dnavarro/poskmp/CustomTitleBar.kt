package com.dnavarro.poskmp

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import poskmp.shared.generated.resources.Res
import poskmp.shared.generated.resources.app_window_title
import poskmp.shared.generated.resources.point_of_sale
import java.awt.MouseInfo
import java.awt.Point
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.time.Duration.Companion.seconds

private fun formatCurrentDateTime(dateTime: LocalDateTime = LocalDateTime.now()): String {
    val locale = Locale.forLanguageTag("es-MX")
    val dayOfWeek = dateTime.dayOfWeek.getDisplayName(TextStyle.FULL, locale)
    val dayOfMonth = dateTime.dayOfMonth
    val month = dateTime.month.getDisplayName(TextStyle.FULL, locale)
    val year = dateTime.year

    val hour12 = dateTime.format(DateTimeFormatter.ofPattern("h:mm", locale))
    val amPm = dateTime.format(DateTimeFormatter.ofPattern("a", locale))


    return "$dayOfWeek, $dayOfMonth de $month del $year - $hour12 $amPm"
}

@Composable
fun FrameWindowScope.CustomTitleBar(
    state: WindowState,
    onCloseRequest: () -> Unit,
    title: String = stringResource(Res.string.app_window_title)
) {
    var initialMouseLoc by remember { mutableStateOf<Point?>(null) }
    var initialWindowLoc by remember { mutableStateOf<Point?>(null) }

    var currentDateTimeText by remember { mutableStateOf(formatCurrentDateTime()) }

    LaunchedEffect(Unit) {
        while (isActive) {
            currentDateTimeText = formatCurrentDateTime()
            delay(1.seconds)
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth().height(36.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = {
                            try {
                                initialMouseLoc = MouseInfo.getPointerInfo().location
                                initialWindowLoc = window.location
                            } catch (_: Exception) {}
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            try {
                                val currentMouseLoc = MouseInfo.getPointerInfo().location
                                val startMouseLoc = initialMouseLoc
                                val startWinLoc = initialWindowLoc
                                if (currentMouseLoc != null && startMouseLoc != null && startWinLoc != null) {
                                    val dx = currentMouseLoc.x - startMouseLoc.x
                                    val dy = currentMouseLoc.y - startMouseLoc.y
                                    window.setLocation(startWinLoc.x + dx, startWinLoc.y + dy)
                                }
                            } catch (_: Exception) {}
                        }
                    )
                }
        ) {
            // Left: Logo & App Title
            Row(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxHeight()
                    .padding(horizontal = 12.dp),
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
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Center: Spanish Date & Time Ticker
            Text(
                text = currentDateTimeText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Center)
            )

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
                )

                // Maximize / Restore Button
                val isMaximized = state.placement == WindowPlacement.Maximized
                WindowControlButton(
                    onClick = {
                        state.placement = if (isMaximized) WindowPlacement.Floating else WindowPlacement.Maximized
                    },
                    hoverColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                )

                // Close Button
                WindowControlButton(
                    onClick = onCloseRequest,
                    hoverColor = MaterialTheme.colorScheme.error
                ) { isHovered ->
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar",
                        tint = if (isHovered) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
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
    content: @Composable (isHovered: Boolean) -> Unit = { }
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
