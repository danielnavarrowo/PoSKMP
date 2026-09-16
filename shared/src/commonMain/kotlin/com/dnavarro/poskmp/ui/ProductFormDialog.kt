package com.dnavarro.poskmp.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.dnavarro.poskmp.data.source.remote.GeminiCategoryResult
import com.dnavarro.poskmp.data.source.remote.GeminiCategoryServiceImpl
import com.dnavarro.poskmp.data.source.remote.GeminiInferenceResult
import com.dnavarro.poskmp.data.source.remote.OpenFoodFactsServiceImpl
import com.dnavarro.poskmp.db.Products
import com.dnavarro.poskmp.theme.ShapeDefaults
import com.dnavarro.poskmp.util.CategorySuggester
import com.dnavarro.poskmp.util.currentTimeMillis
import com.dnavarro.poskmp.util.encodeToJsonBarcodes
import com.dnavarro.poskmp.util.generateUUID
import com.dnavarro.poskmp.util.isAndroid
import com.dnavarro.poskmp.util.matchesBarcode
import com.dnavarro.poskmp.util.normalizeBarcode
import com.dnavarro.poskmp.util.parseBarcodes
import com.dnavarro.poskmp.util.roundPrice
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import poskmp.shared.generated.resources.Res
import poskmp.shared.generated.resources.active_label
import poskmp.shared.generated.resources.add
import poskmp.shared.generated.resources.add_button
import poskmp.shared.generated.resources.barcode_already_exists_error
import poskmp.shared.generated.resources.barcode_duplicate_in_form_error
import poskmp.shared.generated.resources.barcode_lookup_in_progress
import poskmp.shared.generated.resources.barcode_scanner
import poskmp.shared.generated.resources.barcodes_label
import poskmp.shared.generated.resources.cancel
import poskmp.shared.generated.resources.category
import poskmp.shared.generated.resources.category_label
import poskmp.shared.generated.resources.check
import poskmp.shared.generated.resources.close
import poskmp.shared.generated.resources.close_button
import poskmp.shared.generated.resources.cost_label
import poskmp.shared.generated.resources.delivery_margin_label
import poskmp.shared.generated.resources.delivery_price
import poskmp.shared.generated.resources.error_delivery_less_than_cost
import poskmp.shared.generated.resources.error_retail_less_than_cost
import poskmp.shared.generated.resources.error_retail_less_than_wholesale
import poskmp.shared.generated.resources.error_wholesale_less_than_cost
import poskmp.shared.generated.resources.gemini_lookup_action
import poskmp.shared.generated.resources.gemini_lookup_in_progress
import poskmp.shared.generated.resources.gemini_suggestion_badge
import poskmp.shared.generated.resources.info
import poskmp.shared.generated.resources.mark_as_favorite_label
import poskmp.shared.generated.resources.modify_product_title
import poskmp.shared.generated.resources.new_category
import poskmp.shared.generated.resources.off_suggestion_badge
import poskmp.shared.generated.resources.product_name_label
import poskmp.shared.generated.resources.product_pieces_label
import poskmp.shared.generated.resources.register_new_product_title
import poskmp.shared.generated.resources.retail_margin_label
import poskmp.shared.generated.resources.retail_price_required_label
import poskmp.shared.generated.resources.save_button
import poskmp.shared.generated.resources.save_button_desktop
import poskmp.shared.generated.resources.save_changes_button
import poskmp.shared.generated.resources.save_changes_button_desktop
import poskmp.shared.generated.resources.sell_by_weight_label
import poskmp.shared.generated.resources.suggested_categories_title
import poskmp.shared.generated.resources.warning
import poskmp.shared.generated.resources.warning_delivery_margin_below_default
import poskmp.shared.generated.resources.warning_retail_margin_below_default
import poskmp.shared.generated.resources.warning_wholesale_margin_below_default
import poskmp.shared.generated.resources.wholesale_margin_label
import poskmp.shared.generated.resources.wholesale_price
import kotlin.math.roundToLong
import kotlin.time.Duration.Companion.milliseconds

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

@Composable
private fun MarginWarningBadge(warningText: String) {
    val warningColor = if (isSystemInDarkTheme()) Color(0xFFFFB74D) else Color(0xFFD97706)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 4.dp, top = 2.dp)
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

private data class FormCategorySuggestion(
    val category: String,
    val isAi: Boolean = false,
    val isOpenFoodFacts: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductFormDialog(
    product: Products?, // Null or product with empty ID means new product
    onDismiss: () -> Unit,
    onSave: (Products) -> Unit,
    onValidateBarcodes: (suspend (List<String>) -> Pair<String, Products>?)? = null,
    existingCategories: List<String> = emptyList(),
    existingProducts: List<Products> = emptyList(),
    defaultRetailMarginPercentage: Double = 0.0,
    defaultWholesaleMarginPercentage: Double = 0.0,
    defaultDeliveryMarginPercentage: Double = 0.0,
    roundProductPrices: Boolean = false,
    autoLookupBarcodeProducts: Boolean = true,
    geminiGroundingEnabled: Boolean = false,
    geminiApiKey: String = "",
    googleSearchEngineId: String = "",
    googleSearchApiKey: String = "",
    readOnly: Boolean = false
) {
    val isNew = product == null || product.id.isEmpty()
    val focusManager = LocalFocusManager.current

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
        val initialMargin =
            if (cost != null && cost > 0.0 && wholesale != null && wholesale > 0.0) {
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
    var formMargenDelivery by remember(product, defaultDeliveryMarginPercentage) {
        val cost = product?.costo
        val delivery = product?.precio_delivery
        val initialMargin = if (cost != null && cost > 0.0 && delivery != null && delivery > 0.0) {
            ((delivery - cost) / cost) * 100.0
        } else if (defaultDeliveryMarginPercentage > 0.0) {
            defaultDeliveryMarginPercentage
        } else {
            null
        }
        mutableStateOf(initialMargin?.let { formatMargin(it) } ?: "")
    }
    var formPrecioDelivery by remember(product, defaultDeliveryMarginPercentage) {
        val delivery = product?.precio_delivery
        val cost = product?.costo
        val initialDelivery = if (delivery != null && delivery > 0.0) {
            formatNumber(delivery)
        } else if (cost != null && cost > 0.0 && defaultDeliveryMarginPercentage > 0.0) {
            formatNumber(cost * (1.0 + defaultDeliveryMarginPercentage / 100.0))
        } else {
            ""
        }
        mutableStateOf(initialDelivery)
    }
    var formPiezas by remember(product) {
        val pieces = product?.piezas
        mutableStateOf(
            if (pieces == null || pieces == 0.0) "1" else if (pieces % 1.0 == 0.0) pieces.toLong()
                .toString() else pieces.toString()
        )
    }
    var formCategoria by remember(product) { mutableStateOf(product?.categoria ?: "") }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }

    val allCategories = remember(existingCategories) {
        existingCategories
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }

    var isLookingUpGemini by remember { mutableStateOf(false) }
    var geminiResult by remember { mutableStateOf<GeminiCategoryResult?>(null) }
    var geminiErrorMessage by remember { mutableStateOf<String?>(null) }
    var geminiIsError by remember { mutableStateOf(false) }
    val geminiCategoryService = remember { GeminiCategoryServiceImpl() }
    var openFoodFactsCategory by remember { mutableStateOf<String?>(null) }

    val categorySuggestions: List<FormCategorySuggestion> = remember(
        formNombre,
        allCategories,
        existingProducts,
        geminiResult,
        geminiGroundingEnabled,
        openFoodFactsCategory
    ) {
        val resultList = mutableListOf<FormCategorySuggestion>()
        val seenCategories = mutableSetOf<String>()

        fun addSuggestion(cat: String?, isAi: Boolean = false, isOpenFoodFacts: Boolean = false) {
            val trimmed = cat?.trim()
            if (!trimmed.isNullOrBlank() && seenCategories.add(trimmed.lowercase())) {
                resultList.add(
                    FormCategorySuggestion(
                        trimmed,
                        isAi = isAi,
                        isOpenFoodFacts = isOpenFoodFacts
                    )
                )
            }
        }

        val localSuggestions = CategorySuggester.suggestCategories(
            productName = formNombre,
            existingCategories = allCategories,
            existingProducts = existingProducts,
            limit = 4
        )

        if (geminiGroundingEnabled && geminiResult != null) {
            // Cuando la búsqueda web con IA esté activada: la IA tiene la máxima prioridad
            addSuggestion(geminiResult?.suggestedCategory, isAi = true)
            geminiResult?.alternativeCategories?.forEach { alt ->
                addSuggestion(alt, isAi = true)
            }
            // A continuación, las sugerencias del algoritmo local
            localSuggestions.forEach { local ->
                addSuggestion(local, isAi = false)
            }
            // Al final, la categoría de Open Food Facts
            addSuggestion(openFoodFactsCategory, isOpenFoodFacts = true)
        } else {
            // Cuando la búsqueda web con IA esté desactivada: se prioriza el algoritmo local
            localSuggestions.forEach { local ->
                addSuggestion(local, isAi = false)
            }
            // Al final, la categoría sugerida por Open Food Facts
            addSuggestion(openFoodFactsCategory, isOpenFoodFacts = true)
        }

        resultList
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

    var isLookingUpBarcode by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val openFoodFactsService = remember { OpenFoodFactsServiceImpl() }

    val lookupGeminiCategory: (String) -> Unit = { targetName: String ->
        val trimmed = targetName.trim()
        if (trimmed.length < 2) {
            geminiIsError = true
            geminiErrorMessage = "Ingresa al menos 2 letras en el nombre del producto."
        } else if (!geminiGroundingEnabled) {
            geminiIsError = true
            geminiErrorMessage = "La búsqueda con IA está desactivada en Ajustes."
        } else if (geminiApiKey.isBlank()) {
            geminiIsError = true
            geminiErrorMessage = "Ingresa tu clave de API en Ajustes > Precios e Inventario."
        } else {
            coroutineScope.launch {
                isLookingUpGemini = true
                geminiErrorMessage = null
                geminiIsError = false
                try {
                    println("[PoSKMP-UI] Solicitando inferencia para '$trimmed'...")
                    val inference = geminiCategoryService.inferCategoryDetailed(
                        productName = trimmed,
                        existingCategories = allCategories,
                        apiKey = geminiApiKey,
                        googleSearchEngineId = googleSearchEngineId,
                        googleSearchApiKey = googleSearchApiKey
                    )
                    when (inference) {
                        is GeminiInferenceResult.Success -> {
                            println("[PoSKMP-UI] ✅ Éxito de IA: ${inference.result.suggestedCategory}")
                            geminiResult = inference.result
                            geminiIsError = false
                            geminiErrorMessage = null
                            if (formCategoria.isBlank()) {
                                formCategoria = inference.result.suggestedCategory
                            }
                        }

                        is GeminiInferenceResult.Failure -> {
                            println("[PoSKMP-UI] ❌ Error de IA: ${inference.errorMessage}")
                            geminiIsError = true
                            geminiErrorMessage = inference.errorMessage
                        }
                    }
                } catch (e: Exception) {
                    println("[PoSKMP-UI] 💥 Excepción inesperada: ${e.message}")
                    geminiIsError = true
                    geminiErrorMessage = "Error inesperado: ${e.message}"
                } finally {
                    isLookingUpGemini = false
                }
            }
        }
    }

    LaunchedEffect(
        formNombre,
        geminiGroundingEnabled,
        geminiApiKey,
        googleSearchEngineId,
        googleSearchApiKey
    ) {
        val trimmed = formNombre.trim()
        if (geminiGroundingEnabled && geminiApiKey.isNotBlank() && trimmed.length >= 3) {
            delay(1200.milliseconds)
            if (trimmed == formNombre.trim()) {
                lookupGeminiCategory(trimmed)
            }
        }
    }

    val tryLookupBarcode = { code: String ->
        if (autoLookupBarcodeProducts) {
            coroutineScope.launch {
                isLookingUpBarcode = true
                try {
                    val result = openFoodFactsService.lookupProduct(code, allCategories)
                    if (result != null) {
                        if (formNombre.isBlank()) {
                            formNombre = result.displayName
                        }
                        result.suggestedCategory?.let { offCat ->
                            if (offCat.isNotBlank()) {
                                openFoodFactsCategory = offCat
                            }
                        }
                    }
                } finally {
                    isLookingUpBarcode = false
                }
            }
        }
    }

    LaunchedEffect(formBarcodes, autoLookupBarcodeProducts) {
        if (autoLookupBarcodeProducts && openFoodFactsCategory == null && formBarcodes.isNotEmpty()) {
            val code = formBarcodes.firstOrNull { it.isNotBlank() }
            if (code != null) {
                tryLookupBarcode(code)
            }
        }
    }

    var barcodeValidationError by remember { mutableStateOf<String?>(null) }
    var isValidatingBarcode by remember { mutableStateOf(false) }

    val alreadyExistsErrFmt = stringResource(Res.string.barcode_already_exists_error)
    val duplicateInFormErrFmt = stringResource(Res.string.barcode_duplicate_in_form_error)

    val costVal = formCosto.toDoubleOrNull()
    val retailVal = formPrecio.toDoubleOrNull()
    val wholesaleVal = formPrecioMayoreo.toDoubleOrNull()
    val deliveryVal = formPrecioDelivery.toDoubleOrNull()

    val wholesalePriceError: String? = when {
        costVal != null && costVal > 0.0 && wholesaleVal != null && wholesaleVal > 0.0 && wholesaleVal < costVal -> {
            stringResource(Res.string.error_wholesale_less_than_cost)
        }

        else -> null
    }

    val deliveryPriceError: String? = when {
        costVal != null && costVal > 0.0 && deliveryVal != null && deliveryVal > 0.0 && deliveryVal < costVal -> {
            stringResource(Res.string.error_delivery_less_than_cost)
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

    val currentRetailMargin: Double? =
        if (costVal != null && costVal > 0.0 && retailVal != null && retailVal > 0.0) {
            formMargenVenta.toDoubleOrNull() ?: (((retailVal - costVal) / costVal) * 100.0)
        } else null

    val currentWholesaleMargin: Double? =
        if (costVal != null && costVal > 0.0 && wholesaleVal != null && wholesaleVal > 0.0) {
            formMargenMayoreo.toDoubleOrNull() ?: (((wholesaleVal - costVal) / costVal) * 100.0)
        } else null

    val currentDeliveryMargin: Double? =
        if (costVal != null && costVal > 0.0 && deliveryVal != null && deliveryVal > 0.0) {
            formMargenDelivery.toDoubleOrNull() ?: (((deliveryVal - costVal) / costVal) * 100.0)
        } else null

    val retailMarginWarning: String? = when {
        retailPriceError != null -> null
        currentRetailMargin != null && defaultRetailMarginPercentage > 0.0 && currentRetailMargin < (defaultRetailMarginPercentage - 0.001) -> {
            stringResource(
                Res.string.warning_retail_margin_below_default,
                formatMargin(currentRetailMargin),
                formatMargin(defaultRetailMarginPercentage)
            )
        }

        else -> null
    }

    val wholesaleMarginWarning: String? = when {
        wholesalePriceError != null -> null
        currentWholesaleMargin != null && defaultWholesaleMarginPercentage > 0.0 && currentWholesaleMargin < (defaultWholesaleMarginPercentage - 0.001) -> {
            stringResource(
                Res.string.warning_wholesale_margin_below_default,
                formatMargin(currentWholesaleMargin),
                formatMargin(defaultWholesaleMarginPercentage)
            )
        }

        else -> null
    }

    val deliveryMarginWarning: String? = when {
        deliveryPriceError != null -> null
        currentDeliveryMargin != null && defaultDeliveryMarginPercentage > 0.0 && currentDeliveryMargin < (defaultDeliveryMarginPercentage - 0.001) -> {
            stringResource(
                Res.string.warning_delivery_margin_below_default,
                formatMargin(currentDeliveryMargin),
                formatMargin(defaultDeliveryMarginPercentage)
            )
        }

        else -> null
    }

    val isPriceValid =
        retailVal != null && wholesalePriceError == null && retailPriceError == null && deliveryPriceError == null

    fun addBarcodeFromInput() {
        val codesToAdd = parseBarcodes(barcodeInput)
        if (codesToAdd.isNotEmpty()) {
            val updated = formBarcodes.toMutableList()
            var firstAdded: String? = null
            for (code in codesToAdd) {
                if (!updated.matchesBarcode(code)) {
                    updated.add(code)
                    if (firstAdded == null) firstAdded = code
                }
            }
            formBarcodes = updated
            barcodeInput = ""
            if (firstAdded != null) {
                tryLookupBarcode(firstAdded)
            }
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
            barcodeValidationError = duplicateInFormErrFmt.replace($$"%1$s", internalDuplicate)
            return@LaunchedEffect
        }

        val totalCodes = allCodes.distinct()
        if (onValidateBarcodes != null) {
            isValidatingBarcode = true
            val conflict = onValidateBarcodes(totalCodes)
            isValidatingBarcode = false
            if (conflict != null) {
                val (matchingCode, conflictingProduct) = conflict
                barcodeValidationError =
                    alreadyExistsErrFmt.replace($$"%1$s", matchingCode).replace(
                        $$"%2$s", conflictingProduct.nombre
                    )
            } else {
                barcodeValidationError = null
            }
        } else {
            barcodeValidationError = null
        }
    }

    val firstNameFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        if (!isAndroid()) {
            delay(100.milliseconds)
            try {
                firstNameFocusRequester.requestFocus()
            } catch (_: Exception) {
            }
        }
    }

    fun submitForm() {
        if (readOnly) return
        val id = product?.id?.ifEmpty { generateUUID() } ?: generateUUID()
        val finalBarcodes = (formBarcodes + parseBarcodes(barcodeInput)).distinct()
        val formattedCodes = finalBarcodes.encodeToJsonBarcodes()

        val rawPrice = formPrecio.toDoubleOrNull() ?: 0.0
        val finalPrice = if (roundProductPrices) roundPrice(rawPrice) else rawPrice
        val rawWholesale = formPrecioMayoreo.toDoubleOrNull() ?: 0.0
        val finalWholesale = if (roundProductPrices) roundPrice(rawWholesale) else rawWholesale
        val rawDelivery = formPrecioDelivery.toDoubleOrNull() ?: 0.0
        val finalDelivery = if (roundProductPrices) roundPrice(rawDelivery) else rawDelivery

        val p = Products(
            id = id,
            codigos = formattedCodes,
            nombre = formNombre.trim(),
            precio = finalPrice,
            costo = formCosto.toDoubleOrNull() ?: 0.0,
            categoria = formCategoria.trim(),
            activo = if (formActivo) 1L else 0L,
            por_peso = if (formPorPeso) 1L else 0L,
            precio_mayoreo = finalWholesale,
            es_favorito = if (formEsFavorito) 1L else 0L,
            piezas = formPiezas.toDoubleOrNull() ?: 1.0,
            precio_delivery = finalDelivery,
            created_at = if (product != null && product.created_at > 0L) product.created_at else currentTimeMillis(),
            updated_at = currentTimeMillis(),
            sync_state = if (isNew) "PENDING_INSERT" else "PENDING_UPDATE"
        )
        onSave(p)
    }

    val isNameValid = formNombre.trim().isNotEmpty()

    val confirmButtonContent: @Composable () -> Unit = {
        if (!readOnly) {
            Button(
                onClick = { submitForm() },
                enabled = isNameValid && isPriceValid && barcodeValidationError == null && !isValidatingBarcode,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    if (isAndroid()) {
                        if (isNew) stringResource(Res.string.save_button) else stringResource(Res.string.save_changes_button)
                    } else {
                        if (isNew) stringResource(Res.string.save_button_desktop) else stringResource(
                            Res.string.save_changes_button_desktop
                        )
                    }
                )
            }
        }
    }

    val dismissButtonContent: @Composable () -> Unit = {
        TextButton(onClick = onDismiss) {
            Text(
                if (readOnly) "Cerrar" else stringResource(Res.string.cancel),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    @Composable
    fun FormFields() {


        // Barcodes input field and chips
        Column(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = barcodeInput,
                onValueChange = { barcodeInput = it },
                modifier = Modifier
                    .fillMaxWidth().then(
                        if (!isAndroid()) {
                            Modifier.focusRequester(firstNameFocusRequester)
                        } else Modifier
                    ).onPreviewKeyEvent { keyEvent ->
                        keyEvent.type == KeyEventType.KeyDown &&
                                (keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter) && if (barcodeInput.trim()
                                .isNotEmpty()
                        ) {
                            addBarcodeFromInput()
                            true
                        } else false
                    },
                label = {
                    Text(
                        stringResource(Res.string.barcodes_label),
                        style = MaterialTheme.typography.labelLarge
                    )
                },
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
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(
                    onNext = {
                        if (barcodeInput.trim().isNotEmpty()) {
                            addBarcodeFromInput()
                        }
                        focusManager.moveFocus(FocusDirection.Next)
                    }
                ),
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
            } else if (isLookingUpBarcode) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(Res.string.barcode_lookup_in_progress),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
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
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(
                                    alpha = 0.6f
                                ),
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

        OutlinedTextField(
            value = formNombre,
            onValueChange = { formNombre = it },
            modifier = Modifier
                .fillMaxWidth(),
            label = {
                Text(
                    stringResource(Res.string.product_name_label),
                    style = MaterialTheme.typography.labelLarge
                )
            },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = {
                    focusManager.moveFocus(FocusDirection.Next)
                }
            ),
            singleLine = true
        )

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
                            val marginDelivery = formMargenDelivery.toDoubleOrNull()
                            if (marginDelivery != null) {
                                val newDelivery = cost * (1.0 + marginDelivery / 100.0)
                                formPrecioDelivery = formatNumber(newDelivery)
                            }
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                prefix = { Text("$", fontWeight = FontWeight.Bold) },
                label = {
                    Text(
                        stringResource(Res.string.cost_label),
                        style = MaterialTheme.typography.labelLarge
                    )
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
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
                label = {
                    Text(
                        stringResource(Res.string.product_pieces_label),
                        style = MaterialTheme.typography.labelLarge
                    )
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
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
                label = {
                    Text(
                        stringResource(Res.string.retail_price_required_label),
                        style = MaterialTheme.typography.labelLarge
                    )
                },
                isError = retailPriceError != null,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
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
                label = {
                    Text(
                        stringResource(Res.string.retail_margin_label),
                        style = MaterialTheme.typography.labelLarge
                    )
                },
                isError = retailPriceError != null,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
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
        } else if (retailMarginWarning != null) {
            MarginWarningBadge(retailMarginWarning)
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
                label = {
                    Text(
                        stringResource(Res.string.wholesale_price),
                        style = MaterialTheme.typography.labelLarge
                    )
                },
                isError = wholesalePriceError != null,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
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
                label = {
                    Text(
                        stringResource(Res.string.wholesale_margin_label),
                        style = MaterialTheme.typography.labelLarge
                    )
                },
                isError = wholesalePriceError != null,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
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
        } else if (wholesaleMarginWarning != null) {
            MarginWarningBadge(wholesaleMarginWarning)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = formPrecioDelivery,
                onValueChange = { input ->
                    if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                        formPrecioDelivery = input
                        val delivery = input.toDoubleOrNull()
                        val cost = formCosto.toDoubleOrNull()
                        if (delivery != null && cost != null && cost > 0) {
                            val margin = ((delivery - cost) / cost) * 100.0
                            formMargenDelivery = formatMargin(margin)
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                prefix = { Text("$", fontWeight = FontWeight.Bold) },
                label = {
                    Text(
                        stringResource(Res.string.delivery_price),
                        style = MaterialTheme.typography.labelLarge
                    )
                },
                isError = deliveryPriceError != null,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
                singleLine = true
            )
            OutlinedTextField(
                value = formMargenDelivery,
                onValueChange = { input ->
                    if (input.isEmpty() || input.matches(Regex("^-?\\d*\\.?\\d{0,2}$"))) {
                        formMargenDelivery = input
                        val margin = input.toDoubleOrNull()
                        val cost = formCosto.toDoubleOrNull()
                        if (margin != null && cost != null && cost > 0) {
                            val newDelivery = cost * (1.0 + margin / 100.0)
                            formPrecioDelivery = formatNumber(newDelivery)
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                suffix = { Text("%", fontWeight = FontWeight.Bold) },
                label = {
                    Text(
                        stringResource(Res.string.delivery_margin_label),
                        style = MaterialTheme.typography.labelLarge
                    )
                },
                isError = deliveryPriceError != null,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
                singleLine = true
            )
        }

        if (deliveryPriceError != null) {
            Text(
                text = deliveryPriceError,
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
            )
        } else if (deliveryMarginWarning != null) {
            MarginWarningBadge(deliveryMarginWarning)
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
                label = {
                    Text(
                        stringResource(Res.string.category_label),
                        style = MaterialTheme.typography.labelLarge
                    )
                },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded)
                },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        categoryDropdownExpanded = false
                        focusManager.clearFocus()
                    }
                ),
                singleLine = true
            )
            if (filteredCategories.isNotEmpty() || (formCategoria.trim()
                    .isNotEmpty() && !allCategories.any {
                    it.equals(
                        formCategoria.trim(),
                        ignoreCase = true
                    )
                })
            ) {
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

        if (!readOnly && (categorySuggestions.isNotEmpty() || isLookingUpGemini || (geminiGroundingEnabled && geminiApiKey.isNotBlank() && formNombre.trim().length >= 2) || (geminiIsError && geminiErrorMessage != null))) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(Res.string.suggested_categories_title),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (geminiGroundingEnabled && geminiApiKey.isNotBlank() && formNombre.trim().length >= 2 && !isLookingUpGemini) {
                    TextButton(
                        onClick = { lookupGeminiCategory(formNombre.trim()) },
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                    ) {
                        Text(
                            text = "✨ " + stringResource(Res.string.gemini_lookup_action),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (isLookingUpGemini) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(Res.string.gemini_lookup_in_progress),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (geminiIsError && geminiErrorMessage != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.info),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = geminiErrorMessage.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            if (categorySuggestions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    categorySuggestions.forEach { item ->
                        val suggestion = item.category
                        val isSelected = formCategoria.trim().equals(suggestion, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                formCategoria = if (isSelected) {
                                    ""
                                } else {
                                    suggestion
                                }
                            },
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = suggestion,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                    if (item.isAi) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                            shape = MaterialTheme.shapes.extraSmall
                                        ) {
                                            Text(
                                                text = "✨ " + stringResource(Res.string.gemini_suggestion_badge),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(
                                                    horizontal = 4.dp,
                                                    vertical = 1.dp
                                                )
                                            )
                                        }
                                    } else if (item.isOpenFoodFacts) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                                            shape = MaterialTheme.shapes.extraSmall
                                        ) {
                                            Text(
                                                text = "🌐 " + stringResource(Res.string.off_suggestion_badge),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.secondary,
                                                modifier = Modifier.padding(
                                                    horizontal = 4.dp,
                                                    vertical = 1.dp
                                                )
                                            )
                                        }
                                    }
                                }
                            },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(if (isSelected) Res.drawable.check else Res.drawable.category),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(
                                    alpha = 0.5f
                                ),
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            shape = MaterialTheme.shapes.small
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
                Text(
                    stringResource(Res.string.active_label),
                    style = MaterialTheme.typography.labelLarge
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Checkbox(checked = formPorPeso, onCheckedChange = { formPorPeso = it })
                Text(
                    stringResource(Res.string.sell_by_weight_label),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = formEsFavorito, onCheckedChange = { formEsFavorito = it })
            Text(
                stringResource(Res.string.mark_as_favorite_label),
                style = MaterialTheme.typography.labelLarge
            )
        }
    }

    if (isAndroid()) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 16.dp)
            ) {
                Text(
                    text = if (readOnly) "Detalle del Producto" else if (isNew) stringResource(Res.string.register_new_product_title) else stringResource(
                        Res.string.modify_product_title
                    ),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FormFields()
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    dismissButtonContent()
                    Spacer(modifier = Modifier.width(8.dp))
                    confirmButtonContent()
                }
            }
        }
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false),
            modifier = Modifier
                .widthIn(min = 340.dp, max = 640.dp)
                .fillMaxWidth(0.92f)
                .then(
                    Modifier.onPreviewKeyEvent { keyEvent ->
                        if (keyEvent.type == KeyEventType.KeyDown) {
                            val isEnter =
                                keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter
                            val isCtrlOrMeta = keyEvent.isCtrlPressed || keyEvent.isMetaPressed
                            isEnter && isCtrlOrMeta && if (formNombre.trim()
                                    .isNotEmpty() && isPriceValid && barcodeValidationError == null && !isValidatingBarcode
                            ) {
                                submitForm()
                                true
                            } else false
                        } else false
                    }
                ),
            shape = ShapeDefaults.cardShape,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            title = {
                Text(
                    text = if (readOnly) "Detalle del Producto" else if (isNew) stringResource(Res.string.register_new_product_title) else stringResource(
                        Res.string.modify_product_title
                    ),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FormFields()
                }
            },
            confirmButton = confirmButtonContent,
            dismissButton = dismissButtonContent
        )
    }

    if (showCameraScanner) {
        PlatformBarcodeScanner(
            onScanResult = { scannedBarcode ->
                showCameraScanner = false
                val code = scannedBarcode.trim()
                if (code.isNotEmpty()) {
                    if (!formBarcodes.contains(code)) {
                        formBarcodes = formBarcodes + code
                        tryLookupBarcode(code)
                    }
                }
            },
            onClose = { showCameraScanner = false }
        )
    }
}
