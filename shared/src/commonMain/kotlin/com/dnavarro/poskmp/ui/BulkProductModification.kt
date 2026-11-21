package com.dnavarro.poskmp.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.focusable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dnavarro.poskmp.domain.model.PriceRoundingConfig
import androidx.compose.ui.window.DialogProperties
import com.dnavarro.poskmp.db.Products
import com.dnavarro.poskmp.theme.ShapeDefaults
import com.dnavarro.poskmp.util.connectedToggleButtonShapes
import com.dnavarro.poskmp.util.formatPrice
import com.dnavarro.poskmp.util.isAndroid
import com.dnavarro.poskmp.util.roundPrice
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.roundToLong
import kotlin.time.Duration.Companion.milliseconds
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import poskmp.shared.generated.resources.*

enum class BulkProductOperation(val titleRes: StringResource) {
    CHANGE_PRICES(Res.string.bulk_op_change_prices_title),
    SET_PROFIT(Res.string.bulk_op_set_profit_title),
    CHANGE_CATEGORY(Res.string.bulk_op_change_category_title),
    CHANGE_PIECES(Res.string.bulk_op_change_pieces_title),
    MARK_AS_FAVORITE(Res.string.bulk_op_mark_as_favorite_title),
    DEACTIVATE(Res.string.bulk_op_deactivate_title),
    DELETE(Res.string.bulk_op_delete_title)
}

enum class BulkToggleMode {
    MARK,
    UNMARK,
    INVERT
}

private enum class BulkPriceEditMode(
    val labelRes: StringResource,
    val iconRes: DrawableResource
) {
    MANUAL_PRICES(Res.string.bulk_price_mode_manual, Res.drawable.price),
    PROFIT_PERCENTAGES(Res.string.bulk_price_mode_profit, Res.drawable.percent_discount_24px)
}

/**
 * Returns the [BulkToggleMode] options that actually change at least one product in
 * [selectedProducts] for [operation]. Falls back to all options when [selectedProducts] is empty.
 */
fun availableToggleModes(
    operation: BulkProductOperation,
    selectedProducts: List<Products>
): List<BulkToggleMode> {
    if (operation != BulkProductOperation.MARK_AS_FAVORITE &&
        operation != BulkProductOperation.DEACTIVATE
    ) {
        return emptyList()
    }
    if (selectedProducts.isEmpty()) return BulkToggleMode.entries
    return BulkToggleMode.entries.filter { mode ->
        val modification = BulkProductModification(operation = operation, toggleMode = mode)
        selectedProducts.any { product ->
            val updated = applyBulkProductModification(product, modification)
            updated != null && updated != product
        }
    }
}

internal const val SHARED_PRICE_THRESHOLD = 0.05

internal fun formatBulkPriceInput(value: Double): String {
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

internal fun formatBulkMarginInput(value: Double): String {
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

internal fun formatBulkPieces(value: Double): String {
    val rounded = (value * 1000.0).roundToLong() / 1000.0
    return if (rounded % 1.0 == 0.0) {
        rounded.toLong().toString()
    } else {
        val str = rounded.toString()
        if (str.contains('.')) {
            val parts = str.split('.')
            val dec = parts[1].take(3).trimEnd('0')
            if (dec.isEmpty()) parts[0] else "${parts[0]}.$dec"
        } else str
    }
}

data class BulkManualPriceInitialState(
    val shareSamePrices: Boolean,
    val changeCost: Boolean,
    val changeRetail: Boolean,
    val changeWholesale: Boolean,
    val changeDelivery: Boolean,
    val costText: String,
    val retailText: String,
    val wholesaleText: String,
    val deliveryText: String,
    val retailMarginText: String,
    val wholesaleMarginText: String,
    val deliveryMarginText: String,
    val targetRetailMargin: Double?,
    val targetWholesaleMargin: Double?,
    val targetDeliveryMargin: Double?
)

/**
 * Computes initial manual price fields and checkbox states from [selectedProducts].
 * When all 4 price fields vary by at most [SHARED_PRICE_THRESHOLD] across products, shared positive
 * fields are checked and autofilled; otherwise each field is autofilled with the maximum value and
 * all checkboxes start unchecked.
 */
fun computeInitialBulkManualPrices(
    selectedProducts: List<Products>,
    defaultRetailMarginPercentage: Double = 0.0,
    defaultWholesaleMarginPercentage: Double = 0.0,
    defaultDeliveryMarginPercentage: Double = 0.0
): BulkManualPriceInitialState {
    if (selectedProducts.isEmpty()) {
        return BulkManualPriceInitialState(
            shareSamePrices = false,
            changeCost = false,
            changeRetail = true,
            changeWholesale = false,
            changeDelivery = false,
            costText = "",
            retailText = "",
            wholesaleText = "",
            deliveryText = "",
            retailMarginText = "",
            wholesaleMarginText = "",
            deliveryMarginText = "",
            targetRetailMargin = defaultRetailMarginPercentage.takeIf { it > 0.0 },
            targetWholesaleMargin = defaultWholesaleMarginPercentage.takeIf { it > 0.0 },
            targetDeliveryMargin = defaultDeliveryMarginPercentage.takeIf { it > 0.0 }
        )
    }

    val minCost = selectedProducts.minOf { it.costo }
    val maxCost = selectedProducts.maxOf { it.costo }
    val minRetail = selectedProducts.minOf { it.precio }
    val maxRetail = selectedProducts.maxOf { it.precio }
    val minWholesale = selectedProducts.minOf { it.precio_mayoreo }
    val maxWholesale = selectedProducts.maxOf { it.precio_mayoreo }
    val minDelivery = selectedProducts.minOf { it.precio_delivery }
    val maxDelivery = selectedProducts.maxOf { it.precio_delivery }

    val tolerance = SHARED_PRICE_THRESHOLD + 1e-9
    val shareSamePrices = (maxCost - minCost) <= tolerance &&
        (maxRetail - minRetail) <= tolerance &&
        (maxWholesale - minWholesale) <= tolerance &&
        (maxDelivery - minDelivery) <= tolerance

    val costText = formatBulkPriceInput(maxCost)
    val retailText = formatBulkPriceInput(maxRetail)
    val wholesaleText = formatBulkPriceInput(maxWholesale)
    val deliveryText = formatBulkPriceInput(maxDelivery)

    val costVal = costText.toDoubleOrNull()

    fun initialMargin(priceText: String, defaultMarginPct: Double): Pair<String, Double?> {
        val priceVal = priceText.toDoubleOrNull()
        val margin = if (costVal != null && costVal > 0.0 && priceVal != null && priceVal > 0.0) {
            ((priceVal - costVal) / costVal) * 100.0
        } else {
            null
        }
        val marginText = margin?.let(::formatBulkMarginInput) ?: ""
        val targetMargin = margin ?: defaultMarginPct.takeIf { it > 0.0 }
        return marginText to targetMargin
    }

    val (retailMarginText, targetRetailMargin) = initialMargin(retailText, defaultRetailMarginPercentage)
    val (wholesaleMarginText, targetWholesaleMargin) = initialMargin(wholesaleText, defaultWholesaleMarginPercentage)
    val (deliveryMarginText, targetDeliveryMargin) = initialMargin(deliveryText, defaultDeliveryMarginPercentage)

    return BulkManualPriceInitialState(
        shareSamePrices = shareSamePrices,
        changeCost = shareSamePrices && maxCost > 0.0,
        changeRetail = shareSamePrices && maxRetail > 0.0,
        changeWholesale = shareSamePrices && maxWholesale > 0.0,
        changeDelivery = shareSamePrices && maxDelivery > 0.0,
        costText = costText,
        retailText = retailText,
        wholesaleText = wholesaleText,
        deliveryText = deliveryText,
        retailMarginText = retailMarginText,
        wholesaleMarginText = wholesaleMarginText,
        deliveryMarginText = deliveryMarginText,
        targetRetailMargin = targetRetailMargin,
        targetWholesaleMargin = targetWholesaleMargin,
        targetDeliveryMargin = targetDeliveryMargin
    )
}

internal fun recalculateTierOnCostChange(
    cost: Double,
    targetMargin: Double?,
    marginText: String,
    defaultMarginPercentage: Double,
    shouldRound: Boolean
): Pair<String, String>? {
    if (cost <= 0.0) return null
    val margin = targetMargin
        ?: marginText.replace(',', '.').toDoubleOrNull()
        ?: defaultMarginPercentage.takeIf { it > 0.0 }
        ?: return null
    val rawPrice = cost * (1.0 + margin / 100.0)
    val finalPrice = if (shouldRound) roundPrice(rawPrice) else rawPrice
    val realMargin = if (shouldRound) ((finalPrice - cost) / cost) * 100.0 else margin
    return formatBulkPriceInput(finalPrice) to formatBulkMarginInput(realMargin)
}

internal fun calculateMarginOnPriceChange(
    priceInput: String,
    costText: String
): Pair<String, Double>? {
    val price = priceInput.replace(',', '.').toDoubleOrNull() ?: return null
    val cost = costText.replace(',', '.').toDoubleOrNull()?.takeIf { it > 0.0 } ?: return null
    val margin = ((price - cost) / cost) * 100.0
    return formatBulkMarginInput(margin) to margin
}

internal fun calculatePriceOnMarginChange(
    marginInput: String,
    costText: String,
    shouldRound: Boolean
): Pair<Double?, String?> {
    val margin = marginInput.replace(',', '.').toDoubleOrNull()
    val cost = costText.replace(',', '.').toDoubleOrNull()?.takeIf { it > 0.0 }
    if (margin == null || cost == null) return margin to null
    val rawPrice = cost * (1.0 + margin / 100.0)
    val finalPrice = if (shouldRound) roundPrice(rawPrice) else rawPrice
    return margin to formatBulkPriceInput(finalPrice)
}

internal fun recomputeRoundedMargin(priceText: String, costText: String): String? {
    val cost = costText.replace(',', '.').toDoubleOrNull()?.takeIf { it > 0.0 } ?: return null
    val price = priceText.replace(',', '.').toDoubleOrNull()?.takeIf { it > 0.0 } ?: return null
    return formatBulkMarginInput(((price - cost) / cost) * 100.0)
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
    val category: String? = null,
    val pieces: Double? = null,
    val toggleMode: BulkToggleMode = BulkToggleMode.MARK
)

data class BulkProgressState(
    val operation: BulkProductOperation,
    val current: Int,
    val total: Int,
    val toggleMode: BulkToggleMode = BulkToggleMode.MARK
) {
    val fraction: Float
        get() = if (total > 0) (current.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f

    val percentage: Int
        get() = (fraction * 100).toInt()
}

fun applyBulkProductModification(
    product: Products,
    modification: BulkProductModification,
    rounding: PriceRoundingConfig = PriceRoundingConfig()
): Products? = when (modification.operation) {
    BulkProductOperation.CHANGE_PRICES -> product.copy(
        costo = modification.costPrice ?: product.costo,
        precio = modification.retailPrice?.let { if (rounding.retail) roundPrice(it) else it } ?: product.precio,
        precio_mayoreo = modification.wholesalePrice?.let { if (rounding.wholesale) roundPrice(it) else it } ?: product.precio_mayoreo,
        precio_delivery = modification.deliveryPrice?.let { if (rounding.delivery) roundPrice(it) else it } ?: product.precio_delivery
    )

    BulkProductOperation.SET_PROFIT -> if (product.costo > 0.0) {
        fun calcPrice(profitPct: Double, shouldRound: Boolean): Double {
            val raw = kotlin.math.round(product.costo * (1.0 + profitPct / 100.0) * 100.0) / 100.0
            return if (shouldRound) roundPrice(raw) else raw
        }

        product.copy(
            precio = modification.retailProfitPercentage?.let { calcPrice(it, rounding.retail) } ?: product.precio,
            precio_mayoreo = modification.wholesaleProfitPercentage?.let { calcPrice(it, rounding.wholesale) } ?: product.precio_mayoreo,
            precio_delivery = modification.deliveryProfitPercentage?.let { calcPrice(it, rounding.delivery) } ?: product.precio_delivery
        )
    } else {
        product
    }

    BulkProductOperation.CHANGE_CATEGORY -> product.copy(categoria = modification.category)
    BulkProductOperation.CHANGE_PIECES -> modification.pieces?.let { product.copy(piezas = it) } ?: product
    BulkProductOperation.MARK_AS_FAVORITE -> {
        val nextFavorite = when (modification.toggleMode) {
            BulkToggleMode.MARK -> 1L
            BulkToggleMode.UNMARK -> 0L
            BulkToggleMode.INVERT -> if (product.es_favorito == 1L) 0L else 1L
        }
        product.copy(es_favorito = nextFavorite)
    }

    BulkProductOperation.DEACTIVATE -> {
        val nextActive = when (modification.toggleMode) {
            BulkToggleMode.MARK -> 0L
            BulkToggleMode.UNMARK -> 1L
            BulkToggleMode.INVERT -> if (product.activo == 1L) 0L else 1L
        }
        product.copy(activo = nextActive)
    }

    BulkProductOperation.DELETE -> null
}

enum class BulkField { COST, RETAIL, WHOLESALE, DELIVERY, CATEGORY, PIECES, FAVORITE, STATUS }

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
    rounding: PriceRoundingConfig = PriceRoundingConfig()
): List<BulkProductPreview> = products.mapNotNull { product ->
    val updated = applyBulkProductModification(product, modification, rounding)
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
        if (abs(updated.piezas - product.piezas) >= 0.0005) {
            add(
                BulkFieldChange(
                    BulkField.PIECES,
                    formatBulkPieces(product.piezas),
                    formatBulkPieces(updated.piezas)
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
    rounding: PriceRoundingConfig = PriceRoundingConfig()
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
            BulkProductOperation.CHANGE_PIECES -> stringResource(
                Res.string.bulk_progress_change_pieces,
                progress.current,
                progress.total,
                progress.percentage
            )
            BulkProductOperation.MARK_AS_FAVORITE -> stringResource(
                when (progress.toggleMode) {
                    BulkToggleMode.MARK -> Res.string.bulk_progress_mark_as_favorite
                    BulkToggleMode.UNMARK -> Res.string.bulk_progress_unmark_favorite
                    BulkToggleMode.INVERT -> Res.string.bulk_progress_invert_favorite
                },
                progress.current,
                progress.total,
                progress.percentage
            )
            BulkProductOperation.DEACTIVATE -> stringResource(
                when (progress.toggleMode) {
                    BulkToggleMode.MARK -> Res.string.bulk_progress_deactivate
                    BulkToggleMode.UNMARK -> Res.string.bulk_progress_activate
                    BulkToggleMode.INVERT -> Res.string.bulk_progress_invert_status
                },
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

    val initialManualPrices = remember(
        operation,
        selectedProducts,
        defaultRetailMarginPercentage,
        defaultWholesaleMarginPercentage,
        defaultDeliveryMarginPercentage
    ) {
        computeInitialBulkManualPrices(
            selectedProducts = selectedProducts,
            defaultRetailMarginPercentage = defaultRetailMarginPercentage,
            defaultWholesaleMarginPercentage = defaultWholesaleMarginPercentage,
            defaultDeliveryMarginPercentage = defaultDeliveryMarginPercentage
        )
    }
    var changeCost by remember(operation, selectedProducts) { mutableStateOf(initialManualPrices.changeCost) }
    var changeRetail by remember(operation, selectedProducts) { mutableStateOf(initialManualPrices.changeRetail) }
    var changeWholesale by remember(operation, selectedProducts) { mutableStateOf(initialManualPrices.changeWholesale) }
    var changeDelivery by remember(operation, selectedProducts) { mutableStateOf(initialManualPrices.changeDelivery) }
    var costText by remember(operation, selectedProducts) { mutableStateOf(initialManualPrices.costText) }
    var retailText by remember(operation, selectedProducts) { mutableStateOf(initialManualPrices.retailText) }
    var wholesaleText by remember(operation, selectedProducts) { mutableStateOf(initialManualPrices.wholesaleText) }
    var deliveryText by remember(operation, selectedProducts) { mutableStateOf(initialManualPrices.deliveryText) }
    var retailMarginText by remember(operation, selectedProducts) { mutableStateOf(initialManualPrices.retailMarginText) }
    var wholesaleMarginText by remember(operation, selectedProducts) { mutableStateOf(initialManualPrices.wholesaleMarginText) }
    var deliveryMarginText by remember(operation, selectedProducts) { mutableStateOf(initialManualPrices.deliveryMarginText) }
    var targetRetailMargin by remember(operation, selectedProducts) { mutableStateOf(initialManualPrices.targetRetailMargin) }
    var targetWholesaleMargin by remember(operation, selectedProducts) { mutableStateOf(initialManualPrices.targetWholesaleMargin) }
    var targetDeliveryMargin by remember(operation, selectedProducts) { mutableStateOf(initialManualPrices.targetDeliveryMargin) }
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
    var piecesText by remember(operation, selectedProducts) { mutableStateOf("") }
    var priceEditMode by remember(operation) {
        mutableStateOf(
            if (operation == BulkProductOperation.SET_PROFIT) {
                BulkPriceEditMode.PROFIT_PERCENTAGES
            } else {
                BulkPriceEditMode.MANUAL_PRICES
            }
        )
    }
    val effectiveOperation = when (operation) {
        BulkProductOperation.CHANGE_PRICES if priceEditMode == BulkPriceEditMode.PROFIT_PERCENTAGES ->
            BulkProductOperation.SET_PROFIT
        else -> operation
    }
    val availableModes = remember(operation, selectedProducts) {
        availableToggleModes(operation, selectedProducts)
    }
    var toggleMode by remember(operation) {
        mutableStateOf(availableModes.firstOrNull() ?: BulkToggleMode.MARK)
    }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Non-null while the user is reviewing the preview step, before the changes are applied.
    var previewModification by remember { mutableStateOf<BulkProductModification?>(null) }
    val previewItems = remember(previewModification, selectedProducts, rounding) {
        previewModification?.let { buildBulkPreview(selectedProducts, it, rounding) }
    }
    val isPreviewing = previewItems != null

    LaunchedEffect(availableModes, isProcessing, isPreviewing) {
        if (!isProcessing && !isPreviewing && availableModes.isNotEmpty() && toggleMode !in availableModes) {
            toggleMode = availableModes.first()
        }
    }

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

    val hasTextField = when (effectiveOperation) {
        BulkProductOperation.CHANGE_PRICES,
        BulkProductOperation.SET_PROFIT,
        BulkProductOperation.CHANGE_CATEGORY,
        BulkProductOperation.CHANGE_PIECES -> true
        else -> false
    }

    LaunchedEffect(effectiveOperation, isPreviewing) {
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
    val enterPiecesErr = stringResource(Res.string.bulk_enter_pieces_error)

    // Cost field still drives margin calculation and price-vs-cost checks even when not overwriting Cost.
    val effectiveCostVal = costText.replace(',', '.').toDoubleOrNull()
    val retailVal = if (changeRetail) retailText.replace(',', '.').toDoubleOrNull() else null
    val wholesaleVal = if (changeWholesale) wholesaleText.replace(',', '.').toDoubleOrNull() else null
    val deliveryVal = if (changeDelivery) deliveryText.replace(',', '.').toDoubleOrNull() else null

    val wholesalePriceError: String? = when {
        effectiveOperation == BulkProductOperation.CHANGE_PRICES &&
            effectiveCostVal != null && effectiveCostVal > 0.0 && wholesaleVal != null && wholesaleVal > 0.0 && wholesaleVal < effectiveCostVal -> {
            stringResource(Res.string.error_wholesale_less_than_cost)
        }
        else -> null
    }

    val deliveryPriceError: String? = when {
        effectiveOperation == BulkProductOperation.CHANGE_PRICES &&
            effectiveCostVal != null && effectiveCostVal > 0.0 && deliveryVal != null && deliveryVal > 0.0 && deliveryVal < effectiveCostVal -> {
            stringResource(Res.string.error_delivery_less_than_cost)
        }
        else -> null
    }

    val retailPriceError: String? = when (effectiveOperation) {
        BulkProductOperation.CHANGE_PRICES if effectiveCostVal != null && effectiveCostVal > 0.0 && retailVal != null && retailVal > 0.0 && retailVal < effectiveCostVal -> {
            stringResource(Res.string.error_retail_less_than_cost)
        }
        BulkProductOperation.CHANGE_PRICES if wholesaleVal != null && wholesaleVal > 0.0 && retailVal != null && retailVal > 0.0 && retailVal < wholesaleVal -> {
            stringResource(Res.string.error_retail_less_than_wholesale)
        }
        else -> null
    }

    val retailMarginWarning = rememberMarginWarningOrNull(
        enabled = effectiveOperation == BulkProductOperation.CHANGE_PRICES && changeRetail,
        priceError = retailPriceError,
        costVal = effectiveCostVal,
        priceVal = retailVal,
        marginText = retailMarginText,
        defaultMarginPercentage = defaultRetailMarginPercentage,
        warningRes = Res.string.warning_retail_margin_below_default
    )
    val wholesaleMarginWarning = rememberMarginWarningOrNull(
        enabled = effectiveOperation == BulkProductOperation.CHANGE_PRICES && changeWholesale,
        priceError = wholesalePriceError,
        costVal = effectiveCostVal,
        priceVal = wholesaleVal,
        marginText = wholesaleMarginText,
        defaultMarginPercentage = defaultWholesaleMarginPercentage,
        warningRes = Res.string.warning_wholesale_margin_below_default
    )
    val deliveryMarginWarning = rememberMarginWarningOrNull(
        enabled = effectiveOperation == BulkProductOperation.CHANGE_PRICES && changeDelivery,
        priceError = deliveryPriceError,
        costVal = effectiveCostVal,
        priceVal = deliveryVal,
        marginText = deliveryMarginText,
        defaultMarginPercentage = defaultDeliveryMarginPercentage,
        warningRes = Res.string.warning_delivery_margin_below_default
    )

    val retailProfitVal = if (effectiveOperation == BulkProductOperation.SET_PROFIT) retailProfitText.replace(',', '.').toDoubleOrNull() else null
    val wholesaleProfitVal = if (effectiveOperation == BulkProductOperation.SET_PROFIT) wholesaleProfitText.replace(',', '.').toDoubleOrNull() else null
    val deliveryProfitVal = if (effectiveOperation == BulkProductOperation.SET_PROFIT) deliveryProfitText.replace(',', '.').toDoubleOrNull() else null

    val profitError: String? = when (effectiveOperation) {
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
        return when (effectiveOperation) {
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
                        operation = effectiveOperation,
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
                    effectiveOperation,
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
                } else BulkProductModification(effectiveOperation, category = category)
            }

            BulkProductOperation.CHANGE_PIECES -> {
                val parsed = piecesText.replace(',', '.').toDoubleOrNull()
                if (parsed == null || parsed <= 0.0) {
                    errorMessage = enterPiecesErr
                    null
                } else BulkProductModification(effectiveOperation, pieces = parsed)
            }

            BulkProductOperation.MARK_AS_FAVORITE, BulkProductOperation.DEACTIVATE ->
                BulkProductModification(effectiveOperation, toggleMode = toggleMode)
            BulkProductOperation.DELETE -> BulkProductModification(effectiveOperation)
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
                } else {
                    if (operation == BulkProductOperation.CHANGE_PRICES) {
                        PriceEditModeToggleBar(
                            selectedMode = priceEditMode,
                            onSelectMode = { mode ->
                                priceEditMode = mode
                                errorMessage = null
                            },
                            enabled = !isProcessing
                        )
                    }
                    when (effectiveOperation) {
                        BulkProductOperation.CHANGE_PRICES -> {
                            PriceOption(
                                label = stringResource(Res.string.cost_price),
                                checked = changeCost,
                                onCheckedChange = { changeCost = it },
                                value = costText,
                                onValueChange = { input ->
                                    costText = input
                                    val newCost = input.replace(',', '.').toDoubleOrNull()
                                    if (newCost != null && newCost > 0.0) {
                                        if (changeRetail) {
                                            recalculateTierOnCostChange(
                                                cost = newCost,
                                                targetMargin = targetRetailMargin,
                                                marginText = retailMarginText,
                                                defaultMarginPercentage = defaultRetailMarginPercentage,
                                                shouldRound = rounding.retail
                                            )?.let { (newPrice, newMargin) ->
                                                retailText = newPrice
                                                retailMarginText = newMargin
                                            }
                                        }
                                        if (changeWholesale) {
                                            recalculateTierOnCostChange(
                                                cost = newCost,
                                                targetMargin = targetWholesaleMargin,
                                                marginText = wholesaleMarginText,
                                                defaultMarginPercentage = defaultWholesaleMarginPercentage,
                                                shouldRound = rounding.wholesale
                                            )?.let { (newPrice, newMargin) ->
                                                wholesaleText = newPrice
                                                wholesaleMarginText = newMargin
                                            }
                                        }
                                        if (changeDelivery) {
                                            recalculateTierOnCostChange(
                                                cost = newCost,
                                                targetMargin = targetDeliveryMargin,
                                                marginText = deliveryMarginText,
                                                defaultMarginPercentage = defaultDeliveryMarginPercentage,
                                                shouldRound = rounding.delivery
                                            )?.let { (newPrice, newMargin) ->
                                                deliveryText = newPrice
                                                deliveryMarginText = newMargin
                                            }
                                        }
                                    }
                                },
                                enabled = !isProcessing
                            )
                            BulkTierPriceRow(
                                priceLabel = stringResource(Res.string.retail_price),
                                marginLabel = stringResource(Res.string.retail_margin_label),
                                checked = changeRetail,
                                onCheckedChange = { changeRetail = it },
                                priceText = retailText,
                                onPriceTextChange = { retailText = it },
                                marginText = retailMarginText,
                                onMarginTextChange = { retailMarginText = it },
                                onTargetMarginChange = { targetRetailMargin = it },
                                costText = costText,
                                shouldRound = rounding.retail,
                                priceError = retailPriceError,
                                marginWarning = retailMarginWarning,
                                enabled = !isProcessing,
                                priceModifier = if (!isAndroid()) Modifier.focusRequester(firstFocusRequester) else Modifier
                            )
                            BulkTierPriceRow(
                                priceLabel = stringResource(Res.string.wholesale_price),
                                marginLabel = stringResource(Res.string.wholesale_margin_label),
                                checked = changeWholesale,
                                onCheckedChange = { changeWholesale = it },
                                priceText = wholesaleText,
                                onPriceTextChange = { wholesaleText = it },
                                marginText = wholesaleMarginText,
                                onMarginTextChange = { wholesaleMarginText = it },
                                onTargetMarginChange = { targetWholesaleMargin = it },
                                costText = costText,
                                shouldRound = rounding.wholesale,
                                priceError = wholesalePriceError,
                                marginWarning = wholesaleMarginWarning,
                                enabled = !isProcessing
                            )
                            BulkTierPriceRow(
                                priceLabel = stringResource(Res.string.delivery_price),
                                marginLabel = stringResource(Res.string.delivery_margin_label),
                                checked = changeDelivery,
                                onCheckedChange = { changeDelivery = it },
                                priceText = deliveryText,
                                onPriceTextChange = { deliveryText = it },
                                marginText = deliveryMarginText,
                                onMarginTextChange = { deliveryMarginText = it },
                                onTargetMarginChange = { targetDeliveryMargin = it },
                                costText = costText,
                                shouldRound = rounding.delivery,
                                priceError = deliveryPriceError,
                                marginWarning = deliveryMarginWarning,
                                enabled = !isProcessing
                            )
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

                        BulkProductOperation.CHANGE_PIECES -> {
                            Text(
                                text = stringResource(Res.string.bulk_op_change_pieces_desc),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedTextField(
                                value = piecesText,
                                onValueChange = { input ->
                                    if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,3}$"))) {
                                        piecesText = input
                                        errorMessage = null
                                    }
                                },
                                enabled = !isProcessing,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .then(if (!isAndroid()) Modifier.focusRequester(firstFocusRequester) else Modifier),
                                label = {
                                    Text(
                                        stringResource(Res.string.product_pieces_label),
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                },
                                isError = errorMessage != null,
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                            )
                        }

                        BulkProductOperation.MARK_AS_FAVORITE,
                        BulkProductOperation.DEACTIVATE -> ToggleModeSelector(
                            selectedMode = toggleMode,
                            onSelectMode = { toggleMode = it },
                            enabled = !isProcessing,
                            options = buildToggleModeOptions(
                                operation = effectiveOperation,
                                selectedCount = selectedCount,
                                availableModes = availableModes
                            )
                        )
                        BulkProductOperation.DELETE -> Text(
                            stringResource(Res.string.bulk_delete_confirmation, selectedCount),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
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
            items(
                items = items,
                key = { it.product.id },
                contentType = { "bulk_product_preview" }
            ) { item ->
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
            BulkField.PIECES -> Res.string.product_pieces_label
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
private fun rememberMarginWarningOrNull(
    enabled: Boolean,
    priceError: String?,
    costVal: Double?,
    priceVal: Double?,
    marginText: String,
    defaultMarginPercentage: Double,
    warningRes: StringResource
): String? {
    if (!enabled || priceError != null || defaultMarginPercentage <= 0.0) return null
    if (costVal == null || costVal <= 0.0 || priceVal == null || priceVal <= 0.0) return null
    val currentMargin = marginText.replace(',', '.').toDoubleOrNull()
        ?: (((priceVal - costVal) / costVal) * 100.0)
    if (currentMargin >= defaultMarginPercentage - 0.001) return null
    return stringResource(
        warningRes,
        formatBulkMarginInput(currentMargin),
        formatBulkMarginInput(defaultMarginPercentage)
    )
}

@Composable
private fun BulkTierPriceRow(
    priceLabel: String,
    marginLabel: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    priceText: String,
    onPriceTextChange: (String) -> Unit,
    marginText: String,
    onMarginTextChange: (String) -> Unit,
    onTargetMarginChange: (Double?) -> Unit,
    costText: String,
    shouldRound: Boolean,
    priceError: String?,
    marginWarning: String?,
    enabled: Boolean = true,
    priceModifier: Modifier = Modifier
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        PriceWithMarginOption(
            priceLabel = priceLabel,
            marginLabel = marginLabel,
            checked = checked,
            onCheckedChange = onCheckedChange,
            priceValue = priceText,
            onPriceValueChange = { input ->
                onPriceTextChange(input)
                calculateMarginOnPriceChange(input, costText)?.let { (formattedMargin, rawMargin) ->
                    onMarginTextChange(formattedMargin)
                    onTargetMarginChange(rawMargin)
                }
            },
            marginValue = marginText,
            onMarginValueChange = { input ->
                onMarginTextChange(input)
                val (parsedMargin, recalculatedPrice) = calculatePriceOnMarginChange(
                    marginInput = input,
                    costText = costText,
                    shouldRound = shouldRound
                )
                onTargetMarginChange(parsedMargin)
                if (recalculatedPrice != null) {
                    onPriceTextChange(recalculatedPrice)
                }
            },
            onMarginFocusLost = {
                if (shouldRound) {
                    recomputeRoundedMargin(priceText, costText)?.let(onMarginTextChange)
                }
            },
            isError = priceError != null,
            enabled = enabled,
            priceModifier = priceModifier
        )
        if (priceError != null) {
            Text(
                text = priceError,
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 48.dp, top = 2.dp)
            )
        } else if (marginWarning != null) {
            BulkMarginWarningBadge(
                warningText = marginWarning,
                modifier = Modifier.padding(start = 48.dp, top = 2.dp)
            )
        }
    }
}

@Composable
private fun PriceWithMarginOption(
    priceLabel: String,
    marginLabel: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    priceValue: String,
    onPriceValueChange: (String) -> Unit,
    marginValue: String,
    onMarginValueChange: (String) -> Unit,
    onMarginFocusLost: () -> Unit,
    isError: Boolean = false,
    enabled: Boolean = true,
    priceModifier: Modifier = Modifier
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
        Row(
            modifier = Modifier.weight(1f).padding(start = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = priceValue,
                onValueChange = { input ->
                    if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                        onPriceValueChange(input)
                    }
                },
                modifier = Modifier.weight(1f).then(priceModifier),
                prefix = { Text("$", fontWeight = FontWeight.Bold) },
                label = { Text(priceLabel, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                enabled = checked && enabled,
                isError = isError,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
            OutlinedTextField(
                value = marginValue,
                onValueChange = { input ->
                    if (input.isEmpty() || input.matches(Regex("^-?\\d*\\.?\\d{0,2}$"))) {
                        onMarginValueChange(input)
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged { focusState ->
                        if (!focusState.isFocused) {
                            onMarginFocusLost()
                        }
                    },
                suffix = { Text("%", fontWeight = FontWeight.Bold) },
                label = { Text(marginLabel, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                enabled = checked && enabled,
                isError = isError,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
        }
    }
}

@Composable
private fun BulkMarginWarningBadge(
    warningText: String,
    modifier: Modifier = Modifier
) {
    val warningColor = if (isSystemInDarkTheme()) Color(0xFFFFB74D) else Color(0xFFD97706)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Icon(
            painter = painterResource(Res.drawable.warning),
            contentDescription = null,
            tint = warningColor,
            modifier = Modifier.size(13.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = warningText,
            color = warningColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
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

private data class ToggleModeOption(
    val mode: BulkToggleMode,
    val title: String,
    val description: String
)

/**
 * Switches between entering explicit prices manually and computing them via profit percentages
 * inside the bulk price-change dialog.
 */
@Composable
private fun PriceEditModeToggleBar(
    selectedMode: BulkPriceEditMode,
    onSelectMode: (BulkPriceEditMode) -> Unit,
    enabled: Boolean = true
) {
    val modes = BulkPriceEditMode.entries
    Row(
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
        modifier = Modifier.fillMaxWidth()
    ) {
        modes.forEachIndexed { index, mode ->
            ToggleButton(
                checked = selectedMode == mode,
                onCheckedChange = { onSelectMode(mode) },
                enabled = enabled,
                colors = ToggleButtonDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    checkedContainerColor = MaterialTheme.colorScheme.primary,
                    checkedContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .weight(1f)
                    .semantics { role = Role.RadioButton },
                shapes = connectedToggleButtonShapes(index, modes.size)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(mode.iconRes),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(mode.labelRes),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun buildToggleModeOptions(
    operation: BulkProductOperation,
    selectedCount: Int,
    availableModes: List<BulkToggleMode>
): List<ToggleModeOption> {
    val isFavoriteOp = operation == BulkProductOperation.MARK_AS_FAVORITE
    val allOptions = listOf(
        ToggleModeOption(
            mode = BulkToggleMode.MARK,
            title = stringResource(
                if (isFavoriteOp) Res.string.bulk_favorite_opt_mark_title
                else Res.string.bulk_deactivate_opt_mark_title
            ),
            description = stringResource(
                if (isFavoriteOp) Res.string.bulk_favorite_opt_mark_desc
                else Res.string.bulk_deactivate_opt_mark_desc,
                selectedCount
            )
        ),
        ToggleModeOption(
            mode = BulkToggleMode.UNMARK,
            title = stringResource(
                if (isFavoriteOp) Res.string.bulk_favorite_opt_unmark_title
                else Res.string.bulk_deactivate_opt_unmark_title
            ),
            description = stringResource(
                if (isFavoriteOp) Res.string.bulk_favorite_opt_unmark_desc
                else Res.string.bulk_deactivate_opt_unmark_desc,
                selectedCount
            )
        ),
        ToggleModeOption(
            mode = BulkToggleMode.INVERT,
            title = stringResource(Res.string.bulk_opt_invert_title),
            description = stringResource(
                if (isFavoriteOp) Res.string.bulk_favorite_opt_invert_desc
                else Res.string.bulk_deactivate_opt_invert_desc
            )
        )
    )
    return allOptions.filter { it.mode in availableModes }
}

/**
 * Renders the applicable bulk flag actions (mark, unmark, or invert)
 * for favorite and active-status operations.
 */
@Composable
private fun ToggleModeSelector(
    selectedMode: BulkToggleMode,
    onSelectMode: (BulkToggleMode) -> Unit,
    options: List<ToggleModeOption>,
    enabled: Boolean = true
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { option ->
            val isSelected = selectedMode == option.mode
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .selectable(
                        selected = isSelected,
                        enabled = enabled,
                        role = Role.RadioButton,
                        onClick = { onSelectMode(option.mode) }
                    ),
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                ),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLowest
                    }
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = null,
                        enabled = enabled
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = option.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = option.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}