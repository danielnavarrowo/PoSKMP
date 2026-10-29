package com.dnavarro.poskmp.ui.clientes

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import com.dnavarro.poskmp.theme.ShapeDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dnavarro.poskmp.domain.model.Customer
import com.dnavarro.poskmp.domain.model.CustomerDiscountType
import com.dnavarro.poskmp.util.isAndroid
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds
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
import poskmp.shared.generated.resources.*

@Composable
fun CustomerFormDialog(
    customer: Customer?,
    onDismissRequest: () -> Unit,
    onSave: (
        id: String?,
        nombre: String,
        telefono: String,
        direccion: String,
        notas: String,
        limiteCredito: Double,
        siempreMayoreo: Boolean,
        tipoDescuento: CustomerDiscountType,
        porcentajeDescuento: Double
    ) -> Unit
) {
    val isEdit = customer != null
    var nombre by remember(customer) { mutableStateOf(customer?.nombre ?: "") }
    var telefono by remember(customer) { mutableStateOf(customer?.telefono ?: "") }
    var direccion by remember(customer) { mutableStateOf(customer?.direccion ?: "") }
    var notas by remember(customer) { mutableStateOf(customer?.notas ?: "") }

    var hasSpecialPricing by remember(customer) {
        mutableStateOf(
            customer?.hasSpecialPricing == true || customer?.siempreMayoreo == true
        )
    }
    var isCustomDiscount by remember(customer) {
        mutableStateOf(
            customer?.effectiveDiscountType == CustomerDiscountType.COST_PLUS_PERCENTAGE ||
            customer?.effectiveDiscountType == CustomerDiscountType.RETAIL_MINUS_PERCENTAGE
        )
    }
    var customDiscountType by remember(customer) {
        mutableStateOf(
            if (customer?.effectiveDiscountType == CustomerDiscountType.RETAIL_MINUS_PERCENTAGE) {
                CustomerDiscountType.RETAIL_MINUS_PERCENTAGE
            } else {
                CustomerDiscountType.COST_PLUS_PERCENTAGE
            }
        )
    }
    var discountPercentageText by remember(customer) {
        mutableStateOf(
            if (customer != null && customer.porcentajeDescuento > 0.0) {
                if (customer.porcentajeDescuento % 1.0 == 0.0) customer.porcentajeDescuento.toInt().toString()
                else customer.porcentajeDescuento.toString()
            } else ""
        )
    }

    var limiteCreditoText by remember(customer) {
        mutableStateOf(
            if (customer != null && customer.limiteCredito > 0.0) {
                if (customer.limiteCredito % 1.0 == 0.0) customer.limiteCredito.toInt().toString() else customer.limiteCredito.toString()
            } else ""
        )
    }
    var hasAttemptedSave by remember { mutableStateOf(false) }
    val isNombreValid = nombre.trim().isNotEmpty()

    val parsedPercentage = discountPercentageText.toDoubleOrNull()
    val isPercentageValid = !hasSpecialPricing || !isCustomDiscount || parsedPercentage != null && parsedPercentage > 0.0 &&
            (customDiscountType != CustomerDiscountType.RETAIL_MINUS_PERCENTAGE || parsedPercentage < 100.0)

    val nombreFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        if (!isAndroid()) {
            delay(100.milliseconds)
            try {
                nombreFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    fun attemptSave() {
        hasAttemptedSave = true
        if (isNombreValid && isPercentageValid) {
            val limite = limiteCreditoText.toDoubleOrNull() ?: 0.0
            val resolvedType = when {
                !hasSpecialPricing -> CustomerDiscountType.NONE
                !isCustomDiscount -> CustomerDiscountType.WHOLESALE
                else -> customDiscountType
            }
            val resolvedPercentage = if (hasSpecialPricing && isCustomDiscount) {
                parsedPercentage ?: 0.0
            } else {
                0.0
            }
            val resolvedSiempreMayoreo = resolvedType == CustomerDiscountType.WHOLESALE

            onSave(
                customer?.id,
                nombre,
                telefono,
                direccion,
                notas,
                limite,
                resolvedSiempreMayoreo,
                resolvedType,
                resolvedPercentage
            )
        }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = Modifier
            .widthIn(max = 500.dp)
            .fillMaxWidth()
            .then(
                if (!isAndroid()) {
                    Modifier
                        .focusable()
                        .onPreviewKeyEvent { keyEvent ->
                            keyEvent.type == KeyEventType.KeyDown && when (keyEvent.key) {
                                Key.Enter, Key.NumPadEnter -> {
                                    attemptSave()
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (!isAndroid()) Modifier.focusRequester(nombreFocusRequester) else Modifier)
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

                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text(
                                    text = stringResource(Res.string.customer_special_pricing_title),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(Res.string.customer_special_pricing_subtitle),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = hasSpecialPricing,
                                onCheckedChange = { hasSpecialPricing = it }
                            )
                        }

                        if (hasSpecialPricing) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                val modeOptions = listOf(
                                    Pair(false, stringResource(Res.string.customer_pricing_mode_wholesale)),
                                    Pair(true, stringResource(Res.string.customer_pricing_mode_custom))
                                )
                                modeOptions.forEachIndexed { index, (customOption, label) ->
                                    val isSelected = isCustomDiscount == customOption
                                    ToggleButton(
                                        checked = isSelected,
                                        onCheckedChange = { isCustomDiscount = customOption },
                                        colors = ToggleButtonDefaults.colors(
                                            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            checkedContainerColor = MaterialTheme.colorScheme.primary,
                                            checkedContentColor = MaterialTheme.colorScheme.onPrimary
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .semantics { role = Role.RadioButton },
                                        shapes = when (index) {
                                            0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                            modeOptions.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                                        }
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            if (!isCustomDiscount) {
                                Text(
                                    text = stringResource(Res.string.field_customer_always_wholesale_hint),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    val subOptions = listOf(
                                        Pair(CustomerDiscountType.COST_PLUS_PERCENTAGE, stringResource(Res.string.customer_pricing_cost_plus)),
                                        Pair(CustomerDiscountType.RETAIL_MINUS_PERCENTAGE, stringResource(Res.string.customer_pricing_retail_minus))
                                    )
                                    subOptions.forEachIndexed { index, (typeOption, label) ->
                                        val isSelected = customDiscountType == typeOption
                                        ToggleButton(
                                            checked = isSelected,
                                            onCheckedChange = { customDiscountType = typeOption },
                                            colors = ToggleButtonDefaults.colors(
                                                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                                checkedContainerColor = MaterialTheme.colorScheme.primary,
                                                checkedContentColor = MaterialTheme.colorScheme.onPrimary
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .semantics { role = Role.RadioButton },
                                            shapes = when (index) {
                                                0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                                subOptions.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                                else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                                            }
                                        ) {
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }

                                val formulaHint = when (customDiscountType) {
                                    CustomerDiscountType.COST_PLUS_PERCENTAGE ->
                                        stringResource(Res.string.customer_pricing_cost_plus_formula_hint)
                                    CustomerDiscountType.RETAIL_MINUS_PERCENTAGE ->
                                        stringResource(Res.string.customer_pricing_retail_minus_formula_hint)
                                    else -> ""
                                }
                                Text(
                                    text = formulaHint,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                val isError = hasAttemptedSave && !isPercentageValid
                                OutlinedTextField(
                                    value = discountPercentageText,
                                    onValueChange = { text ->
                                        if (text.isEmpty() || text.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                                            discountPercentageText = text
                                        }
                                    },
                                    label = {
                                        Text(
                                            if (customDiscountType == CustomerDiscountType.COST_PLUS_PERCENTAGE) {
                                                stringResource(Res.string.customer_pricing_percentage_markup_label)
                                            } else {
                                                stringResource(Res.string.customer_pricing_percentage_discount_label)
                                            }
                                        )
                                    },
                                    suffix = { Text("%") },
                                    singleLine = true,
                                    isError = isError,
                                    supportingText = if (isError) {
                                        {
                                            Text(
                                                if (parsedPercentage == null || parsedPercentage <= 0.0) {
                                                    stringResource(Res.string.customer_pricing_percentage_zero_error)
                                                } else {
                                                    stringResource(Res.string.customer_pricing_percentage_max_error)
                                                }
                                            )
                                        }
                                    } else null,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

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
                onClick = { attemptSave() }
            ) {
                Text(
                    if (isAndroid()) stringResource(Res.string.accept_button)
                    else "${stringResource(Res.string.accept_button)} (Enter)"
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

@Composable
fun CustomerFormDialog(
    customer: Customer?,
    onDismissRequest: () -> Unit,
    onSave: (
        id: String?,
        nombre: String,
        telefono: String,
        direccion: String,
        notas: String,
        limiteCredito: Double,
        siempreMayoreo: Boolean
    ) -> Unit
) = CustomerFormDialog(
    customer = customer,
    onDismissRequest = onDismissRequest,
    onSave = { id, nombre, telefono, direccion, notas, limiteCredito, siempreMayoreo, _, _ ->
        onSave(id, nombre, telefono, direccion, notas, limiteCredito, siempreMayoreo)
    }
)
