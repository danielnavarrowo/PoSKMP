package com.dnavarro.poskmp.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.toShape
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dnavarro.poskmp.db.Products
import com.dnavarro.poskmp.theme.ShapeDefaults
import com.dnavarro.poskmp.ui.productos.ProductosViewModel
import com.dnavarro.poskmp.util.PlatformBackHandler
import com.dnavarro.poskmp.util.formatPrice
import com.dnavarro.poskmp.util.isAndroid
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import poskmp.shared.generated.resources.Res
import poskmp.shared.generated.resources.add
import poskmp.shared.generated.resources.barcode_scanner
import poskmp.shared.generated.resources.bulk_op_change_category_title
import poskmp.shared.generated.resources.bulk_op_change_prices_title
import poskmp.shared.generated.resources.bulk_op_deactivate_title
import poskmp.shared.generated.resources.bulk_op_delete_title
import poskmp.shared.generated.resources.bulk_op_set_profit_title
import poskmp.shared.generated.resources.category_label_format
import poskmp.shared.generated.resources.clear_desc
import poskmp.shared.generated.resources.close
import poskmp.shared.generated.resources.codes_display_label
import poskmp.shared.generated.resources.cost_display_label
import poskmp.shared.generated.resources.delete
import poskmp.shared.generated.resources.delete_desc
import poskmp.shared.generated.resources.disabled
import poskmp.shared.generated.resources.edit
import poskmp.shared.generated.resources.favorite_desc
import poskmp.shared.generated.resources.header_category
import poskmp.shared.generated.resources.header_codes
import poskmp.shared.generated.resources.header_cost
import poskmp.shared.generated.resources.header_product_name
import poskmp.shared.generated.resources.header_retail_price
import poskmp.shared.generated.resources.money
import poskmp.shared.generated.resources.new_product_button
import poskmp.shared.generated.resources.no_category
import poskmp.shared.generated.resources.no_products_registered
import poskmp.shared.generated.resources.price_display_label
import poskmp.shared.generated.resources.product_admin_title
import poskmp.shared.generated.resources.products
import poskmp.shared.generated.resources.remove
import poskmp.shared.generated.resources.sad_face
import poskmp.shared.generated.resources.search
import poskmp.shared.generated.resources.search_desc
import poskmp.shared.generated.resources.search_placeholder
import poskmp.shared.generated.resources.star_filled
import poskmp.shared.generated.resources.status_inactive
import poskmp.shared.generated.resources.wholesale

enum class ProductSortField {
    CODIGO, NOMBRE, CATEGORIA, PRECIO, COSTO, MAYOREO
}

enum class ProductSortOrder {
    ASC, DESC
}

@Composable
fun RowScope.TableHeader(
    text: String,
    weight: Float,
    field: ProductSortField,
    currentField: ProductSortField,
    currentOrder: ProductSortOrder,
    onHeaderClick: (ProductSortField) -> Unit
) {
    Row(
        modifier = Modifier
            .weight(weight)
            .clickable { onHeaderClick(field) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.Bold,
            color = if (field == currentField) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp
        )
        if (field == currentField) {
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (currentOrder == ProductSortOrder.ASC) "▲" else "▼",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 10.sp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ProductosScreen(
    viewModel: ProductosViewModel,
    modifier: Modifier = Modifier
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
    var pendingScanCode by remember { mutableStateOf<String?>(null) }
    val fabContainerColor = MaterialTheme.colorScheme.secondary

    LaunchedEffect(pendingScanCode, sortedProducts) {
        val code = pendingScanCode
        if (!code.isNullOrBlank()) {
            val matched = sortedProducts.find { p ->
                val cleanCodes = p.codigos
                    .replace("[", "")
                    .replace("]", "")
                    .replace("\"", "")
                    .split(",")
                    .map { it.trim() }
                cleanCodes.contains(code) || p.codigos.contains(code) || p.id == code
            }
            if (matched != null) {
                viewModel.onShowProductDialog(matched)
                pendingScanCode = null
            }
        }
    }

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
                                    tint = MaterialTheme.colorScheme.onSecondary,
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

                // New Product FAB (Stays visible ALL THE TIME at the bottom)
                ExtendedFloatingActionButton(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    onClick = {
                        viewModel.onShowProductDialog(
                            Products("", "[]", "", 0.0, 0.0, "", 1L, 0L, 0.0, 0L, 0L, "")
                        )
                    },
                    icon = {
                        Icon(painter = painterResource(Res.drawable.add), contentDescription = null)
                    },
                    text = {
                        Text(stringResource(Res.string.new_product_button))
                    }
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize().padding(16.dp)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
                .background(MaterialTheme.colorScheme.background)
        ) {
            // SEARCH BAR
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                modifier = Modifier.fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.small),
                placeholder = { Text(stringResource(Res.string.search_placeholder)) },
                leadingIcon = {
                    Icon(
                        painter = painterResource(Res.drawable.search),
                        contentDescription = stringResource(Res.string.search_desc)
                    )
                },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = {
                                viewModel.onSearchQueryChanged("")
                                pendingScanCode = null
                            }) {
                                Icon(
                                    painter = painterResource(Res.drawable.close),
                                    contentDescription = stringResource(Res.string.clear_desc)
                                )
                            }
                        }
                        if (isAndroid()) {
                            IconButton(onClick = { showCameraScanner = true }) {
                                Icon(
                                    painter = painterResource(Res.drawable.barcode_scanner),
                                    contentDescription = null
                                )
                            }
                        }
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.small,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // PRODUCTS TABLE
            BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
                val isCompact = maxWidth < 720.dp

                Card(
                    modifier = Modifier.fillMaxSize(),
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
                                verticalArrangement = Arrangement.spacedBy(2.dp)
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
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { viewModel.onShowProductDialog(product) },
                                        shape = shape,
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Checkbox(
                                                    checked = product.id in selectedProductIds,
                                                    onCheckedChange = {
                                                        viewModel.onToggleSelectProduct(
                                                            product.id
                                                        )
                                                    }
                                                )
                                                Text(
                                                    text = product.nombre,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    modifier = Modifier.weight(1f),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )

                                                IconButton(
                                                    onClick = {
                                                        viewModel.deleteProductSoft(product.id)
                                                    },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(
                                                        painter = painterResource(Res.drawable.delete),
                                                        contentDescription = stringResource(Res.string.delete_desc)
                                                    )
                                                }

                                            }

                                            Spacer(modifier = Modifier.height(4.dp))

                                            // Barcodes
                                            val codesDisplay = try {
                                                product.codigos
                                                    .replace("[", "")
                                                    .replace("]", "")
                                                    .replace("\"", "")
                                                    .split(",")
                                                    .filter { it.isNotEmpty() }
                                                    .joinToString(", ")
                                                    .ifEmpty { "N/A" }
                                            } catch (_: Exception) {
                                                "N/A"
                                            }
                                            Text(
                                                stringResource(
                                                    Res.string.codes_display_label,
                                                    codesDisplay
                                                ),
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )

                                            Spacer(modifier = Modifier.height(4.dp))

                                            // Category
                                            Text(
                                                stringResource(
                                                    Res.string.category_label_format,
                                                    product.categoria
                                                        ?: stringResource(Res.string.no_category)
                                                ),
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )

                                            Spacer(modifier = Modifier.height(6.dp))

                                            // Prices & Cost
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text(
                                                        text = stringResource(
                                                            Res.string.price_display_label,
                                                            product.precio.toString().formatPrice()
                                                        ),
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                    Text(
                                                        stringResource(
                                                            Res.string.cost_display_label,
                                                            product.costo.toString().formatPrice()
                                                        ),
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    if (product.es_favorito == 1L) {
                                                        Badge(
                                                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                                            modifier = Modifier.clip(MaterialShapes.Cookie12Sided.toShape()).size(28.dp)
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
                                                            modifier = Modifier.clip(MaterialShapes.Sunny.toShape()).size(28.dp)
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
                                        }
                                    }
                                }
                            }
                        } else {
                            // Desktop Table Layout
                            Column(modifier = Modifier.fillMaxSize()) {
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

                                val isAllFilteredSelected =
                                    sortedProducts.isNotEmpty() && sortedProducts.all { it.id in selectedProductIds }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surfaceContainer)
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isAllFilteredSelected,
                                        onCheckedChange = {
                                            viewModel.onSelectAllProducts(sortedProducts.map { it.id })
                                        },
                                        modifier = Modifier.weight(0.05f)
                                    )
                                    TableHeader(
                                        stringResource(Res.string.header_codes),
                                        0.18f,
                                        ProductSortField.CODIGO,
                                        sortField,
                                        sortOrder,
                                        onHeaderClick
                                    )
                                    TableHeader(
                                        stringResource(Res.string.header_product_name),
                                        0.28f,
                                        ProductSortField.NOMBRE,
                                        sortField,
                                        sortOrder,
                                        onHeaderClick
                                    )
                                    TableHeader(
                                        stringResource(Res.string.header_category),
                                        0.14f,
                                        ProductSortField.CATEGORIA,
                                        sortField,
                                        sortOrder,
                                        onHeaderClick
                                    )
                                    TableHeader(
                                        stringResource(Res.string.header_retail_price),
                                        0.10f,
                                        ProductSortField.PRECIO,
                                        sortField,
                                        sortOrder,
                                        onHeaderClick
                                    )
                                    TableHeader(
                                        stringResource(Res.string.header_cost),
                                        0.10f,
                                        ProductSortField.COSTO,
                                        sortField,
                                        sortOrder,
                                        onHeaderClick
                                    )
                                    TableHeader(
                                        stringResource(Res.string.wholesale),
                                        0.10f,
                                        ProductSortField.MAYOREO,
                                        sortField,
                                        sortOrder,
                                        onHeaderClick
                                    )
                                }

                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(2.dp),
                                    contentPadding = PaddingValues(bottom = 12.dp)
                                ) {
                                    itemsIndexed(sortedProducts) { index, product ->
                                        val shape =
                                            if (sortedProducts.size == 1 || index == sortedProducts.lastIndex) ShapeDefaults.bottomListItemShape
                                            else ShapeDefaults.middleListItemShape

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(shape)
                                                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                                                .clickable { viewModel.onShowProductDialog(product) }
                                                .padding(horizontal = 16.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(
                                                checked = product.id in selectedProductIds,
                                                onCheckedChange = {
                                                    viewModel.onToggleSelectProduct(
                                                        product.id
                                                    )
                                                },
                                                modifier = Modifier.weight(0.05f)
                                            )
                                            val codesDisplay = try {
                                                product.codigos
                                                    .replace("[", "")
                                                    .replace("]", "")
                                                    .replace("\"", "")
                                                    .split(",")
                                                    .filter { it.isNotEmpty() }
                                                    .joinToString(", ")
                                                    .ifEmpty { "N/A" }
                                            } catch (_: Exception) {
                                                "N/A"
                                            }
                                            Text(
                                                text = codesDisplay,
                                                modifier = Modifier.weight(0.18f),
                                                style = MaterialTheme.typography.bodyMedium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )

                                            Row(
                                                modifier = Modifier.weight(0.28f),
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
                                                        modifier = Modifier.clip(MaterialShapes.Cookie12Sided.toShape()).size(28.dp)
                                                    ) {
                                                        Icon(
                                                            painter = painterResource(Res.drawable.star_filled),
                                                            contentDescription = stringResource(Res.string.favorite_desc),
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                }
                                                if (
                                                    product.activo == 0L
                                                    ) {
                                                    Spacer(modifier = Modifier.width(4.dp))

                                                    Badge(
                                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                                                        modifier = Modifier.clip(MaterialShapes.Sunny.toShape()).size(28.dp)
                                                    ) {
                                                        Icon(
                                                            painter = painterResource(Res.drawable.disabled),
                                                            contentDescription = stringResource(Res.string.status_inactive),
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            Text(
                                                text = product.categoria
                                                    ?: stringResource(Res.string.no_category),
                                                modifier = Modifier.weight(0.14f),
                                                style = MaterialTheme.typography.bodyMedium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )

                                            Text(
                                                text = "$${
                                                    product.precio.toString().formatPrice()
                                                }",
                                                modifier = Modifier.weight(0.10f),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold
                                            )

                                            Text(
                                                text = "$${product.costo.toString().formatPrice()}",
                                                modifier = Modifier.weight(0.10f),
                                                style = MaterialTheme.typography.bodyMedium,
                                            )

                                            Text(
                                                text = "$${
                                                    product.precio_mayoreo.toString().formatPrice()
                                                }",
                                                modifier = Modifier.weight(0.10f),
                                                style = MaterialTheme.typography.bodyMedium,
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
                }
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
                        pendingScanCode = code
                    }
                },
                onClose = { showCameraScanner = false }
            )
        }
    }
}




