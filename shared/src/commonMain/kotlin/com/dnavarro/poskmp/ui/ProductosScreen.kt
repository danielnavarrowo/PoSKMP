package com.dnavarro.poskmp.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dnavarro.poskmp.data.ProductRepository
import com.dnavarro.poskmp.db.Products
import com.dnavarro.poskmp.ui.productos.ProductosViewModel
import com.dnavarro.poskmp.util.PlatformBackHandler
import com.dnavarro.poskmp.util.currentTimeMillis
import com.dnavarro.poskmp.util.formatPrice
import com.dnavarro.poskmp.util.isAndroid
import com.dnavarro.poskmp.util.parseImportFile
import com.dnavarro.poskmp.util.pickFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import poskmp.shared.generated.resources.Res
import poskmp.shared.generated.resources.accept_button
import poskmp.shared.generated.resources.add
import poskmp.shared.generated.resources.back_button
import poskmp.shared.generated.resources.barcode_scanner
import poskmp.shared.generated.resources.bulk_op_change_category_title
import poskmp.shared.generated.resources.bulk_op_change_prices_title
import poskmp.shared.generated.resources.bulk_op_deactivate_title
import poskmp.shared.generated.resources.bulk_op_delete_title
import poskmp.shared.generated.resources.bulk_op_set_profit_title
import poskmp.shared.generated.resources.cancel
import poskmp.shared.generated.resources.category_label_format
import poskmp.shared.generated.resources.check
import poskmp.shared.generated.resources.clear_desc
import poskmp.shared.generated.resources.close
import poskmp.shared.generated.resources.close_button
import poskmp.shared.generated.resources.codes_display_label
import poskmp.shared.generated.resources.confirm_and_import_button
import poskmp.shared.generated.resources.cost_display_label
import poskmp.shared.generated.resources.delete
import poskmp.shared.generated.resources.delete_and_replace_button
import poskmp.shared.generated.resources.delete_desc
import poskmp.shared.generated.resources.edit
import poskmp.shared.generated.resources.edit_desc
import poskmp.shared.generated.resources.export_error_title
import poskmp.shared.generated.resources.export_success_title
import poskmp.shared.generated.resources.favorite_desc
import poskmp.shared.generated.resources.header_actions
import poskmp.shared.generated.resources.header_category
import poskmp.shared.generated.resources.header_codes
import poskmp.shared.generated.resources.header_cost
import poskmp.shared.generated.resources.header_name
import poskmp.shared.generated.resources.header_price
import poskmp.shared.generated.resources.header_product_name
import poskmp.shared.generated.resources.header_retail_price
import poskmp.shared.generated.resources.header_status
import poskmp.shared.generated.resources.import_choose_action
import poskmp.shared.generated.resources.import_col_name_req
import poskmp.shared.generated.resources.import_col_price_req
import poskmp.shared.generated.resources.import_db_save_error
import poskmp.shared.generated.resources.import_detected_products
import poskmp.shared.generated.resources.import_file_label
import poskmp.shared.generated.resources.import_no_valid_products_error
import poskmp.shared.generated.resources.import_opt_replace_desc
import poskmp.shared.generated.resources.import_opt_replace_title
import poskmp.shared.generated.resources.import_opt_update_desc
import poskmp.shared.generated.resources.import_opt_update_title
import poskmp.shared.generated.resources.import_optional_columns_hint
import poskmp.shared.generated.resources.import_parse_error
import poskmp.shared.generated.resources.import_products_title
import poskmp.shared.generated.resources.import_progress_inserting
import poskmp.shared.generated.resources.import_progress_saving
import poskmp.shared.generated.resources.import_progress_starting
import poskmp.shared.generated.resources.import_required_columns_title
import poskmp.shared.generated.resources.import_select_file_hint
import poskmp.shared.generated.resources.import_select_file_title
import poskmp.shared.generated.resources.import_step_format
import poskmp.shared.generated.resources.import_success_replace_message
import poskmp.shared.generated.resources.import_success_title
import poskmp.shared.generated.resources.import_success_update_message
import poskmp.shared.generated.resources.import_warning_replace_all
import poskmp.shared.generated.resources.money
import poskmp.shared.generated.resources.new_product_button
import poskmp.shared.generated.resources.next_button
import poskmp.shared.generated.resources.no_catalog_products
import poskmp.shared.generated.resources.no_category
import poskmp.shared.generated.resources.no_products_registered
import poskmp.shared.generated.resources.price_display_label
import poskmp.shared.generated.resources.product_admin_title
import poskmp.shared.generated.resources.products
import poskmp.shared.generated.resources.remove
import poskmp.shared.generated.resources.remove_desc
import poskmp.shared.generated.resources.search
import poskmp.shared.generated.resources.search_desc
import poskmp.shared.generated.resources.search_placeholder
import poskmp.shared.generated.resources.star_filled
import poskmp.shared.generated.resources.status_active
import poskmp.shared.generated.resources.status_inactive
import poskmp.shared.generated.resources.upload
import poskmp.shared.generated.resources.warning
import kotlin.time.Duration.Companion.milliseconds

enum class ProductSortField {
    CODIGO, NOMBRE, CATEGORIA, PRECIO, COSTO, ESTADO
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
    modifier: Modifier = Modifier,
    repository: ProductRepository = koinInject()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery = uiState.searchQuery
    val sortedProducts = uiState.sortedProducts
    val sortField = uiState.sortField
    val sortOrder = uiState.sortOrder
    val showProductDialogFor = uiState.showProductDialogFor
    val showImportDialog = uiState.showImportDialog
    val showBulkModificationFor = uiState.showBulkModificationFor
    val selectedProductIds = uiState.selectedProductIds

    var exportSuccessMessage by remember { mutableStateOf<String?>(null) }
    var exportErrorMessage by remember { mutableStateOf<String?>(null) }
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
                            scaleIn(initialScale = 0.8f, animationSpec = spring(stiffness = Spring.StiffnessMediumLow)),
                    exit = fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) +
                           scaleOut(targetScale = 0.8f, animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
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
                                val iconRes = if (checkedProgress > 0.5f) Res.drawable.close else Res.drawable.edit
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
                            icon = { Icon(painter = painterResource(Res.drawable.money), contentDescription = null) },
                            text = { Text(stringResource(Res.string.bulk_op_change_prices_title)) },
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        FloatingActionButtonMenuItem(
                            onClick = {
                                isFabMenuExpanded = false
                                viewModel.onShowBulkModificationDialog(BulkProductOperation.SET_PROFIT)
                            },
                            icon = { Icon(painter = painterResource(Res.drawable.edit), contentDescription = null) },
                            text = { Text(stringResource(Res.string.bulk_op_set_profit_title)) },
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        FloatingActionButtonMenuItem(
                            onClick = {
                                isFabMenuExpanded = false
                                viewModel.onShowBulkModificationDialog(BulkProductOperation.CHANGE_CATEGORY)
                            },
                            icon = { Icon(painter = painterResource(Res.drawable.products), contentDescription = null) },
                            text = { Text(stringResource(Res.string.bulk_op_change_category_title)) },
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        FloatingActionButtonMenuItem(
                            onClick = {
                                isFabMenuExpanded = false
                                viewModel.onShowBulkModificationDialog(BulkProductOperation.DEACTIVATE)
                            },
                            icon = { Icon(painter = painterResource(Res.drawable.remove), contentDescription = null) },
                            text = { Text(stringResource(Res.string.bulk_op_deactivate_title)) },
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        FloatingActionButtonMenuItem(
                            onClick = {
                                isFabMenuExpanded = false
                                viewModel.onShowBulkModificationDialog(BulkProductOperation.DELETE)
                            },
                            icon = { Icon(painter = painterResource(Res.drawable.delete), contentDescription = null) },
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
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp)
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
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    if (isCompact) {
                        // Mobile Compact List
                        if (sortedProducts.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    stringResource(Res.string.no_products_registered),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize().padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                                contentPadding = PaddingValues(bottom = 12.dp)
                            ) {
                                itemsIndexed(sortedProducts) { index, product ->
                                    val shape = if (sortedProducts.size == 1) {
                                        MaterialTheme.shapes.medium
                                    } else if (index == 0) {
                                        RoundedCornerShape(
                                            topStart = MaterialTheme.shapes.medium.topStart,
                                            topEnd = MaterialTheme.shapes.medium.topEnd,
                                            bottomStart = MaterialTheme.shapes.extraSmall.bottomStart,
                                            bottomEnd = MaterialTheme.shapes.extraSmall.bottomEnd
                                        )
                                    } else if (index == sortedProducts.lastIndex) {
                                        RoundedCornerShape(
                                            topStart = MaterialTheme.shapes.extraSmall.topStart,
                                            topEnd = MaterialTheme.shapes.extraSmall.topEnd,
                                            bottomStart = MaterialTheme.shapes.medium.bottomStart,
                                            bottomEnd = MaterialTheme.shapes.medium.bottomEnd
                                        )
                                    } else {
                                        RoundedCornerShape(MaterialTheme.shapes.extraSmall.topStart)
                                    }

                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { viewModel.onShowProductDialog(product) },
                                        shape = shape,
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                                        border = BorderStroke(
                                            1.dp,
                                            MaterialTheme.colorScheme.outlineVariant
                                        )
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Checkbox(
                                                    checked = product.id in selectedProductIds,
                                                    onCheckedChange = { viewModel.onToggleSelectProduct(product.id) }
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
                                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    IconButton(
                                                        onClick = {
                                                            viewModel.onShowProductDialog(product)
                                                        },
                                                        modifier = Modifier.size(28.dp)
                                                    ) {
                                                        Icon(
                                                            painter = painterResource(Res.drawable.edit),
                                                            contentDescription = stringResource(Res.string.edit_desc)
                                                        )
                                                    }
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
                                                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                                                        ) {
                                                            Text(
                                                                "★",
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                modifier = Modifier.padding(
                                                                    horizontal = 4.dp
                                                                )
                                                            )
                                                        }
                                                    }
                                                    if (product.activo == 1L) {
                                                        Badge(
                                                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                                                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                                        ) {
                                                            Text(
                                                                stringResource(Res.string.status_active),
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                modifier = Modifier.padding(
                                                                    horizontal = 4.dp
                                                                )
                                                            )
                                                        }
                                                    } else {
                                                        Badge(
                                                            containerColor = MaterialTheme.colorScheme.errorContainer,
                                                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                                                        ) {
                                                            Text(
                                                                stringResource(Res.string.status_inactive),
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                modifier = Modifier.padding(
                                                                    horizontal = 4.dp
                                                                )
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

                            val isAllFilteredSelected = sortedProducts.isNotEmpty() && sortedProducts.all { it.id in selectedProductIds }

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
                                    stringResource(Res.string.header_status),
                                    0.10f,
                                    ProductSortField.ESTADO,
                                    sortField,
                                    sortOrder,
                                    onHeaderClick
                                )
                                Text(
                                    stringResource(Res.string.header_actions),
                                    modifier = Modifier.weight(0.10f),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 13.sp
                                )
                            }

                            if (sortedProducts.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        stringResource(Res.string.no_catalog_products),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(2.dp),
                                    contentPadding = PaddingValues(bottom = 12.dp)
                                ) {
                                    itemsIndexed(sortedProducts) { index, product ->
                                        val shape = if (sortedProducts.size == 1) {
                                            MaterialTheme.shapes.medium
                                        } else if (index == 0) {
                                            RoundedCornerShape(
                                                topStart = MaterialTheme.shapes.medium.topStart,
                                                topEnd = MaterialTheme.shapes.medium.topEnd,
                                                bottomStart = MaterialTheme.shapes.extraSmall.bottomStart,
                                                bottomEnd = MaterialTheme.shapes.extraSmall.bottomEnd
                                            )
                                        } else if (index == sortedProducts.lastIndex) {
                                            RoundedCornerShape(
                                                topStart = MaterialTheme.shapes.extraSmall.topStart,
                                                topEnd = MaterialTheme.shapes.extraSmall.topEnd,
                                                bottomStart = MaterialTheme.shapes.medium.bottomStart,
                                                bottomEnd = MaterialTheme.shapes.medium.bottomEnd
                                            )
                                        } else {
                                            RoundedCornerShape(MaterialTheme.shapes.extraSmall.topStart)
                                        }

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(shape)
                                                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                                                .clickable { viewModel.onShowProductDialog(product) }
                                                .border(
                                                    BorderStroke(
                                                        0.5.dp,
                                                        MaterialTheme.colorScheme.outlineVariant
                                                    ), shape
                                                )
                                                .padding(horizontal = 16.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(
                                                checked = product.id in selectedProductIds,
                                                onCheckedChange = { viewModel.onToggleSelectProduct(product.id) },
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
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )

                                            Row(
                                                modifier = Modifier.weight(0.28f),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = product.nombre,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                if (product.es_favorito == 1L) {
                                                    Spacer(modifier = Modifier.width(4.dp))

                                                    Icon(
                                                        painter = painterResource(Res.drawable.star_filled),
                                                        contentDescription = stringResource(Res.string.favorite_desc),
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }

                                            Text(
                                                text = product.categoria
                                                    ?: stringResource(Res.string.no_category),
                                                modifier = Modifier.weight(0.14f),
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )

                                            Text(
                                                text = "$${
                                                    product.precio.toString().formatPrice()
                                                }",
                                                modifier = Modifier.weight(0.10f),
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold
                                            )

                                            Text(
                                                text = "$${product.costo.toString().formatPrice()}",
                                                modifier = Modifier.weight(0.10f),
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )

                                            Box(modifier = Modifier.weight(0.10f)) {
                                                if (product.activo == 1L) {
                                                    Badge(
                                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                                    ) {
                                                        Text(
                                                            stringResource(Res.string.status_active),
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.padding(horizontal = 4.dp)
                                                        )
                                                    }
                                                } else {
                                                    Badge(
                                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                                                    ) {
                                                        Text(
                                                            stringResource(Res.string.status_inactive),
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.padding(horizontal = 4.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            Row(
                                                modifier = Modifier.weight(0.10f),
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                IconButton(
                                                     onClick = { viewModel.onShowProductDialog(product) },
                                                     modifier = Modifier.size(28.dp)
                                                 ) {
                                                     Icon(
                                                       painter = painterResource(Res.drawable.edit),
                                                         contentDescription = stringResource(Res.string.edit_desc),
                                                         tint = MaterialTheme.colorScheme.primary,
                                                         modifier = Modifier.size(16.dp)
                                                     )
                                                 }

                                                 IconButton(
                                                     onClick = {
                                                         viewModel.deleteProductSoft(product.id)
                                                     },
                                                     modifier = Modifier.size(28.dp)
                                                 ) {
                                                    Icon(
                                                      painter = painterResource(Res.drawable.delete),
                                                        contentDescription = stringResource(Res.string.remove_desc),
                                                        tint = MaterialTheme.colorScheme.error,
                                                        modifier = Modifier.size(16.dp)
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

        // NEW IMPORT/EXPORT DIALOGS
        if (showImportDialog) {
            ImportProductsDialog(
                onDismiss = { viewModel.onShowImportDialog(false) },
                repository = repository
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

        if (exportSuccessMessage != null) {
            AlertDialog(
                onDismissRequest = { exportSuccessMessage = null },
                shape = MaterialTheme.shapes.medium,
                title = {
                    Text(
                        stringResource(Res.string.export_success_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                text = { Text(exportSuccessMessage!!) },
                confirmButton = {
                    Button(
                        onClick = { exportSuccessMessage = null },
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(stringResource(Res.string.accept_button))
                    }
                }
            )
        }

        if (exportErrorMessage != null) {
            AlertDialog(
                onDismissRequest = { exportErrorMessage = null },
                shape = MaterialTheme.shapes.medium,
                title = {
                    Text(
                        stringResource(Res.string.export_error_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                text = { Text(exportErrorMessage!!) },
                confirmButton = {
                    Button(
                        onClick = { exportErrorMessage = null },
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(stringResource(Res.string.accept_button))
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportProductsDialog(
    onDismiss: () -> Unit,
    repository: ProductRepository
) {
    var currentStep by remember { mutableIntStateOf(1) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var selectedFileBytes by remember { mutableStateOf<ByteArray?>(null) }
    var parsedProducts by remember { mutableStateOf<List<Products>>(emptyList()) }
    var importError by remember { mutableStateOf<String?>(null) }
    var importSuccessMessage by remember { mutableStateOf<String?>(null) }
    var updateExistingOption by remember { mutableStateOf(true) }
    var isProcessing by remember { mutableStateOf(false) }
    var importProgressFraction by remember { mutableFloatStateOf(0f) }
    var importProgressText by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    val noValidProductsErr = stringResource(Res.string.import_no_valid_products_error)
    val parseErr = stringResource(Res.string.import_parse_error)
    val startingImportText = stringResource(Res.string.import_progress_starting)
    val savingFmt = stringResource(Res.string.import_progress_saving)
    val updateSuccessFmt = stringResource(Res.string.import_success_update_message)
    val insertingFmt = stringResource(Res.string.import_progress_inserting)
    val replaceSuccessFmt = stringResource(Res.string.import_success_replace_message)
    val dbSaveErrFmt = stringResource(Res.string.import_db_save_error)

    Dialog(onDismissRequest = { if (!isProcessing) onDismiss() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth(if (currentStep == 2) 0.95f else 0.85f)
                .wrapContentHeight()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header of Dialog
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(Res.string.import_products_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (currentStep in 1..3) {
                        Text(
                            text = stringResource(Res.string.import_step_format, currentStep),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Content based on step
                when (currentStep) {
                    1 -> {
                        // Instructions
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = stringResource(Res.string.import_required_columns_title),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    stringResource(Res.string.import_col_name_req),
                                    fontSize = 12.sp
                                )
                                Text(
                                    stringResource(Res.string.import_col_price_req),
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = stringResource(Res.string.import_optional_columns_hint),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // File Selection Area Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                                    shape = MaterialTheme.shapes.medium
                                )
                                .border(
                                    width = 2.dp,
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                    shape = MaterialTheme.shapes.medium
                                )
                                .clickable {
                                    pickFile(
                                        allowedExtensions = listOf("csv", "xlsx"),
                                        onFilePicked = { name, bytes ->
                                            try {
                                                val prods = parseImportFile(name, bytes)
                                                if (prods.isEmpty()) {
                                                    importError = noValidProductsErr
                                                } else {
                                                    selectedFileName = name
                                                    selectedFileBytes = bytes
                                                    parsedProducts = prods
                                                    importError = null
                                                    currentStep = 2
                                                }
                                            } catch (e: Exception) {
                                                importError = e.message ?: parseErr
                                            }
                                        },
                                        onError = { importError = it }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Icon(
                                    painter = painterResource(Res.drawable.upload),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(48.dp)
                                )
                                Text(
                                    text = stringResource(Res.string.import_select_file_title),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(Res.string.import_select_file_hint),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (importError != null) {
                            Text(
                                text = importError!!,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Footer Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = onDismiss,
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(stringResource(Res.string.cancel))
                            }
                        }
                    }

                    2 -> {
                        // Preview Step
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = stringResource(
                                    Res.string.import_file_label,
                                    selectedFileName ?: ""
                                ),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = stringResource(
                                    Res.string.import_detected_products,
                                    parsedProducts.size
                                ),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            // Scrollable table preview
                            Card(
                                modifier = Modifier.fillMaxWidth().height(260.dp),
                                border = BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.outlineVariant
                                ),
                                shape = MaterialTheme.shapes.small,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
                            ) {
                                LazyColumn(modifier = Modifier.fillMaxSize()) {
                                    // Table Header
                                    item {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(MaterialTheme.colorScheme.surfaceContainer)
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                stringResource(Res.string.header_name),
                                                modifier = Modifier.weight(0.4f),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            )
                                            Text(
                                                stringResource(Res.string.header_codes),
                                                modifier = Modifier.weight(0.25f),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            )
                                            Text(
                                                stringResource(Res.string.header_price),
                                                modifier = Modifier.weight(0.18f),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            )
                                            Text(
                                                stringResource(Res.string.header_category),
                                                modifier = Modifier.weight(0.17f),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }

                                    // Table Content
                                    items(parsedProducts.take(20)) { product ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = product.nombre,
                                                modifier = Modifier.weight(0.4f),
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            val displayCodes =
                                                product.codigos.replace("[", "").replace("]", "")
                                                    .replace("\"", "").trim()
                                            Text(
                                                text = displayCodes.ifEmpty { "N/A" },
                                                modifier = Modifier.weight(0.25f),
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "$${
                                                    product.precio.toString().formatPrice()
                                                }",
                                                modifier = Modifier.weight(0.18f),
                                                fontSize = 11.sp
                                            )
                                            Text(
                                                text = product.categoria
                                                    ?: stringResource(Res.string.no_category),
                                                modifier = Modifier.weight(0.17f),
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        HorizontalDivider(
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(
                                                alpha = 0.5f
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        // Footer Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = { currentStep = 1 },
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(stringResource(Res.string.back_button))
                            }

                            Button(
                                onClick = { currentStep = 3 },
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(stringResource(Res.string.next_button))
                            }
                        }
                    }

                    3 -> {
                        // Options Step
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = stringResource(Res.string.import_choose_action),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Option A
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { if (!isProcessing) updateExistingOption = true },
                                border = BorderStroke(
                                    width = if (updateExistingOption) 2.dp else 1.dp,
                                    color = if (updateExistingOption) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                ),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (updateExistingOption) MaterialTheme.colorScheme.primaryContainer.copy(
                                        alpha = 0.15f
                                    )
                                    else MaterialTheme.colorScheme.surfaceContainerLowest
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = updateExistingOption,
                                        onClick = {
                                            if (!isProcessing) updateExistingOption = true
                                        },
                                        enabled = !isProcessing
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            stringResource(Res.string.import_opt_update_title),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            stringResource(Res.string.import_opt_update_desc),
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            // Option B
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { if (!isProcessing) updateExistingOption = false },
                                border = BorderStroke(
                                    width = if (!updateExistingOption) 2.dp else 1.dp,
                                    color = if (!updateExistingOption) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant
                                ),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (!updateExistingOption) MaterialTheme.colorScheme.errorContainer.copy(
                                        alpha = 0.1f
                                    )
                                    else MaterialTheme.colorScheme.surfaceContainerLowest
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = !updateExistingOption,
                                        onClick = {
                                            if (!isProcessing) updateExistingOption = false
                                        },
                                        colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.error),
                                        enabled = !isProcessing
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            stringResource(Res.string.import_opt_replace_title),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                        Text(
                                            stringResource(Res.string.import_opt_replace_desc),
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            if (!updateExistingOption) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                                    shape = MaterialTheme.shapes.small
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            painter = painterResource(Res.drawable.warning),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = stringResource(Res.string.import_warning_replace_all),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                }
                            }

                            if (importError != null) {
                                Text(
                                    text = importError!!,
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (isProcessing) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    LinearProgressIndicator(
                                        progress = { importProgressFraction },
                                        modifier = Modifier.fillMaxWidth(),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    Text(
                                        text = importProgressText,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        // Footer Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = { currentStep = 2 },
                                enabled = !isProcessing,
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(stringResource(Res.string.back_button))
                            }

                            Button(
                                onClick = {
                                    isProcessing = true
                                    importError = null
                                    importProgressFraction = 0f
                                    importProgressText = startingImportText
                                    scope.launch {
                                        try {
                                            withContext(Dispatchers.IO) {
                                                if (updateExistingOption) {
                                                    val existingProducts =
                                                        repository.getAllProductsList()
                                                    val existingById =
                                                        existingProducts.associateBy { it.id }
                                                    val existingByBarcode =
                                                        mutableMapOf<String, Products>()
                                                    existingProducts.forEach { prod ->
                                                        val codes = try {
                                                            prod.codigos
                                                                .replace("[", "")
                                                                .replace("]", "")
                                                                .replace("\"", "")
                                                                .split(",")
                                                                .map { it.trim() }
                                                                .filter { it.isNotEmpty() }
                                                        } catch (_: Exception) {
                                                            emptyList()
                                                        }
                                                        codes.forEach { code ->
                                                            existingByBarcode[code] = prod
                                                        }
                                                    }

                                                    var updated = 0
                                                    var inserted = 0
                                                    val total = parsedProducts.size

                                                    for ((index, p) in parsedProducts.withIndex()) {
                                                        var targetId = p.id
                                                        var isExisting = false

                                                        if (existingById.containsKey(targetId)) {
                                                            isExisting = true
                                                        } else {
                                                            val pCodes = try {
                                                                p.codigos
                                                                    .replace("[", "")
                                                                    .replace("]", "")
                                                                    .replace("\"", "")
                                                                    .split(",")
                                                                    .map { it.trim() }
                                                                    .filter { it.isNotEmpty() }
                                                            } catch (_: Exception) {
                                                                emptyList()
                                                            }
                                                            for (code in pCodes) {
                                                                val matched =
                                                                    existingByBarcode[code]
                                                                if (matched != null) {
                                                                    targetId = matched.id
                                                                    isExisting = true
                                                                    break
                                                                }
                                                            }
                                                        }

                                                        val pToInsert = if (isExisting) {
                                                            p.copy(
                                                                id = targetId,
                                                                updated_at = currentTimeMillis(),
                                                                sync_state = "PENDING_UPDATE"
                                                            )
                                                        } else {
                                                            p.copy(
                                                                id = targetId,
                                                                updated_at = currentTimeMillis(),
                                                                sync_state = "PENDING_INSERT"
                                                            )
                                                        }

                                                        repository.insertProduct(pToInsert)
                                                        if (isExisting) updated++ else inserted++

                                                        val currentProcessed = index + 1
                                                        withContext(Dispatchers.Main) {
                                                            importProgressFraction =
                                                                currentProcessed.toFloat() / total
                                                            importProgressText =
                                                                savingFmt.replace($$"%1$s", p.nombre)
                                                                    .replace(
                                                                        $$"%2$d",
                                                                        currentProcessed.toString()
                                                                    ).replace(
                                                                        $$"%3$d",
                                                                    total.toString()
                                                                )
                                                        }
                                                        delay(10.milliseconds)
                                                    }
                                                    importSuccessMessage = updateSuccessFmt.replace(
                                                        $$"%1$d",
                                                        inserted.toString()
                                                    ).replace($$"%2$d", updated.toString())
                                                } else {
                                                    repository.deleteAllProducts()
                                                    val total = parsedProducts.size
                                                    for ((index, p) in parsedProducts.withIndex()) {
                                                        repository.insertProduct(
                                                            p.copy(
                                                                updated_at = currentTimeMillis(),
                                                                sync_state = "PENDING_INSERT"
                                                            )
                                                        )
                                                        val currentProcessed = index + 1
                                                        withContext(Dispatchers.Main) {
                                                            importProgressFraction =
                                                                currentProcessed.toFloat() / total
                                                            importProgressText =
                                                                insertingFmt.replace(
                                                                    $$"%1$s",
                                                                    p.nombre
                                                                ).replace(
                                                                    $$"%2$d",
                                                                    currentProcessed.toString()
                                                                ).replace($$"%3$d", total.toString())
                                                        }
                                                        delay(10.milliseconds)
                                                    }
                                                    importSuccessMessage =
                                                        replaceSuccessFmt.replace(
                                                            $$"%1$d",
                                                            parsedProducts.size.toString()
                                                        )
                                                }
                                            }
                                            currentStep = 4
                                        } catch (e: Exception) {
                                            importError =
                                                dbSaveErrFmt.replace($$"%1$s", e.message ?: "")
                                        } finally {
                                            isProcessing = false
                                        }
                                    }
                                },
                                enabled = !isProcessing,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (updateExistingOption) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                ),
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    if (updateExistingOption) stringResource(Res.string.confirm_and_import_button) else stringResource(
                                        Res.string.delete_and_replace_button
                                    )
                                )
                            }
                        }
                    }

                    4 -> {
                        // Success Step
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.check),
                                contentDescription = null,
                                tint = Color(0xFF10B981), // Beautiful Green
                                modifier = Modifier.size(64.dp)
                            )
                            Text(
                                text = stringResource(Res.string.import_success_title),
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = importSuccessMessage ?: "",
                                textAlign = TextAlign.Center,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Footer Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Button(
                                onClick = onDismiss,
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(stringResource(Res.string.close_button))
                            }
                        }
                    }
                }
            }
        }
    }
}


