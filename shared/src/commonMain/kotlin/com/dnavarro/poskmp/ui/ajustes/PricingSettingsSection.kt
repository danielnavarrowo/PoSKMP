package com.dnavarro.poskmp.ui.ajustes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dnavarro.poskmp.ui.components.SyncedSettingBadge
import org.jetbrains.compose.resources.stringResource
import poskmp.shared.generated.resources.Res
import poskmp.shared.generated.resources.auto_wholesale_by_quantity_subtitle
import poskmp.shared.generated.resources.auto_wholesale_by_quantity_title
import poskmp.shared.generated.resources.auto_wholesale_by_total_subtitle
import poskmp.shared.generated.resources.auto_wholesale_by_total_title
import poskmp.shared.generated.resources.auto_wholesale_pieces_suffix
import poskmp.shared.generated.resources.auto_wholesale_quantity_threshold_label
import poskmp.shared.generated.resources.auto_wholesale_section_subtitle
import poskmp.shared.generated.resources.auto_wholesale_section_title
import poskmp.shared.generated.resources.auto_wholesale_total_threshold_label
import poskmp.shared.generated.resources.default_margins_section_subtitle
import poskmp.shared.generated.resources.default_margins_section_title
import poskmp.shared.generated.resources.delivery_margin_label
import poskmp.shared.generated.resources.delivery_sales_section_subtitle
import poskmp.shared.generated.resources.delivery_sales_section_title
import poskmp.shared.generated.resources.disallow_card_on_wholesale_subtitle
import poskmp.shared.generated.resources.disallow_card_on_wholesale_title
import poskmp.shared.generated.resources.enable_rounding_subtitle
import poskmp.shared.generated.resources.enable_rounding_title
import poskmp.shared.generated.resources.payment_policies_section_subtitle
import poskmp.shared.generated.resources.payment_policies_section_title
import poskmp.shared.generated.resources.prioritize_delivery_price_subtitle
import poskmp.shared.generated.resources.prioritize_delivery_price_title
import poskmp.shared.generated.resources.retail_margin_label
import poskmp.shared.generated.resources.round_product_prices_subtitle
import poskmp.shared.generated.resources.round_product_prices_title
import poskmp.shared.generated.resources.round_ticket_total_subtitle
import poskmp.shared.generated.resources.round_ticket_total_title
import poskmp.shared.generated.resources.rounding_section_subtitle
import poskmp.shared.generated.resources.rounding_section_title
import poskmp.shared.generated.resources.wholesale_margin_label

@Composable
fun PricingSettingsSection(
    defaultRetailMargin: Double,
    onDefaultRetailMarginChange: (Double) -> Unit,
    defaultWholesaleMargin: Double,
    onDefaultWholesaleMarginChange: (Double) -> Unit,
    defaultDeliveryMargin: Double = 0.0,
    onDefaultDeliveryMarginChange: (Double) -> Unit = {},
    isRoundingEnabled: Boolean,
    onIsRoundingEnabledChange: (Boolean) -> Unit,
    roundProductPrices: Boolean,
    onRoundProductPricesChange: (Boolean) -> Unit,
    roundTicketTotal: Boolean,
    onRoundTicketTotalChange: (Boolean) -> Unit,
    disallowCardPaymentOnWholesale: Boolean,
    onDisallowCardPaymentOnWholesaleChange: (Boolean) -> Unit,
    prioritizeDeliveryPrice: Boolean,
    onPrioritizeDeliveryPriceChange: (Boolean) -> Unit,
    autoWholesaleByQuantity: Boolean = false,
    onAutoWholesaleByQuantityChange: (Boolean) -> Unit = {},
    autoWholesaleQuantityThreshold: Int = 3,
    onAutoWholesaleQuantityThresholdChange: (Int) -> Unit = {},
    autoWholesaleByTicketTotal: Boolean = false,
    onAutoWholesaleByTicketTotalChange: (Boolean) -> Unit = {},
    autoWholesaleTicketTotalThreshold: Double = 0.0,
    onAutoWholesaleTicketTotalThresholdChange: (Double) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var retailMarginText by remember(defaultRetailMargin) {
        mutableStateOf(if (defaultRetailMargin > 0.0) {
            if (defaultRetailMargin % 1.0 == 0.0) defaultRetailMargin.toLong().toString() else defaultRetailMargin.toString()
        } else "")
    }
    var wholesaleMarginText by remember(defaultWholesaleMargin) {
        mutableStateOf(if (defaultWholesaleMargin > 0.0) {
            if (defaultWholesaleMargin % 1.0 == 0.0) defaultWholesaleMargin.toLong().toString() else defaultWholesaleMargin.toString()
        } else "")
    }
    var deliveryMarginText by remember(defaultDeliveryMargin) {
        mutableStateOf(if (defaultDeliveryMargin > 0.0) {
            if (defaultDeliveryMargin % 1.0 == 0.0) defaultDeliveryMargin.toLong().toString() else defaultDeliveryMargin.toString()
        } else "")
    }
    var quantityThresholdText by remember(autoWholesaleQuantityThreshold) {
        mutableStateOf(if (autoWholesaleQuantityThreshold > 0) autoWholesaleQuantityThreshold.toString() else "")
    }
    var ticketTotalThresholdText by remember(autoWholesaleTicketTotalThreshold) {
        mutableStateOf(if (autoWholesaleTicketTotalThreshold > 0.0) {
            if (autoWholesaleTicketTotalThreshold % 1.0 == 0.0) autoWholesaleTicketTotalThreshold.toLong().toString() else autoWholesaleTicketTotalThreshold.toString()
        } else "")
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Card: Márgenes de Ganancia Predeterminados
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.default_margins_section_title),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    SyncedSettingBadge()
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(Res.string.default_margins_section_subtitle),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = retailMarginText,
                        onValueChange = { input ->
                            if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                                retailMarginText = input
                                onDefaultRetailMarginChange(input.toDoubleOrNull() ?: 0.0)
                            }
                        },
                        label = { Text(stringResource(Res.string.retail_margin_label)) },
                        suffix = { Text("%", fontWeight = FontWeight.Bold) },
                        placeholder = { Text("0") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = wholesaleMarginText,
                        onValueChange = { input ->
                            if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                                wholesaleMarginText = input
                                onDefaultWholesaleMarginChange(input.toDoubleOrNull() ?: 0.0)
                            }
                        },
                        label = { Text(stringResource(Res.string.wholesale_margin_label)) },
                        suffix = { Text("%", fontWeight = FontWeight.Bold) },
                        placeholder = { Text("0") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = deliveryMarginText,
                        onValueChange = { input ->
                            if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                                deliveryMarginText = input
                                onDefaultDeliveryMarginChange(input.toDoubleOrNull() ?: 0.0)
                            }
                        },
                        label = { Text(stringResource(Res.string.delivery_margin_label)) },
                        suffix = { Text("%", fontWeight = FontWeight.Bold) },
                        placeholder = { Text("0") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Card: Redondeo de Precios
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.rounding_section_title),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    SyncedSettingBadge()
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(Res.string.rounding_section_subtitle),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Master Toggle: Activar redondeo
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                        Text(
                            text = stringResource(Res.string.enable_rounding_title),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(Res.string.enable_rounding_subtitle),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = isRoundingEnabled,
                        onCheckedChange = onIsRoundingEnabledChange
                    )
                }

                // Secondary Toggles visible when master is enabled
                AnimatedVisibility(
                    visible = isRoundingEnabled,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Column(modifier = Modifier.padding(top = 16.dp)) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            thickness = 1.dp
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // Secondary Toggle 1: Redondear precios al guardar producto
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                                Text(
                                    text = stringResource(Res.string.round_product_prices_title),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = stringResource(Res.string.round_product_prices_subtitle),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Switch(
                                checked = roundProductPrices,
                                onCheckedChange = onRoundProductPricesChange
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            thickness = 1.dp
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // Secondary Toggle 3: Redondear total del ticket antes del cobro
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                                Text(
                                    text = stringResource(Res.string.round_ticket_total_title),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = stringResource(Res.string.round_ticket_total_subtitle),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Switch(
                                checked = roundTicketTotal,
                                onCheckedChange = onRoundTicketTotalChange
                            )
                        }
                    }
                }
            }
        }

        // Card: Políticas de Cobro
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.payment_policies_section_title),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    SyncedSettingBadge()
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(Res.string.payment_policies_section_subtitle),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                        Text(
                            text = stringResource(Res.string.disallow_card_on_wholesale_title),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(Res.string.disallow_card_on_wholesale_subtitle),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = disallowCardPaymentOnWholesale,
                        onCheckedChange = onDisallowCardPaymentOnWholesaleChange
                    )
                }
            }
        }
        // Card: Modalidad de Venta a Domicilio 
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = stringResource(Res.string.delivery_sales_section_title),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(Res.string.delivery_sales_section_subtitle),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                        Text(
                            text = stringResource(Res.string.prioritize_delivery_price_title),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(Res.string.prioritize_delivery_price_subtitle),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = prioritizeDeliveryPrice,
                        onCheckedChange = onPrioritizeDeliveryPriceChange
                    )
                }
            }
        }

        // Card: Mayoreo Automático
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = stringResource(Res.string.auto_wholesale_section_title),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(Res.string.auto_wholesale_section_subtitle),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Trigger 1: Por cantidad de piezas de un producto
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                        Text(
                            text = stringResource(Res.string.auto_wholesale_by_quantity_title),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(Res.string.auto_wholesale_by_quantity_subtitle),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = autoWholesaleByQuantity,
                        onCheckedChange = onAutoWholesaleByQuantityChange
                    )
                }

                AnimatedVisibility(
                    visible = autoWholesaleByQuantity,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Column(modifier = Modifier.padding(top = 12.dp)) {
                        OutlinedTextField(
                            value = quantityThresholdText,
                            onValueChange = { input ->
                                if (input.isEmpty() || input.matches(Regex("^\\d+$"))) {
                                    quantityThresholdText = input
                                    onAutoWholesaleQuantityThresholdChange(input.toIntOrNull() ?: 0)
                                }
                            },
                            label = { Text(stringResource(Res.string.auto_wholesale_quantity_threshold_label)) },
                            suffix = { Text(stringResource(Res.string.auto_wholesale_pieces_suffix), fontWeight = FontWeight.Medium) },
                            placeholder = { Text("3") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(0.5f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    thickness = 1.dp
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Trigger 2: Por monto total del ticket
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                        Text(
                            text = stringResource(Res.string.auto_wholesale_by_total_title),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(Res.string.auto_wholesale_by_total_subtitle),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = autoWholesaleByTicketTotal,
                        onCheckedChange = onAutoWholesaleByTicketTotalChange
                    )
                }

                AnimatedVisibility(
                    visible = autoWholesaleByTicketTotal,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Column(modifier = Modifier.padding(top = 12.dp)) {
                        OutlinedTextField(
                            value = ticketTotalThresholdText,
                            onValueChange = { input ->
                                if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                                    ticketTotalThresholdText = input
                                    onAutoWholesaleTicketTotalThresholdChange(input.toDoubleOrNull() ?: 0.0)
                                }
                            },
                            label = { Text(stringResource(Res.string.auto_wholesale_total_threshold_label)) },
                            prefix = { Text("$", fontWeight = FontWeight.Bold) },
                            placeholder = { Text("1000") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(0.5f)
                        )
                    }
                }
            }
        }
    }
}
