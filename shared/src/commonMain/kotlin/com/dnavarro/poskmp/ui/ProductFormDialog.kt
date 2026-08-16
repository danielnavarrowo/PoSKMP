package com.dnavarro.poskmp.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import com.dnavarro.poskmp.util.matchesBarcode
import com.dnavarro.poskmp.util.normalizeBarcode
import com.dnavarro.poskmp.util.parseBarcodes
import kotlin.math.roundToLong
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import poskmp.shared.generated.resources.*

private fun formatNumber(value: Double): String {
    val rounded = (value * 100.0).roundToLong() / 100.0
    return if (rounded <= 0.0) ""
    else if (rounded % 1.0 == 0.0) rounded.toLong().toString()
    else {
        val str = rounded.toString()
        if (str.contains('.')) {
            val parts = str.split('.')
            val dec = parts[1].take(2)
            if (dec.length == 1) "${parts[0]}.${dec}0" else "${parts[0]}.$dec"
        } else str
    }
}

private fun formatMargin(value: Double): String {
    val rounded = (value * 100.0).roundToLong() / 100.0
    return if (rounded % 1.0 == 0.0) {
        rounded.toLong().toString()
    } else {
        val str = rounded.toString()
        if (str.contains('.')) {
            val parts = str.split('.')
            val dec = parts[1].take(2).trimEnd('0')
            if (dec.isEmpty()) parts[0] else "${parts[0]}.$dec"
        } else str
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ProductFormDialog(
    product: Products?, // Null or product with empty ID means new product
    onDismiss: () -> Unit,
    onSave: (Products) -> Unit,
    onValidateBarcodes: (suspend (List<String>) -> Pair<String, Products>?)? = null,
    existingCategories: List<String> = emptyList(),
    defaultRetailMarginPercentage: Double = 0.0,
    defaultWholesaleMarginPercentage: Double = 0.0
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
    var formCosto by remember(product) {
        val cost = product?.costo
        mutableStateOf(if (cost == null || cost == 0.0) "" else formatNumber(cost))
    }
    var formMargenVenta by remember(product, defaultRetailMarginPercentage) {
        val cost = product?.costo
        val price = product?.precio
        val initialMargin = if (cost != null && cost > 0.0 && price != null && price > 0.0) {
            ((price - cost) / cost) * 100.0
        } else if (defaultRetailMarginPercentage > 0.0) {
            defaultRetailMarginPercentage
        } else {
            null
        }
        mutableStateOf(initialMargin?.let { formatMargin(it) } ?: "")
    }
    var formPrecio by remember(product, defaultRetailMarginPercentage) {
        val price = product?.precio
        val cost = product?.costo
        val initialPrice = if (price != null && price > 0.0) {
            formatNumber(price)
        } else if (cost != null && cost > 0.0 && defaultRetailMarginPercentage > 0.0) {
            formatNumber(cost * (1.0 + defaultRetailMarginPercentage / 100.0))
        } else {
            ""
        }
        mutableStateOf(initialPrice)
    }
    var formMargenMayoreo by remember(product, defaultWholesaleMarginPercentage) {
        val cost = product?.costo
        val wholesale = product?.precio_mayoreo
        val initialMargin = if (cost != null && cost > 0.0 && wholesale != null && wholesale > 0.0) {
            ((wholesale - cost) / cost) * 100.0
        } else if (defaultWholesaleMarginPercentage > 0.0) {
            defaultWholesaleMarginPercentage
        } else {
            null
        }
        mutableStateOf(initialMargin?.let { formatMargin(it) } ?: "")
    }
    var formPrecioMayoreo by remember(product, defaultWholesaleMarginPercentage) {
        val wholesale = product?.precio_mayoreo
        val cost = product?.costo
        val initialWholesale = if (wholesale != null && wholesale > 0.0) {
            formatNumber(wholesale)
        } else if (cost != null && cost > 0.0 && defaultWholesaleMarginPercentage > 0.0) {
            formatNumber(cost * (1.0 + defaultWholesaleMarginPercentage / 100.0))
        } else {
            ""
        }
        mutableStateOf(initialWholesale)
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
            } else matches.ifEmpty {
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
    val duplicateInFormErrFmt = stringResource(Res.string.barcode_duplicate_in_form_error)

    val costVal = formCosto.toDoubleOrNull()
    val retailVal = formPrecio.toDoubleOrNull()
    val wholesaleVal = formPrecioMayoreo.toDoubleOrNull()

    val wholesalePriceError: String? = when {
        costVal != null && costVal > 0.0 && wholesaleVal != null && wholesaleVal > 0.0 && wholesaleVal < costVal -> {
            stringResource(Res.string.error_wholesale_less_than_cost)
        }
        else -> null
    }

    val retailPriceError: String? = when {
        costVal != null && costVal > 0.0 && retailVal != null && retailVal > 0.0 && retailVal < costVal -> {
            stringResource(Res.string.error_retail_less_than_cost)
        }
        wholesaleVal != null && wholesaleVal > 0.0 && retailVal != null && retailVal > 0.0 && retailVal < wholesaleVal -> {
            stringResource(Res.string.error_retail_less_than_wholesale)
        }
        else -> null
    }

    val isPriceValid = retailVal != null && wholesalePriceError == null && retailPriceError == null

    fun addBarcodeFromInput() {
        val codesToAdd = parseBarcodes(barcodeInput)
        if (codesToAdd.isNotEmpty()) {
            val updated = formBarcodes.toMutableList()
            for (code in codesToAdd) {
                if (!updated.matchesBarcode(code)) {
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
        val allCodes = formBarcodes + pendingInputCodes

        if (allCodes.isEmpty()) {
            barcodeValidationError = null
            return@LaunchedEffect
        }

        // Check internal duplicates within the form itself (exact or normalized)
        val normalizedSeen = mutableSetOf<String>()
        var internalDuplicate: String? = null
        for (code in allCodes) {
            val norm = normalizeBarcode(code)
            if (norm.isNotEmpty()) {
                if (normalizedSeen.contains(norm)) {
                    internalDuplicate = code
                    break
                }
                normalizedSeen.add(norm)
            }
        }

        if (internalDuplicate != null) {
            barcodeValidationError = duplicateInFormErrFmt.replace("%1\$s", internalDuplicate)
            return@LaunchedEffect
        }

        val totalCodes = allCodes.distinct()
        if (onValidateBarcodes != null) {
            isValidatingBarcode = true
            val conflict = onValidateBarcodes(totalCodes)
            isValidatingBarcode = false
            if (conflict != null) {
                val (matchingCode, conflictingProduct) = conflict
                barcodeValidationError = alreadyExistsErrFmt.replace("%1\$s", matchingCode).replace(
                    "%2\$s", conflictingProduct.nombre)
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
                if (formNombre.trim().isNotEmpty() && isPriceValid && barcodeValidationError == null && !isValidatingBarcode) {
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
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedTextField(
                    value = formNombre,
                    onValueChange = { formNombre = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(Res.string.product_name_label), style = MaterialTheme.typography.labelLarge) },
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
                        label = { Text(stringResource(Res.string.barcodes_label), style = MaterialTheme.typography.labelLarge) },
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
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(formBarcodes, key = { it }) { code ->
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
                        value = formCosto,
                        onValueChange = { input ->
                            if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                                formCosto = input
                                val cost = input.toDoubleOrNull()
                                if (cost != null && cost > 0) {
                                    val marginVenta = formMargenVenta.toDoubleOrNull()
                                    if (marginVenta != null) {
                                        val newPrice = cost * (1.0 + marginVenta / 100.0)
                                        formPrecio = formatNumber(newPrice)
                                    }
                                    val marginMayoreo = formMargenMayoreo.toDoubleOrNull()
                                    if (marginMayoreo != null) {
                                        val newWholesale = cost * (1.0 + marginMayoreo / 100.0)
                                        formPrecioMayoreo = formatNumber(newWholesale)
                                    }
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        prefix = { Text("$", fontWeight = FontWeight.Bold) },
                        label = { Text(stringResource(Res.string.cost_label), style = MaterialTheme.typography.labelLarge) },
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
                        label = { Text(stringResource(Res.string.product_pieces_label), style = MaterialTheme.typography.labelLarge) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = formPrecio,
                        onValueChange = { input ->
                            if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                                formPrecio = input
                                val price = input.toDoubleOrNull()
                                val cost = formCosto.toDoubleOrNull()
                                if (price != null && cost != null && cost > 0) {
                                    val margin = ((price - cost) / cost) * 100.0
                                    formMargenVenta = formatMargin(margin)
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        prefix = { Text("$", fontWeight = FontWeight.Bold) },
                        label = { Text(stringResource(Res.string.retail_price_required_label), style = MaterialTheme.typography.labelLarge) },
                        isError = retailPriceError != null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = formMargenVenta,
                        onValueChange = { input ->
                            if (input.isEmpty() || input.matches(Regex("^-?\\d*\\.?\\d{0,2}$"))) {
                                formMargenVenta = input
                                val margin = input.toDoubleOrNull()
                                val cost = formCosto.toDoubleOrNull()
                                if (margin != null && cost != null && cost > 0) {
                                    val newPrice = cost * (1.0 + margin / 100.0)
                                    formPrecio = formatNumber(newPrice)
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        suffix = { Text("%", fontWeight = FontWeight.Bold) },
                        label = { Text(stringResource(Res.string.retail_margin_label), style = MaterialTheme.typography.labelLarge) },
                        isError = retailPriceError != null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }

                if (retailPriceError != null) {
                    Text(
                        text = retailPriceError,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = formPrecioMayoreo,
                        onValueChange = { input ->
                            if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                                formPrecioMayoreo = input
                                val wholesale = input.toDoubleOrNull()
                                val cost = formCosto.toDoubleOrNull()
                                if (wholesale != null && cost != null && cost > 0) {
                                    val margin = ((wholesale - cost) / cost) * 100.0
                                    formMargenMayoreo = formatMargin(margin)
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        prefix = { Text("$", fontWeight = FontWeight.Bold) },
                        label = { Text(stringResource(Res.string.wholesale_price), style = MaterialTheme.typography.labelLarge) },
                        isError = wholesalePriceError != null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = formMargenMayoreo,
                        onValueChange = { input ->
                            if (input.isEmpty() || input.matches(Regex("^-?\\d*\\.?\\d{0,2}$"))) {
                                formMargenMayoreo = input
                                val margin = input.toDoubleOrNull()
                                val cost = formCosto.toDoubleOrNull()
                                if (margin != null && cost != null && cost > 0) {
                                    val newWholesale = cost * (1.0 + margin / 100.0)
                                    formPrecioMayoreo = formatNumber(newWholesale)
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        suffix = { Text("%", fontWeight = FontWeight.Bold) },
                        label = { Text(stringResource(Res.string.wholesale_margin_label), style = MaterialTheme.typography.labelLarge) },
                        isError = wholesalePriceError != null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }

                if (wholesalePriceError != null) {
                    Text(
                        text = wholesalePriceError,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp)
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
                        label = { Text(stringResource(Res.string.category_label), style = MaterialTheme.typography.labelLarge) },
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
                        Text(stringResource(Res.string.active_label), style = MaterialTheme.typography.labelLarge)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Checkbox(checked = formPorPeso, onCheckedChange = { formPorPeso = it })
                        Text(stringResource(Res.string.sell_by_weight_label), style = MaterialTheme.typography.labelLarge)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = formEsFavorito, onCheckedChange = { formEsFavorito = it })
                    Text(stringResource(Res.string.mark_as_favorite_label), style = MaterialTheme.typography.labelLarge)
                }
            }
        },
        confirmButton = {
            val isNameValid = formNombre.trim().isNotEmpty()

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
