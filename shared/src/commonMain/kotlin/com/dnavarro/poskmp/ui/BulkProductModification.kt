package com.dnavarro.poskmp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.dnavarro.poskmp.db.Products
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import poskmp.shared.generated.resources.Res
import poskmp.shared.generated.resources.apply_changes_button
import poskmp.shared.generated.resources.bulk_deactivate_confirmation
import poskmp.shared.generated.resources.bulk_delete_confirmation
import poskmp.shared.generated.resources.bulk_enter_category_error
import poskmp.shared.generated.resources.bulk_invalid_value_error
import poskmp.shared.generated.resources.bulk_label_cost_price
import poskmp.shared.generated.resources.bulk_label_retail_price
import poskmp.shared.generated.resources.bulk_label_retail_profit
import poskmp.shared.generated.resources.bulk_label_wholesale_price
import poskmp.shared.generated.resources.bulk_label_wholesale_profit
import poskmp.shared.generated.resources.bulk_mod_subtitle
import poskmp.shared.generated.resources.bulk_op_change_category_title
import poskmp.shared.generated.resources.bulk_op_change_prices_title
import poskmp.shared.generated.resources.bulk_op_deactivate_title
import poskmp.shared.generated.resources.bulk_op_delete_title
import poskmp.shared.generated.resources.bulk_op_set_profit_title
import poskmp.shared.generated.resources.bulk_profit_cost_requirement
import poskmp.shared.generated.resources.bulk_select_price_error
import poskmp.shared.generated.resources.bulk_select_profit_error
import poskmp.shared.generated.resources.cancel
import poskmp.shared.generated.resources.cost_price
import poskmp.shared.generated.resources.delete_button
import poskmp.shared.generated.resources.new_category
import poskmp.shared.generated.resources.retail_price
import poskmp.shared.generated.resources.retail_profit_pct
import poskmp.shared.generated.resources.wholesale_price
import poskmp.shared.generated.resources.wholesale_profit_pct

enum class BulkProductOperation(val titleRes: StringResource) {
    CHANGE_PRICES(Res.string.bulk_op_change_prices_title),
    SET_PROFIT(Res.string.bulk_op_set_profit_title),
    CHANGE_CATEGORY(Res.string.bulk_op_change_category_title),
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
    BulkProductOperation.DEACTIVATE -> product.copy(activo = 0L)
    BulkProductOperation.DELETE -> null
}

@Composable
fun BulkProductModificationDialog(
    selectedCount: Int,
    operation: BulkProductOperation,
    onDismiss: () -> Unit,
    onApply: (BulkProductModification) -> Unit
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

    val costPriceLabel = stringResource(Res.string.bulk_label_cost_price)
    val retailPriceLabel = stringResource(Res.string.bulk_label_retail_price)
    val wholesalePriceLabel = stringResource(Res.string.bulk_label_wholesale_price)
    val retailProfitLabel = stringResource(Res.string.bulk_label_retail_profit)
    val wholesaleProfitLabel = stringResource(Res.string.bulk_label_wholesale_profit)
    val selectPriceErr = stringResource(Res.string.bulk_select_price_error)
    val selectProfitErr = stringResource(Res.string.bulk_select_profit_error)
    val enterCategoryErr = stringResource(Res.string.bulk_enter_category_error)

    fun numberOrError(text: String, labelStr: String, errorFmt: String): Double? {
        return text.replace(',', '.').toDoubleOrNull()?.takeIf { it >= 0 } ?: run {
            errorMessage = errorFmt.replace($$"%1$s", labelStr)
            null
        }
    }

    val invalidValueFmt = stringResource(Res.string.bulk_invalid_value_error)

    fun createModification(): BulkProductModification? {
        errorMessage = null
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

            BulkProductOperation.DEACTIVATE, BulkProductOperation.DELETE -> BulkProductModification(operation)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
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
                        PriceOption(stringResource(Res.string.cost_price), changeCost, { changeCost = it }, costText, { costText = it })
                        PriceOption(stringResource(Res.string.retail_price), changeRetail, { changeRetail = it }, retailText, { retailText = it })
                        PriceOption(stringResource(Res.string.wholesale_price), changeWholesale, { changeWholesale = it }, wholesaleText, { wholesaleText = it })
                    }

                    BulkProductOperation.SET_PROFIT -> {
                        Text(stringResource(Res.string.bulk_profit_cost_requirement))
                        DecimalInput(stringResource(Res.string.retail_profit_pct), retailProfitText) { retailProfitText = it }
                        DecimalInput(stringResource(Res.string.wholesale_profit_pct), wholesaleProfitText) { wholesaleProfitText = it }
                    }

                    BulkProductOperation.CHANGE_CATEGORY -> OutlinedTextField(
                        value = categoryText,
                        onValueChange = { categoryText = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(Res.string.new_category)) },
                        singleLine = true
                    )

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
            Button(onClick = {
                createModification()?.let(onApply)
            }) {
                Text(
                    if (operation == BulkProductOperation.DELETE) stringResource(Res.string.delete_button)
                    else stringResource(Res.string.apply_changes_button)
                )
            }
        }
    )
}

@Composable
private fun PriceOption(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, value: String, onValueChange: (String) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        OutlinedTextField(
            value = value,
            onValueChange = { input ->
                if (input.isEmpty() || input.matches(Regex("^-?\\d*\\.?\\d{0,2}$"))) {
                    onValueChange(input)
                }
            },
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp),
            prefix = { Text("$") },
            label = { Text(label) },
            enabled = checked,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )
    }
}

@Composable
private fun DecimalInput(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { input ->
            if (input.isEmpty() || input.matches(Regex("^-?\\d*\\.?\\d{0,2}$"))) {
                onValueChange(input)
            }
        },
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
    )
}