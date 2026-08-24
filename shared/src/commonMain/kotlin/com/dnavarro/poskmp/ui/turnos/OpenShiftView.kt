package com.dnavarro.poskmp.ui.turnos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dnavarro.poskmp.domain.model.Cashier
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import poskmp.shared.generated.resources.Res
import poskmp.shared.generated.resources.cashier_label
import poskmp.shared.generated.resources.initial_cash_label
import poskmp.shared.generated.resources.open_shift_button
import poskmp.shared.generated.resources.open_shift_dialog_subtitle
import poskmp.shared.generated.resources.open_shift_dialog_title
import poskmp.shared.generated.resources.person
import poskmp.shared.generated.resources.pin_label
import poskmp.shared.generated.resources.pin_placeholder
import poskmp.shared.generated.resources.warning

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpenShiftView(
    cashiers: List<Cashier>,
    isOpening: Boolean,
    errorMessage: String?,
    onOpenShift: (cashierId: String, pin: String, initialCash: Double) -> Unit,
    onClearError: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCashier by remember(cashiers) { mutableStateOf(cashiers.firstOrNull()) }
    var isDropdownExpanded by remember { mutableStateOf(false) }
    var pinText by remember { mutableStateOf("") }
    var initialCashText by remember { mutableStateOf("0") }

    val quickAmounts = listOf(0, 200, 500, 1000, 2000)

    LaunchedEffect(cashiers) {
        if (selectedCashier == null || !cashiers.any { it.id == selectedCashier?.id }) {
            selectedCashier = cashiers.firstOrNull()
        }
    }

    LaunchedEffect(pinText, initialCashText, selectedCashier) {
        if (errorMessage != null) {
            onClearError()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        ElevatedCard(
            modifier = Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 32.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Header Icon & Title
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = CircleShape,
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(Res.drawable.person),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.open_shift_dialog_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = stringResource(Res.string.open_shift_dialog_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                // Form Fields
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Selector de cajero
                    ExposedDropdownMenuBox(
                        expanded = isDropdownExpanded,
                        onExpandedChange = { isDropdownExpanded = !isDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedCashier?.nombre ?: stringResource(Res.string.cashier_label),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(Res.string.cashier_label)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            shape = MaterialTheme.shapes.medium,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )

                        ExposedDropdownMenu(
                            expanded = isDropdownExpanded,
                            onDismissRequest = { isDropdownExpanded = false }
                        ) {
                            cashiers.forEach { cashier ->
                                DropdownMenuItem(
                                    text = { Text(cashier.nombre, style = MaterialTheme.typography.bodyLarge) },
                                    onClick = {
                                        selectedCashier = cashier
                                        isDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Campo de PIN
                    OutlinedTextField(
                        value = pinText,
                        onValueChange = { if (it.length <= 8) pinText = it },
                        label = { Text(stringResource(Res.string.pin_label)) },
                        placeholder = { Text(stringResource(Res.string.pin_placeholder)) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium
                    )

                    // Campo de Fondo Inicial
                    Column {
                        OutlinedTextField(
                            value = initialCashText,
                            onValueChange = { initialCashText = it.filter { char -> char.isDigit() || char == '.' } },
                            label = { Text(stringResource(Res.string.initial_cash_label)) },
                            prefix = { Text("$ ", fontWeight = FontWeight.Bold) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Chips de montos rápidos
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            quickAmounts.forEach { amount ->
                                val isSelected = initialCashText.toDoubleOrNull() == amount.toDouble()
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { initialCashText = amount.toString() },
                                    label = { Text("$$amount", fontSize = 12.sp) },
                                    shape = FilterChipDefaults.shape
                                )
                            }
                        }
                    }

                    // Mensaje de Error si ocurrió alguno
                    if (errorMessage != null) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(Res.drawable.warning),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = errorMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }

                // Botón Iniciar Turno
                Button(
                    onClick = {
                        val cashier = selectedCashier
                        val amount = initialCashText.toDoubleOrNull() ?: 0.0
                        if (cashier != null) {
                            onOpenShift(cashier.id, pinText, amount)
                        }
                    },
                    enabled = selectedCashier != null && pinText.isNotBlank() && !isOpening,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    if (isOpening) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                    }
                    Text(
                        text = stringResource(Res.string.open_shift_button),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
