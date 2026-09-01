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
import poskmp.shared.generated.resources.default_margins_section_subtitle
import poskmp.shared.generated.resources.default_margins_section_title
import poskmp.shared.generated.resources.disallow_card_on_wholesale_subtitle
import poskmp.shared.generated.resources.disallow_card_on_wholesale_title
import poskmp.shared.generated.resources.enable_rounding_subtitle
import poskmp.shared.generated.resources.enable_rounding_title
import poskmp.shared.generated.resources.payment_policies_section_subtitle
import poskmp.shared.generated.resources.payment_policies_section_title
import poskmp.shared.generated.resources.retail_margin_label
import poskmp.shared.generated.resources.round_retail_price_subtitle
import poskmp.shared.generated.resources.round_retail_price_title
import poskmp.shared.generated.resources.round_ticket_total_subtitle
import poskmp.shared.generated.resources.round_ticket_total_title
import poskmp.shared.generated.resources.round_wholesale_price_subtitle
import poskmp.shared.generated.resources.round_wholesale_price_title
import poskmp.shared.generated.resources.rounding_section_subtitle
import poskmp.shared.generated.resources.rounding_section_title
import poskmp.shared.generated.resources.wholesale_margin_label

@Composable
fun PricingSettingsSection(
    defaultRetailMargin: Double,
    onDefaultRetailMarginChange: (Double) -> Unit,
    defaultWholesaleMargin: Double,
    onDefaultWholesaleMarginChange: (Double) -> Unit,
    isRoundingEnabled: Boolean,
    onIsRoundingEnabledChange: (Boolean) -> Unit,
    roundRetailPrice: Boolean,
    onRoundRetailPriceChange: (Boolean) -> Unit,
    roundWholesalePrice: Boolean,
    onRoundWholesalePriceChange: (Boolean) -> Unit,
    roundTicketTotal: Boolean,
    onRoundTicketTotalChange: (Boolean) -> Unit,
    disallowCardPaymentOnWholesale: Boolean,
    onDisallowCardPaymentOnWholesaleChange: (Boolean) -> Unit,
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

                        // Secondary Toggle 1: Redondear precio de venta al guardar producto
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                                Text(
                                    text = stringResource(Res.string.round_retail_price_title),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = stringResource(Res.string.round_retail_price_subtitle),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Switch(
                                checked = roundRetailPrice,
                                onCheckedChange = onRoundRetailPriceChange
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            thickness = 1.dp
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // Secondary Toggle 2: Redondear precio de mayoreo al guardar producto
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                                Text(
                                    text = stringResource(Res.string.round_wholesale_price_title),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = stringResource(Res.string.round_wholesale_price_subtitle),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Switch(
                                checked = roundWholesalePrice,
                                onCheckedChange = onRoundWholesalePriceChange
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
    }
}
