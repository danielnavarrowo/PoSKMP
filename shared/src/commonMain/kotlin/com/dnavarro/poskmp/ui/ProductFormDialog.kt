package com.dnavarro.poskmp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dnavarro.poskmp.db.Products
import com.dnavarro.poskmp.util.currentTimeMillis
import com.dnavarro.poskmp.util.generateUUID
import org.jetbrains.compose.resources.stringResource
import poskmp.shared.generated.resources.*

@Composable
fun ProductFormDialog(
    product: Products?, // Null or product with empty ID means new product
    onDismiss: () -> Unit,
    onSave: (Products) -> Unit
) {
    val isNew = product == null || product.id.isEmpty()

    val defaultCategory = stringResource(Res.string.default_category_abarrotes)
    val noCategoryStr = stringResource(Res.string.no_category)

    // Form inputs state
    var formNombre by remember(product) { mutableStateOf(product?.nombre ?: "") }
    var formCodigo by remember(product) {
        val initialCodes = product?.codigos?.let { codes ->
            try {
                codes.replace("[", "")
                    .replace("]", "")
                    .replace("\"", "")
                    .split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .joinToString(", ")
            } catch (_: Exception) {
                ""
            }
        } ?: ""
        mutableStateOf(initialCodes)
    }
    var formPrecio by remember(product) {
        val price = product?.precio
        mutableStateOf(if (price == null || price == 0.0) "" else price.toString())
    }
    var formCosto by remember(product) {
        val cost = product?.costo
        mutableStateOf(if (cost == null || cost == 0.0) "" else cost.toString())
    }
    var formPrecioMayoreo by remember(product) {
        val wholesale = product?.precio_mayoreo
        mutableStateOf(if (wholesale == null || wholesale == 0.0) "" else wholesale.toString())
    }
    var formCategoria by remember(product) { mutableStateOf(product?.categoria ?: defaultCategory) }
    var formActivo by remember(product) { mutableStateOf(product?.activo == 1L || product == null) }
    var formPorPeso by remember(product) { mutableStateOf(product?.por_peso == 1L) }
    var formEsFavorito by remember(product) { mutableStateOf(product?.es_favorito == 1L) }

    fun submitForm() {
        val id = product?.id?.ifEmpty { generateUUID() } ?: generateUUID()
        val formattedCodes = formCodigo.split(",")
            .map { it.trim().replace("\"", "") }
            .filter { it.isNotEmpty() }
            .joinToString(separator = "\",\"", prefix = "[\"", postfix = "\"]") { it }
            .let { if (it == "[\"\"]") "[]" else it }

        val p = Products(
            id = id,
            codigos = formattedCodes,
            nombre = formNombre.trim(),
            precio = formPrecio.toDoubleOrNull() ?: 0.0,
            costo = formCosto.toDoubleOrNull() ?: 0.0,
            categoria = formCategoria.trim().ifEmpty { noCategoryStr },
            activo = if (formActivo) 1L else 0L,
            por_peso = if (formPorPeso) 1L else 0L,
            precio_mayoreo = formPrecioMayoreo.toDoubleOrNull() ?: 0.0,
            es_favorito = if (formEsFavorito) 1L else 0L,
            updated_at = currentTimeMillis(),
            sync_state = if (isNew) "PENDING_INSERT" else "PENDING_UPDATE"
        )
        onSave(p)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.onPreviewKeyEvent { keyEvent ->
            if (keyEvent.type == KeyEventType.KeyDown &&
                (keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter)
            ) {
                if (formNombre.trim().isNotEmpty() && formPrecio.toDoubleOrNull() != null) {
                    submitForm()
                    true
                } else false
            } else false
        },
        shape = MaterialTheme.shapes.large,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        title = {
            Text(
                text = if (isNew) stringResource(Res.string.register_new_product_title) else stringResource(Res.string.modify_product_title),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = formNombre,
                    onValueChange = { formNombre = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(Res.string.product_name_label)) },
                    singleLine = true
                )

                OutlinedTextField(
                    value = formCodigo,
                    onValueChange = { formCodigo = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(Res.string.barcodes_label)) },
                    placeholder = { Text(stringResource(Res.string.barcodes_placeholder)) },
                    singleLine = true
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = formPrecio,
                        onValueChange = { formPrecio = it },
                        modifier = Modifier.weight(1f),
                        label = { Text(stringResource(Res.string.retail_price_required_label)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = formCosto,
                        onValueChange = { formCosto = it },
                        modifier = Modifier.weight(1f),
                        label = { Text(stringResource(Res.string.cost_label)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = formPrecioMayoreo,
                    onValueChange = { formPrecioMayoreo = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(Res.string.wholesale_price_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                OutlinedTextField(
                    value = formCategoria,
                    onValueChange = { formCategoria = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(Res.string.category_label)) },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Checkbox(checked = formActivo, onCheckedChange = { formActivo = it })
                        Text(stringResource(Res.string.active_label), fontSize = 14.sp)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Checkbox(checked = formPorPeso, onCheckedChange = { formPorPeso = it })
                        Text(stringResource(Res.string.sell_by_weight_label), fontSize = 14.sp)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = formEsFavorito, onCheckedChange = { formEsFavorito = it })
                    Text(stringResource(Res.string.mark_as_favorite_label), fontSize = 14.sp)
                }
            }
        },
        confirmButton = {
            val isNameValid = formNombre.trim().isNotEmpty()
            val isPriceValid = formPrecio.toDoubleOrNull() != null

            Button(
                onClick = { submitForm() },
                enabled = isNameValid && isPriceValid,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = MaterialTheme.shapes.small
            ) {
                Text(if (isNew) stringResource(Res.string.save_button) else stringResource(Res.string.save_changes_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}
