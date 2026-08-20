package com.dnavarro.poskmp.ui.clientes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.dnavarro.poskmp.domain.model.Customer
import org.jetbrains.compose.resources.stringResource
import poskmp.shared.generated.resources.Res
import poskmp.shared.generated.resources.accept_button
import poskmp.shared.generated.resources.cancel
import poskmp.shared.generated.resources.customer_name_required_error
import poskmp.shared.generated.resources.dialog_edit_customer_title
import poskmp.shared.generated.resources.dialog_new_customer_title
import poskmp.shared.generated.resources.field_customer_address
import poskmp.shared.generated.resources.field_customer_credit_limit
import poskmp.shared.generated.resources.field_customer_credit_limit_hint
import poskmp.shared.generated.resources.field_customer_name
import poskmp.shared.generated.resources.field_customer_notes
import poskmp.shared.generated.resources.field_customer_phone

@Composable
fun CustomerFormDialog(
    customer: Customer?,
    onDismissRequest: () -> Unit,
    onSave: (id: String?, nombre: String, telefono: String, direccion: String, notas: String, limiteCredito: Double) -> Unit
) {
    val isEdit = customer != null
    var nombre by remember(customer) { mutableStateOf(customer?.nombre ?: "") }
    var telefono by remember(customer) { mutableStateOf(customer?.telefono ?: "") }
    var direccion by remember(customer) { mutableStateOf(customer?.direccion ?: "") }
    var notas by remember(customer) { mutableStateOf(customer?.notas ?: "") }
    var limiteCreditoText by remember(customer) {
        mutableStateOf(
            if (customer != null && customer.limiteCredito > 0.0) {
                if (customer.limiteCredito % 1.0 == 0.0) customer.limiteCredito.toInt().toString() else customer.limiteCredito.toString()
            } else ""
        )
    }
    var hasAttemptedSave by remember { mutableStateOf(false) }
    val isNombreValid = nombre.trim().isNotEmpty()

    AlertDialog(
        onDismissRequest = onDismissRequest,
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        title = {
            Text(
                text = stringResource(if (isEdit) Res.string.dialog_edit_customer_title else Res.string.dialog_new_customer_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text(stringResource(Res.string.field_customer_name)) },
                    isError = hasAttemptedSave && !isNombreValid,
                    supportingText = if (hasAttemptedSave && !isNombreValid) {
                        { Text(stringResource(Res.string.customer_name_required_error)) }
                    } else null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = telefono,
                    onValueChange = { telefono = it },
                    label = { Text(stringResource(Res.string.field_customer_phone)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = direccion,
                    onValueChange = { direccion = it },
                    label = { Text(stringResource(Res.string.field_customer_address)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = limiteCreditoText,
                    onValueChange = { text ->
                        if (text.isEmpty() || text.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                            limiteCreditoText = text
                        }
                    },
                    label = { Text(stringResource(Res.string.field_customer_credit_limit)) },
                    supportingText = { Text(stringResource(Res.string.field_customer_credit_limit_hint)) },
                    prefix = { Text("$ ") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notas,
                    onValueChange = { notas = it },
                    label = { Text(stringResource(Res.string.field_customer_notes)) },
                    minLines = 2,
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    hasAttemptedSave = true
                    if (isNombreValid) {
                        val limite = limiteCreditoText.toDoubleOrNull() ?: 0.0
                        onSave(customer?.id, nombre, telefono, direccion, notas, limite)
                    }
                }
            ) {
                Text(stringResource(Res.string.accept_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(Res.string.cancel))
            }
        }
    )
}
