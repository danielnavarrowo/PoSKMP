package com.dnavarro.poskmp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.focusable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.OutlinedButton
import com.dnavarro.poskmp.domain.model.Customer
import com.dnavarro.poskmp.theme.ShapeDefaults
import com.dnavarro.poskmp.ui.venta.HeldTicket
import com.dnavarro.poskmp.util.formatPrice
import com.dnavarro.poskmp.util.formatQuantity
import com.dnavarro.poskmp.util.isAndroid
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import poskmp.shared.generated.resources.*
import kotlin.math.roundToInt

@Composable
fun TicketSection(
    cartItems: List<CartItem>,
    total: Double,
    onClearCart: () -> Unit,
    onUpdateQuantity: (CartItem, Double) -> Unit,
    onSetQuantity: (CartItem, Double) -> Unit,
    onRemoveItem: (CartItem) -> Unit,
    onCheckout: () -> Unit,
    modifier: Modifier = Modifier,
    selectedIndex: Int = -1,
    onSelectedIndexChange: (Int) -> Unit = {},
    onBackClick: (() -> Unit)? = null,
    canUndo: Boolean = false,
    onUndo: () -> Unit = {},
    heldTickets: List<HeldTicket> = emptyList(),
    onHoldTicket: () -> Unit = {},
    onResumeHeldTicket: (HeldTicket) -> Unit = {},
    onDiscardHeldTicket: (HeldTicket) -> Unit = {},
    selectedCustomer: Customer? = null,
    onAssignCustomerClick: () -> Unit = {},
    onClearCustomerClick: () -> Unit = {}
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        if (isAndroid()) {
            delay(50.milliseconds)
            focusManager.clearFocus(force = true)
            keyboardController?.hide()
        }
    }

    val focusRequesters = remember { mutableStateMapOf<Int, FocusRequester>() }
    var previousSize by remember { mutableIntStateOf(0) }
    val listState = rememberLazyListState()

    LaunchedEffect(selectedIndex) {
        if (selectedIndex in cartItems.indices) {
            try {
                listState.animateScrollToItem(selectedIndex)
            } catch (_: Exception) {}
        }
    }

    LaunchedEffect(cartItems.size) {
        if (cartItems.isNotEmpty()) {
            val targetIndex = when {
                cartItems.size > previousSize -> cartItems.size - 1
                selectedIndex >= cartItems.size -> cartItems.size - 1
                selectedIndex < 0 -> 0
                else -> selectedIndex
            }
            onSelectedIndexChange(targetIndex)
        } else {
            onSelectedIndexChange(-1)
        }
        previousSize = cartItems.size
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .widthIn(min = 280.dp)
            .padding(16.dp)
    ) {
        // Section Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onBackClick != null) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            painter = painterResource(Res.drawable.back),
                            contentDescription = stringResource(Res.string.back_to_catalog_desc)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = stringResource(Res.string.current_ticket_title),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onUndo,
                    enabled = canUndo
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.undo),
                        contentDescription = stringResource(Res.string.undo_button_desc)
                    )
                }
                IconButton(
                    onClick = onHoldTicket,
                    enabled = cartItems.isNotEmpty()
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.pause),
                        contentDescription = stringResource(Res.string.hold_ticket_button_desc)
                    )
                }
                if (cartItems.isNotEmpty()) {
                    IconButton(onClick = onClearCart) {
                        Icon(
                            painter = painterResource(Res.drawable.trash),
                            contentDescription = stringResource(Res.string.clear_all_button)
                        )
                    }
                }
            }
        }

        // Held Tickets Row (if any)
        if (heldTickets.isNotEmpty()) {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    itemsIndexed(heldTickets) { _, ticket ->
                        val piecesFormatted = if (ticket.totalItemsCount % 1.0 == 0.0) ticket.totalItemsCount.toLong().toString() else ticket.totalItemsCount.toString()
                        Surface(
                            onClick = { onResumeHeldTicket(ticket) },
                            shape = ShapeDefaults.cardShape,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.height(36.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(start = 10.dp, end = 4.dp)
                            ) {
                                Text(
                                    text = "$piecesFormatted pzs • $${ticket.total.toString().formatPrice()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(
                                    onClick = { onDiscardHeldTicket(ticket) },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(Res.drawable.close),
                                        contentDescription = stringResource(Res.string.discard_held_ticket_desc),
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Cart Items List
        if (cartItems.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        painter = painterResource(Res.drawable.shopping_cart),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        stringResource(Res.string.ticket_empty_message),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 15.sp
                    )
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                itemsIndexed(cartItems) { index, item ->
                    val shape = if (cartItems.size == 1) {
                        MaterialTheme.shapes.medium
                    } else if (index == 0) {
                        RoundedCornerShape(
                            topStart = MaterialTheme.shapes.medium.topStart,
                            topEnd = MaterialTheme.shapes.medium.topEnd,
                            bottomStart = MaterialTheme.shapes.extraSmall.bottomStart,
                            bottomEnd = MaterialTheme.shapes.extraSmall.bottomEnd
                        )
                    } else if (index == cartItems.lastIndex) {
                        RoundedCornerShape(
                            topStart = MaterialTheme.shapes.extraSmall.topStart,
                            topEnd = MaterialTheme.shapes.extraSmall.topEnd,
                            bottomStart = MaterialTheme.shapes.medium.bottomStart,
                            bottomEnd = MaterialTheme.shapes.medium.bottomEnd
                        )
                    } else {
                        RoundedCornerShape(MaterialTheme.shapes.extraSmall.topStart)
                    }

                    val focusRequester = remember(index) { focusRequesters.getOrPut(index) { FocusRequester() } }
                    val isRowFocused = selectedIndex == index

                    TicketItemRow(
                        item = item,
                        index = index,
                        cartItemsSize = cartItems.size,
                        shape = shape,
                        isRowFocused = isRowFocused,
                        focusRequester = focusRequester,
                        focusRequesters = focusRequesters,
                        onSelectedIndexChange = onSelectedIndexChange,
                        onUpdateQuantity = onUpdateQuantity,
                        onSetQuantity = onSetQuantity,
                        onRemoveItem = onRemoveItem
                    )
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        // Totals and Checkout Button
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val productCount = cartItems.size
            val piecesCount = cartItems.sumOf { it.quantity }
            val formattedPieces = if (piecesCount % 1.0 == 0.0) {
                piecesCount.toInt().toString()
            } else {
                ((piecesCount * 1000.0).roundToInt() / 1000.0).toString()
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(Res.string.items_count_label, productCount),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(Res.string.pieces_count_label, formattedPieces),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            val totalWithoutDiscount = cartItems.sumOf { it.originalPrice * it.quantity }
            val hasDiscount = cartItems.any { it.product.precio < it.originalPrice }

            if (hasDiscount) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        stringResource(Res.string.total_without_discount_label),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                    Text(
                        "$${totalWithoutDiscount.toString().formatPrice()}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        style = TextStyle(textDecoration = TextDecoration.LineThrough)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    stringResource(Res.string.total_label),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    "$${total.toString().formatPrice()}",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Selector / Asignación de Cliente
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.person),
                        contentDescription = null,
                        tint = if (selectedCustomer != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = selectedCustomer?.nombre ?: stringResource(Res.string.general_public_label),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (selectedCustomer?.siempreMayoreo == true) {
                                Surface(
                                    shape = MaterialTheme.shapes.extraSmall,
                                    color = MaterialTheme.colorScheme.tertiaryContainer
                                ) {
                                    Text(
                                        text = stringResource(Res.string.badge_customer_always_wholesale),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                        if (selectedCustomer != null) {
                            if (selectedCustomer.saldoDeudor > 0.0) {
                                Text(
                                    text = stringResource(Res.string.customer_balance_format, selectedCustomer.saldoDeudor.toString().formatPrice()),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.SemiBold
                                )
                            } else {
                                Text(
                                    text = stringResource(Res.string.customer_no_debt_pending),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            Text(
                                text = stringResource(Res.string.no_customer_assigned),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (selectedCustomer != null) {
                        IconButton(
                            onClick = onClearCustomerClick,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.close),
                                contentDescription = stringResource(Res.string.remove_customer_button),
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    OutlinedButton(
                        onClick = onAssignCustomerClick,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text(
                            text = if (selectedCustomer != null) stringResource(Res.string.change_customer_button) else stringResource(Res.string.assign_customer_button),
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onCheckout,
                enabled = cartItems.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                ),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Icon(painter = painterResource(Res.drawable.money), contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isAndroid()) stringResource(Res.string.checkout_button) else stringResource(Res.string.checkout_hotkey),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun TicketItemRow(
    item: CartItem,
    index: Int,
    cartItemsSize: Int,
    shape: androidx.compose.ui.graphics.Shape,
    isRowFocused: Boolean,
    focusRequester: FocusRequester,
    focusRequesters: Map<Int, FocusRequester>,
    onSelectedIndexChange: (Int) -> Unit,
    onUpdateQuantity: (CartItem, Double) -> Unit,
    onSetQuantity: (CartItem, Double) -> Unit,
    onRemoveItem: (CartItem) -> Unit
) {
    var isTextFieldFocused by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .then(
                if (!isAndroid()) {
                    Modifier
                        .focusRequester(focusRequester)
                        .focusable()
                        .onFocusChanged { focusState ->
                            if (focusState.isFocused || focusState.hasFocus) {
                                onSelectedIndexChange(index)
                            }
                        }
                        .onKeyEvent { keyEvent ->
                            !isTextFieldFocused && keyEvent.type == KeyEventType.KeyDown && when (keyEvent.key) {
                                Key.DirectionUp -> {
                                    if (index > 0) {
                                        focusRequesters[index - 1]?.requestFocus()
                                    }
                                    true
                                }

                                Key.DirectionDown -> {
                                    if (index < cartItemsSize - 1) {
                                        focusRequesters[index + 1]?.requestFocus()
                                    }
                                    true
                                }

                                Key.Plus, Key.NumPadAdd, Key.Equals -> {
                                    val increment = if (item.product.por_peso == 1L) 0.1 else 1.0
                                    onUpdateQuantity(item, increment)
                                    true
                                }

                                Key.Minus, Key.NumPadSubtract -> {
                                    val decrement = if (item.product.por_peso == 1L) 0.1 else 1.0
                                    onUpdateQuantity(item, -decrement)
                                    true
                                }

                                Key.Delete -> {
                                    onRemoveItem(item)
                                    true
                                }

                                else -> false
                            }
                        }
                        .clickable {
                            focusRequester.requestFocus()
                            onSelectedIndexChange(index)
                        }
                } else {
                    Modifier.clickable {
                        onSelectedIndexChange(index)
                    }
                }
            )
            .background(
                if (isRowFocused && !isAndroid()) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                else MaterialTheme.colorScheme.surfaceContainerLow
            )
            .then(
                if (isRowFocused && !isAndroid()) {
                    Modifier.border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = shape
                    )
                } else Modifier
            )
            .padding(12.dp)
    ) {
        val isNarrow = maxWidth < 380.dp

        if (isNarrow) {
            // NARROW VIEW (< 380dp): 2-Row Layout
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                // TOP ROW: Product Name & Wholesale Badge (Left) | Total Price (Right)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ProductTitleAndBadge(item = item, modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.width(8.dp))
                    ItemTotalPriceText(item = item)
                }

                // BOTTOM ROW: Quantity Controls (Left) | Unit Price calculation (Right)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ItemQuantityControls(
                        item = item,
                        index = index,
                        isTextFieldFocused = isTextFieldFocused,
                        onFocusChanged = { isTextFieldFocused = it },
                        onUpdateQuantity = onUpdateQuantity,
                        onSetQuantity = onSetQuantity,
                        onRemoveItem = onRemoveItem,
                        focusRequester = focusRequester,
                        onSelectedIndexChange = onSelectedIndexChange
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    ItemUnitPriceCalcText(item = item)
                }
            }
        } else {
            // WIDE VIEW (>= 380dp): Single-Row Layout
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ProductTitleAndBadge(item = item, modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ItemQuantityControls(
                        item = item,
                        index = index,
                        isTextFieldFocused = isTextFieldFocused,
                        onFocusChanged = { isTextFieldFocused = it },
                        onUpdateQuantity = onUpdateQuantity,
                        onSetQuantity = onSetQuantity,
                        onRemoveItem = onRemoveItem,
                        focusRequester = focusRequester,
                        onSelectedIndexChange = onSelectedIndexChange
                    )
                    ItemUnitPriceCalcText(item = item)
                    ItemTotalPriceText(item = item)
                }
            }
        }
    }
}

@Composable
private fun ProductTitleAndBadge(item: CartItem, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = item.product.nombre,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        val isApplied = item.product.precio == item.product.precio_mayoreo
        if (isApplied) {
            Spacer(modifier = Modifier.width(6.dp))
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = MaterialTheme.shapes.extraSmall
            ) {
                Text(
                    text = stringResource(Res.string.wholesale_badge),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
        }
    }
}

@Composable
private fun ItemQuantityControls(
    item: CartItem,
    index: Int,
    isTextFieldFocused: Boolean,
    onFocusChanged: (Boolean) -> Unit,
    onUpdateQuantity: (CartItem, Double) -> Unit,
    onSetQuantity: (CartItem, Double) -> Unit,
    onRemoveItem: (CartItem) -> Unit,
    focusRequester: FocusRequester,
    onSelectedIndexChange: (Int) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        IconButton(
            onClick = {
                val decrement = if (item.product.por_peso == 1L) 0.1 else 1.0
                onUpdateQuantity(item, -decrement)
            },
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                painter = painterResource(Res.drawable.remove),
                contentDescription = stringResource(Res.string.decrease_desc),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }

        var textValue by remember(item.quantity) {
            mutableStateOf(item.quantity.formatQuantity(item.product.por_peso == 1L))
        }

        BasicTextField(
            value = textValue,
            onValueChange = { newValue ->
                val filtered = if (item.product.por_peso == 1L) {
                    newValue.filter { it.isDigit() || it == '.' }
                } else {
                    newValue.filter { it.isDigit() }
                }
                textValue = filtered
            },
            textStyle = TextStyle(
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            ),
            keyboardOptions = KeyboardOptions(
                keyboardType = if (item.product.por_peso == 1L) KeyboardType.Decimal else KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    val parsed = textValue.toDoubleOrNull()
                    if (parsed != null && parsed > 0.0) {
                        onSetQuantity(item, parsed)
                    } else if (parsed == 0.0) {
                        onRemoveItem(item)
                    } else {
                        textValue = item.quantity.formatQuantity(item.product.por_peso == 1L)
                    }
                    if (!isAndroid()) {
                        focusRequester.requestFocus()
                    }
                }
            ),
            modifier = Modifier
                .width(44.dp)
                .background(
                    if (isTextFieldFocused) MaterialTheme.colorScheme.surface
                    else Color.Transparent,
                )
                .padding(vertical = 2.dp)
                .onFocusChanged { focusState ->
                    onFocusChanged(focusState.isFocused)
                    if (focusState.isFocused) {
                        onSelectedIndexChange(index)
                    }
                    if (!focusState.isFocused) {
                        val parsed = textValue.toDoubleOrNull()
                        if (parsed != null && parsed > 0.0) {
                            if (parsed != item.quantity) {
                                onSetQuantity(item, parsed)
                            }
                        } else if (parsed == 0.0) {
                            onRemoveItem(item)
                        } else {
                            textValue = item.quantity.formatQuantity(item.product.por_peso == 1L)
                        }
                    }
                },
            singleLine = true
        )

        IconButton(
            onClick = {
                val increment = if (item.product.por_peso == 1L) 0.1 else 1.0
                onUpdateQuantity(item, increment)
            },
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                painter = painterResource(Res.drawable.add),
                contentDescription = stringResource(Res.string.increase_desc),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun ItemUnitPriceCalcText(item: CartItem) {
    Text(
        text = "$${item.product.precio.toString().formatPrice()} x ${item.quantity.formatQuantity(item.product.por_peso == 1L)}",
        fontSize = 11.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        softWrap = false
    )
}

@Composable
private fun ItemTotalPriceText(item: CartItem) {
    Text(
        text = "$${(item.product.precio * item.quantity).toString().formatPrice()}",
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        softWrap = false,
        textAlign = TextAlign.End
    )
}
