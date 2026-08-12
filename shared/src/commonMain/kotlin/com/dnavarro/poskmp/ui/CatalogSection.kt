package com.dnavarro.poskmp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dnavarro.poskmp.db.Products
import com.dnavarro.poskmp.theme.ShapeDefaults
import com.dnavarro.poskmp.util.formatPrice
import com.dnavarro.poskmp.util.isAndroid
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import poskmp.shared.generated.resources.Res
import poskmp.shared.generated.resources.barcode_scanner
import poskmp.shared.generated.resources.checkout_button
import poskmp.shared.generated.resources.checkout_hotkey
import poskmp.shared.generated.resources.clear_desc
import poskmp.shared.generated.resources.close
import poskmp.shared.generated.resources.close_scanner_desc
import poskmp.shared.generated.resources.edit
import poskmp.shared.generated.resources.empty_icon_desc
import poskmp.shared.generated.resources.favorite_desc
import poskmp.shared.generated.resources.mark_as_favorite
import poskmp.shared.generated.resources.modify
import poskmp.shared.generated.resources.no_category
import poskmp.shared.generated.resources.no_products_found
import poskmp.shared.generated.resources.not_registered
import poskmp.shared.generated.resources.not_registered_hotkey
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
import poskmp.shared.generated.resources.wholesale_item
import poskmp.shared.generated.resources.wholesale_item_hotkey
import poskmp.shared.generated.resources.wholesale_ticket
import poskmp.shared.generated.resources.wholesale_ticket_hotkey

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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
    onViewCartClick: (() -> Unit)? = null,
    onOpenScanner: (() -> Unit)? = null,
    cartCount: Int = 0,
    cartTotal: Double = 0.0,
    onSellUnregisteredClick: () -> Unit,
    onApplyItemWholesaleClick: () -> Unit = {},
    onApplyWholesaleClick: () -> Unit = {},
    onCheckoutClick: () -> Unit = {},
    searchFocusRequester: FocusRequester? = null,
    onBarcodeScan: ((String) -> Unit)? = null,
    onSearchKeyIntercept: ((KeyEvent) -> Boolean)? = null
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
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
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            ),
                            textAlign = TextAlign.Start,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    BasicTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .let { mod ->
                                if (searchFocusRequester != null && !isAndroid()) mod.focusRequester(searchFocusRequester) else mod
                            }
                            .onPreviewKeyEvent { keyEvent ->
                                onSearchKeyIntercept != null && onSearchKeyIntercept(keyEvent) || !isAndroid() &&
                                        keyEvent.type == KeyEventType.KeyDown &&
                                        (keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter) && if (onBarcodeScan != null && searchQuery.isNotBlank()) {
                                    onBarcodeScan(searchQuery)
                                    true
                                } else false
                            },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Start
                        ),
                        singleLine = true,
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
                    )
                }

                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        modifier = Modifier.size(32.dp),
                        onClick = { onSearchQueryChange("") }
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
            val sortedProducts = remember(productsList) {
                productsList.sortedWith(
                    compareByDescending<Products> { it.es_favorito == 1L }
                        .thenBy { it.nombre.lowercase() }
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(sortedProducts.take(50)) { product ->
                        var showContextMenu by remember { mutableStateOf(false) }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                            shape = ShapeDefaults.cardShape,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .combinedClickable(
                                    onClick = { onProductClick(product) },
                                    onLongClick = { showContextMenu = true }
                                )
                                .pointerInput(product) {
                                    awaitPointerEventScope {
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            if (event.type == PointerEventType.Press) {
                                                val isRightClick = event.buttons.isSecondaryPressed
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
                                            text = product.categoria ?: stringResource(Res.string.no_category),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = product.nombre,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Bottom
                                    ) {
                                        if (product.por_peso == 1L) {
                                            Text(
                                                text = "$${
                                                    product.precio.toString().formatPrice()
                                                } / Kg",
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        } else {
                                            Text(
                                                text = "$${
                                                    product.precio.toString().formatPrice()
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
                                            .padding(8.dp).clip(MaterialShapes.Cookie12Sided.toShape()).size(28.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(Res.drawable.star_filled),
                                            contentDescription = stringResource(Res.string.favorite_desc),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                DropdownMenu(

                                    expanded = showContextMenu,
                                    shape = MaterialTheme.shapes.medium,
                                    onDismissRequest = { showContextMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(if (product.es_favorito == 1L) stringResource(Res.string.remove_from_favorites) else stringResource(Res.string.mark_as_favorite))
                                        },
                                        onClick = {
                                            showContextMenu = false
                                            onToggleFavorite(product)
                                        },
                                        leadingIcon = {
                                            Icon(
                                                painter = if (product.es_favorito == 1L) painterResource(Res.drawable.star_filled) else painterResource(Res.drawable.star),
                                                contentDescription = stringResource(Res.string.favorite_desc),
                                                tint = if (product.es_favorito == 1L) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(Res.string.modify)) },
                                        onClick = {
                                            showContextMenu = false
                                            onModifyProduct(product)
                                        },
                                        leadingIcon = {
                                            Icon(painter = painterResource(Res.drawable.edit), contentDescription = stringResource(Res.string.modify))
                                        }
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
                                    if (cartCount > 0) stringResource(Res.string.view_ticket_fab, cartCount, cartTotal.toString().formatPrice())
                                    else stringResource(Res.string.tab_ticket)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            item {
                SuggestionChip(
                    onClick = onSellUnregisteredClick,
                    label = { Text(if (isAndroid()) stringResource(Res.string.not_registered) else stringResource(Res.string.not_registered_hotkey), fontWeight = FontWeight.Bold) },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                )
            }
            item {
                SuggestionChip(
                    onClick = onApplyItemWholesaleClick,
                    label = {
                        Text(
                            if (isAndroid()) stringResource(Res.string.wholesale_item) else stringResource(Res.string.wholesale_item_hotkey),
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                )
            }
            item {
                SuggestionChip(
                    onClick = onApplyWholesaleClick,
                    label = {
                        Text(
                            if (isAndroid()) stringResource(Res.string.wholesale_ticket) else stringResource(Res.string.wholesale_ticket_hotkey),
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                )
            }

            item {
                SuggestionChip(
                    onClick = onCheckoutClick,
                    label = { Text(if (isAndroid()) stringResource(Res.string.checkout_button) else stringResource(Res.string.checkout_hotkey), fontWeight = FontWeight.Bold) },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                )
            }
        }
    }
}
