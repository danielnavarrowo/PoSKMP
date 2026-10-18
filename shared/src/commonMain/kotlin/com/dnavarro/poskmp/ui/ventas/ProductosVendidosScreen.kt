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
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Badge
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetValue
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.material3.toShape
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import com.dnavarro.poskmp.util.PlatformBackHandler
import com.dnavarro.poskmp.util.isAndroid
import kotlinx.coroutines.launch
import poskmp.shared.generated.resources.Res
import poskmp.shared.generated.resources.apply_filters_button
import poskmp.shared.generated.resources.arrow_up
import poskmp.shared.generated.resources.back
import poskmp.shared.generated.resources.btn_back
import poskmp.shared.generated.resources.check
import poskmp.shared.generated.resources.clear_desc
import poskmp.shared.generated.resources.close
import poskmp.shared.generated.resources.empty_sold_products
import poskmp.shared.generated.resources.filter
import poskmp.shared.generated.resources.filter_and_sort_title
import poskmp.shared.generated.resources.filter_category_all
import poskmp.shared.generated.resources.filter_category_title
import poskmp.shared.generated.resources.filter_off
import poskmp.shared.generated.resources.filter_on
import poskmp.shared.generated.resources.header_price
import poskmp.shared.generated.resources.header_product_name
import poskmp.shared.generated.resources.header_sold_pieces
import poskmp.shared.generated.resources.no_category
import poskmp.shared.generated.resources.reset_filters
import poskmp.shared.generated.resources.search
import poskmp.shared.generated.resources.search_desc
import poskmp.shared.generated.resources.search_placeholder
import poskmp.shared.generated.resources.sold_products_total_summary
import poskmp.shared.generated.resources.sort_order_asc
import poskmp.shared.generated.resources.sort_order_desc
import poskmp.shared.generated.resources.sort_order_section_title
import poskmp.shared.generated.resources.sort_section_title
import poskmp.shared.generated.resources.title_sold_products

enum class SoldProductSortField(val label: String) {
    PIECES("Piezas vendidas"),
    REVENUE("Total recaudado"),
    PROFIT("Ganancia generada"),
    NAME("Nombre del producto")
}

enum class SoldProductUnitTypeFilter(val label: String) {
    ALL("Todos"),
    BY_PIECE("Por pieza"),
    BY_WEIGHT("Por peso / a granel")
}

enum class SoldProductCatalogFilter(val label: String) {
    ALL("Todos"),
    CATALOG("En catálogo"),
    CUSTOM("Venta rápida")
}

@Composable
fun ProductosVendidosScreen(
    soldProducts: List<ProductSalesMetric>,
    isSyncing: Boolean = false,
    onRefresh: () -> Unit = {},
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showFilterBottomSheet by remember { mutableStateOf(false) }
    var sortField by rememberSaveable { mutableStateOf(SoldProductSortField.PIECES) }
    var isAscending by rememberSaveable { mutableStateOf(false) }
    var selectedCategory by rememberSaveable { mutableStateOf<String?>(null) }
    var unitTypeFilter by rememberSaveable { mutableStateOf(SoldProductUnitTypeFilter.ALL) }
    var catalogFilter by rememberSaveable { mutableStateOf(SoldProductCatalogFilter.ALL) }

    val availableCategories = remember(soldProducts) {
        soldProducts.mapNotNull { it.categoria }
            .filter { it.isNotBlank() && !it.equals("Sin categoría", ignoreCase = true) && !it.equals("Sin categoria", ignoreCase = true) }
            .distinct()
            .sorted()
    }

    val listState = rememberLazyListState()
    val searchQueryState = rememberTextFieldState()
    val searchBarFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    val hasActiveFilters = sortField != SoldProductSortField.PIECES || isAscending || selectedCategory != null || unitTypeFilter != SoldProductUnitTypeFilter.ALL || catalogFilter != SoldProductCatalogFilter.ALL

    val resetFilters = {
        sortField = SoldProductSortField.PIECES
        isAscending = false
        selectedCategory = null
        unitTypeFilter = SoldProductUnitTypeFilter.ALL
        catalogFilter = SoldProductCatalogFilter.ALL
        searchQueryState.clearText()
    }

    val sortArrowRotation by animateFloatAsState(
        targetValue = if (isAscending) 0f else 180f,
        label = "SortArrowRotation"
    )

    val searchQuery = searchQueryState.text.toString()

    val filteredProducts = remember(
        soldProducts,
        searchQuery,
        sortField,
        isAscending,
        selectedCategory,
        unitTypeFilter,
        catalogFilter
    ) {
        val trimmed = searchQuery.trim()
        val baseList = soldProducts.filter { metric ->
            val matchesSearch = if (trimmed.isEmpty()) {
                true
            } else {
                val tokens = trimmed.split("\\s+".toRegex()).filter { it.isNotEmpty() }
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

            val matchesCategory = when (selectedCategory) {
                null -> true
                "NO_CATEGORY" -> metric.categoria.isNullOrBlank() ||
                        metric.categoria.equals("Sin categoría", ignoreCase = true) ||
                        metric.categoria.equals("Sin categoria", ignoreCase = true)
                else -> metric.categoria.equals(selectedCategory, ignoreCase = true)
            }

            val matchesUnitType = when (unitTypeFilter) {
                SoldProductUnitTypeFilter.ALL -> true
                SoldProductUnitTypeFilter.BY_PIECE -> metric.totalUnidades % 1.0 == 0.0
                SoldProductUnitTypeFilter.BY_WEIGHT -> metric.totalUnidades % 1.0 != 0.0
            }

            val matchesCatalog = when (catalogFilter) {
                SoldProductCatalogFilter.ALL -> true
                SoldProductCatalogFilter.CATALOG -> metric.productId != null
                SoldProductCatalogFilter.CUSTOM -> metric.productId == null
            }

            matchesSearch && matchesCategory && matchesUnitType && matchesCatalog
        }

        when (sortField) {
            SoldProductSortField.PIECES -> {
                if (isAscending) baseList.sortedBy { it.totalUnidades } else baseList.sortedByDescending { it.totalUnidades }
            }
            SoldProductSortField.REVENUE -> {
                if (isAscending) baseList.sortedBy { it.totalRecaudado } else baseList.sortedByDescending { it.totalRecaudado }
            }
            SoldProductSortField.PROFIT -> {
                if (isAscending) baseList.sortedBy { it.gananciaGenerada } else baseList.sortedByDescending { it.gananciaGenerada }
            }
            SoldProductSortField.NAME -> {
                if (isAscending) {
                    baseList.sortedBy { it.productNombre.lowercase() }
                } else {
                    baseList.sortedByDescending { it.productNombre.lowercase() }
                }
            }
        }
    }

    LaunchedEffect(searchQuery, sortField, isAscending, selectedCategory, unitTypeFilter, catalogFilter) {
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
                // Search Bar and Filter Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
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

                    Spacer(modifier = Modifier.width(8.dp))

                    Box {
                        IconButton(
                            onClick = { showFilterBottomSheet = true },
                            modifier = Modifier
                                .size(54.dp)
                                .clip(MaterialShapes.Cookie4Sided.toShape())
                                .background(
                                    if (hasActiveFilters) MaterialTheme.colorScheme.tertiaryContainer
                                    else MaterialTheme.colorScheme.secondaryContainer
                                )
                        ) {
                            Icon(
                                painter = if (hasActiveFilters) painterResource(Res.drawable.filter_on)
                                else painterResource(Res.drawable.filter),
                                contentDescription = stringResource(Res.string.filter_and_sort_title),
                                tint = if (hasActiveFilters) MaterialTheme.colorScheme.onTertiaryContainer
                                else MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        if (hasActiveFilters) {
                            Badge(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = (-2).dp, y = 2.dp)
                                    .size(10.dp),
                                containerColor = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }

                if (soldProducts.isEmpty()) {
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
                } else if (filteredProducts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "No se encontraron productos que coincidan con los filtros aplicados.",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            if (hasActiveFilters || searchQuery.isNotEmpty()) {
                                OutlinedButton(onClick = resetFilters) {
                                    Icon(
                                        painter = painterResource(Res.drawable.filter_off),
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(stringResource(Res.string.reset_filters))
                                }
                            }
                        }
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
                                    if (sortField == SoldProductSortField.NAME) {
                                        isAscending = !isAscending
                                    } else {
                                        sortField = SoldProductSortField.NAME
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
                                color = if (sortField == SoldProductSortField.NAME) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (sortField == SoldProductSortField.NAME) {
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
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(Res.string.header_price),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.End,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Column 3: Piezas Vendidas
                        Row(
                            modifier = Modifier
                                .weight(0.30f)
                                .clip(MaterialTheme.shapes.extraSmall)
                                .clickable {
                                    if (sortField == SoldProductSortField.PIECES) {
                                        isAscending = !isAscending
                                    } else {
                                        sortField = SoldProductSortField.PIECES
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
                                color = if (sortField == SoldProductSortField.PIECES) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.End,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (sortField == SoldProductSortField.PIECES) {
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
                                .padding(vertical = 4.dp, horizontal = 8.dp)
                        )
                    }
                }
            }
        }

        if (showFilterBottomSheet) {
            SoldProductFilterAndSortBottomSheet(
                sortField = sortField,
                isAscending = isAscending,
                selectedCategory = selectedCategory,
                availableCategories = availableCategories,
                unitTypeFilter = unitTypeFilter,
                catalogFilter = catalogFilter,
                onSortFieldSelected = { sortField = it },
                onSortOrderSelected = { isAscending = it },
                onCategorySelected = { selectedCategory = it },
                onUnitTypeFilterSelected = { unitTypeFilter = it },
                onCatalogFilterSelected = { catalogFilter = it },
                onResetFilters = resetFilters,
                onDismissRequest = { showFilterBottomSheet = false }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoldProductFilterAndSortBottomSheet(
    sortField: SoldProductSortField,
    isAscending: Boolean,
    selectedCategory: String?,
    availableCategories: List<String>,
    unitTypeFilter: SoldProductUnitTypeFilter,
    catalogFilter: SoldProductCatalogFilter,
    onSortFieldSelected: (SoldProductSortField) -> Unit,
    onSortOrderSelected: (Boolean) -> Unit,
    onCategorySelected: (String?) -> Unit,
    onUnitTypeFilterSelected: (SoldProductUnitTypeFilter) -> Unit,
    onCatalogFilterSelected: (SoldProductCatalogFilter) -> Unit,
    onResetFilters: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
    )
    val coroutineScope = rememberCoroutineScope()
    var isDismissing by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier.then(
            if (!isAndroid()) {
                Modifier.onPreviewKeyEvent { keyEvent ->
                    keyEvent.type == KeyEventType.KeyDown && when (keyEvent.key) {
                        Key.Enter, Key.NumPadEnter -> {
                            onDismissRequest()
                            true
                        }
                        else -> false
                    }
                }
            } else Modifier
        )
    ) {
        PlatformBackHandler(enabled = sheetState.isVisible && !isDismissing) {
            isDismissing = true
            coroutineScope.launch {
                try {
                    sheetState.hide()
                } finally {
                    if (!sheetState.isVisible) {
                        onDismissRequest()
                    } else {
                        isDismissing = false
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(Res.string.filter_and_sort_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(
                    onClick = onResetFilters,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(MaterialShapes.Ghostish.toShape())
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.filter_off),
                        contentDescription = stringResource(Res.string.reset_filters)
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Scrollable Content
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section 1: Sorting Field
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(Res.string.sort_section_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            SoldProductSortField.entries.forEach { field ->
                                FilterChip(
                                    selected = sortField == field,
                                    onClick = { onSortFieldSelected(field) },
                                    label = { Text(field.label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }
                        }
                    }
                }

                // Section 2: Sort Direction (Connected ToggleButtons)
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(Res.string.sort_order_section_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val orderOptions = listOf(
                                false to "▼ " + stringResource(Res.string.sort_order_desc),
                                true to "▲ " + stringResource(Res.string.sort_order_asc)
                            )
                            orderOptions.forEachIndexed { index, (order, label) ->
                                val isSelected = isAscending == order
                                ToggleButton(
                                    checked = isSelected,
                                    onCheckedChange = { onSortOrderSelected(order) },
                                    colors = ToggleButtonDefaults.colors(
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
                                        orderOptions.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                                    }
                                ) {
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

                // Section 3: Categoría
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(Res.string.filter_category_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            FilterChip(
                                selected = selectedCategory == null,
                                onClick = { onCategorySelected(null) },
                                label = { Text(stringResource(Res.string.filter_category_all)) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                            availableCategories.forEach { cat ->
                                FilterChip(
                                    selected = selectedCategory == cat,
                                    onClick = { onCategorySelected(cat) },
                                    label = { Text(cat) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }
                            FilterChip(
                                selected = selectedCategory == "NO_CATEGORY",
                                onClick = { onCategorySelected("NO_CATEGORY") },
                                label = { Text(stringResource(Res.string.no_category)) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                }

                // Section 4: Tipo de Cobro / Unidad
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Tipo de cobro / Unidad",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            SoldProductUnitTypeFilter.entries.forEachIndexed { index, option ->
                                val isSelected = unitTypeFilter == option
                                ToggleButton(
                                    checked = isSelected,
                                    onCheckedChange = { onUnitTypeFilterSelected(option) },
                                    colors = ToggleButtonDefaults.colors(
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
                                        SoldProductUnitTypeFilter.entries.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                                    }
                                ) {
                                    Text(
                                        text = option.label,
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

                // Section 5: Tipo de Producto (Catálogo vs Venta rápida)
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Tipo de producto",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            SoldProductCatalogFilter.entries.forEachIndexed { index, option ->
                                val isSelected = catalogFilter == option
                                ToggleButton(
                                    checked = isSelected,
                                    onCheckedChange = { onCatalogFilterSelected(option) },
                                    colors = ToggleButtonDefaults.colors(
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
                                        SoldProductCatalogFilter.entries.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                                    }
                                ) {
                                    Text(
                                        text = option.label,
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
            }

            // Apply Button
            ElevatedButton(
                onClick = onDismissRequest,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.elevatedButtonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary
                )
            ) {
                Icon(
                    painter = painterResource(Res.drawable.check),
                    contentDescription = stringResource(Res.string.apply_filters_button),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(Res.string.apply_filters_button),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

