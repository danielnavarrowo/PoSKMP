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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dnavarro.poskmp.domain.model.ProductSalesMetric
import com.dnavarro.poskmp.theme.ShapeDefaults
import com.dnavarro.poskmp.ui.components.AppVerticalScrollbar
import com.dnavarro.poskmp.util.formatPrice
import com.dnavarro.poskmp.util.formatWithThousandsSeparator
import com.dnavarro.poskmp.util.matchesBarcode
import com.dnavarro.poskmp.util.matchesSearchQuery
import com.dnavarro.poskmp.util.parseBarcodes
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import poskmp.shared.generated.resources.Res
import poskmp.shared.generated.resources.arrow_up
import poskmp.shared.generated.resources.back
import poskmp.shared.generated.resources.btn_back
import poskmp.shared.generated.resources.clear_desc
import poskmp.shared.generated.resources.close
import poskmp.shared.generated.resources.empty_sold_products
import poskmp.shared.generated.resources.header_price
import poskmp.shared.generated.resources.header_product_name
import poskmp.shared.generated.resources.header_sold_pieces
import poskmp.shared.generated.resources.search
import poskmp.shared.generated.resources.search_desc
import poskmp.shared.generated.resources.search_placeholder
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
    val searchQueryState = rememberTextFieldState()
    val searchBarFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    val sortArrowRotation by animateFloatAsState(
        targetValue = if (isAscending) 0f else 180f,
        label = "SortArrowRotation"
    )

    val searchQuery = searchQueryState.text.toString()

    val filteredProducts = remember(soldProducts, searchQuery, sortColumn, isAscending) {
        val trimmed = searchQuery.trim()
        val baseList = if (trimmed.isEmpty()) {
            soldProducts
        } else {
            val tokens = trimmed.split("\\s+".toRegex()).filter { it.isNotEmpty() }
            soldProducts.filter { metric ->
                val barcodes = parseBarcodes(metric.codigos)
                if (tokens.size > 1) {
                    tokens.all { token ->
                        metric.productNombre.matchesSearchQuery(token) ||
                        barcodes.matchesBarcode(token) ||
                        barcodes.any { it.contains(token, ignoreCase = true) }
                    }
                } else {
                    metric.productNombre.matchesSearchQuery(trimmed) ||
                    barcodes.matchesBarcode(trimmed) ||
                    barcodes.any { it.contains(trimmed, ignoreCase = true) }
                }
            }
        }
        when (sortColumn) {
            SoldProductSortColumn.PIECES -> {
                if (isAscending) {
                    baseList.sortedBy { it.totalUnidades }
                } else {
                    baseList.sortedByDescending { it.totalUnidades }
                }
            }
            SoldProductSortColumn.PRICE -> {
                if (isAscending) {
                    baseList.sortedBy { if (it.totalUnidades > 0) it.totalRecaudado / it.totalUnidades else 0.0 }
                } else {
                    baseList.sortedByDescending { if (it.totalUnidades > 0) it.totalRecaudado / it.totalUnidades else 0.0 }
                }
            }
            SoldProductSortColumn.NAME -> {
                if (isAscending) {
                    baseList.sortedBy { it.productNombre.lowercase() }
                } else {
                    baseList.sortedByDescending { it.productNombre.lowercase() }
                }
            }
        }
    }

    LaunchedEffect(searchQuery, sortColumn, isAscending) {
        try {
            listState.scrollToItem(0)
        } catch (_: Exception) {}
    }

    val totalPieces = remember(filteredProducts) { filteredProducts.sumOf { it.totalUnidades } }
    val piecesFormatted = remember(totalPieces) {
        if (totalPieces % 1.0 == 0.0) formatWithThousandsSeparator(totalPieces.toLong().toString()) else totalPieces.formatPrice()
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
                        if (filteredProducts.isNotEmpty()) {
                            Text(
                                text = stringResource(
                                    Res.string.sold_products_total_summary,
                                    piecesFormatted,
                                    filteredProducts.size
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
                // Search Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .height(54.dp)
                        .background(
                            color = if (searchQueryState.text.isNotEmpty())
                                MaterialTheme.colorScheme.surfaceContainerLowest
                            else
                                MaterialTheme.colorScheme.surfaceContainer,
                            shape = ShapeDefaults.cardShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.search),
                            contentDescription = stringResource(Res.string.search_desc),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (searchQueryState.text.isEmpty()) {
                                Text(
                                    text = stringResource(Res.string.search_placeholder),
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        color = MaterialTheme.colorScheme.onSurface.copy(
                                            alpha = 0.6f
                                        )
                                    ),
                                    textAlign = TextAlign.Start
                                )
                            }

                            BasicTextField(
                                state = searchQueryState,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(searchBarFocusRequester)
                                    .onPreviewKeyEvent { keyEvent ->
                                        keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.Escape && if (searchQueryState.text.isNotEmpty()) {
                                            searchQueryState.clearText()
                                            true
                                        } else false
                                    },
                                keyboardOptions = KeyboardOptions(
                                    imeAction = ImeAction.Search
                                ),
                                onKeyboardAction = {
                                    keyboardController?.hide()
                                },
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Start
                                ),
                                lineLimits = TextFieldLineLimits.SingleLine
                            )
                        }

                        if (searchQueryState.text.isNotEmpty()) {
                            IconButton(
                                modifier = Modifier.size(32.dp),
                                onClick = {
                                    searchQueryState.clearText()
                                }
                            ) {
                                Icon(
                                    painter = painterResource(Res.drawable.close),
                                    contentDescription = stringResource(Res.string.clear_desc),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                if (filteredProducts.isEmpty()) {
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
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            itemsIndexed(
                                filteredProducts,
                                key = { _, item -> item.productId ?: item.productNombre },
                                contentType = { _, _ -> "sold_product_row" }
                            ) { index, metric ->
                                val shape = if (filteredProducts.size == 1) {
                                    ShapeDefaults.cardShape
                                } else if (index == 0) {
                                    ShapeDefaults.topListItemShape
                                } else if (index == filteredProducts.lastIndex) {
                                    ShapeDefaults.bottomListItemShape
                                } else {
                                    ShapeDefaults.middleListItemShape
                                }

                                val unitPrice = if (metric.totalUnidades > 0) metric.totalRecaudado / metric.totalUnidades else 0.0
                                val qtyFormatted = if (metric.totalUnidades % 1.0 == 0.0) {
                                    formatWithThousandsSeparator(metric.totalUnidades.toLong().toString())
                                } else {
                                    metric.totalUnidades.formatPrice()
                                }

                                Surface(
                                    shape = shape,
                                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
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
                                                text = "Recaudado: $${metric.totalRecaudado.formatPrice()}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text(
                                            text = "$${unitPrice.formatPrice()}",
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

                                            Text(
                                                text = qtyFormatted,
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                textAlign = TextAlign.End,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        AppVerticalScrollbar(
                            state = listState,
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .fillMaxHeight()
                                .padding(vertical = 4.dp, horizontal = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

