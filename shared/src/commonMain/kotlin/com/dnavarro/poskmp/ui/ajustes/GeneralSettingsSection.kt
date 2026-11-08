package com.dnavarro.poskmp.ui.ajustes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dnavarro.poskmp.theme.ShapeDefaults
import com.dnavarro.poskmp.ui.Screen
import com.dnavarro.poskmp.util.AdaptiveBreakpoints
import com.dnavarro.poskmp.util.connectedToggleButtonShapes
import com.dnavarro.poskmp.util.isAndroid
import com.dnavarro.poskmp.util.pickDirectory
import com.dnavarro.poskmp.util.pickSingleFile
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import poskmp.shared.generated.resources.Res
import poskmp.shared.generated.resources.barcode_scanner
import poskmp.shared.generated.resources.beep_on_barcode_scan_subtitle
import poskmp.shared.generated.resources.beep_on_barcode_scan_title
import poskmp.shared.generated.resources.catalog_layout_grid
import poskmp.shared.generated.resources.catalog_layout_subtitle
import poskmp.shared.generated.resources.catalog_layout_table
import poskmp.shared.generated.resources.catalog_layout_title
import poskmp.shared.generated.resources.checador_background_subtitle
import poskmp.shared.generated.resources.checador_background_title
import poskmp.shared.generated.resources.checador_bg_clear
import poskmp.shared.generated.resources.checador_bg_custom_image
import poskmp.shared.generated.resources.checador_bg_no_selection
import poskmp.shared.generated.resources.checador_bg_pick_folder
import poskmp.shared.generated.resources.checador_bg_pick_image
import poskmp.shared.generated.resources.checador_bg_shader
import poskmp.shared.generated.resources.checador_layout_dialog
import poskmp.shared.generated.resources.checador_layout_fullscreen
import poskmp.shared.generated.resources.checador_layout_subtitle
import poskmp.shared.generated.resources.checador_layout_title
import poskmp.shared.generated.resources.close
import poskmp.shared.generated.resources.default_screen_subtitle
import poskmp.shared.generated.resources.default_screen_title
import poskmp.shared.generated.resources.fullscreen
import poskmp.shared.generated.resources.grid
import poskmp.shared.generated.resources.light_mode
import poskmp.shared.generated.resources.list
import poskmp.shared.generated.resources.photo_camera
import poskmp.shared.generated.resources.pip
import poskmp.shared.generated.resources.point_of_sale
import poskmp.shared.generated.resources.products
import poskmp.shared.generated.resources.show_extra_prices_checador_subtitle
import poskmp.shared.generated.resources.show_extra_prices_checador_title
import poskmp.shared.generated.resources.swap_venta_layout_order_subtitle
import poskmp.shared.generated.resources.swap_venta_layout_order_title
import poskmp.shared.generated.resources.tab_checador
import poskmp.shared.generated.resources.tab_productos
import poskmp.shared.generated.resources.tab_venta
import poskmp.shared.generated.resources.upload

private val CHECADOR_IMAGE_EXTENSIONS = listOf("png", "jpg", "jpeg", "webp", "bmp", "gif")

@Composable
fun GeneralSettingsSection(
    defaultScreen: Screen,
    onDefaultScreenChange: (Screen) -> Unit,
    isChecadorDialog: Boolean,
    onIsChecadorDialogChange: (Boolean) -> Unit,
    showExtraPricesChecador: Boolean,
    onShowExtraPricesChecadorChange: (Boolean) -> Unit,
    useCustomChecadorBackground: Boolean = false,
    onUseCustomChecadorBackgroundChange: (Boolean) -> Unit = {},
    checadorBackgroundPath: String = "",
    onChecadorBackgroundPathChange: (String) -> Unit = {},
    useProductTableInCatalog: Boolean,
    onUseProductTableInCatalogChange: (Boolean) -> Unit,
    swapVentaLayoutOrder: Boolean,
    onSwapVentaLayoutOrderChange: (Boolean) -> Unit,
    beepOnBarcodeScan: Boolean = false,
    onBeepOnBarcodeScanChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        // 1. Pantalla Principal al Abrir
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            shape = ShapeDefaults.topListItemShape,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = stringResource(Res.string.default_screen_title),
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(Res.string.default_screen_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

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

                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val isNarrow = maxWidth < AdaptiveBreakpoints.SegmentedGroupWrapWidth
                    val isCompactWidth = maxWidth < AdaptiveBreakpoints.SegmentedGroupCompactWidth
                    val rows = when {
                        isCompactWidth -> options.chunked(1)
                        isNarrow -> options.chunked(2)
                        else -> listOf(options)
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
                    ) {
                        rows.forEach { rowOptions ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                rowOptions.forEachIndexed { index, (screenOption, label, iconRes) ->
                                    val isSelected = defaultScreen == screenOption
                                    ToggleButton(
                                        checked = isSelected,
                                        onCheckedChange = { onDefaultScreenChange(screenOption) },
                                        colors = ToggleButtonDefaults.colors(
                                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            checkedContainerColor = MaterialTheme.colorScheme.primary,
                                            checkedContentColor = MaterialTheme.colorScheme.onPrimary
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .semantics { role = Role.RadioButton },
                                        shapes = connectedToggleButtonShapes(index, rowOptions.size)
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
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodySmall,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Modo Checador (Solo Escritorio)
        if (!isAndroid()) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                shape = ShapeDefaults.middleListItemShape,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = stringResource(Res.string.checador_layout_title),
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(Res.string.checador_layout_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

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

                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        val isCompactWidth = maxWidth < AdaptiveBreakpoints.SegmentedGroupCompactWidth
                        val rows = if (isCompactWidth) checadorOptions.chunked(1) else listOf(checadorOptions)

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
                        ) {
                            rows.forEach { rowOptions ->
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    rowOptions.forEachIndexed { index, (isDialogOption, label, icon) ->
                                        val isSelected = isChecadorDialog == isDialogOption
                                        ToggleButton(
                                            checked = isSelected,
                                            onCheckedChange = { onIsChecadorDialogChange(isDialogOption) },
                                            colors = ToggleButtonDefaults.colors(
                                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                                checkedContainerColor = MaterialTheme.colorScheme.primary,
                                                checkedContentColor = MaterialTheme.colorScheme.onPrimary
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .semantics { role = Role.RadioButton },
                                            shapes = connectedToggleButtonShapes(index, rowOptions.size)
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
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Show Extra Prices in Checador Toggle (Costo y Mayoreo)
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            shape = ShapeDefaults.middleListItemShape,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                    Text(
                        text = stringResource(Res.string.show_extra_prices_checador_title),
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(Res.string.show_extra_prices_checador_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Switch(
                    checked = showExtraPricesChecador,
                    onCheckedChange = onShowExtraPricesChecadorChange
                )
            }
        }

        // 4. Checador Fullscreen Background Mode (Shader vs Custom Image / Folder) (Solo Escritorio)
        if (!isAndroid()) {
            var pickerError by remember { mutableStateOf<String?>(null) }
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                shape = ShapeDefaults.middleListItemShape,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = stringResource(Res.string.checador_background_title),
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(Res.string.checador_background_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val bgOptions = listOf(
                        Triple(
                            false,
                            stringResource(Res.string.checador_bg_shader),
                            Res.drawable.light_mode
                        ),
                        Triple(
                            true,
                            stringResource(Res.string.checador_bg_custom_image),
                            Res.drawable.photo_camera
                        )
                    )

                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        val isCompactWidth = maxWidth < AdaptiveBreakpoints.SegmentedGroupCompactWidth
                        val rows = if (isCompactWidth) bgOptions.chunked(1) else listOf(bgOptions)

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
                        ) {
                            rows.forEach { rowOptions ->
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    rowOptions.forEachIndexed { index, (isCustomOption, label, icon) ->
                                        val isSelected = useCustomChecadorBackground == isCustomOption
                                        ToggleButton(
                                            checked = isSelected,
                                            onCheckedChange = {
                                                pickerError = null
                                                onUseCustomChecadorBackgroundChange(isCustomOption)
                                            },
                                            colors = ToggleButtonDefaults.colors(
                                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                                checkedContainerColor = MaterialTheme.colorScheme.primary,
                                                checkedContentColor = MaterialTheme.colorScheme.onPrimary
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .semantics { role = Role.RadioButton },
                                            shapes = connectedToggleButtonShapes(index, rowOptions.size)
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
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    AnimatedVisibility(visible = useCustomChecadorBackground) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(
                                            MaterialTheme.colorScheme.surfaceContainerHigh,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 12.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        text = checadorBackgroundPath.ifBlank {
                                            stringResource(Res.string.checador_bg_no_selection)
                                        },
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = if (checadorBackgroundPath.isNotBlank()) FontFamily.Monospace else FontFamily.Default,
                                            fontSize = 12.sp
                                        ),
                                        color = if (checadorBackgroundPath.isNotBlank()) {
                                            MaterialTheme.colorScheme.onSurface
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                if (checadorBackgroundPath.isNotBlank()) {
                                    IconButton(
                                        onClick = {
                                            pickerError = null
                                            onChecadorBackgroundPathChange("")
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(Res.drawable.close),
                                            contentDescription = stringResource(Res.string.checador_bg_clear),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        pickerError = null
                                        pickSingleFile(
                                            allowedExtensions = CHECADOR_IMAGE_EXTENSIONS,
                                            onFilePicked = { filePath ->
                                                onChecadorBackgroundPathChange(filePath)
                                                onUseCustomChecadorBackgroundChange(true)
                                            },
                                            onError = { err -> pickerError = err }
                                        )
                                    },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                    shape = MaterialTheme.shapes.small,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        painter = painterResource(Res.drawable.photo_camera),
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = stringResource(Res.string.checador_bg_pick_image),
                                        style = MaterialTheme.typography.labelMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                OutlinedButton(
                                    onClick = {
                                        pickerError = null
                                        pickDirectory(
                                            initialPath = checadorBackgroundPath,
                                            onDirectoryPicked = { dirPath ->
                                                onChecadorBackgroundPathChange(dirPath)
                                                onUseCustomChecadorBackgroundChange(true)
                                            },
                                            onError = { err -> pickerError = err }
                                        )
                                    },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                    shape = MaterialTheme.shapes.small,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        painter = painterResource(Res.drawable.upload),
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = stringResource(Res.string.checador_bg_pick_folder),
                                        style = MaterialTheme.typography.labelMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            if (!pickerError.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = pickerError!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Catalog Layout in Sale Screen Toggle (Tarjetas vs Lista/Tabla)
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            shape = ShapeDefaults.middleListItemShape,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = stringResource(Res.string.catalog_layout_title),
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(Res.string.catalog_layout_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                val catalogLayoutOptions = listOf(
                    Triple(
                        false,
                        stringResource(Res.string.catalog_layout_grid),
                        Res.drawable.grid
                    ),
                    Triple(
                        true,
                        stringResource(Res.string.catalog_layout_table),
                        Res.drawable.list
                    )
                )

                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val isCompactWidth = maxWidth < AdaptiveBreakpoints.SegmentedGroupCompactWidth
                    val rows = if (isCompactWidth) catalogLayoutOptions.chunked(1) else listOf(catalogLayoutOptions)

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
                    ) {
                        rows.forEach { rowOptions ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                rowOptions.forEachIndexed { index, (useTableOption, label, icon) ->
                                    val isSelected = useProductTableInCatalog == useTableOption
                                    ToggleButton(
                                        checked = isSelected,
                                        onCheckedChange = { onUseProductTableInCatalogChange(useTableOption) },
                                        colors = ToggleButtonDefaults.colors(
                                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            checkedContainerColor = MaterialTheme.colorScheme.primary,
                                            checkedContentColor = MaterialTheme.colorScheme.onPrimary
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .semantics { role = Role.RadioButton },
                                        shapes = connectedToggleButtonShapes(index, rowOptions.size)
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
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodySmall,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Swap Venta Layout Order Toggle (Ticket on left, Catalog on right)
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            shape = ShapeDefaults.middleListItemShape,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                    Text(
                        text = stringResource(Res.string.swap_venta_layout_order_title),
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(Res.string.swap_venta_layout_order_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Switch(
                    checked = swapVentaLayoutOrder,
                    onCheckedChange = onSwapVentaLayoutOrderChange
                )
            }
        }

        // 6. Beep on Barcode Scan Success Toggle
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            shape = ShapeDefaults.bottomListItemShape,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                    Text(
                        text = stringResource(Res.string.beep_on_barcode_scan_title),
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(Res.string.beep_on_barcode_scan_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Switch(
                    checked = beepOnBarcodeScan,
                    onCheckedChange = onBeepOnBarcodeScanChange
                )
            }
        }
    }
}
