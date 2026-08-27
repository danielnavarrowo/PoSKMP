package com.dnavarro.poskmp.ui.venta

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dnavarro.poskmp.domain.model.Customer
import com.dnavarro.poskmp.util.formatPrice
import com.dnavarro.poskmp.util.isAndroid
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import poskmp.shared.generated.resources.Res
import poskmp.shared.generated.resources.cancel
import poskmp.shared.generated.resources.close
import poskmp.shared.generated.resources.close_button
import poskmp.shared.generated.resources.customer_balance_format
import poskmp.shared.generated.resources.customer_no_debt
import poskmp.shared.generated.resources.empty_customers_search
import poskmp.shared.generated.resources.empty_customers_title
import poskmp.shared.generated.resources.general_public_label
import poskmp.shared.generated.resources.general_public_subtitle
import poskmp.shared.generated.resources.person
import poskmp.shared.generated.resources.search
import poskmp.shared.generated.resources.search_customer_placeholder
import poskmp.shared.generated.resources.select_customer_dialog_title
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun CustomerSelectionDialog(
    customers: List<Customer>,
    searchQuery: String,
    selectedCustomer: Customer?,
    onSearchQueryChange: (String) -> Unit,
    onSelectCustomer: (Customer?) -> Unit,
    onDismissRequest: () -> Unit
) {
    val initialHighlightIndex = remember(customers, selectedCustomer) {
        if (selectedCustomer != null) {
            val found = customers.indexOfFirst { it.id == selectedCustomer.id }
            if (found >= 0) found + 1 else 0
        } else 0
    }

    var highlightedIndex by remember(customers) { mutableIntStateOf(initialHighlightIndex) }
    val searchBarFocusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        if (!isAndroid()) {
            delay(100.milliseconds)
            try {
                searchBarFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    LaunchedEffect(highlightedIndex) {
        if (highlightedIndex in 0..customers.size) {
            try {
                listState.animateScrollToItem(highlightedIndex)
            } catch (_: Exception) {}
        }
    }

    val handleKeyEvent: (KeyEvent) -> Boolean = { keyEvent ->
        if (keyEvent.type == KeyEventType.KeyDown) {
            when (keyEvent.key) {
                Key.DirectionDown -> {
                    if (highlightedIndex < customers.size) {
                        highlightedIndex++
                    }
                    true
                }
                Key.DirectionUp -> {
                    if (highlightedIndex > 0) {
                        highlightedIndex--
                    }
                    true
                }
                Key.Enter, Key.NumPadEnter -> {
                    if (highlightedIndex == 0) {
                        onSelectCustomer(null)
                    } else {
                        val customerIndex = highlightedIndex - 1
                        if (customerIndex in customers.indices) {
                            onSelectCustomer(customers[customerIndex])
                        }
                    }
                    true
                }
                Key.Escape -> {
                    onDismissRequest()
                    true
                }
                else -> false
            }
        } else false
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.85f)
                .then(
                    if (!isAndroid()) {
                        Modifier.onPreviewKeyEvent(handleKeyEvent)
                    } else Modifier
                ),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerLowest
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(Res.string.select_customer_dialog_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    IconButton(onClick = onDismissRequest) {
                        Icon(
                            painter = painterResource(Res.drawable.close),
                            contentDescription = stringResource(Res.string.close_button)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        onSearchQueryChange(it)
                        highlightedIndex = 0
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (!isAndroid()) {
                                Modifier
                                    .focusRequester(searchBarFocusRequester)
                                    .onPreviewKeyEvent(handleKeyEvent)
                            } else Modifier
                        ),
                    placeholder = {
                        Text(
                            stringResource(Res.string.search_customer_placeholder),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(Res.drawable.search),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = {
                                onSearchQueryChange("")
                                highlightedIndex = 0
                            }) {
                                Icon(
                                    painter = painterResource(Res.drawable.close),
                                    contentDescription = stringResource(Res.string.close_button),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.large
                )

                Spacer(modifier = Modifier.height(12.dp))

                // List
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Option 1: Public General (Clear customer)
                    item {
                        val isHighlighted = highlightedIndex == 0
                        val isSelected = selectedCustomer == null

                        Surface(
                            onClick = {
                                highlightedIndex = 0
                                onSelectCustomer(null)
                            },
                            shape = MaterialTheme.shapes.medium,
                            color = when {
                                isHighlighted -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                else -> MaterialTheme.colorScheme.surfaceContainerLow
                            },
                            border = if (isHighlighted) {
                                BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                            } else null,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(
                                            MaterialTheme.colorScheme.surfaceVariant,
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(Res.drawable.person),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(Res.string.general_public_label),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = stringResource(Res.string.general_public_subtitle),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    if (customers.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (searchQuery.isNotBlank()) {
                                        stringResource(Res.string.empty_customers_search, searchQuery)
                                    } else {
                                        stringResource(Res.string.empty_customers_title)
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        itemsIndexed(customers, key = { _, it -> it.id }) { index, customer ->
                            val itemIndex = index + 1
                            val isHighlighted = highlightedIndex == itemIndex
                            val isSelected = selectedCustomer?.id == customer.id
                            val hasDebt = customer.saldoDeudor > 0.0

                            Surface(
                                onClick = {
                                    highlightedIndex = itemIndex
                                    onSelectCustomer(customer)
                                },
                                shape = MaterialTheme.shapes.medium,
                                color = when {
                                    isHighlighted -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                    isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                    else -> MaterialTheme.colorScheme.surfaceContainerLow
                                },
                                border = if (isHighlighted) {
                                    BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                                } else null,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Initials
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(
                                                color = if (hasDebt) {
                                                    MaterialTheme.colorScheme.errorContainer
                                                } else {
                                                    MaterialTheme.colorScheme.secondaryContainer
                                                },
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = customer.nombre.take(1).uppercase(),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = if (hasDebt) {
                                                MaterialTheme.colorScheme.onErrorContainer
                                            } else {
                                                MaterialTheme.colorScheme.onSecondaryContainer
                                            }
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = customer.nombre,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (customer.telefono.isNotBlank() || customer.direccion.isNotBlank()) {
                                            Text(
                                                text = listOfNotNull(
                                                    customer.telefono.takeIf { it.isNotBlank() },
                                                    customer.direccion.takeIf { it.isNotBlank() }
                                                ).joinToString(" • "),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    // Debt Badge
                                    Surface(
                                        shape = MaterialTheme.shapes.small,
                                        color = if (hasDebt) {
                                            MaterialTheme.colorScheme.errorContainer
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant
                                        }
                                    ) {
                                        Text(
                                            text = if (hasDebt) {
                                                stringResource(Res.string.customer_balance_format, customer.saldoDeudor.toString().formatPrice())
                                            } else {
                                                stringResource(Res.string.customer_no_debt)
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (hasDebt) {
                                                MaterialTheme.colorScheme.onErrorContainer
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text(stringResource(Res.string.cancel))
                    }
                }
            }
        }
    }
}
