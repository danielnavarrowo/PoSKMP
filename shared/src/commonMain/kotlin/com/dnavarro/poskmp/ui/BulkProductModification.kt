package com.dnavarro.poskmp.ui

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material3.LinearWavyProgressIndicator
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.dnavarro.poskmp.db.Products
import com.dnavarro.poskmp.theme.ShapeDefaults
import com.dnavarro.poskmp.util.formatPrice
import com.dnavarro.poskmp.util.isAndroid
import com.dnavarro.poskmp.util.roundPrice
import kotlinx.coroutines.delay
import kotlin.math.abs
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
    val deliveryPrice: Double? = null,
    val retailProfitPercentage: Double? = null,
    val wholesaleProfitPercentage: Double? = null,
    val deliveryProfitPercentage: Double? = null,
    val category: String? = null
)

data class BulkProgressState(
    val operation: BulkProductOperation,
    val current: Int,
    val total: Int
) {
    val fraction: Float
        get() = if (total > 0) (current.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f

    val percentage: Int
        get() = (fraction * 100).toInt()
}

fun applyBulkProductModification(
    product: Products,
    modification: BulkProductModification,
    roundProductPrices: Boolean = false
): Products? = when (modification.operation) {
    BulkProductOperation.CHANGE_PRICES -> product.copy(
        costo = modification.costPrice ?: product.costo,
        precio = modification.retailPrice?.let { if (roundProductPrices) roundPrice(it) else it } ?: product.precio,
        precio_mayoreo = modification.wholesalePrice?.let { if (roundProductPrices) roundPrice(it) else it } ?: product.precio_mayoreo,
        precio_delivery = modification.deliveryPrice?.let { if (roundProductPrices) roundPrice(it) else it } ?: product.precio_delivery
    )

    BulkProductOperation.SET_PROFIT -> if (product.costo > 0.0) {
        fun calcPrice(profitPct: Double, shouldRound: Boolean): Double {
            val raw = kotlin.math.round(product.costo * (1.0 + profitPct / 100.0) * 100.0) / 100.0
            return if (shouldRound) roundPrice(raw) else raw
        }

        product.copy(
            precio = modification.retailProfitPercentage?.let { calcPrice(it, roundProductPrices) } ?: product.precio,
            precio_mayoreo = modification.wholesaleProfitPercentage?.let { calcPrice(it, roundProductPrices) } ?: product.precio_mayoreo,
            precio_delivery = modification.deliveryProfitPercentage?.let { calcPrice(it, roundProductPrices) } ?: product.precio_delivery
        )
    } else {
        product
    }

    BulkProductOperation.CHANGE_CATEGORY -> product.copy(categoria = modification.category)
    BulkProductOperation.MARK_AS_FAVORITE -> product.copy(es_favorito = 1L)
    BulkProductOperation.DEACTIVATE -> product.copy(activo = 0L)
    BulkProductOperation.DELETE -> null
}

enum class BulkField { COST, RETAIL, WHOLESALE, DELIVERY, CATEGORY, FAVORITE, STATUS }

/**
 * A single field difference for a product. For [BulkField.FAVORITE] and [BulkField.STATUS],
 * [before] and [after] hold `"true"`/`"false"`; for the rest they hold display-ready text.
 */
data class BulkFieldChange(val field: BulkField, val before: String, val after: String)

data class BulkProductPreview(
    val product: Products,
    val changes: List<BulkFieldChange>,
    val willBeDeleted: Boolean = false
)

/**
 * Computes what [modification] would do to each of [products] without persisting anything.
 * Products that would not change are omitted from the result.
 */
fun buildBulkPreview(
    products: List<Products>,
    modification: BulkProductModification,
    roundProductPrices: Boolean = false
): List<BulkProductPreview> = products.mapNotNull { product ->
    val updated = applyBulkProductModification(product, modification, roundProductPrices)
        ?: return@mapNotNull BulkProductPreview(product, emptyList(), willBeDeleted = true)

    val changes = buildList {
        fun price(field: BulkField, before: Double, after: Double) {
            if (abs(after - before) >= 0.005) {
                add(BulkFieldChange(field, "$${before.formatPrice()}", "$${after.formatPrice()}"))
            }
        }
        price(BulkField.COST, product.costo, updated.costo)
        price(BulkField.RETAIL, product.precio, updated.precio)
        price(BulkField.WHOLESALE, product.precio_mayoreo, updated.precio_mayoreo)
        price(BulkField.DELIVERY, product.precio_delivery, updated.precio_delivery)
        if (updated.categoria != product.categoria) {
            add(
                BulkFieldChange(
                    BulkField.CATEGORY,
                    product.categoria?.takeIf { it.isNotBlank() } ?: "—",
                    updated.categoria?.takeIf { it.isNotBlank() } ?: "—"
                )
            )
        }
        if (updated.es_favorito != product.es_favorito) {
            add(BulkFieldChange(BulkField.FAVORITE, (product.es_favorito == 1L).toString(), (updated.es_favorito == 1L).toString()))
        }
        if (updated.activo != product.activo) {
            add(BulkFieldChange(BulkField.STATUS, (product.activo == 1L).toString(), (updated.activo == 1L).toString()))
        }
    }
    if (changes.isEmpty()) null else BulkProductPreview(product, changes)
}

@Composable
fun BulkProductModificationDialog(
    selectedCount: Int,
    operation: BulkProductOperation,
    onDismiss: () -> Unit,
    onApply: (BulkProductModification) -> Unit,
    existingCategories: List<String> = emptyList(),
    defaultRetailMarginPercentage: Double = 0.0,
    defaultWholesaleMarginPercentage: Double = 0.0,
    defaultDeliveryMarginPercentage: Double = 0.0,
    progress: BulkProgressState? = null,
    selectedProducts: List<Products> = emptyList(),
    roundProductPrices: Boolean = false
) {
    val isProcessing = progress != null
    val progressText = if (progress != null) {
        when (progress.operation) {
            BulkProductOperation.CHANGE_PRICES -> stringResource(
                Res.string.bulk_progress_change_prices,
                progress.current,
                progress.total,
                progress.percentage
            )
            BulkProductOperation.SET_PROFIT -> stringResource(
                Res.string.bulk_progress_set_profit,
                progress.current,
                progress.total,
                progress.percentage
            )
            BulkProductOperation.CHANGE_CATEGORY -> stringResource(
                Res.string.bulk_progress_change_category,
                progress.current,
                progress.total,
                progress.percentage
            )
            BulkProductOperation.MARK_AS_FAVORITE -> stringResource(
                Res.string.bulk_progress_mark_as_favorite,
                progress.current,
                progress.total,
                progress.percentage
            )
            BulkProductOperation.DEACTIVATE -> stringResource(
                Res.string.bulk_progress_deactivate,
                progress.current,
                progress.total,
                progress.percentage
            )
            BulkProductOperation.DELETE -> stringResource(
                Res.string.bulk_progress_delete,
                progress.current,
                progress.total,
                progress.percentage
            )
        }
    } else ""

    var changeCost by remember { mutableStateOf(false) }
    var changeRetail by remember { mutableStateOf(true) }
    var changeWholesale by remember { mutableStateOf(false) }
    var changeDelivery by remember { mutableStateOf(false) }
    var costText by remember { mutableStateOf("") }
    var retailText by remember { mutableStateOf("") }
    var wholesaleText by remember { mutableStateOf("") }
    var deliveryText by remember { mutableStateOf("") }
    var retailProfitText by remember(defaultRetailMarginPercentage) {
        mutableStateOf(if (defaultRetailMarginPercentage > 0.0) {
            if (defaultRetailMarginPercentage % 1.0 == 0.0) defaultRetailMarginPercentage.toLong().toString() else defaultRetailMarginPercentage.toString()
        } else "")
    }
    var wholesaleProfitText by remember(defaultWholesaleMarginPercentage) {
        mutableStateOf(if (defaultWholesaleMarginPercentage > 0.0) {
            if (defaultWholesaleMarginPercentage % 1.0 == 0.0) defaultWholesaleMarginPercentage.toLong().toString() else defaultWholesaleMarginPercentage.toString()
        } else "")
    }
    var deliveryProfitText by remember(defaultDeliveryMarginPercentage) {
        mutableStateOf(if (defaultDeliveryMarginPercentage > 0.0) {
            if (defaultDeliveryMarginPercentage % 1.0 == 0.0) defaultDeliveryMarginPercentage.toLong().toString() else defaultDeliveryMarginPercentage.toString()
        } else "")
    }
    var categoryText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Non-null while the user is reviewing the preview step, before the changes are applied.
    var previewModification by remember { mutableStateOf<BulkProductModification?>(null) }
    val previewItems = remember(previewModification, selectedProducts, roundProductPrices) {
        previewModification?.let { buildBulkPreview(selectedProducts, it, roundProductPrices) }
    }
    val isPreviewing = previewItems != null

    var categoryDropdownExpanded by remember { mutableStateOf(false) }

    val allCategories = remember(existingCategories) {
        existingCategories
            .filter { it.isNotBlank() }
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

    LaunchedEffect(operation, isPreviewing) {
        if (!isAndroid()) {
            delay(100.milliseconds)
            try {
                if (hasTextField && !isPreviewing) {
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
    val deliveryPriceLabel = stringResource(Res.string.bulk_label_delivery_price)
    val retailProfitLabel = stringResource(Res.string.bulk_label_retail_profit)
    val wholesaleProfitLabel = stringResource(Res.string.bulk_label_wholesale_profit)
    val deliveryProfitLabel = stringResource(Res.string.bulk_label_delivery_profit)
    val selectPriceErr = stringResource(Res.string.bulk_select_price_error)
    val selectProfitErr = stringResource(Res.string.bulk_select_profit_error)
    val enterCategoryErr = stringResource(Res.string.bulk_enter_category_error)

    val costVal = if (changeCost) costText.replace(',', '.').toDoubleOrNull() else null
    val retailVal = if (changeRetail) retailText.replace(',', '.').toDoubleOrNull() else null
    val wholesaleVal = if (changeWholesale) wholesaleText.replace(',', '.').toDoubleOrNull() else null
    val deliveryVal = if (changeDelivery) deliveryText.replace(',', '.').toDoubleOrNull() else null

    val wholesalePriceError: String? = when {
        operation == BulkProductOperation.CHANGE_PRICES &&
            costVal != null && costVal > 0.0 && wholesaleVal != null && wholesaleVal > 0.0 && wholesaleVal < costVal -> {
            stringResource(Res.string.error_wholesale_less_than_cost)
        }
        else -> null
    }

    val deliveryPriceError: String? = when {
        operation == BulkProductOperation.CHANGE_PRICES &&
            costVal != null && costVal > 0.0 && deliveryVal != null && deliveryVal > 0.0 && deliveryVal < costVal -> {
            stringResource(Res.string.error_delivery_less_than_cost)
        }
        else -> null
    }

    val retailPriceError: String? = when (operation) {
        BulkProductOperation.CHANGE_PRICES if costVal != null && costVal > 0.0 && retailVal != null && retailVal > 0.0 && retailVal < costVal -> {
            stringResource(Res.string.error_retail_less_than_cost)
        }
        BulkProductOperation.CHANGE_PRICES if wholesaleVal != null && wholesaleVal > 0.0 && retailVal != null && retailVal > 0.0 && retailVal < wholesaleVal -> {
            stringResource(Res.string.error_retail_less_than_wholesale)
        }
        else -> null
    }

    val retailProfitVal = if (operation == BulkProductOperation.SET_PROFIT) retailProfitText.replace(',', '.').toDoubleOrNull() else null
    val wholesaleProfitVal = if (operation == BulkProductOperation.SET_PROFIT) wholesaleProfitText.replace(',', '.').toDoubleOrNull() else null
    val deliveryProfitVal = if (operation == BulkProductOperation.SET_PROFIT) deliveryProfitText.replace(',', '.').toDoubleOrNull() else null

    val profitError: String? = when (operation) {
        BulkProductOperation.SET_PROFIT if wholesaleProfitVal != null && wholesaleProfitVal < 0.0 -> {
            stringResource(Res.string.error_wholesale_less_than_cost)
        }
        BulkProductOperation.SET_PROFIT if retailProfitVal != null && retailProfitVal < 0.0 -> {
            stringResource(Res.string.error_retail_less_than_cost)
        }
        BulkProductOperation.SET_PROFIT if deliveryProfitVal != null && deliveryProfitVal < 0.0 -> {
            stringResource(Res.string.error_delivery_less_than_cost)
        }
        BulkProductOperation.SET_PROFIT if retailProfitVal != null && wholesaleProfitVal != null && retailProfitVal < wholesaleProfitVal -> {
            stringResource(Res.string.error_retail_less_than_wholesale)
        }
        else -> null
    }

    val isPriceModificationValid = wholesalePriceError == null && retailPriceError == null && deliveryPriceError == null && profitError == null

    fun numberOrError(text: String, labelStr: String, errorFmt: String): Double? {
        return text.replace(',', '.').toDoubleOrNull()?.takeIf { it >= 0 } ?: run {
            errorMessage = errorFmt.replace($$"%1$s", labelStr)
            null
        }
    }

    val invalidValueFmt = stringResource(Res.string.bulk_invalid_value_error)

    fun createModification(): BulkProductModification? {
        errorMessage = null
        if (!isPriceModificationValid) return null
        return when (operation) {
            BulkProductOperation.CHANGE_PRICES -> {
                if (!changeCost && !changeRetail && !changeWholesale && !changeDelivery) {
                    errorMessage = selectPriceErr
                    null
                } else {
                    val cost = if (changeCost) numberOrError(costText, costPriceLabel, invalidValueFmt) else 0.0
                    val retail = if (changeRetail) numberOrError(retailText, retailPriceLabel, invalidValueFmt) else 0.0
                    val wholesale = if (changeWholesale) numberOrError(wholesaleText, wholesalePriceLabel, invalidValueFmt) else 0.0
                    val delivery = if (changeDelivery) numberOrError(deliveryText, deliveryPriceLabel, invalidValueFmt) else 0.0
                    if (errorMessage == null) BulkProductModification(
                        operation = operation,
                        costPrice = if (changeCost) cost else null,
                        retailPrice = if (changeRetail) retail else null,
                        wholesalePrice = if (changeWholesale) wholesale else null,
                        deliveryPrice = if (changeDelivery) delivery else null
                    ) else null
                }
            }

            BulkProductOperation.SET_PROFIT -> {
                val retail = retailProfitText.takeIf { it.isNotBlank() }?.let { numberOrError(it, retailProfitLabel, invalidValueFmt) }
                val wholesale = wholesaleProfitText.takeIf { it.isNotBlank() }?.let { numberOrError(it, wholesaleProfitLabel, invalidValueFmt) }
                val delivery = deliveryProfitText.takeIf { it.isNotBlank() }?.let { numberOrError(it, deliveryProfitLabel, invalidValueFmt) }
                if (errorMessage == null && retail == null && wholesale == null && delivery == null) {
                    errorMessage = selectProfitErr
                }
                if (errorMessage == null) BulkProductModification(
                    operation,
                    retailProfitPercentage = retail,
                    wholesaleProfitPercentage = wholesale,
                    deliveryProfitPercentage = delivery
                ) else null
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

    fun confirm() {
        val reviewed = previewModification
        if (reviewed != null) {
            onApply(reviewed)
            return
        }
        val modification = createModification() ?: return
        if (selectedProducts.isEmpty()) onApply(modification) else previewModification = modification
    }

    AlertDialog(
        onDismissRequest = {
            if (!isProcessing) {
                onDismiss()
            }
        },
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .widthIn(min = 340.dp, max = 560.dp)
            .fillMaxWidth(0.92f)
            .then(
            if (!isAndroid()) {
                Modifier
                    .focusable()
                    .onPreviewKeyEvent { keyEvent ->
                        keyEvent.type == KeyEventType.KeyDown && when (keyEvent.key) {
                            Key.Enter, Key.NumPadEnter -> {
                                if (!isProcessing && (previewItems == null || previewItems.isNotEmpty())) {
                                    confirm()
                                }
                                true
                            }

                            else -> false
                        }
                    }
            } else Modifier
        ),
        shape = ShapeDefaults.cardShape,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        title = { Text(stringResource(operation.titleRes), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    stringResource(Res.string.bulk_mod_subtitle, selectedCount),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HorizontalDivider()
                if (previewItems != null) {
                    BulkPreviewContent(
                        items = previewItems,
                        totalSelected = selectedProducts.size
                    )
                } else when (operation) {
                    BulkProductOperation.CHANGE_PRICES -> {
                        PriceOption(
                            label = stringResource(Res.string.cost_price),
                            checked = changeCost,
                            onCheckedChange = { changeCost = it },
                            value = costText,
                            onValueChange = { costText = it },
                            enabled = !isProcessing
                        )
                        PriceOption(
                            label = stringResource(Res.string.retail_price),
                            checked = changeRetail,
                            onCheckedChange = { changeRetail = it },
                            value = retailText,
                            onValueChange = { retailText = it },
                            isError = retailPriceError != null,
                            enabled = !isProcessing,
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
                            isError = wholesalePriceError != null,
                            enabled = !isProcessing
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
                        PriceOption(
                            label = stringResource(Res.string.delivery_price),
                            checked = changeDelivery,
                            onCheckedChange = { changeDelivery = it },
                            value = deliveryText,
                            onValueChange = { deliveryText = it },
                            isError = deliveryPriceError != null,
                            enabled = !isProcessing
                        )
                        if (deliveryPriceError != null) {
                            Text(
                                text = deliveryPriceError,
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
                            enabled = !isProcessing,
                            modifier = if (!isAndroid()) Modifier.focusRequester(firstFocusRequester) else Modifier
                        ) { retailProfitText = it }
                        DecimalInput(
                            label = stringResource(Res.string.wholesale_profit_pct),
                            value = wholesaleProfitText,
                            isError = profitError != null,
                            enabled = !isProcessing
                        ) { wholesaleProfitText = it }
                        DecimalInput(
                            label = stringResource(Res.string.delivery_profit_pct),
                            value = deliveryProfitText,
                            isError = profitError != null,
                            enabled = !isProcessing
                        ) { deliveryProfitText = it }
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
                            expanded = categoryDropdownExpanded && !isProcessing,
                            onExpandedChange = { if (!isProcessing) categoryDropdownExpanded = it },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = categoryText,
                                onValueChange = {
                                    categoryText = it
                                    categoryDropdownExpanded = true
                                },
                                enabled = !isProcessing,
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
                                    expanded = categoryDropdownExpanded && !isProcessing,
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
                if (progress != null) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        LinearWavyProgressIndicator(
                            progress = { progress.fraction },
                            modifier = Modifier.fillMaxWidth().height(8.dp),
                            color = if (operation == BulkProductOperation.DELETE) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Text(
                            text = progressText,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        },
        dismissButton = {
            Row {
                TextButton(
                    onClick = onDismiss,
                    enabled = !isProcessing
                ) { Text(stringResource(Res.string.cancel)) }
                if (isPreviewing) {
                    TextButton(
                        onClick = { previewModification = null },
                        enabled = !isProcessing
                    ) { Text(stringResource(Res.string.bulk_preview_back)) }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { confirm() },
                enabled = isPriceModificationValid && !isProcessing && (previewItems == null || previewItems.isNotEmpty()),
                modifier = if (!isAndroid() && (!hasTextField || isPreviewing)) Modifier.focusRequester(confirmButtonFocusRequester) else Modifier
            ) {
                Text(
                    if (selectedProducts.isNotEmpty() && !isPreviewing) {
                        if (isAndroid()) stringResource(Res.string.bulk_preview_button)
                        else stringResource(Res.string.bulk_preview_button_desktop)
                    } else if (isAndroid()) {
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
private fun BulkPreviewContent(
    items: List<BulkProductPreview>,
    totalSelected: Int
) {
    val unchangedCount = totalSelected - items.size
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(Res.string.bulk_preview_title),
            fontWeight = FontWeight.SemiBold
        )
        if (items.isEmpty()) {
            Text(
                text = stringResource(Res.string.bulk_preview_no_changes),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@Column
        }
        Text(
            text = stringResource(Res.string.bulk_preview_summary, items.size),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp
        )
        if (unchangedCount > 0) {
            Text(
                text = stringResource(Res.string.bulk_preview_unchanged, unchangedCount),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
        }
        LazyColumn(
            modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(items, key = { it.product.id }) { item ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = item.product.nombre,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (item.willBeDeleted) {
                        Text(
                            text = stringResource(Res.string.bulk_preview_will_delete),
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 13.sp
                        )
                    }
                    item.changes.forEach { change ->
                        BulkFieldChangeRow(change)
                    }
                }
            }
        }
    }
}

@Composable
private fun BulkFieldChangeRow(change: BulkFieldChange) {
    val label = stringResource(
        when (change.field) {
            BulkField.COST -> Res.string.cost_price
            BulkField.RETAIL -> Res.string.retail_price
            BulkField.WHOLESALE -> Res.string.bulk_preview_field_wholesale
            BulkField.DELIVERY -> Res.string.bulk_preview_field_delivery
            BulkField.CATEGORY -> Res.string.category_label
            BulkField.FAVORITE -> Res.string.bulk_preview_field_favorite
            BulkField.STATUS -> Res.string.bulk_preview_field_status
        }
    )
    @Composable
    fun display(value: String): String = when (change.field) {
        BulkField.FAVORITE -> stringResource(
            if (value == "true") Res.string.bulk_preview_value_yes else Res.string.bulk_preview_value_no
        )
        BulkField.STATUS -> stringResource(
            if (value == "true") Res.string.bulk_preview_value_active else Res.string.bulk_preview_value_inactive
        )
        else -> value
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "$label:",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = display(change.before),
            fontSize = 13.sp,
            textDecoration = TextDecoration.LineThrough,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(text = "→", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = display(change.after),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun PriceOption(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    enabled: Boolean = true
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
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
            enabled = checked && enabled,
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
    enabled: Boolean = true,
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
        enabled = enabled,
        isError = isError,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
    )
}