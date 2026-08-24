package com.dnavarro.poskmp.ui.turnos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.dnavarro.poskmp.domain.model.ShiftSummary
import com.dnavarro.poskmp.util.formatEpochMillisToDateTime
import com.dnavarro.poskmp.util.formatPrice
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import poskmp.shared.generated.resources.Res
import poskmp.shared.generated.resources.cancel
import poskmp.shared.generated.resources.close_shift_button
import poskmp.shared.generated.resources.close_shift_dialog_subtitle
import poskmp.shared.generated.resources.close_shift_dialog_title
import poskmp.shared.generated.resources.close_shift_notes_label
import poskmp.shared.generated.resources.close_shift_notes_placeholder
import poskmp.shared.generated.resources.counted_cash_label
import poskmp.shared.generated.resources.difference_balanced
import poskmp.shared.generated.resources.difference_shortage
import poskmp.shared.generated.resources.difference_surplus
import poskmp.shared.generated.resources.expected_cash_label
import poskmp.shared.generated.resources.initial_cash_label
import poskmp.shared.generated.resources.shift_cash_inflows
import poskmp.shared.generated.resources.shift_cash_outflows
import poskmp.shared.generated.resources.shift_cash_sales
import poskmp.shared.generated.resources.shift_credit_sales
import poskmp.shared.generated.resources.shift_summary_cash_section
import poskmp.shared.generated.resources.shift_summary_other_payments_section
import poskmp.shared.generated.resources.shift_total_sales
import poskmp.shared.generated.resources.shift_total_tickets
import poskmp.shared.generated.resources.warning

@Composable
fun CloseShiftDialog(
    summary: ShiftSummary,
    isClosing: Boolean,
    errorMessage: String?,
    onConfirmClose: (countedCash: Double, notes: String?) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var countedCashText by remember { mutableStateOf("") }
    var notesText by remember { mutableStateOf("") }

    val countedCash = countedCashText.toDoubleOrNull()
    val expectedCash = summary.efectivoEsperado
    val difference = if (countedCash != null) countedCash - expectedCash else null

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier.width(520.dp),
        title = {
            Column {
                Text(
                    text = stringResource(Res.string.close_shift_dialog_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(
                        Res.string.close_shift_dialog_subtitle,
                        summary.shift.cashierName,
                        formatEpochMillisToDateTime(summary.shift.startTime)
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Card 1: Balance de Efectivo en Caja
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = stringResource(Res.string.shift_summary_cash_section),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        ShiftRowItem(
                            label = stringResource(Res.string.initial_cash_label),
                            value = summary.shift.initialCash.toString().formatPrice()
                        )
                        ShiftRowItem(
                            label = stringResource(Res.string.shift_cash_sales),
                            value = "(+) ${summary.ventasEfectivo.toString().formatPrice()}",
                            valueColor = Color(0xFF10B981)
                        )
                        if (summary.totalEntradas > 0.0) {
                            ShiftRowItem(
                                label = stringResource(Res.string.shift_cash_inflows),
                                value = "(+) ${summary.totalEntradas.toString().formatPrice()}",
                                valueColor = Color(0xFF10B981)
                            )
                        }
                        if (summary.totalSalidas > 0.0) {
                            ShiftRowItem(
                                label = stringResource(Res.string.shift_cash_outflows),
                                value = "(-) ${summary.totalSalidas.toString().formatPrice()}",
                                valueColor = MaterialTheme.colorScheme.error
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        // Efectivo Esperado Destacado
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(Res.string.expected_cash_label),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = expectedCash.toString().formatPrice(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Card 2: Otros medios de pago y total ventas
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = stringResource(Res.string.shift_summary_other_payments_section),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        ShiftRowItem(
                            label = stringResource(Res.string.shift_total_sales),
                            value = summary.totalVentas.toString().formatPrice(),
                            bold = true
                        )
                        ShiftRowItem(
                            label = stringResource(Res.string.shift_total_tickets),
                            value = summary.totalTransacciones.toString()
                        )
                        if (summary.ventasTarjeta > 0.0) {
                            ShiftRowItem(label = "Tarjeta:", value = summary.ventasTarjeta.toString().formatPrice())
                        }
                        if (summary.ventasTransferencia > 0.0) {
                            ShiftRowItem(label = "Transferencia:", value = summary.ventasTransferencia.toString().formatPrice())
                        }
                        if (summary.ventasCredito > 0.0) {
                            ShiftRowItem(label = stringResource(Res.string.shift_credit_sales), value = summary.ventasCredito.toString().formatPrice())
                        }
                    }
                }

                // Campo: Efectivo Contado
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedTextField(
                        value = countedCashText,
                        onValueChange = { countedCashText = it.filter { char -> char.isDigit() || char == '.' } },
                        label = { Text(stringResource(Res.string.counted_cash_label)) },
                        prefix = { Text("$ ", fontWeight = FontWeight.Bold) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium
                    )

                    // Indicador de Diferencia
                    if (difference != null) {
                        val (diffText, diffColor) = when {
                            kotlin.math.abs(difference) < 0.01 -> Pair(
                                stringResource(Res.string.difference_balanced),
                                Color(0xFF10B981)
                            )
                            difference > 0.0 -> Pair(
                                stringResource(Res.string.difference_surplus, difference.toString().formatPrice()),
                                Color(0xFF0284C7)
                            )
                            else -> Pair(
                                stringResource(Res.string.difference_shortage, kotlin.math.abs(difference).toString().formatPrice()),
                                MaterialTheme.colorScheme.error
                            )
                        }

                        Surface(
                            color = diffColor.copy(alpha = 0.12f),
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                        ) {
                            Text(
                                text = diffText,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = diffColor,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // Notas u observaciones
                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text(stringResource(Res.string.close_shift_notes_label)) },
                    placeholder = { Text(stringResource(Res.string.close_shift_notes_placeholder)) },
                    singleLine = false,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                )

                // Mensaje de Error
                if (errorMessage != null) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.warning),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = errorMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (countedCash != null) {
                        onConfirmClose(countedCash, notesText.ifBlank { null })
                    }
                },
                enabled = countedCash != null && !isClosing,
                shape = MaterialTheme.shapes.small
            ) {
                if (isClosing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(stringResource(Res.string.close_shift_button))
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                enabled = !isClosing,
                shape = MaterialTheme.shapes.small
            ) {
                Text(stringResource(Res.string.cancel))
            }
        }
    )
}

@Composable
private fun ShiftRowItem(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    bold: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium,
            color = valueColor
        )
    }
}
