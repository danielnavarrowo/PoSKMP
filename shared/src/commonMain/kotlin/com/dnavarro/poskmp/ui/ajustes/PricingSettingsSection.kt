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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dnavarro.poskmp.theme.ShapeDefaults
import com.dnavarro.poskmp.ui.components.SyncedSettingBadge
import org.jetbrains.compose.resources.stringResource
import poskmp.shared.generated.resources.Res
import poskmp.shared.generated.resources.auto_wholesale_by_quantity_subtitle
import poskmp.shared.generated.resources.auto_wholesale_by_quantity_title
import poskmp.shared.generated.resources.auto_wholesale_by_total_subtitle
import poskmp.shared.generated.resources.auto_wholesale_by_total_title
import poskmp.shared.generated.resources.auto_wholesale_pieces_suffix
import poskmp.shared.generated.resources.auto_wholesale_quantity_threshold_label
import poskmp.shared.generated.resources.auto_wholesale_section_title
import poskmp.shared.generated.resources.auto_wholesale_total_threshold_label
import poskmp.shared.generated.resources.autofilling_title
import poskmp.shared.generated.resources.default_margins_section_subtitle
import poskmp.shared.generated.resources.default_margins_section_title
import poskmp.shared.generated.resources.delivery_margin_label
import poskmp.shared.generated.resources.disallow_card_on_wholesale_subtitle
import poskmp.shared.generated.resources.disallow_card_on_wholesale_title
import poskmp.shared.generated.resources.enable_barcode_lookup_subtitle
import poskmp.shared.generated.resources.enable_barcode_lookup_title
import poskmp.shared.generated.resources.enable_gemini_grounding_subtitle
import poskmp.shared.generated.resources.enable_gemini_grounding_title
import poskmp.shared.generated.resources.enable_rounding_subtitle
import poskmp.shared.generated.resources.enable_rounding_title
import poskmp.shared.generated.resources.gemini_api_key_helper
import poskmp.shared.generated.resources.gemini_api_key_label
import poskmp.shared.generated.resources.gemini_api_key_placeholder
import poskmp.shared.generated.resources.gemini_api_key_warning
import poskmp.shared.generated.resources.google_search_api_key_helper
import poskmp.shared.generated.resources.google_search_api_key_label
import poskmp.shared.generated.resources.google_search_api_key_placeholder
import poskmp.shared.generated.resources.google_search_engine_id_helper
import poskmp.shared.generated.resources.google_search_engine_id_label
import poskmp.shared.generated.resources.google_search_engine_id_placeholder
import poskmp.shared.generated.resources.prioritize_delivery_price_subtitle
import poskmp.shared.generated.resources.prioritize_delivery_price_title
import poskmp.shared.generated.resources.retail_margin_label
import poskmp.shared.generated.resources.round_product_prices_subtitle
import poskmp.shared.generated.resources.round_product_prices_title
import poskmp.shared.generated.resources.round_ticket_total_subtitle
import poskmp.shared.generated.resources.round_ticket_total_title
import poskmp.shared.generated.resources.rounding_section_title
import poskmp.shared.generated.resources.supabase_hide_key
import poskmp.shared.generated.resources.supabase_show_key
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
    autoLookupBarcodeProducts: Boolean = true,
    onAutoLookupBarcodeProductsChange: (Boolean) -> Unit = {},
    geminiGroundingEnabled: Boolean = false,
    onGeminiGroundingEnabledChange: (Boolean) -> Unit = {},
    geminiApiKey: String = "",
    onGeminiApiKeyChange: (String) -> Unit = {},
    googleSearchEngineId: String = "",
    onGoogleSearchEngineIdChange: (String) -> Unit = {},
    googleSearchApiKey: String = "",
    onGoogleSearchApiKeyChange: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var localGeminiApiKey by remember(geminiApiKey) { mutableStateOf(geminiApiKey) }
    var isApiKeyVisible by remember { mutableStateOf(false) }
    var localGoogleSearchEngineId by remember(googleSearchEngineId) {
        mutableStateOf(
            googleSearchEngineId
        )
    }
    var localGoogleSearchApiKey by remember(googleSearchApiKey) { mutableStateOf(googleSearchApiKey) }
    var isSearchApiKeyVisible by remember { mutableStateOf(false) }
    var retailMarginText by remember(defaultRetailMargin) {
        mutableStateOf(
            if (defaultRetailMargin > 0.0) {
                if (defaultRetailMargin % 1.0 == 0.0) defaultRetailMargin.toLong()
                    .toString() else defaultRetailMargin.toString()
            } else ""
        )
    }
    var wholesaleMarginText by remember(defaultWholesaleMargin) {
        mutableStateOf(
            if (defaultWholesaleMargin > 0.0) {
                if (defaultWholesaleMargin % 1.0 == 0.0) defaultWholesaleMargin.toLong()
                    .toString() else defaultWholesaleMargin.toString()
            } else ""
        )
    }
    var deliveryMarginText by remember(defaultDeliveryMargin) {
        mutableStateOf(
            if (defaultDeliveryMargin > 0.0) {
                if (defaultDeliveryMargin % 1.0 == 0.0) defaultDeliveryMargin.toLong()
                    .toString() else defaultDeliveryMargin.toString()
            } else ""
        )
    }
    var quantityThresholdText by remember(autoWholesaleQuantityThreshold) {
        mutableStateOf(if (autoWholesaleQuantityThreshold > 0) autoWholesaleQuantityThreshold.toString() else "")
    }
    var ticketTotalThresholdText by remember(autoWholesaleTicketTotalThreshold) {
        mutableStateOf(
            if (autoWholesaleTicketTotalThreshold > 0.0) {
                if (autoWholesaleTicketTotalThreshold % 1.0 == 0.0) autoWholesaleTicketTotalThreshold.toLong()
                    .toString() else autoWholesaleTicketTotalThreshold.toString()
            } else ""
        )
    }

    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column (verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                shape = ShapeDefaults.topListItemShape,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                        Text(
                            text = stringResource(Res.string.disallow_card_on_wholesale_title),
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(Res.string.disallow_card_on_wholesale_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = disallowCardPaymentOnWholesale,
                        onCheckedChange = onDisallowCardPaymentOnWholesaleChange
                    )
                }
            }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                shape = ShapeDefaults.middleListItemShape,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                        Text(
                            text = stringResource(Res.string.prioritize_delivery_price_title),
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(Res.string.prioritize_delivery_price_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = prioritizeDeliveryPrice,
                        onCheckedChange = onPrioritizeDeliveryPriceChange
                    )
                }
            }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                shape = ShapeDefaults.bottomListItemShape,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(Res.string.default_margins_section_title),
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        SyncedSettingBadge()
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(Res.string.default_margins_section_subtitle),
                        style = MaterialTheme.typography.bodySmall,
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
        }


        Spacer(modifier = Modifier.height(24.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp)
        ) {
            Text(
                text = stringResource(Res.string.rounding_section_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            SyncedSettingBadge()
        }
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            shape = if (isRoundingEnabled) ShapeDefaults.topListItemShape else ShapeDefaults.cardShape,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                    Text(
                        text = stringResource(Res.string.enable_rounding_title),
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(Res.string.enable_rounding_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Switch(
                    checked = isRoundingEnabled,
                    onCheckedChange = onIsRoundingEnabledChange
                )
            }
        }

        AnimatedVisibility(
            visible = isRoundingEnabled,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Column {
                Spacer(modifier = Modifier.height(2.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    shape = ShapeDefaults.middleListItemShape,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                            Text(
                                text = stringResource(Res.string.round_product_prices_title),
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(Res.string.round_product_prices_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Switch(
                            checked = roundProductPrices,
                            onCheckedChange = onRoundProductPricesChange
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    shape = ShapeDefaults.bottomListItemShape,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                            Text(
                                text = stringResource(Res.string.round_ticket_total_title),
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(Res.string.round_ticket_total_subtitle),
                                style = MaterialTheme.typography.bodySmall,
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

        Spacer(modifier = Modifier.height(24.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp)
        ) {
            Text(
                text = stringResource(Res.string.auto_wholesale_section_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                shape = ShapeDefaults.topListItemShape,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                            Text(
                                text = stringResource(Res.string.auto_wholesale_by_quantity_title),
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(Res.string.auto_wholesale_by_quantity_subtitle),
                                style = MaterialTheme.typography.bodySmall,
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
                                suffix = {
                                    Text(
                                        stringResource(Res.string.auto_wholesale_pieces_suffix),
                                        fontWeight = FontWeight.Medium
                                    )
                                },
                                placeholder = { Text("3") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                shape = ShapeDefaults.bottomListItemShape,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // Trigger 1: Por cantidad de piezas de un producto
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                            Text(
                                text = stringResource(Res.string.auto_wholesale_by_total_title),
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(Res.string.auto_wholesale_by_total_subtitle),
                                style = MaterialTheme.typography.bodySmall,
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
                                        onAutoWholesaleTicketTotalThresholdChange(
                                            input.toDoubleOrNull() ?: 0.0
                                        )
                                    }
                                },
                                label = { Text(stringResource(Res.string.auto_wholesale_total_threshold_label)) },
                                prefix = { Text("$", fontWeight = FontWeight.Bold) },
                                placeholder = { Text("1000") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp)
        ) {
            Text(
                text = stringResource(Res.string.autofilling_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            shape = ShapeDefaults.topListItemShape,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                        Text(
                            text = stringResource(Res.string.enable_barcode_lookup_title),
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(Res.string.enable_barcode_lookup_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = autoLookupBarcodeProducts,
                        onCheckedChange = onAutoLookupBarcodeProductsChange
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            shape = ShapeDefaults.bottomListItemShape,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                        Text(
                            text = stringResource(Res.string.enable_gemini_grounding_title),
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(Res.string.enable_gemini_grounding_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = geminiGroundingEnabled,
                        onCheckedChange = onGeminiGroundingEnabledChange
                    )
                }

                AnimatedVisibility(
                    visible = geminiGroundingEnabled,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                        val isSuspiciousKey = localGeminiApiKey.isNotBlank() &&
                                !localGeminiApiKey.trim().startsWith("AIzaSy") &&
                                !localGeminiApiKey.trim().startsWith("AQ.")
                        OutlinedTextField(
                            value = localGeminiApiKey,
                            onValueChange = {
                                localGeminiApiKey = it
                                onGeminiApiKeyChange(it)
                            },
                            label = { Text(stringResource(Res.string.gemini_api_key_label)) },
                            placeholder = { Text(stringResource(Res.string.gemini_api_key_placeholder)) },
                            supportingText = {
                                if (isSuspiciousKey) {
                                    Text(
                                        text = stringResource(Res.string.gemini_api_key_warning),
                                        color = MaterialTheme.colorScheme.error
                                    )
                                } else {
                                    Text(stringResource(Res.string.gemini_api_key_helper))
                                }
                            },
                            isError = isSuspiciousKey,
                            singleLine = true,
                            visualTransformation = if (isApiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                TextButton(onClick = { isApiKeyVisible = !isApiKeyVisible }) {
                                    Text(
                                        text = stringResource(if (isApiKeyVisible) Res.string.supabase_hide_key else Res.string.supabase_show_key),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = localGoogleSearchEngineId,
                            onValueChange = {
                                localGoogleSearchEngineId = it
                                onGoogleSearchEngineIdChange(it)
                            },
                            label = { Text(stringResource(Res.string.google_search_engine_id_label)) },
                            placeholder = { Text(stringResource(Res.string.google_search_engine_id_placeholder)) },
                            supportingText = {
                                Text(stringResource(Res.string.google_search_engine_id_helper))
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = localGoogleSearchApiKey,
                            onValueChange = {
                                localGoogleSearchApiKey = it
                                onGoogleSearchApiKeyChange(it)
                            },
                            label = { Text(stringResource(Res.string.google_search_api_key_label)) },
                            placeholder = { Text(stringResource(Res.string.google_search_api_key_placeholder)) },
                            supportingText = {
                                Text(stringResource(Res.string.google_search_api_key_helper))
                            },
                            singleLine = true,
                            visualTransformation = if (isSearchApiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                TextButton(onClick = {
                                    isSearchApiKeyVisible = !isSearchApiKeyVisible
                                }) {
                                    Text(
                                        text = stringResource(if (isSearchApiKeyVisible) Res.string.supabase_hide_key else Res.string.supabase_show_key),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}