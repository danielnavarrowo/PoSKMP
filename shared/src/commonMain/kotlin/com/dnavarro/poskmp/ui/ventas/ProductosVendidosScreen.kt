package com.dnavarro.poskmp.ui.ventas

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dnavarro.poskmp.domain.model.ProductSalesMetric
import com.dnavarro.poskmp.ui.components.AppVerticalScrollbar
import com.dnavarro.poskmp.util.formatPrice
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import poskmp.shared.generated.resources.Res
import poskmp.shared.generated.resources.arrow_up
import poskmp.shared.generated.resources.back
import poskmp.shared.generated.resources.btn_back
import poskmp.shared.generated.resources.empty_sold_products
import poskmp.shared.generated.resources.header_price
import poskmp.shared.generated.resources.header_product_name
import poskmp.shared.generated.resources.header_sold_pieces
import poskmp.shared.generated.resources.sold_products_total_summary
import poskmp.shared.generated.resources.title_sold_products

enum class SoldProductSortColumn {
    NAME,
    PRICE,
    PIECES
}

@Composable
fun ProductosVendidosScreen(
    soldProducts: List<ProductSalesMetric>,
    isSyncing: Boolean = false,
    onRefresh: () -> Unit = {},
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var sortColumn by rememberSaveable { mutableStateOf(SoldProductSortColumn.PIECES) }
    var isAscending by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()

    val sortArrowRotation by animateFloatAsState(
        targetValue = if (isAscending) 0f else 180f,
        label = "SortArrowRotation"
    )

    val sortedProducts = remember(soldProducts, sortColumn, isAscending) {
        when (sortColumn) {
            SoldProductSortColumn.PIECES -> {
                if (isAscending) {
                    soldProducts.sortedBy { it.totalUnidades }
                } else {
                    soldProducts.sortedByDescending { it.totalUnidades }
                }
            }
            SoldProductSortColumn.PRICE -> {
                if (isAscending) {
                    soldProducts.sortedBy { if (it.totalUnidades > 0) it.totalRecaudado / it.totalUnidades else 0.0 }
                } else {
                    soldProducts.sortedByDescending { if (it.totalUnidades > 0) it.totalRecaudado / it.totalUnidades else 0.0 }
                }
            }
            SoldProductSortColumn.NAME -> {
                if (isAscending) {
                    soldProducts.sortedBy { it.productNombre.lowercase() }
                } else {
                    soldProducts.sortedByDescending { it.productNombre.lowercase() }
                }
            }
        }
    }

    LaunchedEffect(sortColumn, isAscending) {
        try {
            listState.scrollToItem(0)
        } catch (_: Exception) {}
    }

    val totalPieces = remember(soldProducts) { soldProducts.sumOf { it.totalUnidades } }
    val piecesFormatted = remember(totalPieces) {
        if (totalPieces % 1.0 == 0.0) totalPieces.toLong().toString() else totalPieces.toString().formatPrice()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(Res.string.title_sold_products),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                        if (soldProducts.isNotEmpty()) {
                            Text(
                                text = stringResource(
                                    Res.string.sold_products_total_summary,
                                    piecesFormatted,
                                    soldProducts.size
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            painter = painterResource(Res.drawable.back),
                            contentDescription = stringResource(Res.string.btn_back),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        val pullToRefreshState = rememberPullToRefreshState()
        PullToRefreshBox(
            state = pullToRefreshState,
            isRefreshing = isSyncing,
            onRefresh = onRefresh,
            indicator = {
                PullToRefreshDefaults.LoadingIndicator(
                    state = pullToRefreshState,
                    isRefreshing = isSyncing,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = paddingValues.calculateBottomPadding())
            ) {
            // Quick Summary Row


            if (sortedProducts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(Res.string.empty_sold_products),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                // Table Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Column 1: Nombre del Producto
                    Row(
                        modifier = Modifier
                            .weight(0.45f)
                            .clip(MaterialTheme.shapes.extraSmall)
                            .clickable {
                                if (sortColumn == SoldProductSortColumn.NAME) {
                                    isAscending = !isAscending
                                } else {
                                    sortColumn = SoldProductSortColumn.NAME
                                    isAscending = true
                                }
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(Res.string.header_product_name),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (sortColumn == SoldProductSortColumn.NAME) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (sortColumn == SoldProductSortColumn.NAME) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                painter = painterResource(Res.drawable.arrow_up),
                                contentDescription = if (isAscending) "Ascendente" else "Descendente",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .size(14.dp)
                                    .graphicsLayer {
                                        rotationZ = sortArrowRotation
                                    }
                            )
                        }
                    }

                    // Column 2: Precio
                    Row(
                        modifier = Modifier
                            .weight(0.25f)
                            .clip(MaterialTheme.shapes.extraSmall)
                            .clickable {
                                if (sortColumn == SoldProductSortColumn.PRICE) {
                                    isAscending = !isAscending
                                } else {
                                    sortColumn = SoldProductSortColumn.PRICE
                                    isAscending = false
                                }
                            }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(Res.string.header_price),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (sortColumn == SoldProductSortColumn.PRICE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.End,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (sortColumn == SoldProductSortColumn.PRICE) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                painter = painterResource(Res.drawable.arrow_up),
                                contentDescription = if (isAscending) "Menor a mayor" else "Mayor a menor",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .size(14.dp)
                                    .graphicsLayer {
                                        rotationZ = sortArrowRotation
                                    }
                            )
                        }
                    }

                    // Column 3: Piezas Vendidas
                    Row(
                        modifier = Modifier
                            .weight(0.30f)
                            .clip(MaterialTheme.shapes.extraSmall)
                            .clickable {
                                if (sortColumn == SoldProductSortColumn.PIECES) {
                                    isAscending = !isAscending
                                } else {
                                    sortColumn = SoldProductSortColumn.PIECES
                                    isAscending = false
                                }
                            }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(Res.string.header_sold_pieces),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (sortColumn == SoldProductSortColumn.PIECES) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.End,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (sortColumn == SoldProductSortColumn.PIECES) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                painter = painterResource(Res.drawable.arrow_up),
                                contentDescription = if (isAscending) "Menor a mayor" else "Mayor a menor",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .size(14.dp)
                                    .graphicsLayer {
                                        rotationZ = sortArrowRotation
                                    }
                            )
                        }
                    }
                }

                // Full Screen LazyColumn Table Rows
                Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        itemsIndexed(
                            sortedProducts,
                            key = { _, item -> item.productId ?: item.productNombre },
                            contentType = { _, _ -> "sold_product_row" }
                        ) { index, metric ->
                            val unitPrice = if (metric.totalUnidades > 0) metric.totalRecaudado / metric.totalUnidades else 0.0
                            val qtyFormatted = if (metric.totalUnidades % 1.0 == 0.0) {
                                metric.totalUnidades.toLong().toString()
                            } else {
                                metric.totalUnidades.toString().formatPrice()
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(0.45f)) {
                                    Text(
                                        text = metric.productNombre,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Recaudado: $${metric.totalRecaudado.toString().formatPrice()}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = "$${unitPrice.toString().formatPrice()}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(0.25f),
                                    textAlign = TextAlign.End,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(
                                    modifier = Modifier.weight(0.30f),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                        shape = MaterialTheme.shapes.extraSmall
                                    ) {
                                        Text(
                                            text = "$qtyFormatted pzas",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            textAlign = TextAlign.End,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            if (index < sortedProducts.lastIndex) {
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                            }
                        }
                    }

                    AppVerticalScrollbar(
                        state = listState,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight()
                            .padding(vertical = 4.dp, horizontal = 2.dp)
                    )
                }
            }
        }
    }
}
}

