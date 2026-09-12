package com.dnavarro.poskmp.ui

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dnavarro.poskmp.db.Products
import com.dnavarro.poskmp.theme.ShapeDefaults
import com.dnavarro.poskmp.ui.components.ProductSimpleCard
import com.dnavarro.poskmp.ui.components.ProductTableHeaderRow
import com.dnavarro.poskmp.ui.components.ProductTableRow
import com.dnavarro.poskmp.ui.productos.ProductTableColumn
import com.dnavarro.poskmp.util.formatBarcodesForDisplay
import com.dnavarro.poskmp.util.formatPrice
import com.dnavarro.poskmp.util.isAndroid
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import poskmp.shared.generated.resources.Res
import poskmp.shared.generated.resources.add
import poskmp.shared.generated.resources.add_to_ticket
import poskmp.shared.generated.resources.barcode_scanner
import poskmp.shared.generated.resources.check
import poskmp.shared.generated.resources.clear_desc
import poskmp.shared.generated.resources.close
import poskmp.shared.generated.resources.close_scanner_desc
import poskmp.shared.generated.resources.decrease_desc
import poskmp.shared.generated.resources.edit
import poskmp.shared.generated.resources.empty_icon_desc
import poskmp.shared.generated.resources.favorite_desc
import poskmp.shared.generated.resources.increase_desc
import poskmp.shared.generated.resources.mark_as_favorite
import poskmp.shared.generated.resources.modify
import poskmp.shared.generated.resources.no_category
import poskmp.shared.generated.resources.no_products_found
import poskmp.shared.generated.resources.not_registered
import poskmp.shared.generated.resources.not_registered_hotkey
import poskmp.shared.generated.resources.remove
import poskmp.shared.generated.resources.remove_from_favorites
import poskmp.shared.generated.resources.sad_face
import poskmp.shared.generated.resources.search
import poskmp.shared.generated.resources.search_desc
import poskmp.shared.generated.resources.search_placeholder
import poskmp.shared.generated.resources.shopping_cart
import poskmp.shared.generated.resources.star
import poskmp.shared.generated.resources.star_filled
import poskmp.shared.generated.resources.tab_ticket
import poskmp.shared.generated.resources.view_ticket_fab
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

private const val SEARCH_DEBOUNCE_MILLIS = 300L

@Composable
fun CatalogSection(
    modifier: Modifier = Modifier,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    productsList: List<Products>,
    onProductClick: (Products) -> Unit,
    onToggleFavorite: (Products) -> Unit,
    onModifyProduct: (Products) -> Unit,
    isCompact: Boolean,
    useProductTable: Boolean = false,
    prioritizeDeliveryPrice: Boolean = false,
    onViewCartClick: (() -> Unit)? = null,
    onOpenScanner: (() -> Unit)? = null,
    cartCount: Int = 0,
    cartTotal: Double = 0.0,
    onSellUnregisteredClick: () -> Unit = {},
    searchFocusRequester: FocusRequester? = null,
    onBarcodeScan: ((String) -> Unit)? = null,
    onSearchKeyIntercept: ((KeyEvent) -> Boolean)? = null,
    onAddProductWithQuantity: ((Products, Double) -> Unit)? = null
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val coroutineScope = rememberCoroutineScope()
    val latestSearchQuery = remember { mutableStateOf(searchQuery) }
    var sortField by remember { mutableStateOf(ProductSortField.NOMBRE) }
    var sortOrder by remember { mutableStateOf(ProductSortOrder.ASC) }
    var selectedCatalogIndex by remember { mutableIntStateOf(-1) }

    val compactListState = rememberLazyListState()
    val tableListState = rememberLazyListState()
    val gridState = rememberLazyGridState()

    fun resetScrollPosition() {
        try {
            tableListState.requestScrollToItem(0)
            compactListState.requestScrollToItem(0)
            gridState.requestScrollToItem(0)
        } catch (_: Exception) {
        }
        coroutineScope.launch {
            try {
                tableListState.scrollToItem(0)
            } catch (_: Exception) {
            }
            try {
                compactListState.scrollToItem(0)
            } catch (_: Exception) {
            }
            try {
                gridState.scrollToItem(0)
            } catch (_: Exception) {
            }
        }
    }

    val handleAddQuantity: (Products, Double) -> Unit = { product, qty ->
        if (onAddProductWithQuantity != null) {
            onAddProductWithQuantity(product, qty)
        } else {
            repeat(qty.toInt().coerceAtLeast(1)) {
                onProductClick(product)
            }
        }
        if (latestSearchQuery.value.isNotEmpty()) {
            latestSearchQuery.value = ""
            onSearchQueryChange("")
        }
        selectedCatalogIndex = -1
        resetScrollPosition()
    }

    val sortedProducts = remember(productsList, sortField, sortOrder) {
        productsList.sortedWith { p1, p2 ->
            val f1 = p1.es_favorito == 1L
            val f2 = p2.es_favorito == 1L
            if (f1 != f2) return@sortedWith if (f1) -1 else 1

            val primaryComp = when (sortField) {
                ProductSortField.NOMBRE -> p1.nombre.lowercase().compareTo(p2.nombre.lowercase())
                ProductSortField.CODIGO -> {
                    val c1 = p1.formatBarcodesForDisplay(emptyFallback = "")
                    val c2 = p2.formatBarcodesForDisplay(emptyFallback = "")
                    c1.lowercase().compareTo(c2.lowercase())
                }

                ProductSortField.CATEGORIA -> (p1.categoria ?: "").lowercase()
                    .compareTo((p2.categoria ?: "").lowercase())

                ProductSortField.PIEZAS -> p1.piezas.compareTo(p2.piezas)
                ProductSortField.PRECIO -> p1.precio.compareTo(p2.precio)
                ProductSortField.COSTO -> p1.costo.compareTo(p2.costo)
                ProductSortField.MAYOREO -> p1.precio_mayoreo.compareTo(p2.precio_mayoreo)
                ProductSortField.DOMICILIO -> p1.precio_delivery.compareTo(p2.precio_delivery)
                else -> p1.nombre.lowercase().compareTo(p2.nombre.lowercase())
            }

            if (sortOrder == ProductSortOrder.ASC) primaryComp else -primaryComp
        }
    }

    val displayedProducts = remember(sortedProducts) {
        sortedProducts.take(50)
    }

    LaunchedEffect(displayedProducts) {
        if (selectedCatalogIndex <= 0) {
            resetScrollPosition()
        }
    }

    LaunchedEffect(searchQuery) {
        if (latestSearchQuery.value != searchQuery) {
            latestSearchQuery.value = searchQuery
            selectedCatalogIndex = -1
            resetScrollPosition()
        }
    }

    LaunchedEffect(latestSearchQuery.value) {
        selectedCatalogIndex = -1
        resetScrollPosition()
        delay(SEARCH_DEBOUNCE_MILLIS.milliseconds)
        onSearchQueryChange(latestSearchQuery.value)
    }

    LaunchedEffect(selectedCatalogIndex) {
        if (selectedCatalogIndex in displayedProducts.indices) {
            try {
                if (useProductTable) {
                    if (isCompact) {
                        compactListState.scrollItemIntoView(selectedCatalogIndex)
                    } else {
                        tableListState.scrollItemIntoView(selectedCatalogIndex)
                    }
                } else {
                    gridState.scrollItemIntoView(selectedCatalogIndex)
                }
            } catch (_: Exception) {
            }
        } else if (selectedCatalogIndex == -1) {
            resetScrollPosition()
        }
    }

    LaunchedEffect(Unit) {
        if (isAndroid()) {
            focusManager.clearFocus(force = true)
            keyboardController?.hide()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .widthIn(min = 320.dp)
            .padding(16.dp)
            .then(
                if (isAndroid()) {
                    Modifier
                        .focusProperties { canFocus = true }
                        .focusable()
                } else Modifier
            )
    ) {
        // Search Bar & Fast Codes
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp)
                .background(
                    color = if (latestSearchQuery.value.isNotEmpty())
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
                    if (latestSearchQuery.value.isEmpty()) {
                        Text(
                            text = stringResource(Res.string.search_placeholder),
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            ),
                            textAlign = TextAlign.Start,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    BasicTextField(
                        value = latestSearchQuery.value,
                        onValueChange = { query ->
                            val sanitized = query.filter { it != '+' && it != '-' }
                            latestSearchQuery.value = sanitized
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .let { mod ->
                                if (searchFocusRequester != null && !isAndroid()) mod.focusRequester(
                                    searchFocusRequester
                                ) else mod
                            }
                            .onPreviewKeyEvent { keyEvent ->
                                val key = keyEvent.key
                                val codePoint = keyEvent.utf16CodePoint
                                val isPlus =
                                    key == Key.Plus || key == Key.NumPadAdd || key == Key.Equals || codePoint == '+'.code
                                val isMinus =
                                    key == Key.Minus || key == Key.NumPadSubtract || codePoint == '-'.code
                                val isUp = key == Key.DirectionUp
                                val isDown = key == Key.DirectionDown
                                val isEnter = key == Key.Enter || key == Key.NumPadEnter

                                if (keyEvent.key == Key.Escape && latestSearchQuery.value.isNotEmpty()) {
                                    if (keyEvent.type == KeyEventType.KeyDown) {
                                        latestSearchQuery.value = ""
                                        onSearchQueryChange("")
                                        selectedCatalogIndex = -1
                                        resetScrollPosition()
                                    }
                                    true
                                } else if (latestSearchQuery.value.isNotEmpty() && (isUp || isDown)) {
                                    if (keyEvent.type == KeyEventType.KeyDown && displayedProducts.isNotEmpty()) {
                                        selectedCatalogIndex = if (isDown) {
                                            if (selectedCatalogIndex < displayedProducts.lastIndex) {
                                                selectedCatalogIndex + 1
                                            } else {
                                                displayedProducts.lastIndex
                                            }
                                        } else {
                                            if (selectedCatalogIndex > 0) {
                                                selectedCatalogIndex - 1
                                            } else {
                                                -1
                                            }
                                        }
                                    }
                                    true
                                } else if (latestSearchQuery.value.isNotEmpty() && isEnter && selectedCatalogIndex in displayedProducts.indices) {
                                    if (keyEvent.type == KeyEventType.KeyDown) {
                                        val selectedProduct =
                                            displayedProducts[selectedCatalogIndex]
                                        onProductClick(selectedProduct)
                                        latestSearchQuery.value = ""
                                        onSearchQueryChange("")
                                        selectedCatalogIndex = -1
                                        resetScrollPosition()
                                    }
                                    true
                                } else if (onSearchKeyIntercept != null && onSearchKeyIntercept(
                                        keyEvent
                                    )
                                ) {
                                    true
                                } else if (isPlus || isMinus) {
                                    true
                                } else if (keyEvent.type == KeyEventType.KeyDown && isEnter) {
                                    val scannedText = latestSearchQuery.value
                                    latestSearchQuery.value = ""
                                    onSearchQueryChange("")
                                    selectedCatalogIndex = -1
                                    resetScrollPosition()
                                    if (scannedText.isNotBlank() && onBarcodeScan != null) {
                                        onBarcodeScan(scannedText)
                                    }
                                    true
                                } else {
                                    false
                                }
                            },
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Search
                        ),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                if (latestSearchQuery.value.isNotEmpty() && selectedCatalogIndex in displayedProducts.indices) {
                                    val selectedProduct = displayedProducts[selectedCatalogIndex]
                                    onProductClick(selectedProduct)
                                    latestSearchQuery.value = ""
                                    onSearchQueryChange("")
                                    selectedCatalogIndex = -1
                                    resetScrollPosition()
                                } else {
                                    val scannedText = latestSearchQuery.value
                                    latestSearchQuery.value = ""
                                    onSearchQueryChange("")
                                    selectedCatalogIndex = -1
                                    resetScrollPosition()
                                    if (scannedText.isNotBlank() && onBarcodeScan != null) {
                                        onBarcodeScan(scannedText)
                                    }
                                }
                            },
                            onDone = {
                                if (latestSearchQuery.value.isNotEmpty() && selectedCatalogIndex in displayedProducts.indices) {
                                    val selectedProduct = displayedProducts[selectedCatalogIndex]
                                    onProductClick(selectedProduct)
                                    latestSearchQuery.value = ""
                                    onSearchQueryChange("")
                                    selectedCatalogIndex = -1
                                    resetScrollPosition()
                                } else {
                                    val scannedText = latestSearchQuery.value
                                    latestSearchQuery.value = ""
                                    onSearchQueryChange("")
                                    selectedCatalogIndex = -1
                                    resetScrollPosition()
                                    if (scannedText.isNotBlank() && onBarcodeScan != null) {
                                        onBarcodeScan(scannedText)
                                    }
                                }
                            }
                        ),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Start
                        ),
                        singleLine = true,
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
                    )
                }

                if (latestSearchQuery.value.isNotEmpty()) {
                    IconButton(
                        modifier = Modifier.size(32.dp),
                        onClick = {
                            latestSearchQuery.value = ""
                            onSearchQueryChange("")
                            selectedCatalogIndex = -1
                            resetScrollPosition()
                            if (!isAndroid()) {
                                coroutineScope.launch {
                                    delay(50.milliseconds)
                                    try {
                                        searchFocusRequester?.requestFocus()
                                    } catch (_: Exception) {
                                    }
                                }
                            }
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

            Spacer(modifier = Modifier.width(6.dp))

            Box {
                TooltipBox(
                    positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                        TooltipAnchorPosition.Below,
                        4.dp
                    ),
                    tooltip = {
                        PlainTooltip {
                            Text(if (isAndroid()) stringResource(Res.string.not_registered) else stringResource(Res.string.not_registered_hotkey))
                        }
                    },
                    state = rememberTooltipState()
                ) {
                    IconButton(
                        onClick = onSellUnregisteredClick,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(MaterialShapes.Cookie4Sided.toShape())
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.add),
                            contentDescription = stringResource(if (isAndroid()) Res.string.not_registered else Res.string.not_registered_hotkey),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (productsList.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        painter = painterResource(Res.drawable.sad_face),
                        contentDescription = stringResource(Res.string.empty_icon_desc),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        stringResource(Res.string.no_products_found),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 16.sp
                    )
                }
            }
        } else {
            Box(modifier = Modifier.weight(1f)) {
                if (useProductTable) {
                    if (isCompact) {
                        LazyColumn(
                            state = compactListState,
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            contentPadding = PaddingValues(bottom = 220.dp)
                        ) {
                            itemsIndexed(displayedProducts, key = { _, product -> product.id }) { index, product ->
                                val shape = if (displayedProducts.size == 1) {
                                    ShapeDefaults.cardShape
                                } else if (index == 0) {
                                    ShapeDefaults.topListItemShape
                                } else if (index == displayedProducts.lastIndex) {
                                    ShapeDefaults.bottomListItemShape
                    } else {
                                    ShapeDefaults.middleListItemShape
                                }
                                var showContextMenu by remember { mutableStateOf(false) }

                                val displayProduct = if (prioritizeDeliveryPrice && product.precio_delivery > 0.0) {
                                    product.copy(precio = product.precio_delivery)
                                } else {
                                    product
                                }

                                ProductSimpleCard(
                                    product = displayProduct,
                                    shape = shape,
                                    isSelected = selectedCatalogIndex == index,
                                    showCheckbox = false,
                                    onClick = {
                                        onProductClick(product)
                                        if (latestSearchQuery.value.isNotEmpty()) {
                                            latestSearchQuery.value = ""
                                            onSearchQueryChange("")
                                        }
                                        selectedCatalogIndex = -1
                                        resetScrollPosition()
                                    },
                                    onLongClick = { showContextMenu = true },
                                    onSecondaryClick = { showContextMenu = true },
                                    contextMenu = {
                                        ProductContextMenu(
                                            expanded = showContextMenu,
                                            onDismissRequest = { showContextMenu = false },
                                            product = product,
                                            onAddQuantity = handleAddQuantity,
                                            onToggleFavorite = onToggleFavorite,
                                            onModifyProduct = onModifyProduct
                                        )
                                    }
                                )
                            }
                        }
                    } else {
                        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                            val availableWidth = maxWidth
                            val activeColumns = remember {
                                listOf(
                                    ProductTableColumn.NOMBRE,
                                    ProductTableColumn.CATEGORIA,
                                    ProductTableColumn.PIEZAS,
                                    ProductTableColumn.PRECIO
                                )
                            }
                            val totalDefaultWeight = remember(activeColumns) {
                                activeColumns.sumOf { it.defaultWeight.toDouble() }.toFloat()
                                    .coerceAtLeast(0.01f)
                            }
                            var columnWeights by remember(activeColumns) {
                                mutableStateOf(activeColumns.map { (it.defaultWeight / totalDefaultWeight) * 1.0f })
                            }
                            val tableWidthPx = with(LocalDensity.current) { availableWidth.toPx() }
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
                                    columnWeights = columnWeights.toMutableList().also { weights ->
                                        weights[index] = current + constrainedDelta
                                        weights[index + 1] = next - constrainedDelta
                                    }
                                }
                            }
                            val onHeaderClick = { field: ProductSortField ->
                                if (sortField == field) {
                                    sortOrder =
                                        if (sortOrder == ProductSortOrder.ASC) ProductSortOrder.DESC else ProductSortOrder.ASC
                                } else {
                                    sortField = field
                                    sortOrder = ProductSortOrder.ASC
                                }
                            }

                            Card(
                                modifier = Modifier.fillMaxSize(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = ShapeDefaults.cardShape
                            ) {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    ProductTableHeaderRow(
                                        visibleColumns = activeColumns,
                                        columnWeights = columnWeights,
                                        totalDefaultWeight = totalDefaultWeight,
                                        showSelectAll = false,
                                        sortField = sortField,
                                        sortOrder = sortOrder,
                                        onHeaderClick = onHeaderClick,
                                        onResizeColumn = resizeColumn
                                    )

                                    LazyColumn(
                                        state = tableListState,
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.spacedBy(2.dp),
                                        contentPadding = PaddingValues(bottom = 220.dp)
                                    ) {
                                        itemsIndexed(displayedProducts, key = { _, product -> product.id }) { index, product ->
                                            val shape =
                                                if (displayedProducts.size == 1 || index == displayedProducts.lastIndex) ShapeDefaults.bottomListItemShape
                                                else ShapeDefaults.middleListItemShape
                                            var showContextMenu by remember { mutableStateOf(false) }

                                            val displayProduct = if (prioritizeDeliveryPrice && product.precio_delivery > 0.0) {
                                                product.copy(precio = product.precio_delivery)
                                            } else {
                                                product
                                            }

                                            ProductTableRow(
                                                product = displayProduct,
                                                visibleColumns = activeColumns,
                                                columnWeights = columnWeights,
                                                totalDefaultWeight = totalDefaultWeight,
                                                shape = shape,
                                                isHighlighted = selectedCatalogIndex == index,
                                                showCheckbox = false,
                                                onClick = {
                                                    onProductClick(product)
                                                    if (latestSearchQuery.value.isNotEmpty()) {
                                                        latestSearchQuery.value = ""
                                                        onSearchQueryChange("")
                                                    }
                                                    selectedCatalogIndex = -1
                                                    resetScrollPosition()
                                                },
                                                onLongClick = { showContextMenu = true },
                                                onSecondaryClick = { showContextMenu = true },
                                                contextMenu = {
                                                    ProductContextMenu(
                                                        expanded = showContextMenu,
                                                        onDismissRequest = { showContextMenu = false },
                                                        product = product,
                                                        onAddQuantity = handleAddQuantity,
                                                        onToggleFavorite = onToggleFavorite,
                                                        onModifyProduct = onModifyProduct
                                                    )
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Adaptive(minSize = 192.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 220.dp)
                    ) {
                        itemsIndexed(displayedProducts, key = { _, product -> product.id }) { index, product ->
                            var showContextMenu by remember { mutableStateOf(false) }
                            val isHighlighted = selectedCatalogIndex == index

                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isHighlighted) MaterialTheme.colorScheme.primaryContainer.copy(
                                        alpha = 0.45f
                                    )
                                    else MaterialTheme.colorScheme.surfaceContainerLow
                                ),
                                shape = ShapeDefaults.cardShape,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .then(
                                        if (isHighlighted) Modifier.border(
                                            2.dp,
                                            MaterialTheme.colorScheme.primary,
                                            ShapeDefaults.cardShape
                                        )
                                        else Modifier
                                    )
                                    .combinedClickable(
                                        onClick = {
                                            onProductClick(product)
                                            if (latestSearchQuery.value.isNotEmpty()) {
                                                latestSearchQuery.value = ""
                                                onSearchQueryChange("")
                                            }
                                            selectedCatalogIndex = -1
                                            resetScrollPosition()
                                        },
                                        onLongClick = { showContextMenu = true }
                                    )
                                    .pointerInput(product) {
                                        awaitPointerEventScope {
                                            while (true) {
                                                val event = awaitPointerEvent()
                                                if (event.type == PointerEventType.Press) {
                                                    val isRightClick =
                                                        event.buttons.isSecondaryPressed
                                                    if (isRightClick) {
                                                        event.changes.forEach { it.consume() }
                                                        showContextMenu = true
                                                    }
                                                }
                                            }
                                        }
                                    }
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    Column(
                                        modifier = Modifier.padding(12.dp).fillMaxSize(),
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(
                                                text = product.categoria
                                                    ?: stringResource(Res.string.no_category),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.primary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = product.nombre,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                style = MaterialTheme.typography.titleMedium,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        val displayPrice = if (prioritizeDeliveryPrice && product.precio_delivery > 0.0) {
                                            product.precio_delivery
                                        } else {
                                            product.precio
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.Bottom
                                        ) {
                                            if (product.por_peso == 1L) {
                                                Text(
                                                    text = "$${
                                                        displayPrice.toString().formatPrice()
                                                    } / Kg",
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            } else {
                                                Text(
                                                    text = "$${
                                                        displayPrice.toString().formatPrice()
                                                    }",
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }

                                    if (product.es_favorito == 1L) {
                                        Badge(
                                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(8.dp)
                                                .clip(MaterialShapes.Cookie12Sided.toShape())
                                                .size(24.dp)
                                        ) {
                                            Icon(
                                                painter = painterResource(Res.drawable.star_filled),
                                                contentDescription = stringResource(Res.string.favorite_desc),
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                    }

                                    ProductContextMenu(
                                        expanded = showContextMenu,
                                        onDismissRequest = { showContextMenu = false },
                                        product = product,
                                        onAddQuantity = handleAddQuantity,
                                        onToggleFavorite = onToggleFavorite,
                                        onModifyProduct = onModifyProduct
                                    )
                                }
                            }
                        }
                    }
                }

                // FABs overlay (Scanner FAB + Cart FAB)
                val openScanner = onOpenScanner
                val showScannerFab = openScanner != null && isCameraScannerAvailable()
                val showCartFab = isCompact && onViewCartClick != null

                if (showScannerFab || showCartFab) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (openScanner != null && isCameraScannerAvailable()) {
                            FloatingActionButton(
                                onClick = openScanner,
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = MaterialTheme.colorScheme.onSecondary
                            ) {
                                Icon(
                                    painter = painterResource(Res.drawable.barcode_scanner),
                                    contentDescription = stringResource(Res.string.close_scanner_desc)
                                )
                            }
                        }

                        if (isCompact && onViewCartClick != null) {
                            ExtendedFloatingActionButton(
                                onClick = onViewCartClick,
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ) {
                                Icon(
                                    painter = painterResource(Res.drawable.shopping_cart),
                                    contentDescription = stringResource(Res.string.tab_ticket)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    if (cartCount > 0) stringResource(
                                        Res.string.view_ticket_fab,
                                        cartCount,
                                        cartTotal.toString().formatPrice()
                                    )
                                    else stringResource(Res.string.tab_ticket)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

}

@Composable
private fun RepeatingIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable () -> Unit
) {
    val currentOnClick by rememberUpdatedState(onClick)
    val coroutineScope = rememberCoroutineScope()
    val isPressed by interactionSource.collectIsPressedAsState()

    Box(
        modifier = modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(
                if (isPressed && enabled) MaterialTheme.colorScheme.surfaceContainerHighest
                else androidx.compose.ui.graphics.Color.Transparent
            )
            .indication(interactionSource, LocalIndication.current)
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures(
                    onPress = { offset ->
                        val press = PressInteraction.Press(offset)
                        interactionSource.emit(press)
                        currentOnClick()
                        val job = coroutineScope.launch {
                            delay(400.milliseconds)
                            var interval = 160L
                            while (isActive) {
                                currentOnClick()
                                delay(interval.milliseconds)
                                interval = (interval - 15).coerceAtLeast(35L)
                            }
                        }
                        val released = tryAwaitRelease()
                        job.cancel()
                        if (released) {
                            interactionSource.emit(PressInteraction.Release(press))
                        } else {
                            interactionSource.emit(PressInteraction.Cancel(press))
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun ProductContextMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    product: Products,
    onAddQuantity: (Products, Double) -> Unit,
    onToggleFavorite: (Products) -> Unit,
    onModifyProduct: (Products) -> Unit,
    modifier: Modifier = Modifier
) {
    var quantityText by remember(expanded) {
        mutableStateOf(
            TextFieldValue(
                text = "2",
                selection = TextRange(0, 1)
            )
        )
    }
    var isInputFocused by remember(expanded) { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    val currentQty = quantityText.text.toDoubleOrNull() ?: 0.0
    val step = if (product.por_peso == 1L) 0.5 else 1.0
    val minLimit = if (product.por_peso == 1L) 0.1 else 1.0
    val canDecrease = currentQty > minLimit

    val decreaseStep = {
        val current = quantityText.text.toDoubleOrNull() ?: 1.0
        if (current > minLimit) {
            val next = (current - step).coerceAtLeast(minLimit)
            val formatted = if (product.por_peso == 1L) {
                val rounded = (next * 1000.0).roundToInt() / 1000.0
                if (rounded % 1.0 == 0.0) rounded.toInt().toString() else "$rounded"
            } else {
                next.toInt().toString()
            }
            quantityText = TextFieldValue(formatted, selection = TextRange(formatted.length))
        }
    }

    val increaseStep = {
        val current = quantityText.text.toDoubleOrNull() ?: 0.0
        val next = current + step
        val formatted = if (product.por_peso == 1L) {
            val rounded = (next * 1000.0).roundToInt() / 1000.0
            if (rounded % 1.0 == 0.0) rounded.toInt().toString() else "$rounded"
        } else {
            next.toInt().toString()
        }
        quantityText = TextFieldValue(formatted, selection = TextRange(formatted.length))
    }

    val submit = {
        val qty = quantityText.text.toDoubleOrNull()
        if (qty != null && qty > 0.0) {
            onDismissRequest()
            onAddQuantity(product, qty)
        }
    }

    DropdownMenu(
        expanded = expanded,
        shape = MaterialTheme.shapes.medium,
        onDismissRequest = onDismissRequest,
        modifier = modifier.widthIn(min = 230.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = stringResource(Res.string.add_to_ticket),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            shape = MaterialTheme.shapes.small
                        )
                        .padding(horizontal = 2.dp, vertical = 2.dp)
                ) {
                    RepeatingIconButton(
                        onClick = decreaseStep,
                        enabled = canDecrease
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.remove),
                            contentDescription = stringResource(Res.string.decrease_desc),
                            modifier = Modifier.size(16.dp),
                            tint = if (canDecrease) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .padding(horizontal = 2.dp)
                            .clip(MaterialTheme.shapes.extraSmall)
                            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                            .border(
                                width = if (isInputFocused) 1.5.dp else 1.dp,
                                color = if (isInputFocused) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                shape = MaterialTheme.shapes.extraSmall
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                try {
                                    focusRequester.requestFocus()
                                } catch (_: Exception) {}
                            }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            BasicTextField(
                                value = quantityText,
                                onValueChange = { newValue ->
                                    val text = newValue.text
                                    val isValid = if (product.por_peso == 1L) {
                                        text.isEmpty() || text.matches(Regex("^\\d*\\.?\\d{0,3}$"))
                                    } else {
                                        text.isEmpty() || text.matches(Regex("^\\d+$"))
                                    }
                                    if (isValid) {
                                        quantityText = newValue
                                    }
                                },
                                modifier = Modifier
                                    .widthIn(min = 28.dp, max = 56.dp)
                                    .focusRequester(focusRequester)
                                    .onFocusChanged { focusState ->
                                        isInputFocused = focusState.isFocused
                                        if (focusState.isFocused) {
                                            quantityText = quantityText.copy(
                                                selection = TextRange(0, quantityText.text.length)
                                            )
                                        }
                                    }
                                    .onPreviewKeyEvent { keyEvent ->
                                        if (keyEvent.type == KeyEventType.KeyDown &&
                                            (keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter)
                                        ) {
                                            submit()
                                            true
                                        } else false
                                    },
                                textStyle = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                singleLine = true,
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = if (product.por_peso == 1L) KeyboardType.Decimal else KeyboardType.Number,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = { submit() }
                                ),
                                decorationBox = { innerTextField ->
                                    Box(contentAlignment = Alignment.Center) {
                                        if (quantityText.text.isEmpty()) {
                                            Text(
                                                text = "0",
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    textAlign = TextAlign.Center,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                                )
                                            )
                                        }
                                        innerTextField()
                                    }
                                }
                            )

                            if (product.por_peso == 1L) {
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "Kg",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    RepeatingIconButton(
                        onClick = increaseStep,
                        enabled = true
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.add),
                            contentDescription = stringResource(Res.string.increase_desc),
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                FilledTonalButton(
                    onClick = { submit() },
                    enabled = currentQty > 0.0,
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.check),
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        DropdownMenuItem(
            text = {
                Text(
                    if (product.es_favorito == 1L) stringResource(
                        Res.string.remove_from_favorites
                    ) else stringResource(Res.string.mark_as_favorite)
                )
            },
            onClick = {
                onDismissRequest()
                onToggleFavorite(product)
            },
            leadingIcon = {
                Icon(
                    painter = if (product.es_favorito == 1L) painterResource(
                        Res.drawable.star_filled
                    ) else painterResource(Res.drawable.star),
                    contentDescription = stringResource(Res.string.favorite_desc),
                    tint = if (product.es_favorito == 1L) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        )

        DropdownMenuItem(
            text = { Text(stringResource(Res.string.modify)) },
            onClick = {
                onDismissRequest()
                onModifyProduct(product)
            },
            leadingIcon = {
                Icon(
                    painter = painterResource(Res.drawable.edit),
                    contentDescription = stringResource(Res.string.modify),
                    modifier = Modifier.size(20.dp)
                )
            }
        )
    }
}

private suspend fun LazyListState.scrollItemIntoView(targetIndex: Int) {
    val items = layoutInfo.visibleItemsInfo
    if (items.isEmpty()) {
        animateScrollToItem(targetIndex)
        return
    }
    val firstVisible = items.first()
    val lastVisible = items.last()

    if (targetIndex < firstVisible.index) {
        animateScrollToItem(targetIndex)
        return
    }

    val visibleBottom = layoutInfo.viewportEndOffset - layoutInfo.afterContentPadding
    val existingItem = items.firstOrNull { it.index == targetIndex }

    if (existingItem != null) {
        if (existingItem.offset < layoutInfo.viewportStartOffset) {
            animateScrollToItem(targetIndex)
        } else if (existingItem.offset + existingItem.size > visibleBottom) {
            val delta = (existingItem.offset + existingItem.size) - visibleBottom
            animateScrollBy(delta.toFloat() + 4f)
        }
        return
    }

    if (targetIndex > lastVisible.index) {
        if (targetIndex - lastVisible.index >= items.size) {
            val targetFirstVisibleIndex = (targetIndex - items.size + 2).coerceAtLeast(0)
            animateScrollToItem(targetFirstVisibleIndex)
        } else {
            val itemHeight = lastVisible.size.takeIf { it > 0 } ?: 48
            val delta = (targetIndex - lastVisible.index) * itemHeight
            animateScrollBy(delta.toFloat() + 4f)
        }
    }
}

private suspend fun LazyGridState.scrollItemIntoView(targetIndex: Int) {
    val items = layoutInfo.visibleItemsInfo
    if (items.isEmpty()) {
        animateScrollToItem(targetIndex)
        return
    }
    val firstVisible = items.first()
    val lastVisible = items.last()

    if (targetIndex < firstVisible.index) {
        animateScrollToItem(targetIndex)
        return
    }

    val visibleBottom = layoutInfo.viewportEndOffset - layoutInfo.afterContentPadding
    val existingItem = items.firstOrNull { it.index == targetIndex }

    if (existingItem != null) {
        if (existingItem.offset.y < layoutInfo.viewportStartOffset) {
            animateScrollToItem(targetIndex)
        } else if (existingItem.offset.y + existingItem.size.height > visibleBottom) {
            val delta = (existingItem.offset.y + existingItem.size.height) - visibleBottom
            animateScrollBy(delta.toFloat() + 4f)
        }
        return
    }

    if (targetIndex > lastVisible.index) {
        if (targetIndex - lastVisible.index >= items.size) {
            val targetFirstVisibleIndex = (targetIndex - items.size + 2).coerceAtLeast(0)
            animateScrollToItem(targetFirstVisibleIndex)
        } else {
            val itemHeight = lastVisible.size.height.takeIf { it > 0 } ?: 140
            val delta = (targetIndex - lastVisible.index) * itemHeight
            animateScrollBy(delta.toFloat() + 4f)
        }
    }
}
