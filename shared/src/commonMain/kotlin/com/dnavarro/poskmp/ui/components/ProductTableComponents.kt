package com.dnavarro.poskmp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dnavarro.poskmp.db.Products
import com.dnavarro.poskmp.domain.model.ProductSalesStats
import com.dnavarro.poskmp.theme.ShapeDefaults
import com.dnavarro.poskmp.ui.ProductSortField
import com.dnavarro.poskmp.ui.ProductSortOrder
import com.dnavarro.poskmp.ui.productos.ProductTableColumn
import com.dnavarro.poskmp.util.formatBarcodesForDisplay
import com.dnavarro.poskmp.util.formatEpochMillisToDateTime
import com.dnavarro.poskmp.util.formatPrice
import com.dnavarro.poskmp.util.formatQuantity
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import poskmp.shared.generated.resources.Res
import poskmp.shared.generated.resources.arrow_up
import poskmp.shared.generated.resources.cost_display_label
import poskmp.shared.generated.resources.disabled
import poskmp.shared.generated.resources.favorite_desc
import poskmp.shared.generated.resources.no_category
import poskmp.shared.generated.resources.price_display_label
import poskmp.shared.generated.resources.star_filled
import poskmp.shared.generated.resources.status_inactive
import poskmp.shared.generated.resources.wholesale_display_label

@Composable
fun RowScope.TableHeader(
    text: String,
    weight: Float,
    field: ProductSortField? = null,
    currentField: ProductSortField? = null,
    currentOrder: ProductSortOrder = ProductSortOrder.ASC,
    onHeaderClick: ((ProductSortField) -> Unit)? = null,
    onResize: ((Float) -> Unit)? = null
) {
    Box(
        modifier = Modifier.weight(weight.coerceAtLeast(0.01f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (field != null && onHeaderClick != null) {
                        Modifier.clickable { onHeaderClick(field) }
                    } else Modifier
                )
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(
                    color = if (field != null && field == currentField) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (field != null && field == currentField) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    painter = painterResource(Res.drawable.arrow_up),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.rotate(if (currentOrder == ProductSortOrder.ASC) 0f else 180f)
                )
            }
        }
        if (onResize != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(12.dp)
                    .height(32.dp)
                    .pointerInput(onResize) {
                        detectHorizontalDragGestures { change, dragAmount ->
                            change.consume()
                            onResize(dragAmount)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(24.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
            }
        }
    }
}

@Composable
fun ProductTableHeaderRow(
    visibleColumns: List<ProductTableColumn>,
    columnWeights: List<Float>,
    modifier: Modifier = Modifier,
    totalDefaultWeight: Float = 1f,
    showSelectAll: Boolean = false,
    selectAllState: ToggleableState = ToggleableState.Off,
    onSelectAllClick: (() -> Unit)? = null,
    sortField: ProductSortField? = null,
    sortOrder: ProductSortOrder = ProductSortOrder.ASC,
    onHeaderClick: ((ProductSortField) -> Unit)? = null,
    onResizeColumn: ((index: Int, dragAmount: Float) -> Unit)? = null
) {
    val effectiveTotalWeight = if (showSelectAll) 0.95f else 1.0f

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showSelectAll) {
            TriStateCheckbox(
                state = selectAllState,
                onClick = { onSelectAllClick?.invoke() },
                modifier = Modifier.weight(0.05f)
            )
        }
        visibleColumns.forEachIndexed { index, col ->
            val colWeight = columnWeights.getOrElse(index) {
                (col.defaultWeight / totalDefaultWeight) * effectiveTotalWeight
            }.coerceAtLeast(0.01f)
            TableHeader(
                text = stringResource(col.titleRes),
                weight = colWeight,
                field = col.sortField,
                currentField = sortField,
                currentOrder = sortOrder,
                onHeaderClick = onHeaderClick,
                onResize = if (onResizeColumn != null && index < visibleColumns.lastIndex) {
                    { onResizeColumn(index, it) }
                } else null
            )
        }
    }
}

@Composable
fun ProductTableRow(
    product: Products,
    visibleColumns: List<ProductTableColumn>,
    columnWeights: List<Float>,
    modifier: Modifier = Modifier,
    totalDefaultWeight: Float = 1f,
    shape: Shape = ShapeDefaults.middleListItemShape,
    isHighlighted: Boolean = false,
    showCheckbox: Boolean = false,
    isChecked: Boolean = false,
    onCheckedChange: ((Boolean) -> Unit)? = null,
    salesStats: Map<String, ProductSalesStats> = emptyMap(),
    defaultRetailMargin: Double = 0.0,
    defaultWholesaleMargin: Double = 0.0,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    onSecondaryClick: (() -> Unit)? = null,
    contextMenu: (@Composable () -> Unit)? = null
) {
    val effectiveTotalWeight = if (showCheckbox) 0.95f else 1.0f

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(
                    if (isHighlighted) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    else MaterialTheme.colorScheme.surfaceContainerLowest
                )
                .then(
                    if (isHighlighted) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, shape)
                    else Modifier
                )
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                )
                .then(
                    if (onSecondaryClick != null) {
                        Modifier.pointerInput(product) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                                        event.changes.forEach { it.consume() }
                                        onSecondaryClick()
                                    }
                                }
                            }
                        }
                    } else Modifier
                )
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showCheckbox) {
                Checkbox(
                    checked = isChecked,
                    onCheckedChange = onCheckedChange,
                    modifier = Modifier.weight(0.05f)
                )
            }
            visibleColumns.forEachIndexed { colIndex, col ->
                val weight = columnWeights.getOrElse(colIndex) {
                    (col.defaultWeight / totalDefaultWeight) * effectiveTotalWeight
                }.coerceAtLeast(0.01f)
                when (col) {
                    ProductTableColumn.CODIGO -> {
                        val codesDisplay = product.formatBarcodesForDisplay()
                        Text(
                            text = codesDisplay,
                            modifier = Modifier.weight(weight),
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    ProductTableColumn.NOMBRE -> {
                        Row(
                            modifier = Modifier.weight(weight),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = product.nombre,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (product.es_favorito == 1L) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier
                                        .clip(MaterialShapes.Cookie12Sided.toShape())
                                        .size(28.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(Res.drawable.star_filled),
                                        contentDescription = stringResource(Res.string.favorite_desc),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            if (product.activo == 0L) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier
                                        .clip(MaterialShapes.Sunny.toShape())
                                        .size(28.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(Res.drawable.disabled),
                                        contentDescription = stringResource(Res.string.status_inactive),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                    ProductTableColumn.CATEGORIA -> {
                        Text(
                            text = product.categoria ?: stringResource(Res.string.no_category),
                            modifier = Modifier.weight(weight),
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    ProductTableColumn.PRECIO -> {
                        val priceText = if (product.por_peso == 1L) {
                            "$${product.precio.toString().formatPrice()} / Kg"
                        } else {
                            "$${product.precio.toString().formatPrice()}"
                        }
                        Text(
                            text = priceText,
                            modifier = Modifier.weight(weight),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    ProductTableColumn.COSTO -> {
                        Text(
                            text = "$${product.costo.toString().formatPrice()}",
                            modifier = Modifier.weight(weight),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    ProductTableColumn.MAYOREO -> {
                        Text(
                            text = "$${product.precio_mayoreo.toString().formatPrice()}",
                            modifier = Modifier.weight(weight),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    ProductTableColumn.MARGEN_VENTA -> {
                        if (product.costo > 0.0 && product.precio > 0.0) {
                            val margin = ((product.precio - product.costo) / product.costo) * 100.0
                            val isBelowDefault = (defaultRetailMargin > 0.0 && margin < defaultRetailMargin) || margin < 0.0
                            Text(
                                text = "${margin.toString().formatPrice()}%",
                                modifier = Modifier.weight(weight),
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isBelowDefault) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (isBelowDefault) FontWeight.Bold else FontWeight.Normal
                            )
                        } else {
                            Text(
                                text = "-",
                                modifier = Modifier.weight(weight),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    ProductTableColumn.MARGEN_MAYOREO -> {
                        if (product.costo > 0.0 && product.precio_mayoreo > 0.0) {
                            val margin = ((product.precio_mayoreo - product.costo) / product.costo) * 100.0
                            val isBelowDefault = (defaultWholesaleMargin > 0.0 && margin < defaultWholesaleMargin) || margin < 0.0
                            Text(
                                text = "${margin.toString().formatPrice()}%",
                                modifier = Modifier.weight(weight),
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isBelowDefault) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (isBelowDefault) FontWeight.Bold else FontWeight.Normal
                            )
                        } else {
                            Text(
                                text = "-",
                                modifier = Modifier.weight(weight),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    ProductTableColumn.VENTAS_TOTALES -> {
                        val stats = salesStats[product.id]
                        val total = stats?.totalVentas ?: 0.0
                        val text = if (total == 0.0) {
                            "0"
                        } else if (product.por_peso == 1L) {
                            total.formatQuantity(isWeight = true)
                        } else {
                            if (total % 1.0 == 0.0) total.toLong().toString() else total.toString().formatPrice()
                        }
                        Text(
                            text = text,
                            modifier = Modifier.weight(weight),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    ProductTableColumn.ULTIMA_VENTA -> {
                        val stats = salesStats[product.id]
                        val text = stats?.ultimaVenta?.let { formatEpochMillisToDateTime(it) } ?: "-"
                        Text(
                            text = text,
                            modifier = Modifier.weight(weight),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (stats?.ultimaVenta != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        contextMenu?.invoke()
    }
}

@Composable
fun ProductSimpleCard(
    product: Products,
    modifier: Modifier = Modifier,
    shape: Shape = ShapeDefaults.middleListItemShape,
    isSelected: Boolean = false,
    showCheckbox: Boolean = false,
    isChecked: Boolean = false,
    onCheckedChange: ((Boolean) -> Unit)? = null,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    onSecondaryClick: (() -> Unit)? = null,
    contextMenu: (@Composable () -> Unit)? = null
) {
    Box(modifier = modifier) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                )
                .then(
                    if (onSecondaryClick != null) {
                        Modifier.pointerInput(product) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                                        event.changes.forEach { it.consume() }
                                        onSecondaryClick()
                                    }
                                }
                            }
                        }
                    } else Modifier
                ),
            shape = shape,
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceContainerLow
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (showCheckbox) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = onCheckedChange,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                    Text(
                        modifier = Modifier.weight(1f),
                        text = product.nombre,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (product.es_favorito == 1L) {
                            Badge(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier
                                    .clip(MaterialShapes.Cookie12Sided.toShape())
                                    .size(28.dp)
                            ) {
                                Icon(
                                    painter = painterResource(Res.drawable.star_filled),
                                    contentDescription = stringResource(Res.string.favorite_desc),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        if (product.activo == 0L) {
                            Badge(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier
                                    .clip(MaterialShapes.Sunny.toShape())
                                    .size(28.dp)
                            ) {
                                Icon(
                                    painter = painterResource(Res.drawable.disabled),
                                    contentDescription = stringResource(Res.string.status_inactive),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (product.costo > 0.0) {
                        Text(
                            text = stringResource(
                                Res.string.cost_display_label,
                                product.costo.toString().formatPrice()
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (product.precio > 0.0) {
                        val priceLabel = if (product.por_peso == 1L) {
                            "$${product.precio.toString().formatPrice()} / Kg"
                        } else {
                            stringResource(Res.string.price_display_label, product.precio.toString().formatPrice())
                        }
                        Text(
                            text = priceLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary
                        )
                    }
                    if (product.precio_mayoreo > 0.0) {
                        Text(
                            text = stringResource(
                                Res.string.wholesale_display_label,
                                product.precio_mayoreo.toString().formatPrice()
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        contextMenu?.invoke()
    }
}
