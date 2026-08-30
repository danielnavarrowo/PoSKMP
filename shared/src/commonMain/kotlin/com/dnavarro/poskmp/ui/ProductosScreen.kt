package com.dnavarro.poskmp.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Badge
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeExtendedFloatingActionButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dnavarro.poskmp.db.Products
import com.dnavarro.poskmp.theme.ShapeDefaults
import com.dnavarro.poskmp.ui.components.ProductSimpleCard
import com.dnavarro.poskmp.ui.components.ProductTableHeaderRow
import com.dnavarro.poskmp.ui.components.ProductTableRow
import com.dnavarro.poskmp.ui.productos.DEFAULT_PRODUCT_TABLE_COLUMNS
import com.dnavarro.poskmp.ui.productos.FavoriteFilterOption
import com.dnavarro.poskmp.ui.productos.ProductTableColumn
import com.dnavarro.poskmp.ui.productos.ProductosViewModel
import com.dnavarro.poskmp.ui.productos.StatusFilterOption
import com.dnavarro.poskmp.util.PlatformBackHandler
import com.dnavarro.poskmp.util.isAndroid
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import poskmp.shared.generated.resources.Res
import poskmp.shared.generated.resources.add
import poskmp.shared.generated.resources.apply_filters_button
import poskmp.shared.generated.resources.barcode_scanner
import poskmp.shared.generated.resources.bulk_op_change_category_title
import poskmp.shared.generated.resources.bulk_op_change_prices_title
import poskmp.shared.generated.resources.bulk_op_deactivate_title
import poskmp.shared.generated.resources.bulk_op_delete_title
import poskmp.shared.generated.resources.bulk_op_mark_as_favorite_title
import poskmp.shared.generated.resources.bulk_op_set_profit_title
import poskmp.shared.generated.resources.check
import poskmp.shared.generated.resources.clear_desc
import poskmp.shared.generated.resources.close
import poskmp.shared.generated.resources.columns_section_title
import poskmp.shared.generated.resources.delete
import poskmp.shared.generated.resources.edit
import poskmp.shared.generated.resources.filter
import poskmp.shared.generated.resources.filter_and_sort_title
import poskmp.shared.generated.resources.filter_category_all
import poskmp.shared.generated.resources.filter_category_title
import poskmp.shared.generated.resources.filter_favorite_all
import poskmp.shared.generated.resources.filter_favorite_only
import poskmp.shared.generated.resources.filter_favorite_title
import poskmp.shared.generated.resources.filter_non_favorite_only
import poskmp.shared.generated.resources.filter_off
import poskmp.shared.generated.resources.filter_on
import poskmp.shared.generated.resources.filter_status_active_only
import poskmp.shared.generated.resources.filter_status_all
import poskmp.shared.generated.resources.filter_status_inactive_only
import poskmp.shared.generated.resources.filter_status_title
import poskmp.shared.generated.resources.header_category
import poskmp.shared.generated.resources.header_codes
import poskmp.shared.generated.resources.header_cost
import poskmp.shared.generated.resources.header_last_sale
import poskmp.shared.generated.resources.header_pieces
import poskmp.shared.generated.resources.header_product_name
import poskmp.shared.generated.resources.header_retail_margin
import poskmp.shared.generated.resources.header_retail_price
import poskmp.shared.generated.resources.header_total_sales
import poskmp.shared.generated.resources.header_wholesale_margin
import poskmp.shared.generated.resources.money
import poskmp.shared.generated.resources.new_product_button
import poskmp.shared.generated.resources.new_product_button_desktop
import poskmp.shared.generated.resources.no_category
import poskmp.shared.generated.resources.no_products_registered
import poskmp.shared.generated.resources.product_admin_title
import poskmp.shared.generated.resources.products
import poskmp.shared.generated.resources.remove
import poskmp.shared.generated.resources.reset_filters
import poskmp.shared.generated.resources.sad_face
import poskmp.shared.generated.resources.scan_with_camera_desc
import poskmp.shared.generated.resources.search
import poskmp.shared.generated.resources.search_desc
import poskmp.shared.generated.resources.search_placeholder
import poskmp.shared.generated.resources.sort_order_asc
import poskmp.shared.generated.resources.sort_order_desc
import poskmp.shared.generated.resources.sort_order_section_title
import poskmp.shared.generated.resources.sort_section_title
import poskmp.shared.generated.resources.star
import poskmp.shared.generated.resources.wholesale
import kotlin.time.Duration.Companion.milliseconds

enum class ProductSortField {
    CODIGO, NOMBRE, CATEGORIA, PIEZAS, PRECIO, COSTO, MAYOREO, MARGEN_VENTA, MARGEN_MAYOREO, VENTAS_TOTALES, ULTIMA_VENTA
}

enum class ProductSortOrder {
    ASC, DESC
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ProductosScreen(
    viewModel: ProductosViewModel,
    modifier: Modifier = Modifier,
    refocusTrigger: Int = 0
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery = uiState.searchQuery
    val sortedProducts = uiState.sortedProducts
    val sortField = uiState.sortField
    val sortOrder = uiState.sortOrder
    val showProductDialogFor = uiState.showProductDialogFor
    val showBulkModificationFor = uiState.showBulkModificationFor
    val selectedProductIds = uiState.selectedProductIds

    var isFabMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var showCameraScanner by remember { mutableStateOf(false) }
    var showFilterBottomSheet by remember { mutableStateOf(false) }
    val fabContainerColor = MaterialTheme.colorScheme.tertiary

    var selectedProductIndex by remember(sortedProducts) { mutableIntStateOf(-1) }
    var selectionAnchorIndex by remember(sortedProducts) { mutableIntStateOf(-1) }
    val searchBarFocusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    fun reclaimSearchBarFocus() {
        if (!isAndroid()) {
            coroutineScope.launch {
                delay(50.milliseconds)
                try {
                    searchBarFocusRequester.requestFocus()
                } catch (_: Exception) {
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!isAndroid()) {
            delay(100.milliseconds)
            try {
                searchBarFocusRequester.requestFocus()
            } catch (_: Exception) {
            }
        }
    }

    LaunchedEffect(showProductDialogFor, showBulkModificationFor, showFilterBottomSheet) {
        if (showProductDialogFor == null && showBulkModificationFor == null && !showFilterBottomSheet && !isAndroid()) {
            reclaimSearchBarFocus()
        }
    }

    LaunchedEffect(refocusTrigger) {
        if (refocusTrigger > 0) {
            reclaimSearchBarFocus()
        }
    }

    LaunchedEffect(selectedProductIndex) {
        if (selectedProductIndex in sortedProducts.indices) {
            try {
                listState.animateScrollToItem(selectedProductIndex)
            } catch (_: Exception) {
            }
        }
    }

    val handleKeyNavigation: (KeyEvent) -> Boolean = { keyEvent ->
        keyEvent.type == KeyEventType.KeyDown && when (keyEvent.key) {
            Key.Escape -> {
                if (searchQuery.isNotEmpty()) {
                    viewModel.onSearchQueryChanged("")
                    selectedProductIndex = -1
                    selectionAnchorIndex = -1
                    true
                } else false
            }

            Key.F3 -> {
                reclaimSearchBarFocus()
                true
            }

            Key.F10 -> {
                viewModel.onShowProductDialog(
                    Products(
                        id = "",
                        codigos = "[]",
                        nombre = "",
                        precio = 0.0,
                        costo = 0.0,
                        categoria = "",
                        activo = 1L,
                        por_peso = 0L,
                        precio_mayoreo = 0.0,
                        es_favorito = 0L,
                        piezas = 1.0,
                        updated_at = 0L,
                        sync_state = ""
                    )
                )
                true
            }

            Key.DirectionDown -> {
                if (sortedProducts.isNotEmpty()) {
                    val prevIndex = selectedProductIndex
                    if (selectedProductIndex < sortedProducts.lastIndex) {
                        selectedProductIndex++
                    } else {
                        selectedProductIndex = sortedProducts.lastIndex
                    }
                    if (keyEvent.isShiftPressed) {
                        if (selectionAnchorIndex == -1) {
                            selectionAnchorIndex = if (prevIndex in sortedProducts.indices) prevIndex else 0
                        }
                        val start = minOf(selectionAnchorIndex, selectedProductIndex)
                        val end = maxOf(selectionAnchorIndex, selectedProductIndex)
                        val rangeIds = sortedProducts.subList(start, end + 1).map { it.id }.toSet()
                        viewModel.onSetSelectedProductIds(rangeIds)
                    } else {
                        selectionAnchorIndex = selectedProductIndex
                    }
                    true
                } else false
            }

            Key.DirectionUp -> {
                if (sortedProducts.isNotEmpty()) {
                    val prevIndex = selectedProductIndex
                    if (selectedProductIndex > 0) {
                        selectedProductIndex--
                    } else {
                        selectedProductIndex = 0
                    }
                    if (keyEvent.isShiftPressed) {
                        if (selectionAnchorIndex == -1) {
                            selectionAnchorIndex = if (prevIndex in sortedProducts.indices) prevIndex else 0
                        }
                        val start = minOf(selectionAnchorIndex, selectedProductIndex)
                        val end = maxOf(selectionAnchorIndex, selectedProductIndex)
                        val rangeIds = sortedProducts.subList(start, end + 1).map { it.id }.toSet()
                        viewModel.onSetSelectedProductIds(rangeIds)
                    } else {
                        selectionAnchorIndex = selectedProductIndex
                    }
                    true
                } else false
            }

            Key.Spacebar -> {
                if (selectedProductIndex in sortedProducts.indices) {
                    val product = sortedProducts[selectedProductIndex]
                    viewModel.onToggleSelectProduct(product.id)
                    selectionAnchorIndex = selectedProductIndex
                    true
                } else false
            }

            Key.Enter, Key.NumPadEnter -> {
                if (selectedProductIndex in sortedProducts.indices) {
                    viewModel.onShowProductDialog(sortedProducts[selectedProductIndex])
                    true
                } else if (sortedProducts.isNotEmpty()) {
                    viewModel.onShowProductDialog(sortedProducts[0])
                    true
                } else false
            }

            else -> false
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isCompact = maxWidth < 720.dp

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = stringResource(Res.string.product_admin_title),
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.titleLarge,
                            textAlign = TextAlign.Center
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            },
            floatingActionButton = {
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    // Menu FAB for Bulk Operations (Appears ABOVE when products are selected)
                    AnimatedVisibility(
                        visible = selectedProductIds.isNotEmpty(),
                        enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) +
                                scaleIn(
                                    initialScale = 0.8f,
                                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                ),
                        exit = fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) +
                                scaleOut(
                                    targetScale = 0.8f,
                                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                )
                    ) {
                        PlatformBackHandler(enabled = isFabMenuExpanded) {
                            isFabMenuExpanded = false
                        }
                        FloatingActionButtonMenu(
                            modifier = Modifier.align(Alignment.End).offset(x = 16.dp),
                            expanded = isFabMenuExpanded,
                            button = {
                                ToggleFloatingActionButton(
                                    checked = isFabMenuExpanded,
                                    containerColor = { _ -> fabContainerColor },
                                    onCheckedChange = { isFabMenuExpanded = !isFabMenuExpanded }
                                ) {
                                    val iconRes =
                                        if (checkedProgress > 0.5f) Res.drawable.close else Res.drawable.edit
                                    Icon(
                                        painter = painterResource(iconRes),
                                        tint = MaterialTheme.colorScheme.onTertiary,
                                        contentDescription = null,
                                        modifier = Modifier.graphicsLayer {
                                            rotationZ = checkedProgress * 180f
                                        }
                                    )
                                }
                            }
                        ) {
                            FloatingActionButtonMenuItem(
                                onClick = {
                                    isFabMenuExpanded = false
                                    viewModel.onShowBulkModificationDialog(BulkProductOperation.CHANGE_PRICES)
                                },
                                icon = {
                                    Icon(
                                        painter = painterResource(Res.drawable.money),
                                        contentDescription = null
                                    )
                                },
                                text = { Text(stringResource(Res.string.bulk_op_change_prices_title)) },
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            FloatingActionButtonMenuItem(
                                onClick = {
                                    isFabMenuExpanded = false
                                    viewModel.onShowBulkModificationDialog(BulkProductOperation.SET_PROFIT)
                                },
                                icon = {
                                    Icon(
                                        painter = painterResource(Res.drawable.edit),
                                        contentDescription = null
                                    )
                                },
                                text = { Text(stringResource(Res.string.bulk_op_set_profit_title)) },
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            FloatingActionButtonMenuItem(
                                onClick = {
                                    isFabMenuExpanded = false
                                    viewModel.onShowBulkModificationDialog(BulkProductOperation.CHANGE_CATEGORY)
                                },
                                icon = {
                                    Icon(
                                        painter = painterResource(Res.drawable.products),
                                        contentDescription = null
                                    )
                                },
                                text = { Text(stringResource(Res.string.bulk_op_change_category_title)) },
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            FloatingActionButtonMenuItem(
                                onClick = {
                                    isFabMenuExpanded = false
                                    viewModel.onShowBulkModificationDialog(BulkProductOperation.MARK_AS_FAVORITE)
                                },
                                icon = {
                                    Icon(
                                        painter = painterResource(Res.drawable.star),
                                        contentDescription = null
                                    )
                                },
                                text = { Text(stringResource(Res.string.bulk_op_mark_as_favorite_title)) },
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            FloatingActionButtonMenuItem(
                                onClick = {
                                    isFabMenuExpanded = false
                                    viewModel.onShowBulkModificationDialog(BulkProductOperation.DEACTIVATE)
                                },
                                icon = {
                                    Icon(
                                        painter = painterResource(Res.drawable.remove),
                                        contentDescription = null
                                    )
                                },
                                text = { Text(stringResource(Res.string.bulk_op_deactivate_title)) },
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            FloatingActionButtonMenuItem(
                                onClick = {
                                    isFabMenuExpanded = false
                                    viewModel.onShowBulkModificationDialog(BulkProductOperation.DELETE)
                                },
                                icon = {
                                    Icon(
                                        painter = painterResource(Res.drawable.delete),
                                        contentDescription = null
                                    )
                                },
                                text = { Text(stringResource(Res.string.bulk_op_delete_title)) },
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }

                    if (isAndroid()) {
                        FloatingActionButton(
                            onClick = {
                                viewModel.onShowProductDialog(
                                    Products(
                                        id = "",
                                        codigos = "[]",
                                        nombre = "",
                                        precio = 0.0,
                                        costo = 0.0,
                                        categoria = "",
                                        activo = 1L,
                                        por_peso = 0L,
                                        precio_mayoreo = 0.0,
                                        es_favorito = 0L,
                                        piezas = 1.0,
                                        updated_at = 0L,
                                        sync_state = ""
                                    )
                                )
                            },
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.add),
                                contentDescription = stringResource(Res.string.new_product_button)
                            )
                        }
                        Spacer(modifier.height(16.dp))

                        if (isCompact) {
                            ExtendedFloatingActionButton(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                onClick = { showCameraScanner = true },
                                icon = {
                                    Icon(
                                        painter = painterResource(Res.drawable.barcode_scanner),
                                        contentDescription = null
                                    )
                                },
                                text = {
                                    Text(stringResource(Res.string.scan_with_camera_desc))
                                }
                            )
                        } else {
                            LargeExtendedFloatingActionButton(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                onClick = { showCameraScanner = true },
                                icon = {
                                    Icon(
                                        painter = painterResource(Res.drawable.barcode_scanner),
                                        contentDescription = null
                                    )
                                },
                                text = {
                                    Text(stringResource(Res.string.scan_with_camera_desc))
                                }
                            )
                        }
                    } else {
                        if (isCompact) {
                            ExtendedFloatingActionButton(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                onClick = {
                                    viewModel.onShowProductDialog(
                                        Products(
                                            id = "",
                                            codigos = "[]",
                                            nombre = "",
                                            precio = 0.0,
                                            costo = 0.0,
                                            categoria = "",
                                            activo = 1L,
                                            por_peso = 0L,
                                            precio_mayoreo = 0.0,
                                            es_favorito = 0L,
                                            piezas = 1.0,
                                            updated_at = 0L,
                                            sync_state = ""
                                        )
                                    )
                                },
                                icon = {
                                    Icon(
                                        painter = painterResource(Res.drawable.add),
                                        contentDescription = null
                                    )
                                },
                                text = {
                                    Text(stringResource(Res.string.new_product_button_desktop))
                                }
                            )
                        } else {
                            LargeExtendedFloatingActionButton(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                onClick = {
                                    viewModel.onShowProductDialog(
                                        Products(
                                            id = "",
                                            codigos = "[]",
                                            nombre = "",
                                            precio = 0.0,
                                            costo = 0.0,
                                            categoria = "",
                                            activo = 1L,
                                            por_peso = 0L,
                                            precio_mayoreo = 0.0,
                                            es_favorito = 0L,
                                            piezas = 1.0,
                                            updated_at = 0L,
                                            sync_state = ""
                                        )
                                    )
                                },
                                icon = {
                                    Icon(
                                        painter = painterResource(Res.drawable.add),
                                        contentDescription = null
                                    )
                                },
                                text = {
                                    Text(stringResource(Res.string.new_product_button_desktop))
                                }
                            )
                        }
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.background,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .then(
                    if (!isAndroid()) {
                        Modifier
                            .onPreviewKeyEvent(handleKeyNavigation)
                    } else Modifier
                )
        ) { innerPadding ->
            PullToRefreshBox(
                isRefreshing = uiState.isSyncing,
                onRefresh = { viewModel.refreshSync() },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding())
            ) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    val isCompact = maxWidth < 720.dp
                    val availableWidth = maxWidth
                    val selectedFilteredCount = sortedProducts.count { it.id in selectedProductIds }
                    val selectAllState = when {
                        sortedProducts.isEmpty() -> ToggleableState.Off
                        selectedFilteredCount == sortedProducts.size -> ToggleableState.On
                        selectedFilteredCount > 0 -> ToggleableState.Indeterminate
                        else -> ToggleableState.Off
                    }

                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // SEARCH BAR
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isCompact) {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(MaterialShapes.Clover8Leaf.toShape())
                                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                                    contentAlignment = Alignment.Center
                                ) {
                                    TriStateCheckbox(
                                        state = selectAllState,
                                        onClick = {
                                            viewModel.onSelectAllProducts(sortedProducts.map { it.id })
                                        }
                                    )
                                }

                                Spacer(modifier = Modifier.width(6.dp))
                            }

                            Box(
                                modifier = Modifier
                                    .height(54.dp)
                                    .weight(1f)
                                    .background(
                                        color = if (searchQuery.isNotEmpty())
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
                                        if (searchQuery.isEmpty()) {
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
                                            value = searchQuery,
                                            onValueChange = {
                                                viewModel.onSearchQueryChanged(it)
                                                selectedProductIndex = -1
                                                selectionAnchorIndex = -1
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .then(
                                                    if (!isAndroid()) {
                                                        Modifier
                                                            .focusRequester(searchBarFocusRequester)
                                                            .onPreviewKeyEvent(handleKeyNavigation)
                                                    } else Modifier
                                                ),
                                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                                color = MaterialTheme.colorScheme.onSurface,
                                                textAlign = TextAlign.Start
                                            ),
                                            singleLine = true
                                        )
                                    }

                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(
                                            modifier = Modifier
                                                .size(32.dp),
                                            onClick = {
                                                viewModel.onSearchQueryChanged("")
                                                selectedProductIndex = -1
                                                selectionAnchorIndex = -1
                                                reclaimSearchBarFocus()
                                            }
                                        ) {
                                            Icon(
                                                painter = painterResource(Res.drawable.close),
                                                contentDescription = stringResource(Res.string.clear_desc),
                                                tint = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Box {
                                IconButton(
                                    onClick = { showFilterBottomSheet = true },
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(MaterialShapes.Cookie4Sided.toShape())
                                        .background(
                                            if (uiState.hasActiveFilters) MaterialTheme.colorScheme.primaryContainer
                                            else MaterialTheme.colorScheme.surfaceContainerHigh
                                        ),
                                ) {
                                    Icon(
                                        painter = if (uiState.hasActiveFilters) painterResource(Res.drawable.filter_on)
                                        else painterResource(Res.drawable.filter),
                                        contentDescription = stringResource(Res.string.filter_and_sort_title),
                                        tint = if (uiState.hasActiveFilters) MaterialTheme.colorScheme.onPrimaryContainer
                                        else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                if (uiState.hasActiveFilters) {
                                    Badge(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .offset(x = (-2).dp, y = 2.dp)
                                            .size(10.dp),
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }



                        Spacer(modifier = Modifier.height(16.dp))

                        // PRODUCTS TABLE
                        Card(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = ShapeDefaults.cardShape,
                        ) {

                            if (sortedProducts.isEmpty()) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center

                                ) {
                                    Icon(
                                        painter = painterResource(Res.drawable.sad_face),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(96.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        stringResource(Res.string.no_products_registered),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.headlineMedium,
                                    )
                                }
                            } else {
                                if (isCompact) {
                                    // Mobile Compact List
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                        contentPadding = PaddingValues(bottom = 156.dp)
                                    ) {
                                        itemsIndexed(sortedProducts) { index, product ->
                                            val shape = if (sortedProducts.size == 1) {
                                                ShapeDefaults.cardShape
                                            } else if (index == 0) {
                                                ShapeDefaults.topListItemShape
                                            } else if (index == sortedProducts.lastIndex) {
                                                ShapeDefaults.bottomListItemShape
                                            } else {
                                                ShapeDefaults.middleListItemShape
                                            }
                                            val isSelected = product.id in selectedProductIds
                                            ProductSimpleCard(
                                                product = product,
                                                shape = shape,
                                                isSelected = isSelected,
                                                showCheckbox = false,
                                                onClick = {
                                                    if (selectedProductIds.isNotEmpty()) {
                                                        viewModel.onToggleSelectProduct(product.id)
                                                    } else {
                                                        viewModel.onShowProductDialog(product)
                                                    }
                                                },
                                                onLongClick = {
                                                    viewModel.onToggleSelectProduct(product.id)
                                                }
                                            )
                                        }
                                    }
                                } else {
                                    // Desktop Table Layout
                                    Column(modifier = Modifier.fillMaxSize()) {
                                        val activeColumns = remember(uiState.visibleColumns) {
                                            ProductTableColumn.entries.filter { it in uiState.visibleColumns }
                                        }
                                        val totalDefaultWeight = remember(activeColumns) {
                                            activeColumns.sumOf { it.defaultWeight.toDouble() }.toFloat().coerceAtLeast(0.01f)
                                        }
                                        var columnWeights by remember(activeColumns) {
                                            mutableStateOf(activeColumns.map { (it.defaultWeight / totalDefaultWeight) * 0.95f })
                                        }
                                        val tableWidthPx =
                                            with(LocalDensity.current) { availableWidth.toPx() }
                                        val resizeColumn = { index: Int, dragAmount: Float ->
                                            if (index in 0 until activeColumns.lastIndex) {
                                                val weightDelta = dragAmount / tableWidthPx
                                                val current = columnWeights[index]
                                                val next = columnWeights[index + 1]
                                                val minimumWeight = 0.05f
                                                val constrainedDelta = weightDelta.coerceIn(
                                                    minimumWeight - current,
                                                    next - minimumWeight
                                                )
                                                columnWeights =
                                                    columnWeights.toMutableList().also { weights ->
                                                        weights[index] = current + constrainedDelta
                                                        weights[index + 1] = next - constrainedDelta
                                                    }
                                            }
                                        }
                                        val onHeaderClick = { field: ProductSortField ->
                                            if (sortField == field) {
                                                viewModel.onSortOrderChanged(
                                                    if (sortOrder == ProductSortOrder.ASC) ProductSortOrder.DESC else ProductSortOrder.ASC
                                                )
                                            } else {
                                                viewModel.onSortFieldChanged(field)
                                                viewModel.onSortOrderChanged(ProductSortOrder.ASC)
                                            }
                                        }

                                        ProductTableHeaderRow(
                                            visibleColumns = activeColumns,
                                            columnWeights = columnWeights,
                                            totalDefaultWeight = totalDefaultWeight,
                                            showSelectAll = true,
                                            selectAllState = selectAllState,
                                            onSelectAllClick = {
                                                viewModel.onSelectAllProducts(sortedProducts.map { it.id })
                                            },
                                            sortField = sortField,
                                            sortOrder = sortOrder,
                                            onHeaderClick = onHeaderClick,
                                            onResizeColumn = resizeColumn
                                        )

                                        LazyColumn(
                                            state = listState,
                                            modifier = Modifier.fillMaxSize(),
                                            verticalArrangement = Arrangement.spacedBy(2.dp),
                                            contentPadding = PaddingValues(bottom = if (isAndroid()) 160.dp else 192.dp)
                                        ) {
                                            itemsIndexed(sortedProducts) { index, product ->
                                                val shape =
                                                    if (sortedProducts.size == 1 || index == sortedProducts.lastIndex) ShapeDefaults.bottomListItemShape
                                                    else ShapeDefaults.middleListItemShape
                                                val isHighlighted = selectedProductIndex == index

                                                ProductTableRow(
                                                    product = product,
                                                    visibleColumns = activeColumns,
                                                    columnWeights = columnWeights,
                                                    totalDefaultWeight = totalDefaultWeight,
                                                    shape = shape,
                                                    isHighlighted = isHighlighted,
                                                    showCheckbox = true,
                                                    isChecked = product.id in selectedProductIds,
                                                    onCheckedChange = {
                                                        selectedProductIndex = index
                                                        selectionAnchorIndex = index
                                                        viewModel.onToggleSelectProduct(product.id)
                                                    },
                                                    salesStats = uiState.salesStats,
                                                    defaultRetailMargin = uiState.defaultRetailMargin,
                                                    defaultWholesaleMargin = uiState.defaultWholesaleMargin,
                                                    onClick = {
                                                        selectedProductIndex = index
                                                        selectionAnchorIndex = index
                                                        viewModel.onShowProductDialog(product)
                                                    }
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

            // PRODUCT FORM DIALOG
            if (showProductDialogFor != null) {
                ProductFormDialog(
                    product = if (showProductDialogFor.id.isEmpty()) null else showProductDialogFor,
                    onDismiss = { viewModel.onDismissProductDialog() },
                    onSave = { updatedProduct ->
                        viewModel.saveProduct(updatedProduct)
                    },
                    onValidateBarcodes = { codes ->
                        viewModel.validateBarcodes(codes, showProductDialogFor.id.ifEmpty { null })
                    },
                    existingCategories = uiState.availableCategories,
                    defaultRetailMarginPercentage = uiState.defaultRetailMargin,
                    defaultWholesaleMarginPercentage = uiState.defaultWholesaleMargin,
                    roundRetailPrice = uiState.roundRetailPrice,
                    roundWholesalePrice = uiState.roundWholesalePrice
                )
            }

            showBulkModificationFor?.let { op ->
                BulkProductModificationDialog(
                    selectedCount = selectedProductIds.size,
                    operation = op,
                    onDismiss = { viewModel.onShowBulkModificationDialog(null) },
                    onApply = { modification ->
                        viewModel.applyBulkModification(modification)
                    }
                )
            }

            if (showCameraScanner) {
                PlatformBarcodeScanner(
                    onScanResult = { scannedCode ->
                        showCameraScanner = false
                        val code = scannedCode.trim()
                        if (code.isNotEmpty()) {
                            viewModel.onSearchQueryChanged(code)
                        }
                    },
                    onClose = { showCameraScanner = false }
                )
            }

            if (showFilterBottomSheet) {
                ProductFilterAndSortBottomSheet(
                    isCompact = isCompact,
                    sortField = sortField,
                    sortOrder = sortOrder,
                    selectedCategory = uiState.selectedCategory,
                    favoriteFilter = uiState.favoriteFilter,
                    statusFilter = uiState.statusFilter,
                    availableCategories = uiState.availableCategories,
                    visibleColumns = uiState.visibleColumns,
                    onToggleColumn = { viewModel.onToggleColumn(it) },
                    onSortFieldSelected = { viewModel.onSortFieldChanged(it) },
                    onSortOrderSelected = { viewModel.onSortOrderChanged(it) },
                    onCategorySelected = { viewModel.onCategoryFilterChanged(it) },
                    onFavoriteFilterSelected = { viewModel.onFavoriteFilterChanged(it) },
                    onStatusFilterSelected = { viewModel.onStatusFilterChanged(it) },
                    onResetFilters = { viewModel.onResetFilters() },
                    onDismissRequest = { showFilterBottomSheet = false }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ProductFilterAndSortBottomSheet(
    isCompact: Boolean = false,
    sortField: ProductSortField,
    sortOrder: ProductSortOrder,
    selectedCategory: String?,
    favoriteFilter: FavoriteFilterOption,
    statusFilter: StatusFilterOption,
    availableCategories: List<String>,
    visibleColumns: Set<ProductTableColumn> = DEFAULT_PRODUCT_TABLE_COLUMNS,
    onToggleColumn: (ProductTableColumn) -> Unit = {},
    onSortFieldSelected: (ProductSortField) -> Unit,
    onSortOrderSelected: (ProductSortOrder) -> Unit,
    onCategorySelected: (String?) -> Unit,
    onFavoriteFilterSelected: (FavoriteFilterOption) -> Unit,
    onStatusFilterSelected: (StatusFilterOption) -> Unit,
    onResetFilters: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
                    modifier = Modifier.size(32.dp).clip(MaterialShapes.Ghostish.toShape())
                        .background(
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        ),

                    ) {
                    Icon(
                        painter = painterResource(Res.drawable.filter_off),
                        contentDescription = stringResource(Res.string.reset_filters),
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
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val options = listOf(
                                ProductSortField.CODIGO to stringResource(Res.string.header_codes),
                                ProductSortField.NOMBRE to stringResource(Res.string.header_product_name),
                                ProductSortField.CATEGORIA to stringResource(Res.string.header_category),
                                ProductSortField.PIEZAS to stringResource(Res.string.header_pieces),
                                ProductSortField.PRECIO to stringResource(Res.string.header_retail_price),
                                ProductSortField.COSTO to stringResource(Res.string.header_cost),
                                ProductSortField.MAYOREO to stringResource(Res.string.wholesale),
                                ProductSortField.MARGEN_VENTA to stringResource(Res.string.header_retail_margin),
                                ProductSortField.MARGEN_MAYOREO to stringResource(Res.string.header_wholesale_margin),
                                ProductSortField.VENTAS_TOTALES to stringResource(Res.string.header_total_sales),
                                ProductSortField.ULTIMA_VENTA to stringResource(Res.string.header_last_sale)
                            )
                            options.forEach { (field, label) ->
                                FilterChip(
                                    selected = sortField == field,
                                    onClick = { onSortFieldSelected(field) },
                                    label = { Text(label) },
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
                                Pair(
                                    ProductSortOrder.ASC,
                                    "▲ " + stringResource(Res.string.sort_order_asc)
                                ),
                                Pair(
                                    ProductSortOrder.DESC,
                                    "▼ " + stringResource(Res.string.sort_order_desc)
                                )
                            )
                            orderOptions.forEachIndexed { index, (order, label) ->
                                val isSelected = sortOrder == order
                                ToggleButton(
                                    checked = isSelected,
                                    onCheckedChange = { onSortOrderSelected(order) },
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

                // Section 3: Visible Columns (Wide View Only)
                if (!isCompact) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = stringResource(Res.string.columns_section_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                ProductTableColumn.entries.forEach { col ->
                                    val isSelected = visibleColumns.contains(col)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { onToggleColumn(col) },
                                        label = { Text(stringResource(col.titleRes)) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // Section 4: Filter by Category
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

                // Section 4: Filter by Favorite (Connected ToggleButtons)
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(Res.string.filter_favorite_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val favOptions = listOf(
                                FavoriteFilterOption.ALL to stringResource(Res.string.filter_favorite_all),
                                FavoriteFilterOption.ONLY_FAVORITES to stringResource(Res.string.filter_favorite_only),
                                FavoriteFilterOption.ONLY_NON_FAVORITES to stringResource(Res.string.filter_non_favorite_only)
                            )
                            favOptions.forEachIndexed { index, (option, label) ->
                                val isSelected = favoriteFilter == option
                                ToggleButton(
                                    checked = isSelected,
                                    onCheckedChange = { onFavoriteFilterSelected(option) },
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
                                        favOptions.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
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

                // Section 5: Filter by Status (Connected ToggleButtons)
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(Res.string.filter_status_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val statusOptions = listOf(
                                StatusFilterOption.ALL to stringResource(Res.string.filter_status_all),
                                StatusFilterOption.ONLY_ACTIVE to stringResource(Res.string.filter_status_active_only),
                                StatusFilterOption.ONLY_INACTIVE to stringResource(Res.string.filter_status_inactive_only)
                            )
                            statusOptions.forEachIndexed { index, (option, label) ->
                                val isSelected = statusFilter == option
                                ToggleButton(
                                    checked = isSelected,
                                    onCheckedChange = { onStatusFilterSelected(option) },
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
                                        statusOptions.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
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
            }

            // Apply Button
            ElevatedButton(
                onClick = onDismissRequest,
                modifier = Modifier.fillMaxWidth().height(48.dp),
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




