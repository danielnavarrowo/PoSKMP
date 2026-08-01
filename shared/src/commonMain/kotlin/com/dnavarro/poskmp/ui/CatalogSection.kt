package com.dnavarro.poskmp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.focusable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dnavarro.poskmp.db.Products
import com.dnavarro.poskmp.theme.ShapeDefaults
import com.dnavarro.poskmp.util.formatPrice
import com.dnavarro.poskmp.util.isAndroid
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import poskmp.shared.generated.resources.*

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
    cartCount: Int = 0,
    cartTotal: Double = 0.0,
    onSellUnregisteredClick: () -> Unit,
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
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.medium)
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
            placeholder = {
                Text(
                    stringResource(Res.string.search_placeholder),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    softWrap = false
                )
            },
            leadingIcon = {
                Icon(
                    painter = painterResource(Res.drawable.search),
                    contentDescription = stringResource(Res.string.search_desc),
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    Icon(
                        painter = painterResource(Res.drawable.close),
                        contentDescription = stringResource(Res.string.clear_desc),
                    )
                }
            },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            )
        )

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
                                            .padding(8.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(Res.drawable.star_filled),
                                            contentDescription = stringResource(Res.string.favorite_desc),
                                            modifier = Modifier.size(12.dp)
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

                // Mobile bottom overlay to access cart
                if (isCompact && onViewCartClick != null) {
                    ExtendedFloatingActionButton(
                        onClick = onViewCartClick,
                        modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (cartCount > 0) stringResource(Res.string.view_ticket_fab, cartCount, cartTotal.toString().formatPrice())
                            else stringResource(Res.string.tab_ticket)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SuggestionChip(
                onClick = onSellUnregisteredClick,
                label = { Text(if (isCompact) stringResource(Res.string.not_registered) else stringResource(Res.string.not_registered_hotkey), fontWeight = FontWeight.Bold) },
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            )
            SuggestionChip(
                onClick = onApplyWholesaleClick,
                label = {
                    Text(
                        if (isCompact) stringResource(Res.string.wholesale_ticket) else stringResource(Res.string.wholesale_ticket_hotkey),
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            )
            SuggestionChip(
                onClick = onCheckoutClick,
                label = { Text(if (isCompact) stringResource(Res.string.checkout_button) else stringResource(Res.string.checkout_hotkey), fontWeight = FontWeight.Bold) },
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            )
        }
    }
}
