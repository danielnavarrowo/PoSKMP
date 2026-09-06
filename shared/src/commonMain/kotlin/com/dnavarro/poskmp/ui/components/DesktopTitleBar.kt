package com.dnavarro.poskmp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dnavarro.poskmp.domain.model.Sale
import com.dnavarro.poskmp.util.formatPrice
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import poskmp.shared.generated.resources.Res
import poskmp.shared.generated.resources.close
import poskmp.shared.generated.resources.close_button
import poskmp.shared.generated.resources.last_sale_change
import poskmp.shared.generated.resources.last_sale_paid
import poskmp.shared.generated.resources.last_sale_title
import poskmp.shared.generated.resources.last_sale_total
import poskmp.shared.generated.resources.minimize_window
import poskmp.shared.generated.resources.restore

/**
 * Title bar for desktop full screen mode with centered date/time, last sale info, and minimize / close window buttons.
 */
@Composable
fun DesktopTitleBar(
    dateTimeText: String = "",
    lastSale: Sale? = null,
    onMinimize: () -> Unit,
    onClose: () -> Unit
) {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .background(MaterialTheme.colorScheme.surface)
                .padding(start = 12.dp)
        ) {

            // Center: Date and Time
            if (dateTimeText.isNotBlank()) {
                Text(
                    text = dateTimeText,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterStart),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Right: Last Sale Info & Window Control Buttons (Minimize & Close)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                // Última Venta Info
                lastSale?.let { sale ->
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = stringResource(Res.string.last_sale_title) + ":",
                                style = MaterialTheme.typography.labelMedium,
                            )
                            Text(
                                text = stringResource(Res.string.last_sale_total, sale.total.toString().formatPrice()),
                                style = MaterialTheme.typography.labelMedium,
                            )
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                            )
                            Text(
                                text = stringResource(Res.string.last_sale_paid, sale.pagoCon.toString().formatPrice()),
                                style = MaterialTheme.typography.labelMedium,
                            )
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                            )
                            Text(
                                text = stringResource(Res.string.last_sale_change, sale.cambio.toString().formatPrice()),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Black,

                            )
                        }
                    }
                }

                // Window Control Buttons (Minimize & Close)
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                // Minimize Button
                TooltipBox(
                    positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Below),
                    tooltip = {
                        PlainTooltip {
                            Text(stringResource(Res.string.minimize_window))
                        }
                    },
                    state = rememberTooltipState()
                ) {
                    IconButton(
                        onClick = onMinimize
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.restore),
                            contentDescription = stringResource(Res.string.minimize_window),
                            modifier = Modifier.size(26.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Close Button
                TooltipBox(
                    positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Below),
                    tooltip = {
                        PlainTooltip {
                            Text(stringResource(Res.string.close_button))
                        }
                    },
                    state = rememberTooltipState()
                ) {
                    IconButton(
                        onClick = onClose
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.close),
                            contentDescription = stringResource(Res.string.close_button),
                            modifier = Modifier.size(26.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
