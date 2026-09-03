package com.dnavarro.poskmp.ui.clientes

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dnavarro.poskmp.domain.model.AccountStatementItem
import com.dnavarro.poskmp.domain.model.AccountStatementType
import com.dnavarro.poskmp.domain.model.Customer
import com.dnavarro.poskmp.theme.ShapeDefaults
import com.dnavarro.poskmp.util.formatEpochMillisToDateTime
import com.dnavarro.poskmp.util.formatPrice
import com.dnavarro.poskmp.util.isAndroid
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import poskmp.shared.generated.resources.Res
import poskmp.shared.generated.resources.cancel
import poskmp.shared.generated.resources.close
import poskmp.shared.generated.resources.close_button
import poskmp.shared.generated.resources.customer_action_payment
import poskmp.shared.generated.resources.customer_credit_limit_format
import poskmp.shared.generated.resources.customer_credit_no_limit
import poskmp.shared.generated.resources.delete
import poskmp.shared.generated.resources.delete_button
import poskmp.shared.generated.resources.money
import poskmp.shared.generated.resources.payments
import poskmp.shared.generated.resources.statement_delete_payment_confirm
import poskmp.shared.generated.resources.statement_empty
import poskmp.shared.generated.resources.statement_running_balance_format
import poskmp.shared.generated.resources.statement_summary_balance
import poskmp.shared.generated.resources.statement_title_format

@Composable
fun AccountStatementDialog(
    customer: Customer,
    statementItems: List<AccountStatementItem>,
    isLoading: Boolean,
    onDismissRequest: () -> Unit,
    onOpenPaymentDialog: () -> Unit,
    onDeletePayment: (paymentId: String) -> Unit
) {
    var paymentToDelete by remember { mutableStateOf<AccountStatementItem?>(null) }

    val closeFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        if (!isAndroid()) {
            delay(100.milliseconds)
            try {
                closeFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 920.dp)
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.88f)
                .then(
                    if (!isAndroid()) {
                        Modifier
                            .focusRequester(closeFocusRequester)
                            .focusable()
                    } else Modifier
                ),
            shape = ShapeDefaults.cardShape,
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(Res.string.statement_title_format, customer.nombre),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(onClick = onDismissRequest) {
                        Icon(
                            painter = painterResource(Res.drawable.close),
                            contentDescription = stringResource(Res.string.close_button),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Summary Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (customer.saldoDeudor > 0.0) {
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                        } else {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        }
                    ),
                    shape = ShapeDefaults.cardShape
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = stringResource(Res.string.statement_summary_balance, customer.saldoDeudor.toString().formatPrice()),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (customer.saldoDeudor > 0.0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (customer.limiteCredito > 0.0) {
                                    stringResource(Res.string.customer_credit_limit_format, customer.limiteCredito.toString().formatPrice())
                                } else {
                                    stringResource(Res.string.customer_credit_no_limit)
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Button(
                            onClick = onOpenPaymentDialog,
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.payments),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(Res.string.customer_action_payment))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(8.dp))

                // Statement Items List
                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (statementItems.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(Res.string.statement_empty),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(statementItems, key = { it.id }) { item ->
                            StatementItemRow(
                                item = item,
                                onDeletePayment = { paymentToDelete = item }
                            )
                        }
                    }
                }
            }
        }
    }

    // Delete Payment Confirmation Dialog
    paymentToDelete?.let { paymentItem ->
        val deletePaymentFocusRequester = remember { FocusRequester() }

        LaunchedEffect(paymentItem) {
            if (!isAndroid()) {
                delay(100.milliseconds)
                try {
                    deletePaymentFocusRequester.requestFocus()
                } catch (_: Exception) {}
            }
        }

        AlertDialog(
            onDismissRequest = { paymentToDelete = null },
            modifier = Modifier
                .widthIn(max = 440.dp)
                .fillMaxWidth()
                .then(
                if (!isAndroid()) {
                    Modifier
                        .focusable()
                        .onPreviewKeyEvent { keyEvent ->
                            keyEvent.type == KeyEventType.KeyDown && when (keyEvent.key) {
                                Key.Enter, Key.NumPadEnter -> {
                                    onDeletePayment(paymentItem.id)
                                    paymentToDelete = null
                                    true
                                }
                                else -> false
                            }
                        }
                } else Modifier
            ),
            shape = ShapeDefaults.cardShape,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            title = {
                Text(
                    text = stringResource(Res.string.statement_delete_payment_confirm, paymentItem.monto.toString().formatPrice()),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeletePayment(paymentItem.id)
                        paymentToDelete = null
                    },
                    modifier = if (!isAndroid()) Modifier.focusRequester(deletePaymentFocusRequester) else Modifier
                ) {
                    Text(
                        if (isAndroid()) stringResource(Res.string.delete_button)
                        else "${stringResource(Res.string.delete_button)} (Enter)",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { paymentToDelete = null }) {
                    Text(stringResource(Res.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun StatementItemRow(
    item: AccountStatementItem,
    onDeletePayment: () -> Unit
) {
    val isCharge = item.tipo == AccountStatementType.CARGO_CREDITO
    val formattedDate = formatEpochMillisToDateTime(item.fecha)

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        color = if (isCharge) {
                            MaterialTheme.colorScheme.errorContainer
                        } else {
                            MaterialTheme.colorScheme.tertiaryContainer
                        },
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(if (isCharge) Res.drawable.payments else Res.drawable.money),
                    contentDescription = null,
                    tint = if (isCharge) {
                        MaterialTheme.colorScheme.onErrorContainer
                    } else {
                        MaterialTheme.colorScheme.onTertiaryContainer
                    },
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.descripcion,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (item.metodoPago != null) {
                        Text(
                            text = "• ${item.metodoPago}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (item.notas.isNotBlank()) {
                        Text(
                            text = "• ${item.notas}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Amount and Resulting Balance
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = (if (isCharge) "+$" else "-$") + item.monto.toString().formatPrice(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isCharge) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
                )
                Text(
                    text = stringResource(Res.string.statement_running_balance_format, item.saldoResultante.toString().formatPrice()),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!isCharge) {
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = onDeletePayment,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.delete),
                        contentDescription = stringResource(Res.string.delete_button),
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
