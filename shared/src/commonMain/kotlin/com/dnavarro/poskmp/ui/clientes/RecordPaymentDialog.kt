package com.dnavarro.poskmp.ui.clientes

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dnavarro.poskmp.domain.model.Customer
import com.dnavarro.poskmp.domain.model.PaymentMethod
import com.dnavarro.poskmp.util.formatPrice
import com.dnavarro.poskmp.util.isAndroid
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import poskmp.shared.generated.resources.Res
import poskmp.shared.generated.resources.cancel
import poskmp.shared.generated.resources.card
import poskmp.shared.generated.resources.field_payment_amount
import poskmp.shared.generated.resources.field_payment_notes
import poskmp.shared.generated.resources.money
import poskmp.shared.generated.resources.money_transfer
import poskmp.shared.generated.resources.payment_amount_exceeds_debt_warning
import poskmp.shared.generated.resources.payment_amount_invalid_error
import poskmp.shared.generated.resources.payment_method_efectivo
import poskmp.shared.generated.resources.payment_method_tarjeta
import poskmp.shared.generated.resources.payment_method_transferencia
import poskmp.shared.generated.resources.record_payment_confirm
import poskmp.shared.generated.resources.record_payment_subtitle_format
import poskmp.shared.generated.resources.record_payment_title

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RecordPaymentDialog(
    customer: Customer,
    onDismissRequest: () -> Unit,
    onConfirm: (monto: Double, metodoPago: String, notas: String) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var selectedMethod by remember { mutableStateOf(PaymentMethod.EFECTIVO) }
    var notes by remember { mutableStateOf("") }
    var hasAttemptedConfirm by remember { mutableStateOf(false) }

    val amount = amountText.toDoubleOrNull() ?: 0.0
    val isAmountValid = amount > 0.0

    val amountFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        if (!isAndroid()) {
            delay(100.milliseconds)
            try {
                amountFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    fun attemptConfirm() {
        hasAttemptedConfirm = true
        if (isAmountValid) {
            onConfirm(amount, selectedMethod.name, notes)
        }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = Modifier.then(
            if (!isAndroid()) {
                Modifier
                    .focusable()
                    .onPreviewKeyEvent { keyEvent ->
                        keyEvent.type == KeyEventType.KeyDown && when (keyEvent.key) {
                            Key.Enter, Key.NumPadEnter -> {
                                if (isAmountValid) {
                                    attemptConfirm()
                                    true
                                } else false
                            }
                            else -> false
                        }
                    }
            } else Modifier
        ),
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        title = {
            Column {
                Text(
                    text = stringResource(Res.string.record_payment_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(
                        Res.string.record_payment_subtitle_format,
                        customer.nombre,
                        customer.saldoDeudor.toString().formatPrice()
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Payment Method Selector
                val methods = listOf(
                    PaymentMethod.EFECTIVO to (stringResource(Res.string.payment_method_efectivo) to Res.drawable.money),
                    PaymentMethod.TARJETA to (stringResource(Res.string.payment_method_tarjeta) to Res.drawable.card),
                    PaymentMethod.TRANSFERENCIA to (stringResource(Res.string.payment_method_transferencia) to Res.drawable.money_transfer)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
                ) {
                    methods.forEach { (method, info) ->
                        val (label, icon) = info
                        val isSelected = selectedMethod == method
                        ToggleButton(
                            checked = isSelected,
                            onCheckedChange = { selectedMethod = method },
                            modifier = Modifier.weight(1f),
                            colors = ToggleButtonDefaults.toggleButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                checkedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                checkedContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Icon(
                                    painter = painterResource(icon),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                // Amount Field
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { text ->
                        if (text.isEmpty() || text.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                            amountText = text
                        }
                    },
                    label = { Text(stringResource(Res.string.field_payment_amount)) },
                    prefix = { Text("$ ", fontWeight = FontWeight.Bold) },
                    singleLine = true,
                    isError = hasAttemptedConfirm && !isAmountValid,
                    supportingText = if (hasAttemptedConfirm && !isAmountValid) {
                        { Text(stringResource(Res.string.payment_amount_invalid_error)) }
                    } else if (amount > customer.saldoDeudor && customer.saldoDeudor > 0.0) {
                        {
                            Text(
                                stringResource(
                                    Res.string.payment_amount_exceeds_debt_warning,
                                    amount.toString().formatPrice(),
                                    customer.saldoDeudor.toString().formatPrice()
                                ),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    } else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (!isAndroid()) Modifier.focusRequester(amountFocusRequester) else Modifier)
                )

                // Notes Field
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(Res.string.field_payment_notes)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { attemptConfirm() },
                enabled = isAmountValid
            ) {
                Text(
                    if (isAndroid()) stringResource(Res.string.record_payment_confirm)
                    else "${stringResource(Res.string.record_payment_confirm)} (Enter)"
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(Res.string.cancel))
            }
        }
    )
}
