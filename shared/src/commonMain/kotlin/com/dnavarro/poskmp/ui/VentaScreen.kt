package com.dnavarro.poskmp.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.*
import androidx.compose.material3.adaptive.navigation.rememberSupportingPaneScaffoldNavigator
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dnavarro.poskmp.data.ProductRepository
import com.dnavarro.poskmp.db.Products
import com.dnavarro.poskmp.util.currentTimeMillis
import com.dnavarro.poskmp.util.formatPrice
import com.dnavarro.poskmp.util.generateUUID
import com.dnavarro.poskmp.util.isAndroid
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import poskmp.shared.generated.resources.*
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

data class CartItem(
    val product: Products,
    var quantity: Double,
    val originalPrice: Double = product.precio
)

@Suppress("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun VentaScreen(
    repository: ProductRepository,
    isCompact: Boolean,
    cartItems: SnapshotStateList<CartItem>,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var productsList by remember { mutableStateOf<List<Products>>(emptyList()) }
    var selectedIndex by remember { mutableIntStateOf(-1) }

    // Weight Dialog state
    var showWeightDialogForProduct by remember { mutableStateOf<Products?>(null) }
    var weightInput by remember { mutableStateOf("1.000") }

    // Checkout Dialog state
    var showCheckoutDialog by remember { mutableStateOf(false) }
    var paymentAmountInput by remember { mutableStateOf("") }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var lastSaleTotal by remember { mutableDoubleStateOf(0.0) }
    var lastSaleChange by remember { mutableDoubleStateOf(0.0) }

    // Unregistered Product Dialog state
    var showUnregisteredDialog by remember { mutableStateOf(false) }
    var unregisteredName by remember { mutableStateOf("") }
    var unregisteredPrice by remember { mutableStateOf("") }
    var unregisteredQuantity by remember { mutableStateOf("1") }
    val unregisteredFocusRequester = remember { FocusRequester() }

    fun openUnregisteredDialog() {
        unregisteredName = ""
        unregisteredPrice = ""
        unregisteredQuantity = "1"
        showUnregisteredDialog = true
    }

    // Edit Product Dialog state (from context menu)
    var showProductDialogFor by remember { mutableStateOf<Products?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val searchBarFocusRequester = remember { FocusRequester() }

    val adaptiveInfo = currentWindowAdaptiveInfo()
    val scaffoldDirective = remember(adaptiveInfo) {
        calculatePaneScaffoldDirectiveWithTwoPanesOnMediumWidth(adaptiveInfo).copy(
            maxVerticalPartitions = 1
        )
    }
    val navigator = rememberSupportingPaneScaffoldNavigator<Nothing>(
        scaffoldDirective = scaffoldDirective
    )
    val paneExpansionState = rememberPaneExpansionState()

    fun reclaimSearchBarFocus() {
        if (!isAndroid()) {
            coroutineScope.launch {
                delay(50.milliseconds)
                try {
                    searchBarFocusRequester.requestFocus()
                } catch (_: Exception) {}
            }
        }
    }

    var showBarcodeNotFoundQuery by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(showBarcodeNotFoundQuery) {
        if (showBarcodeNotFoundQuery != null) {
            delay(2500.milliseconds)
            showBarcodeNotFoundQuery = null
        }
    }

    LaunchedEffect(showWeightDialogForProduct, showCheckoutDialog, showUnregisteredDialog, showProductDialogFor, showBarcodeNotFoundQuery) {
        if (showWeightDialogForProduct == null && !showCheckoutDialog && !showUnregisteredDialog && showProductDialogFor == null && showBarcodeNotFoundQuery == null) {
            reclaimSearchBarFocus()
        }
    }

    fun toggleProductFavorite(product: Products) {
        val updated = product.copy(
            es_favorito = if (product.es_favorito == 1L) 0L else 1L,
            updated_at = currentTimeMillis(),
            sync_state = "PENDING_UPDATE"
        )
        repository.updateProduct(updated)
        reclaimSearchBarFocus()
    }



    // Observe products from DB
    LaunchedEffect(searchQuery) {
        if (searchQuery.isBlank()) {
            repository.getActiveProducts().collectLatest {
                productsList = it
            }
        } else {
            repository.searchProducts(searchQuery).collectLatest {
                productsList = it
            }
        }
    }

    val desktopFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        if (!isAndroid()) {
            delay(100.milliseconds)
            try {
                searchBarFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    // Helper: Add/Update product quantity in cart
    fun addProductToCart(product: Products, qty: Double) {
        val existingIndex = cartItems.indexOfFirst { it.product.id == product.id }
        if (existingIndex != -1) {
            val item = cartItems[existingIndex]
            val newQty = ((item.quantity + qty) * 100.0).roundToInt() / 100.0
            if (newQty <= 0.0) {
                cartItems.removeAt(existingIndex)
            } else {
                cartItems[existingIndex] = item.copy(quantity = newQty)
            }
        } else if (qty > 0.0) {
            cartItems.add(CartItem(product, qty))
        }
        reclaimSearchBarFocus()
    }

    fun setProductQuantityInCart(product: Products, qty: Double) {
        val existingIndex = cartItems.indexOfFirst { it.product.id == product.id }
        if (existingIndex != -1) {
            if (qty <= 0.0) {
                cartItems.removeAt(existingIndex)
            } else {
                val roundedQty = (qty * 100.0).roundToInt() / 100.0
                cartItems[existingIndex] = cartItems[existingIndex].copy(quantity = roundedQty)
            }
        } else if (qty > 0.0) {
            val roundedQty = (qty * 100.0).roundToInt() / 100.0
            cartItems.add(CartItem(product, roundedQty))
        }
        reclaimSearchBarFocus()
    }

    fun toggleWholesalePrice() {
        val eligibleItems = cartItems.filter { it.product.precio_mayoreo > 0.0 }
        if (eligibleItems.isEmpty()) return

        val allWholesale = eligibleItems.all { it.product.precio == it.product.precio_mayoreo }
        for (i in cartItems.indices) {
            val item = cartItems[i]
            if (item.product.precio_mayoreo > 0.0) {
                val targetPrice = if (allWholesale) item.originalPrice else item.product.precio_mayoreo
                val newProduct = item.product.copy(precio = targetPrice)
                cartItems[i] = item.copy(product = newProduct)
            }
        }
        reclaimSearchBarFocus()
    }

    val barcodeScanCallback: (String) -> Unit = { barcode ->
        coroutineScope.launch {
            val trimmed = barcode.trim()
            val p = repository.findProductByBarcode(trimmed)
            if (p != null) {
                if (p.por_peso == 1L) {
                    weightInput = "1.000"
                    showWeightDialogForProduct = p
                } else {
                    addProductToCart(p, 1.0)
                }
                searchQuery = ""
            } else {
                showBarcodeNotFoundQuery = trimmed
                searchQuery = ""
            }
        }
    }

    val handleSearchKeyIntercept: (KeyEvent) -> Boolean = { keyEvent ->
        if (cartItems.isNotEmpty()) {
            val key = keyEvent.key
            val codePoint = keyEvent.utf16CodePoint
            val isPlus = key == Key.Plus || key == Key.NumPadAdd || key == Key.Equals || codePoint == '+'.code
            val isMinus = key == Key.Minus || key == Key.NumPadSubtract || codePoint == '-'.code
            val isUp = key == Key.DirectionUp
            val isDown = key == Key.DirectionDown
            val isDelete = key == Key.Delete
            val isBackspace = key == Key.Backspace

            val shouldIntercept = isUp || isDown || isDelete ||
                    isPlus ||
                    isMinus ||
                    (isBackspace && searchQuery.isEmpty())

            if (shouldIntercept) {
                if (keyEvent.type == KeyEventType.KeyDown) {
                    val currentIndex = selectedIndex.coerceIn(0, cartItems.lastIndex)
                    val currentItem = cartItems[currentIndex]
                    when {
                        isUp -> {
                            if (currentIndex > 0) {
                                selectedIndex = currentIndex - 1
                            }
                        }
                        isDown -> {
                            if (currentIndex < cartItems.lastIndex) {
                                selectedIndex = currentIndex + 1
                            }
                        }
                        isPlus -> {
                            val increment = if (currentItem.product.por_peso == 1L) 0.1 else 1.0
                            addProductToCart(currentItem.product, increment)
                        }
                        isMinus -> {
                            val decrement = if (currentItem.product.por_peso == 1L) 0.1 else 1.0
                            addProductToCart(currentItem.product, -decrement)
                        }

                        else -> {
                            cartItems.removeAt(currentIndex)
                            if (selectedIndex >= cartItems.size) {
                                selectedIndex = cartItems.size - 1
                            }
                            reclaimSearchBarFocus()
                        }
                    }
                }
                true
            } else false
        } else false
    }

    val total = cartItems.sumOf { it.product.precio * it.quantity }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .pointerInput(Unit) {
                detectTapGestures {
                    reclaimSearchBarFocus()
                }
            }
            .then(
                if (!isAndroid()) {
                    Modifier
                        .focusRequester(desktopFocusRequester)
                        .focusable()
                        .onPreviewKeyEvent { keyEvent ->
                            keyEvent.type == KeyEventType.KeyDown && when (keyEvent.key) {
                                Key.F7 -> {
                                    openUnregisteredDialog()
                                    true
                                }

                                Key.F11 -> {
                                    if (keyEvent.isShiftPressed) {
                                        toggleWholesalePrice()
                                        true
                                    } else false
                                }

                                Key.F12 -> {
                                    paymentAmountInput = ""
                                    showCheckoutDialog = true
                                    true
                                }

                                else -> false
                            }
                        }
                } else Modifier
            )
    ) {
        SupportingPaneScaffold(
            directive = navigator.scaffoldDirective,
            value = navigator.scaffoldValue,
            paneExpansionState = paneExpansionState,
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            mainPane = {
                AnimatedPane {
                    CatalogSection(
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it },
                        productsList = productsList,
                        onProductClick = { product ->
                            if (product.por_peso == 1L) {
                                weightInput = "1.000"
                                showWeightDialogForProduct = product
                            } else {
                                addProductToCart(product, 1.0)
                            }
                        },
                        onToggleFavorite = { product -> toggleProductFavorite(product) },
                        onModifyProduct = { product -> showProductDialogFor = product },
                        isCompact = isCompact,
                        onViewCartClick = {
                            coroutineScope.launch {
                                navigator.navigateTo(ThreePaneScaffoldRole.Secondary)
                            }
                        },
                        cartCount = cartItems.size,
                        cartTotal = total,
                        onSellUnregisteredClick = { openUnregisteredDialog() },
                        onApplyWholesaleClick = { toggleWholesalePrice() },
                        onCheckoutClick = {
                            paymentAmountInput = ""
                            showCheckoutDialog = true
                        },
                        searchFocusRequester = searchBarFocusRequester,
                        onBarcodeScan = barcodeScanCallback,
                        onSearchKeyIntercept = handleSearchKeyIntercept
                    )
                }
            },
            supportingPane = {
                AnimatedPane {
                    TicketSection(
                        cartItems = cartItems,
                        total = total,
                        onClearCart = { cartItems.clear() },
                        onUpdateQuantity = { item, delta -> addProductToCart(item.product, delta) },
                        onSetQuantity = { item, qty -> setProductQuantityInCart(item.product, qty) },
                        onRemoveItem = { item -> cartItems.remove(item) },
                        onCheckout = {
                            paymentAmountInput = ""
                            showCheckoutDialog = true
                        },
                        selectedIndex = selectedIndex,
                        onSelectedIndexChange = { selectedIndex = it },
                        onBackClick = if (navigator.canNavigateBack()) {
                            {
                                coroutineScope.launch {
                                    navigator.navigateBack()
                                }
                            }
                        } else null
                    )
                }
            },
            paneExpansionDragHandle = { state ->
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(10.dp)
                        .pointerHoverIcon(PointerIcon.Hand)
                        .paneExpansionDraggable(
                            state = state,
                            minTouchTargetSize = 48.dp,
                            interactionSource = remember { MutableInteractionSource() }
                        )
                        .background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(2.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )
                }
            }
        )
    }

    // Barcode Not Found Warning Dialog
    if (showBarcodeNotFoundQuery != null) {
        AlertDialog(
            onDismissRequest = { showBarcodeNotFoundQuery = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(Res.string.barcode_not_found_title), fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    text = stringResource(Res.string.barcode_not_found_message, showBarcodeNotFoundQuery ?: ""),
                    fontSize = 15.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { showBarcodeNotFoundQuery = null },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(stringResource(Res.string.understood_enter_button))
                }
            },
            modifier = Modifier.onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown &&
                    (keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter || keyEvent.key == Key.Escape)
                ) {
                    showBarcodeNotFoundQuery = null
                    true
                } else false
            }
        )
    }

    // Weight Dialog
    if (showWeightDialogForProduct != null) {
        val product = showWeightDialogForProduct!!

        val focusRequester = remember { FocusRequester() }

        var weightInputValue by remember(product.id) {
            mutableStateOf(
                TextFieldValue(
                    text = "1",
                    selection = TextRange(0, 1)
                )
            )
        }
        var priceInputValue by remember(product.id) {
            val initialPrice = product.precio
            mutableStateOf(if (initialPrice % 1.0 == 0.0) initialPrice.toInt().toString() else initialPrice.toString())
        }

        LaunchedEffect(product.id) {
            delay(50.milliseconds)
            try {
                focusRequester.requestFocus()
            } catch (_: Exception) {}
        }

        fun handleWeightChange(newWeight: String) {
            weightInputValue = TextFieldValue(text = newWeight, selection = TextRange(newWeight.length))
            val weight = newWeight.toDoubleOrNull()
            if (weight != null && weight >= 0.0) {
                val calcPrice = weight * product.precio
                priceInputValue = if (calcPrice % 1.0 == 0.0) calcPrice.toInt()
                    .toString() else ((calcPrice * 100.0).roundToInt() / 100.0).toString()
            } else if (newWeight.isEmpty()) {
                priceInputValue = ""
            }
        }

        fun handlePriceChange(newPrice: String) {
            priceInputValue = newPrice
            val price = newPrice.toDoubleOrNull()
            if (price != null && price >= 0.0) {
                val calcWeight = price / product.precio
                val weightStr = if (calcWeight % 1.0 == 0.0) calcWeight.toInt()
                    .toString() else ((calcWeight * 1000.0).roundToInt() / 1000.0).toString()
                weightInputValue = TextFieldValue(text = weightStr, selection = TextRange(weightStr.length))
            } else if (newPrice.isEmpty()) {
                weightInputValue = TextFieldValue(text = "", selection = TextRange.Zero)
            }
        }


        BasicAlertDialog(
            onDismissRequest = { showWeightDialogForProduct = null },
            modifier = Modifier.background(
                MaterialTheme.colorScheme.surfaceContainerLowest, MaterialTheme.shapes.medium
            )
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown && 
                    (keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter)
                ) {
                    val weight = weightInputValue.text.toDoubleOrNull() ?: 1.0
                    addProductToCart(product, weight)
                    showWeightDialogForProduct = null
                    true
                } else false
            }
            .padding(20.dp),
            content = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(Res.string.quantity_prompt_title, product.nombre),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = stringResource(Res.string.price_per_kg_label, product.precio.toString().formatPrice()),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    val inputFieldsContent = @Composable {
                        // Left Column: Weight
                        Column(modifier = if (isCompact) Modifier.fillMaxWidth() else Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(0.25, 0.5, 0.75, 1.0).forEach { qty ->
                                    val kgSuffix = stringResource(Res.string.kg_suffix)
                                    val label =
                                        "${if (qty % 1.0 == 0.0) qty.toInt().toString() else qty.toString()}$kgSuffix"
                                    Box(
                                        modifier = Modifier
                                            .border(
                                                1.dp,
                                                MaterialTheme.colorScheme.outlineVariant,
                                                MaterialTheme.shapes.small
                                            )
                                            .clip(MaterialTheme.shapes.small)
                                            .background(MaterialTheme.colorScheme.surface)
                                            .clickable { handleWeightChange(qty.toString()) }
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            Text(
                                text = stringResource(Res.string.weight_kg_label),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )

                            OutlinedTextField(
                                value = weightInputValue,
                                onValueChange = { newValue ->
                                    weightInputValue = newValue
                                    val newWeight = newValue.text
                                    val weight = newWeight.toDoubleOrNull()
                                    if (weight != null && weight >= 0.0) {
                                        val calcPrice = weight * product.precio
                                        priceInputValue = if (calcPrice % 1.0 == 0.0) calcPrice.toInt()
                                            .toString() else ((calcPrice * 100.0).roundToInt() / 100.0).toString()
                                    } else if (newWeight.isEmpty()) {
                                        priceInputValue = ""
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surface)
                                    .focusRequester(focusRequester),
                                textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                )
                            )
                        }

                        if (!isCompact) {
                            Spacer(modifier = Modifier.width(16.dp))
                        } else {
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // Right Column: Price/Pesos
                        Column(modifier = if (isCompact) Modifier.fillMaxWidth() else Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(5, 10, 15, 20).forEach { cash ->
                                    val label = "$${cash}"
                                    Box(
                                        modifier = Modifier
                                            .border(
                                                1.dp,
                                                MaterialTheme.colorScheme.outlineVariant,
                                                MaterialTheme.shapes.small
                                            )
                                            .clip(MaterialTheme.shapes.small)
                                            .background(MaterialTheme.colorScheme.surface)
                                            .clickable { handlePriceChange(cash.toString()) }
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            Text(
                                text = stringResource(Res.string.pesos_currency_label),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )

                            OutlinedTextField(
                                value = priceInputValue,
                                onValueChange = { handlePriceChange(it) },
                                modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface),
                                textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                )
                            )
                        }


                    }

                    if (isCompact) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            inputFieldsContent()
                        }
                    } else {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            inputFieldsContent()
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    )
                    {

                        OutlinedButton(
                            onClick = { showWeightDialogForProduct = null },
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            shape = MaterialTheme.shapes.small,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                        ) {
                            Text(stringResource(Res.string.cancel), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Button(
                            onClick = {
                                val weight = weightInputValue.text.toDoubleOrNull() ?: 1.0
                                addProductToCart(product, weight)
                                showWeightDialogForProduct = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = MaterialTheme.shapes.small,
                        ) {
                            Text(stringResource(Res.string.add_button), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                    }
                }
            },
        )
    }

    // Checkout Dialog
    if (showCheckoutDialog) {
        val paymentAmount = paymentAmountInput.toDoubleOrNull() ?: 0.0
        val change = if (paymentAmount >= total) paymentAmount - total else 0.0

        AlertDialog(
            onDismissRequest = { showCheckoutDialog = false },
            modifier = Modifier.onPreviewKeyEvent { keyEvent ->
                keyEvent.type == KeyEventType.KeyDown &&
                        (keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter) && if (paymentAmount >= total || paymentAmountInput.isEmpty()) {
                    lastSaleTotal = total
                    lastSaleChange = change
                    showCheckoutDialog = false
                    showSuccessDialog = true
                    cartItems.clear()
                    true
                } else false
            },
            shape = MaterialTheme.shapes.large,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            title = { Text(stringResource(Res.string.checkout_sale_title), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.small)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            stringResource(Res.string.total_to_pay_label),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "$${total.toString().formatPrice()}",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp
                        )
                    }

                    OutlinedTextField(
                        value = paymentAmountInput,
                        onValueChange = { paymentAmountInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        label = { Text(stringResource(Res.string.cash_received_label)) },
                        placeholder = { Text("0.00") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )

                    if (paymentAmountInput.isNotEmpty() && paymentAmount < total) {
                        Text(
                            stringResource(Res.string.insufficient_amount_error, (total - paymentAmount).toString().formatPrice()),
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else if (paymentAmount >= total) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                stringResource(Res.string.change_to_deliver_label),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.sp
                            )
                            Text(
                                "$${change.toString().formatPrice()}",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 20.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        lastSaleTotal = total
                        lastSaleChange = change
                        showCheckoutDialog = false
                        showSuccessDialog = true
                        cartItems.clear()
                    },
                    enabled = paymentAmount >= total || paymentAmountInput.isEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(stringResource(Res.string.register_sale_button))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCheckoutDialog = false }) {
                    Text(stringResource(Res.string.cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    // Success Dialog
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            modifier = Modifier.onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown && 
                    (keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter)
                ) {
                    showSuccessDialog = false
                    true
                } else false
            },
            shape = MaterialTheme.shapes.large,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            icon = {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(48.dp)
                )
            },
            title = { Text(stringResource(Res.string.sale_success_title), fontWeight = FontWeight.Bold) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(Res.string.sale_success_message), textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        stringResource(Res.string.total_charged_label, lastSaleTotal.toString().formatPrice()),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        stringResource(Res.string.change_delivered_label, lastSaleChange.toString().formatPrice()),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showSuccessDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(stringResource(Res.string.understood_button))
                }
            }
        )
    }

    // Unregistered Product Dialog
    val notRegisteredCategory = stringResource(Res.string.not_registered)

    if (showUnregisteredDialog) {
        LaunchedEffect(Unit) {
            delay(50.milliseconds)
            unregisteredFocusRequester.requestFocus()
        }

        AlertDialog(
            onDismissRequest = { showUnregisteredDialog = false },
            modifier = Modifier.onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown && 
                    (keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter)
                ) {
                    val isNameValid = unregisteredName.isNotBlank()
                    val priceVal = unregisteredPrice.toDoubleOrNull()
                    val isPriceValid = priceVal != null && priceVal > 0
                    val qtyVal = unregisteredQuantity.toDoubleOrNull()
                    val isQtyValid = qtyVal != null && qtyVal > 0
                    if (isNameValid && isPriceValid && isQtyValid) {
                        val dummyProduct = Products(
                            id = "UNREG-${generateUUID()}",
                            codigos = "[]",
                            nombre = unregisteredName.trim(),
                            precio = priceVal,
                            costo = 0.0,
                            categoria = notRegisteredCategory,
                            activo = 1L,
                            por_peso = if (qtyVal % 1.0 != 0.0) 1L else 0L,
                            precio_mayoreo = 0.0,
                            es_favorito = 0L,
                            updated_at = currentTimeMillis(),
                            sync_state = "PENDING_INSERT"
                        )
                        addProductToCart(dummyProduct, qtyVal)
                        showUnregisteredDialog = false
                        true
                    } else false
                } else false
            },
            shape = MaterialTheme.shapes.large,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            title = {
                Text(
                    text = stringResource(Res.string.sell_unregistered_title),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = unregisteredName,
                        onValueChange = { unregisteredName = it },
                        label = { Text(stringResource(Res.string.header_product_name)) },
                        placeholder = { Text(stringResource(Res.string.unregistered_name_placeholder)) },
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(unregisteredFocusRequester),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )

                    OutlinedTextField(
                        value = unregisteredPrice,
                        onValueChange = { input ->
                            if (input.isEmpty() || input.toDoubleOrNull() != null || input.endsWith(".")) {
                                unregisteredPrice = input
                            }
                        },
                        label = { Text(stringResource(Res.string.unit_price_label)) },
                        placeholder = { Text("0.00") },
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )

                    OutlinedTextField(
                        value = unregisteredQuantity,
                        onValueChange = { input ->
                            if (input.isEmpty() || input.toDoubleOrNull() != null || input.endsWith(".")) {
                                unregisteredQuantity = input
                            }
                        },
                        label = { Text(stringResource(Res.string.quantity_weight_label)) },
                        placeholder = { Text(stringResource(Res.string.default_quantity_placeholder)) },
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                }
            },
            confirmButton = {
                val isNameValid = unregisteredName.isNotBlank()
                val priceVal = unregisteredPrice.toDoubleOrNull()
                val isPriceValid = priceVal != null && priceVal > 0
                val qtyVal = unregisteredQuantity.toDoubleOrNull()
                val isQtyValid = qtyVal != null && qtyVal > 0

                Button(
                    onClick = {
                        if (isNameValid && isPriceValid && isQtyValid) {
                            val dummyProduct = Products(
                                id = "UNREG-${generateUUID()}",
                                codigos = "[]",
                                nombre = unregisteredName.trim(),
                                precio = priceVal,
                                costo = 0.0,
                                categoria = notRegisteredCategory,
                                activo = 1L,
                                por_peso = if (qtyVal % 1.0 != 0.0) 1L else 0L,
                                precio_mayoreo = 0.0,
                                es_favorito = 0L,
                                updated_at = currentTimeMillis(),
                                sync_state = "PENDING_INSERT"
                            )
                            addProductToCart(dummyProduct, qtyVal)
                            showUnregisteredDialog = false
                        }
                    },
                    enabled = isNameValid && isPriceValid && isQtyValid,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(stringResource(Res.string.add_to_ticket_button))
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnregisteredDialog = false }) {
                    Text(stringResource(Res.string.cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    // PRODUCT FORM DIALOG (from context menu)
    if (showProductDialogFor != null) {
        ProductFormDialog(
            product = showProductDialogFor,
            onDismiss = { showProductDialogFor = null },
            onSave = { updatedProduct ->
                repository.updateProduct(updatedProduct)
                showProductDialogFor = null
            }
        )
    }
}
