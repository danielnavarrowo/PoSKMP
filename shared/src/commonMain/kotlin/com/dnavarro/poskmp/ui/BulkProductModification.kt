package com.dnavarro.poskmp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.dnavarro.poskmp.db.Products

enum class BulkProductOperation(val title: String, val description: String) {
    CHANGE_PRICES("Cambiar precios", "Actualiza los precios que elijas y conserva los demás sin cambios."),
    SET_PROFIT("Establecer porcentaje de ganancia", "Calcula los precios de venta a partir del costo de cada producto."),
    CHANGE_CATEGORY("Modificar categoría", "Asigna la misma categoría a todos los productos seleccionados."),
    DEACTIVATE("Marcar como no activos", "Los productos dejarán de estar disponibles para la venta."),
    DELETE("Eliminar definitivamente", "Esta acción borra los productos seleccionados y no se puede deshacer.")
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
    onDismiss: () -> Unit,
    onApply: (BulkProductModification) -> Unit
) {
    var step by remember { mutableIntStateOf(1) }
    var operation by remember { mutableStateOf(BulkProductOperation.CHANGE_PRICES) }
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

    fun numberOrError(text: String, label: String): Double? {
        return text.replace(',', '.').toDoubleOrNull()?.takeIf { it >= 0 } ?: run {
            errorMessage = "Ingresa un valor válido para $label."
            null
        }
    }

    fun createModification(): BulkProductModification? {
        errorMessage = null
        return when (operation) {
            BulkProductOperation.CHANGE_PRICES -> {
                if (!changeCost && !changeRetail && !changeWholesale) {
                    errorMessage = "Elige al menos un precio para modificar."
                    null
                } else {
                    val cost = if (changeCost) numberOrError(costText, "el precio de costo") else 0.0
                    val retail = if (changeRetail) numberOrError(retailText, "el precio de venta") else 0.0
                    val wholesale = if (changeWholesale) numberOrError(wholesaleText, "el precio de mayoreo") else 0.0
                    if (errorMessage == null) BulkProductModification(
                        operation = operation,
                        costPrice = if (changeCost) cost else null,
                        retailPrice = if (changeRetail) retail else null,
                        wholesalePrice = if (changeWholesale) wholesale else null
                    ) else null
                }
            }

            BulkProductOperation.SET_PROFIT -> {
                val retail = retailProfitText.takeIf { it.isNotBlank() }?.let { numberOrError(it, "la ganancia de venta") }
                val wholesale = wholesaleProfitText.takeIf { it.isNotBlank() }?.let { numberOrError(it, "la ganancia de mayoreo") }
                if (errorMessage == null && retail == null && wholesale == null) {
                    errorMessage = "Ingresa al menos un porcentaje de ganancia."
                }
                if (errorMessage == null) BulkProductModification(operation, retailProfitPercentage = retail, wholesaleProfitPercentage = wholesale) else null
            }

            BulkProductOperation.CHANGE_CATEGORY -> {
                val category = categoryText.trim()
                if (category.isEmpty()) {
                    errorMessage = "Ingresa la categoría que se asignará."
                    null
                } else BulkProductModification(operation, category = category)
            }

            BulkProductOperation.DEACTIVATE, BulkProductOperation.DELETE -> BulkProductModification(operation)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Modificación masiva") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("$selectedCount producto(s) seleccionado(s) · Paso $step de 2", color = MaterialTheme.colorScheme.onSurfaceVariant)
                HorizontalDivider()
                if (step == 1) {
                    BulkProductOperation.entries.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = operation == item, onClick = { operation = item })
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(item.title, style = MaterialTheme.typography.titleSmall)
                                Text(item.description, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                } else {
                    when (operation) {
                        BulkProductOperation.CHANGE_PRICES -> {
                            PriceOption("Precio de costo", changeCost, { changeCost = it }, costText, { costText = it })
                            PriceOption("Precio de venta", changeRetail, { changeRetail = it }, retailText, { retailText = it })
                            PriceOption("Precio de mayoreo", changeWholesale, { changeWholesale = it }, wholesaleText, { wholesaleText = it })
                        }

                        BulkProductOperation.SET_PROFIT -> {
                            Text("Solo se actualizarán los productos que tengan precio de costo.")
                            DecimalInput("Ganancia de venta (%)", retailProfitText) { retailProfitText = it }
                            DecimalInput("Ganancia de mayoreo (%)", wholesaleProfitText) { wholesaleProfitText = it }
                        }

                        BulkProductOperation.CHANGE_CATEGORY -> OutlinedTextField(
                            value = categoryText,
                            onValueChange = { categoryText = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Nueva categoría") },
                            singleLine = true
                        )

                        BulkProductOperation.DEACTIVATE -> Text("Los $selectedCount productos seleccionados quedarán inactivos.")
                        BulkProductOperation.DELETE -> Text("¿Confirmas la eliminación definitiva de los $selectedCount productos seleccionados?", color = MaterialTheme.colorScheme.error)
                    }
                    errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            }
        },
        dismissButton = {
            if (step == 1) TextButton(onClick = onDismiss) { Text("Cancelar") }
            else TextButton(onClick = { step = 1; errorMessage = null }) { Text("Atrás") }
        },
        confirmButton = {
            Button(onClick = {
                if (step == 1) step = 2 else createModification()?.let(onApply)
            }) {
                Text(if (step == 1) "Siguiente" else if (operation == BulkProductOperation.DELETE) "Eliminar" else "Aplicar cambios")
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
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp),
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
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
    )
}