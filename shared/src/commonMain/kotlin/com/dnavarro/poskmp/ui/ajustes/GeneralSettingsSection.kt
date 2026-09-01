package com.dnavarro.poskmp.ui.ajustes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Badge
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dnavarro.poskmp.ui.Screen
import com.dnavarro.poskmp.util.isAndroid
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import poskmp.shared.generated.resources.Res
import poskmp.shared.generated.resources.barcode_scanner
import poskmp.shared.generated.resources.card
import poskmp.shared.generated.resources.catalog_layout_grid
import poskmp.shared.generated.resources.catalog_layout_subtitle
import poskmp.shared.generated.resources.catalog_layout_table
import poskmp.shared.generated.resources.catalog_layout_title
import poskmp.shared.generated.resources.checador_layout_dialog
import poskmp.shared.generated.resources.checador_layout_fullscreen
import poskmp.shared.generated.resources.checador_layout_subtitle
import poskmp.shared.generated.resources.checador_layout_title
import poskmp.shared.generated.resources.check
import poskmp.shared.generated.resources.database_section_title
import poskmp.shared.generated.resources.default_screen_subtitle
import poskmp.shared.generated.resources.default_screen_title
import poskmp.shared.generated.resources.fullscreen
import poskmp.shared.generated.resources.local_db_connected_desc
import poskmp.shared.generated.resources.local_db_status_connected
import poskmp.shared.generated.resources.local_db_title
import poskmp.shared.generated.resources.pip
import poskmp.shared.generated.resources.point_of_sale
import poskmp.shared.generated.resources.products
import poskmp.shared.generated.resources.show_extra_prices_checador_subtitle
import poskmp.shared.generated.resources.show_extra_prices_checador_title
import poskmp.shared.generated.resources.status_connected
import poskmp.shared.generated.resources.swap_venta_layout_order_subtitle
import poskmp.shared.generated.resources.swap_venta_layout_order_title
import poskmp.shared.generated.resources.tab_checador
import poskmp.shared.generated.resources.tab_productos
import poskmp.shared.generated.resources.tab_venta

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GeneralSettingsSection(
    defaultScreen: Screen,
    onDefaultScreenChange: (Screen) -> Unit,
    isChecadorDialog: Boolean,
    onIsChecadorDialogChange: (Boolean) -> Unit,
    showExtraPricesChecador: Boolean,
    onShowExtraPricesChecadorChange: (Boolean) -> Unit,
    useProductTableInCatalog: Boolean,
    onUseProductTableInCatalogChange: (Boolean) -> Unit,
    swapVentaLayoutOrder: Boolean,
    onSwapVentaLayoutOrderChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Card: Comportamiento y Pantallas
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Comportamiento general",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Pantalla Principal al Abrir
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(Res.string.default_screen_title),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(Res.string.default_screen_subtitle),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val options = listOf(
                            Triple(
                                Screen.VENTA,
                                stringResource(Res.string.tab_venta),
                                Res.drawable.point_of_sale
                            ),
                            Triple(
                                Screen.PRODUCTOS,
                                stringResource(Res.string.tab_productos),
                                Res.drawable.products
                            ),
                            Triple(
                                Screen.CHECADOR,
                                stringResource(Res.string.tab_checador),
                                Res.drawable.barcode_scanner
                            )
                        )
                        options.forEachIndexed { index, (screenOption, label, iconRes) ->
                            val isSelected = defaultScreen == screenOption
                            ToggleButton(
                                checked = isSelected,
                                onCheckedChange = { onDefaultScreenChange(screenOption) },
                                colors = ToggleButtonDefaults.toggleButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    checkedContainerColor = MaterialTheme.colorScheme.primary,
                                    checkedContentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .semantics { role = Role.RadioButton },
                                shapes = when (index) {
                                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                    options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                                }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        painter = painterResource(iconRes),
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = label,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    thickness = 1.dp
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Modo Checador (Solo Escritorio)
                if (!isAndroid()) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = stringResource(Res.string.checador_layout_title),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(Res.string.checador_layout_subtitle),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val checadorOptions = listOf(
                                Triple(
                                    true,
                                    stringResource(Res.string.checador_layout_dialog),
                                    Res.drawable.pip
                                ),
                                Triple(
                                    false,
                                    stringResource(Res.string.checador_layout_fullscreen),
                                    Res.drawable.fullscreen
                                )
                            )
                            checadorOptions.forEachIndexed { index, (isDialogOption, label, icon) ->
                                val isSelected = isChecadorDialog == isDialogOption
                                ToggleButton(
                                    checked = isSelected,
                                    onCheckedChange = { onIsChecadorDialogChange(isDialogOption) },
                                    colors = ToggleButtonDefaults.toggleButtonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        checkedContainerColor = MaterialTheme.colorScheme.primary,
                                        checkedContentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .semantics { role = Role.RadioButton },
                                    shapes = when (index) {
                                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                        checadorOptions.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                                    }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(icon),
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = label,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        thickness = 1.dp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Show Extra Prices in Checador Toggle (Costo y Mayoreo)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                        Text(
                            text = stringResource(Res.string.show_extra_prices_checador_title),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(Res.string.show_extra_prices_checador_subtitle),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = showExtraPricesChecador,
                        onCheckedChange = onShowExtraPricesChecadorChange
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    thickness = 1.dp
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Catalog Layout in Sale Screen Toggle (Tarjetas vs Lista/Tabla)
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(Res.string.catalog_layout_title),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(Res.string.catalog_layout_subtitle),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val catalogLayoutOptions = listOf(
                            Triple(
                                false,
                                stringResource(Res.string.catalog_layout_grid),
                                Res.drawable.card
                            ),
                            Triple(
                                true,
                                stringResource(Res.string.catalog_layout_table),
                                Res.drawable.products
                            )
                        )
                        catalogLayoutOptions.forEachIndexed { index, (useTableOption, label, icon) ->
                            val isSelected = useProductTableInCatalog == useTableOption
                            ToggleButton(
                                checked = isSelected,
                                onCheckedChange = { onUseProductTableInCatalogChange(useTableOption) },
                                colors = ToggleButtonDefaults.toggleButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    checkedContainerColor = MaterialTheme.colorScheme.primary,
                                    checkedContentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .semantics { role = Role.RadioButton },
                                shapes = when (index) {
                                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                    catalogLayoutOptions.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                                }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        painter = painterResource(icon),
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = label,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    thickness = 1.dp
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Swap Venta Layout Order Toggle (Ticket on left, Catalog on right)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                        Text(
                            text = stringResource(Res.string.swap_venta_layout_order_title),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(Res.string.swap_venta_layout_order_subtitle),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = swapVentaLayoutOrder,
                        onCheckedChange = onSwapVentaLayoutOrderChange
                    )
                }
            }
        }

        // Card: Base de Datos Local Status
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = stringResource(Res.string.database_section_title),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(Res.drawable.check),
                            contentDescription = stringResource(Res.string.local_db_connected_desc),
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = stringResource(Res.string.local_db_title),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(Res.string.local_db_status_connected),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Badge(
                        containerColor = Color(0xFFD1FAE5),
                        contentColor = Color(0xFF065F46)
                    ) {
                        Text(
                            stringResource(Res.string.status_connected),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}
