package com.dnavarro.poskmp.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dnavarro.poskmp.db.Products
import com.dnavarro.poskmp.theme.ShapeDefaults
import com.dnavarro.poskmp.util.currentTimeMillis
import com.dnavarro.poskmp.util.encodeToJsonBarcodes
import com.dnavarro.poskmp.util.generateUUID
import com.dnavarro.poskmp.util.isAndroid
import com.dnavarro.poskmp.util.parseBarcodes
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import poskmp.shared.generated.resources.*

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ProductFormDialog(
    product: Products?, // Null or product with empty ID means new product
    onDismiss: () -> Unit,
    onSave: (Products) -> Unit,
    onValidateBarcodes: (suspend (List<String>) -> Pair<String, Products>?)? = null,
    existingCategories: List<String> = emptyList()
) {
    val isNew = product == null || product.id.isEmpty()

    val defaultCategory = stringResource(Res.string.default_category_abarrotes)
    val noCategoryStr = stringResource(Res.string.no_category)

    // Form inputs state
    var formNombre by remember(product) { mutableStateOf(product?.nombre ?: "") }
    var formBarcodes by remember(product) {
        mutableStateOf(product?.parseBarcodes() ?: emptyList())
    }
    var barcodeInput by remember { mutableStateOf("") }
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
    var formPiezas by remember(product) {
        val pieces = product?.piezas
        mutableStateOf(if (pieces == null || pieces == 0.0) "1" else if (pieces % 1.0 == 0.0) pieces.toLong().toString() else pieces.toString())
    }
    var formCategoria by remember(product) { mutableStateOf(product?.categoria ?: defaultCategory) }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }

    val allCategories = remember(existingCategories, defaultCategory) {
        (existingCategories + defaultCategory)
            .filter { it.isNotBlank() && it != noCategoryStr }
            .distinct()
            .sorted()
    }
    val filteredCategories = remember(allCategories, formCategoria) {
        val trimmed = formCategoria.trim()
        if (trimmed.isEmpty()) {
            allCategories
        } else {
            val matches = allCategories.filter { it.contains(trimmed, ignoreCase = true) }
            if (matches.size == 1 && matches.first().equals(trimmed, ignoreCase = true)) {
                allCategories
            } else if (matches.isNotEmpty()) {
                matches
            } else {
                emptyList()
            }
        }
    }
    var formActivo by remember(product) { mutableStateOf(product?.activo == 1L || product == null) }
    var formPorPeso by remember(product) { mutableStateOf(product?.por_peso == 1L) }
    var formEsFavorito by remember(product) { mutableStateOf(product?.es_favorito == 1L) }
    var showCameraScanner by remember { mutableStateOf(false) }

    var barcodeValidationError by remember { mutableStateOf<String?>(null) }
    var isValidatingBarcode by remember { mutableStateOf(false) }

    val alreadyExistsErrFmt = stringResource(Res.string.barcode_already_exists_error)

    fun addBarcodeFromInput() {
        val codesToAdd = parseBarcodes(barcodeInput)
        if (codesToAdd.isNotEmpty()) {
            val updated = formBarcodes.toMutableList()
            for (code in codesToAdd) {
                if (!updated.contains(code)) {
                    updated.add(code)
                }
            }
            formBarcodes = updated
            barcodeInput = ""
        }
    }

    fun removeBarcode(code: String) {
        formBarcodes = formBarcodes.filter { it != code }
    }

    LaunchedEffect(formBarcodes, barcodeInput, product?.id) {
        val pendingInputCodes = parseBarcodes(barcodeInput)
        val totalCodes = (formBarcodes + pendingInputCodes).distinct()

        if (totalCodes.isEmpty()) {
            barcodeValidationError = null
            return@LaunchedEffect
        }

        if (onValidateBarcodes != null) {
            isValidatingBarcode = true
            val conflict = onValidateBarcodes(totalCodes)
            isValidatingBarcode = false
            if (conflict != null) {
                val (matchingCode, conflictingProduct) = conflict
                barcodeValidationError = alreadyExistsErrFmt.replace("%1\$s", matchingCode).replace("%2\$s", conflictingProduct.nombre)
            } else {
                barcodeValidationError = null
            }
        } else {
            barcodeValidationError = null
        }
    }

    fun submitForm() {
        val id = product?.id?.ifEmpty { generateUUID() } ?: generateUUID()
        val finalBarcodes = (formBarcodes + parseBarcodes(barcodeInput)).distinct()
        val formattedCodes = finalBarcodes.encodeToJsonBarcodes()

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
            piezas = formPiezas.toDoubleOrNull() ?: 1.0,
            updated_at = currentTimeMillis(),
            sync_state = if (isNew) "PENDING_INSERT" else "PENDING_UPDATE"
        )
        onSave(p)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.onKeyEvent { keyEvent ->
            if (keyEvent.type == KeyEventType.KeyDown &&
                (keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter)
            ) {
                if (formNombre.trim().isNotEmpty() && formPrecio.toDoubleOrNull() != null && barcodeValidationError == null && !isValidatingBarcode) {
                    submitForm()
                    true
                } else false
            } else false
        },
        shape = ShapeDefaults.cardShape,
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
                    label = { Text(stringResource(Res.string.product_name_label), fontSize = 12.sp) },
                    singleLine = true
                )

                // Barcodes input field and chips
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = barcodeInput,
                        onValueChange = { barcodeInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onPreviewKeyEvent { keyEvent ->
                                if (keyEvent.type == KeyEventType.KeyDown &&
                                    (keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter)
                                ) {
                                    if (barcodeInput.trim().isNotEmpty()) {
                                        addBarcodeFromInput()
                                        true
                                    } else false
                                } else false
                            },
                        label = { Text(stringResource(Res.string.barcodes_label), fontSize = 12.sp) },
                        placeholder = { Text(stringResource(Res.string.barcodes_placeholder), fontSize = 12.sp) },
                        isError = barcodeValidationError != null,
                        trailingIcon = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(end = 4.dp)
                            ) {
                                if (barcodeInput.trim().isNotEmpty()) {
                                    IconButton(onClick = { addBarcodeFromInput() }) {
                                        Icon(
                                            painter = painterResource(Res.drawable.add),
                                            contentDescription = stringResource(Res.string.add_button),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                if (isAndroid()) {
                                    IconButton(onClick = { showCameraScanner = true }) {
                                        Icon(
                                            painter = painterResource(Res.drawable.barcode_scanner),
                                            contentDescription = null
                                        )
                                    }
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { addBarcodeFromInput() }),
                        singleLine = true
                    )

                    if (barcodeValidationError != null) {
                        Text(
                            text = barcodeValidationError!!,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                        )
                    }

                    if (formBarcodes.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            formBarcodes.forEach { code ->
                                InputChip(
                                    selected = false,
                                    onClick = { },
                                    label = {
                                        Text(
                                            text = code,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    },
                                    trailingIcon = {
                                        Icon(
                                            painter = painterResource(Res.drawable.close),
                                            contentDescription = stringResource(Res.string.close_button),
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clickable { removeBarcode(code) }
                                        )
                                    },
                                    shape = MaterialTheme.shapes.small,
                                    colors = InputChipDefaults.inputChipColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                                        labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    ),
                                    border = InputChipDefaults.inputChipBorder(
                                        enabled = true,
                                        selected = false,
                                        borderColor = MaterialTheme.colorScheme.outlineVariant
                                    )
                                )
                            }
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = formPrecio,
                        onValueChange = { input ->
                            if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                                formPrecio = input
                            }
                        },
                        modifier = Modifier.weight(1f),
                        prefix = { Text("$", fontWeight = FontWeight.Bold) },
                        label = { Text(stringResource(Res.string.retail_price_required_label), fontSize = 12.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = formCosto,
                        onValueChange = { input ->
                            if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                                formCosto = input
                            }
                        },
                        modifier = Modifier.weight(1f),
                        prefix = { Text("$", fontWeight = FontWeight.Bold) },
                        label = { Text(stringResource(Res.string.cost_label), fontSize = 12.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = formPrecioMayoreo,
                        onValueChange = { input ->
                            if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                                formPrecioMayoreo = input
                            }
                        },
                        modifier = Modifier.weight(1f),
                        prefix = { Text("$", fontWeight = FontWeight.Bold) },
                        label = { Text(stringResource(Res.string.wholesale_price), fontSize = 12.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = formPiezas,
                        onValueChange = { input ->
                            if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,3}$"))) {
                                formPiezas = input
                            }
                        },
                        modifier = Modifier.weight(1f),
                        label = { Text(stringResource(Res.string.product_pieces_label), fontSize = 12.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }

                ExposedDropdownMenuBox(
                    expanded = categoryDropdownExpanded,
                    onExpandedChange = { categoryDropdownExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = formCategoria,
                        onValueChange = {
                            formCategoria = it
                            categoryDropdownExpanded = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable),
                        label = { Text(stringResource(Res.string.category_label), fontSize = 12.sp) },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded)
                        },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        singleLine = true
                    )
                    if (filteredCategories.isNotEmpty() || (formCategoria.trim().isNotEmpty() && !allCategories.any { it.equals(formCategoria.trim(), ignoreCase = true) })) {
                        ExposedDropdownMenu(
                            expanded = categoryDropdownExpanded,
                            onDismissRequest = { categoryDropdownExpanded = false }
                        ) {
                            filteredCategories.forEach { category ->
                                val isSelected = category.equals(formCategoria.trim(), ignoreCase = true)
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
                                        formCategoria = category
                                        categoryDropdownExpanded = false
                                    },
                                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                )
                            }
                            if (filteredCategories.isEmpty() && formCategoria.trim().isNotEmpty()) {
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
                                                text = "${stringResource(Res.string.new_category)}: \"${formCategoria.trim()}\"",
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
                enabled = isNameValid && isPriceValid && barcodeValidationError == null && !isValidatingBarcode,
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

    if (showCameraScanner) {
        PlatformBarcodeScanner(
            onScanResult = { scannedBarcode ->
                showCameraScanner = false
                val code = scannedBarcode.trim()
                if (code.isNotEmpty()) {
                    if (!formBarcodes.contains(code)) {
                        formBarcodes = formBarcodes + code
                    }
                }
            },
            onClose = { showCameraScanner = false }
        )
    }
}
