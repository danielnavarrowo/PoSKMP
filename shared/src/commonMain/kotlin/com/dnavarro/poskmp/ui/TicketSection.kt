package com.dnavarro.poskmp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dnavarro.poskmp.util.formatPrice
import com.dnavarro.poskmp.util.formatQuantity
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import poskmp.shared.generated.resources.*
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

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
    onBackClick: (() -> Unit)? = null
) {
    val focusRequesters = remember { mutableStateMapOf<Int, FocusRequester>() }
    var previousSize by remember { mutableIntStateOf(0) }

    LaunchedEffect(cartItems.size) {
        if (cartItems.isNotEmpty()) {
            val targetIndex = when {
                cartItems.size > previousSize -> cartItems.size - 1
                selectedIndex >= cartItems.size -> cartItems.size - 1
                selectedIndex < 0 -> 0
                else -> selectedIndex
            }
            onSelectedIndexChange(targetIndex)
            delay(50.milliseconds)
            try {
                if (targetIndex < cartItems.size) {
                    focusRequesters[targetIndex]?.requestFocus()
                }
            } catch (_: Exception) {}
        } else {
            onSelectedIndexChange(-1)
        }
        previousSize = cartItems.size
    }


    Column(
        modifier = modifier
            .fillMaxSize()
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
                            painter = (painterResource(Res.drawable.back)),
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
            if (cartItems.isNotEmpty()) {
                IconButton(onClick = onClearCart) {
                    Icon(
                        painter = painterResource(Res.drawable.trash),
                        contentDescription = stringResource(Res.string.clear_all_button)
                    )
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
                    var isTextFieldFocused by remember { mutableStateOf(false) }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(shape)
                            .focusRequester(focusRequester)
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
                                        if (index < cartItems.size - 1) {
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

                                    Key.Delete, Key.Backspace -> {
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
                            .background(
                                if (isRowFocused) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                else MaterialTheme.colorScheme.surfaceContainerLow
                            )
                            .then(
                                if (isRowFocused) {
                                    Modifier.border(
                                        width = 2.dp,
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = shape
                                    )
                                } else Modifier
                            )
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(Modifier, verticalAlignment = Alignment.CenterVertically)
                        {
                            Text(
                                text = item.product.nombre,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )

                            val isApplied = item.product.precio == item.product.precio_mayoreo
                            if (isApplied){
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    shape = MaterialTheme.shapes.extraSmall
                                ) {
                                    Text(
                                        text = stringResource(Res.string.wholesale_badge),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Row{
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                // Decrease quantity
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
                                            focusRequester.requestFocus()
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
                                            isTextFieldFocused = focusState.isFocused
                                            if (focusState.isFocused) {
                                                onSelectedIndexChange(index)
                                            }
                                            if (!focusState.isFocused) {
                                                val parsed = textValue.toDoubleOrNull()
                                                if (parsed != null && parsed > 0.0) {
                                                    onSetQuantity(item, parsed)
                                                } else if (parsed == 0.0) {
                                                    onRemoveItem(item)
                                                } else {
                                                    textValue = item.quantity.formatQuantity(item.product.por_peso == 1L)
                                                }
                                            }
                                        },
                                    singleLine = true
                                )

                                // Increase quantity
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
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "$${
                                    item.product.precio.toString().formatPrice()
                                } x ${item.quantity.formatQuantity(item.product.por_peso == 1L)}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Text(
                                text = "$${(item.product.precio * item.quantity).toString().formatPrice()}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.widthIn(min = 55.dp),
                                textAlign = TextAlign.End
                            )
                        }
                    }
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
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    stringResource(Res.string.items_count_label, productCount),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    stringResource(Res.string.pieces_count_label, formattedPieces),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
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

            Spacer(modifier = Modifier.height(12.dp))

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
                Text(stringResource(Res.string.checkout_button), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
