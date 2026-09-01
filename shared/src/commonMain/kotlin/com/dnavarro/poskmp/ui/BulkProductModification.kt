package com.dnavarro.poskmp.ui

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dnavarro.poskmp.db.Products
import com.dnavarro.poskmp.util.isAndroid
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import poskmp.shared.generated.resources.*

enum class BulkProductOperation(val titleRes: StringResource) {
    CHANGE_PRICES(Res.string.bulk_op_change_prices_title),
    SET_PROFIT(Res.string.bulk_op_set_profit_title),
    CHANGE_CATEGORY(Res.string.bulk_op_change_category_title),
    MARK_AS_FAVORITE(Res.string.bulk_op_mark_as_favorite_title),
    DEACTIVATE(Res.string.bulk_op_deactivate_title),
    DELETE(Res.string.bulk_op_delete_title)
}

data class BulkProductModification(
    val operation: BulkProductOperation,
    val costPrice: Double? = null,
    val retailPrice: Double? = null,
    val wholesalePrice: Double? = null,
    val retailProfitPercentage: Double? = null,
    val wholesaleProfitPercentage: Double? = null,
    val category: String? = null
)

fun applyBulkProductModification(product: Products, modification: BulkProductModification): Products? = when (modification.operation) {
    BulkProductOperation.CHANGE_PRICES -> product.copy(
        costo = modification.costPrice ?: product.costo,
        precio = modification.retailPrice ?: product.precio,
        precio_mayoreo = modification.wholesalePrice ?: product.precio_mayoreo
    )

    BulkProductOperation.SET_PROFIT -> if (product.costo > 0.0) {
        product.copy(
            precio = modification.retailProfitPercentage?.let { product.costo * (1 + it / 100) } ?: product.precio,
            precio_mayoreo = modification.wholesaleProfitPercentage?.let { product.costo * (1 + it / 100) } ?: product.precio_mayoreo
        )
    } else {
        product
    }

    BulkProductOperation.CHANGE_CATEGORY -> product.copy(categoria = modification.category)
    BulkProductOperation.MARK_AS_FAVORITE -> product.copy(es_favorito = 1L)
    BulkProductOperation.DEACTIVATE -> product.copy(activo = 0L)
    BulkProductOperation.DELETE -> null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BulkProductModificationDialog(
    selectedCount: Int,
    operation: BulkProductOperation,
    onDismiss: () -> Unit,
    onApply: (BulkProductModification) -> Unit,
    existingCategories: List<String> = emptyList()
) {
    var changeCost by remember { mutableStateOf(false) }
    var changeRetail by remember { mutableStateOf(true) }
    var changeWholesale by remember { mutableStateOf(false) }
    var costText by remember { mutableStateOf("") }
    var retailText by remember { mutableStateOf("") }
    var wholesaleText by remember { mutableStateOf("") }
    var retailProfitText by remember { mutableStateOf("") }
    var wholesaleProfitText by remember { mutableStateOf("") }
    var categoryText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val defaultCategory = stringResource(Res.string.default_category_abarrotes)
    val noCategoryStr = stringResource(Res.string.no_category)
    var categoryDropdownExpanded by remember { mutableStateOf(false) }

    val allCategories = remember(existingCategories, defaultCategory) {
        (existingCategories + defaultCategory)
            .filter { it.isNotBlank() && it != noCategoryStr }
            .distinct()
            .sorted()
    }
    val filteredCategories = remember(allCategories, categoryText) {
        val trimmed = categoryText.trim()
        if (trimmed.isEmpty()) {
            allCategories
        } else {
            val matches = allCategories.filter { it.contains(trimmed, ignoreCase = true) }
            if (matches.size == 1 && matches.first().equals(trimmed, ignoreCase = true)) {
                allCategories
            } else matches.ifEmpty {
                emptyList()
            }
        }
    }

    val firstFocusRequester = remember { FocusRequester() }
    val confirmButtonFocusRequester = remember { FocusRequester() }

    val hasTextField = when (operation) {
        BulkProductOperation.CHANGE_PRICES,
        BulkProductOperation.SET_PROFIT,
        BulkProductOperation.CHANGE_CATEGORY -> true
        else -> false
    }

    LaunchedEffect(operation) {
        if (!isAndroid()) {
            delay(100.milliseconds)
            try {
                if (hasTextField) {
                    firstFocusRequester.requestFocus()
                } else {
                    confirmButtonFocusRequester.requestFocus()
                }
            } catch (_: Exception) {}
        }
    }

    val costPriceLabel = stringResource(Res.string.bulk_label_cost_price)
    val retailPriceLabel = stringResource(Res.string.bulk_label_retail_price)
    val wholesalePriceLabel = stringResource(Res.string.bulk_label_wholesale_price)
    val retailProfitLabel = stringResource(Res.string.bulk_label_retail_profit)
    val wholesaleProfitLabel = stringResource(Res.string.bulk_label_wholesale_profit)
    val selectPriceErr = stringResource(Res.string.bulk_select_price_error)
    val selectProfitErr = stringResource(Res.string.bulk_select_profit_error)
    val enterCategoryErr = stringResource(Res.string.bulk_enter_category_error)

    val costVal = if (changeCost) costText.replace(',', '.').toDoubleOrNull() else null
    val retailVal = if (changeRetail) retailText.replace(',', '.').toDoubleOrNull() else null
    val wholesaleVal = if (changeWholesale) wholesaleText.replace(',', '.').toDoubleOrNull() else null

    val wholesalePriceError: String? = when {
        operation == BulkProductOperation.CHANGE_PRICES &&
            costVal != null && costVal > 0.0 && wholesaleVal != null && wholesaleVal > 0.0 && wholesaleVal < costVal -> {
            stringResource(Res.string.error_wholesale_less_than_cost)
        }
        else -> null
    }

    val retailPriceError: String? = when {
        operation == BulkProductOperation.CHANGE_PRICES &&
            costVal != null && costVal > 0.0 && retailVal != null && retailVal > 0.0 && retailVal < costVal -> {
            stringResource(Res.string.error_retail_less_than_cost)
        }
        operation == BulkProductOperation.CHANGE_PRICES &&
            wholesaleVal != null && wholesaleVal > 0.0 && retailVal != null && retailVal > 0.0 && retailVal < wholesaleVal -> {
            stringResource(Res.string.error_retail_less_than_wholesale)
        }
        else -> null
    }

    val retailProfitVal = if (operation == BulkProductOperation.SET_PROFIT) retailProfitText.replace(',', '.').toDoubleOrNull() else null
    val wholesaleProfitVal = if (operation == BulkProductOperation.SET_PROFIT) wholesaleProfitText.replace(',', '.').toDoubleOrNull() else null

    val profitError: String? = when {
        operation == BulkProductOperation.SET_PROFIT && wholesaleProfitVal != null && wholesaleProfitVal < 0.0 -> {
            stringResource(Res.string.error_wholesale_less_than_cost)
        }
        operation == BulkProductOperation.SET_PROFIT && retailProfitVal != null && retailProfitVal < 0.0 -> {
            stringResource(Res.string.error_retail_less_than_cost)
        }
        operation == BulkProductOperation.SET_PROFIT && retailProfitVal != null && wholesaleProfitVal != null && retailProfitVal < wholesaleProfitVal -> {
            stringResource(Res.string.error_retail_less_than_wholesale)
        }
        else -> null
    }

    val isPriceModificationValid = wholesalePriceError == null && retailPriceError == null && profitError == null

    fun numberOrError(text: String, labelStr: String, errorFmt: String): Double? {
        return text.replace(',', '.').toDoubleOrNull()?.takeIf { it >= 0 } ?: run {
            errorMessage = errorFmt.replace("%1\$s", labelStr)
            null
        }
    }

    val invalidValueFmt = stringResource(Res.string.bulk_invalid_value_error)

    fun createModification(): BulkProductModification? {
        errorMessage = null
        if (!isPriceModificationValid) return null
        return when (operation) {
            BulkProductOperation.CHANGE_PRICES -> {
                if (!changeCost && !changeRetail && !changeWholesale) {
                    errorMessage = selectPriceErr
                    null
                } else {
                    val cost = if (changeCost) numberOrError(costText, costPriceLabel, invalidValueFmt) else 0.0
                    val retail = if (changeRetail) numberOrError(retailText, retailPriceLabel, invalidValueFmt) else 0.0
                    val wholesale = if (changeWholesale) numberOrError(wholesaleText, wholesalePriceLabel, invalidValueFmt) else 0.0
                    if (errorMessage == null) BulkProductModification(
                        operation = operation,
                        costPrice = if (changeCost) cost else null,
                        retailPrice = if (changeRetail) retail else null,
                        wholesalePrice = if (changeWholesale) wholesale else null
                    ) else null
                }
            }

            BulkProductOperation.SET_PROFIT -> {
                val retail = retailProfitText.takeIf { it.isNotBlank() }?.let { numberOrError(it, retailProfitLabel, invalidValueFmt) }
                val wholesale = wholesaleProfitText.takeIf { it.isNotBlank() }?.let { numberOrError(it, wholesaleProfitLabel, invalidValueFmt) }
                if (errorMessage == null && retail == null && wholesale == null) {
                    errorMessage = selectProfitErr
                }
                if (errorMessage == null) BulkProductModification(operation, retailProfitPercentage = retail, wholesaleProfitPercentage = wholesale) else null
            }

            BulkProductOperation.CHANGE_CATEGORY -> {
                val category = categoryText.trim()
                if (category.isEmpty()) {
                    errorMessage = enterCategoryErr
                    null
                } else BulkProductModification(operation, category = category)
            }

            BulkProductOperation.MARK_AS_FAVORITE, BulkProductOperation.DEACTIVATE, BulkProductOperation.DELETE -> BulkProductModification(operation)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.then(
            if (!isAndroid()) {
                Modifier
                    .focusable()
                    .onPreviewKeyEvent { keyEvent ->
                        keyEvent.type == KeyEventType.KeyDown && when (keyEvent.key) {
                            Key.Enter, Key.NumPadEnter -> {
                                createModification()?.let(onApply)
                                true
                            }

                            else -> false
                        }
                    }
            } else Modifier
        ),
        title = { Text(stringResource(operation.titleRes)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    stringResource(Res.string.bulk_mod_subtitle, selectedCount),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HorizontalDivider()
                when (operation) {
                    BulkProductOperation.CHANGE_PRICES -> {
                        PriceOption(
                            label = stringResource(Res.string.cost_price),
                            checked = changeCost,
                            onCheckedChange = { changeCost = it },
                            value = costText,
                            onValueChange = { costText = it }
                        )
                        PriceOption(
                            label = stringResource(Res.string.retail_price),
                            checked = changeRetail,
                            onCheckedChange = { changeRetail = it },
                            value = retailText,
                            onValueChange = { retailText = it },
                            isError = retailPriceError != null,
                            modifier = if (!isAndroid()) Modifier.focusRequester(firstFocusRequester) else Modifier
                        )
                        if (retailPriceError != null) {
                            Text(
                                text = retailPriceError,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(start = 48.dp)
                            )
                        }
                        PriceOption(
                            label = stringResource(Res.string.wholesale_price),
                            checked = changeWholesale,
                            onCheckedChange = { changeWholesale = it },
                            value = wholesaleText,
                            onValueChange = { wholesaleText = it },
                            isError = wholesalePriceError != null
                        )
                        if (wholesalePriceError != null) {
                            Text(
                                text = wholesalePriceError,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(start = 48.dp)
                            )
                        }
                    }

                    BulkProductOperation.SET_PROFIT -> {
                        Text(stringResource(Res.string.bulk_profit_cost_requirement))
                        DecimalInput(
                            label = stringResource(Res.string.retail_profit_pct),
                            value = retailProfitText,
                            isError = profitError != null,
                            modifier = if (!isAndroid()) Modifier.focusRequester(firstFocusRequester) else Modifier
                        ) { retailProfitText = it }
                        DecimalInput(
                            label = stringResource(Res.string.wholesale_profit_pct),
                            value = wholesaleProfitText,
                            isError = profitError != null
                        ) { wholesaleProfitText = it }
                        if (profitError != null) {
                            Text(
                                text = profitError,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    BulkProductOperation.CHANGE_CATEGORY -> {
                        ExposedDropdownMenuBox(
                            expanded = categoryDropdownExpanded,
                            onExpandedChange = { categoryDropdownExpanded = it },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = categoryText,
                                onValueChange = {
                                    categoryText = it
                                    categoryDropdownExpanded = true
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable)
                                    .then(if (!isAndroid()) Modifier.focusRequester(firstFocusRequester) else Modifier),
                                label = { Text(stringResource(Res.string.category_label), style = MaterialTheme.typography.labelLarge) },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded)
                                },
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                singleLine = true
                            )
                            if (filteredCategories.isNotEmpty() || (categoryText.trim().isNotEmpty() && !allCategories.any { it.equals(categoryText.trim(), ignoreCase = true) })) {
                                ExposedDropdownMenu(
                                    expanded = categoryDropdownExpanded,
                                    onDismissRequest = { categoryDropdownExpanded = false }
                                ) {
                                    filteredCategories.forEach { category ->
                                        val isSelected = category.equals(categoryText.trim(), ignoreCase = true)
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = category,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                )
                                            },
                                            trailingIcon = if (isSelected) {
                                                {
                                                    Icon(
                                                        painter = painterResource(Res.drawable.check),
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            } else null,
                                            onClick = {
                                                categoryText = category
                                                categoryDropdownExpanded = false
                                            },
                                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                        )
                                    }
                                    if (filteredCategories.isEmpty() && categoryText.trim().isNotEmpty()) {
                                        DropdownMenuItem(
                                            text = {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        painter = painterResource(Res.drawable.add),
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = "${stringResource(Res.string.new_category)}: \"${categoryText.trim()}\"",
                                                        fontWeight = FontWeight.Medium,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            },
                                            onClick = {
                                                categoryDropdownExpanded = false
                                            },
                                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                        )
                                    }
                                }
                            }
                        }
                    }

                    BulkProductOperation.MARK_AS_FAVORITE -> Text(stringResource(Res.string.bulk_favorite_confirmation, selectedCount))
                    BulkProductOperation.DEACTIVATE -> Text(stringResource(Res.string.bulk_deactivate_confirmation, selectedCount))
                    BulkProductOperation.DELETE -> Text(
                        stringResource(Res.string.bulk_delete_confirmation, selectedCount),
                        color = MaterialTheme.colorScheme.error
                    )
                }
                errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) }
        },
        confirmButton = {
            Button(
                onClick = {
                    createModification()?.let(onApply)
                },
                enabled = isPriceModificationValid,
                modifier = if (!isAndroid() && !hasTextField) Modifier.focusRequester(confirmButtonFocusRequester) else Modifier
            ) {
                Text(
                    if (isAndroid()) {
                        if (operation == BulkProductOperation.DELETE) stringResource(Res.string.delete_button)
                        else stringResource(Res.string.apply_changes_button)
                    } else {
                        if (operation == BulkProductOperation.DELETE) stringResource(Res.string.delete_button_desktop)
                        else stringResource(Res.string.apply_changes_button_desktop)
                    }
                )
            }
        }
    )
}

@Composable
private fun PriceOption(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        OutlinedTextField(
            value = value,
            onValueChange = { input ->
                if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                    onValueChange(input)
                }
            },
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp).then(modifier),
            prefix = { Text("$") },
            label = { Text(label) },
            enabled = checked,
            isError = isError,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )
    }
}

@Composable
private fun DecimalInput(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = { input ->
            if (input.isEmpty() || input.matches(Regex("^-?\\d*\\.?\\d{0,2}$"))) {
                onValueChange(input)
            }
        },
        modifier = Modifier.fillMaxWidth().then(modifier),
        label = { Text(label) },
        isError = isError,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
    )
}