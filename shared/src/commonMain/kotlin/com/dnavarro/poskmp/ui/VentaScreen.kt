package com.dnavarro.poskmp.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.SupportingPaneScaffold
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldRole
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirectiveWithTwoPanesOnMediumWidth
import androidx.compose.material3.adaptive.layout.rememberPaneExpansionState
import androidx.compose.material3.adaptive.navigation.rememberSupportingPaneScaffoldNavigator
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dnavarro.poskmp.db.Products
import com.dnavarro.poskmp.domain.model.PaymentMethod
import com.dnavarro.poskmp.theme.ShapeDefaults
import com.dnavarro.poskmp.ui.venta.CustomerSelectionDialog
import com.dnavarro.poskmp.ui.venta.ReceiptPreviewDialog
import com.dnavarro.poskmp.ui.venta.VentaViewModel
import com.dnavarro.poskmp.util.PlatformBackHandler
import com.dnavarro.poskmp.util.SoundManager
import com.dnavarro.poskmp.util.currentTimeMillis
import com.dnavarro.poskmp.util.formatPrice
import com.dnavarro.poskmp.util.generateUUID
import com.dnavarro.poskmp.util.isAndroid
import com.dnavarro.poskmp.util.roundPrice
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import poskmp.shared.generated.resources.Res
import poskmp.shared.generated.resources.add_button
import poskmp.shared.generated.resources.add_to_ticket_button
import poskmp.shared.generated.resources.badge_customer_always_wholesale
import poskmp.shared.generated.resources.barcode_not_found_message
import poskmp.shared.generated.resources.barcode_not_found_title
import poskmp.shared.generated.resources.btn_go_back_to_ticket
import poskmp.shared.generated.resources.cancel
import poskmp.shared.generated.resources.card
import poskmp.shared.generated.resources.card_payment_blocked_wholesale_warning
import poskmp.shared.generated.resources.cash_received_label
import poskmp.shared.generated.resources.change_to_deliver_label
import poskmp.shared.generated.resources.checkout_change_label
import poskmp.shared.generated.resources.checkout_field_credito
import poskmp.shared.generated.resources.checkout_field_efectivo
import poskmp.shared.generated.resources.checkout_field_tarjeta
import poskmp.shared.generated.resources.checkout_field_transferencia
import poskmp.shared.generated.resources.checkout_missing_to_cover_label
import poskmp.shared.generated.resources.checkout_pay_and_print_button
import poskmp.shared.generated.resources.checkout_pay_without_print_button
import poskmp.shared.generated.resources.checkout_sale_title
import poskmp.shared.generated.resources.checkout_total_received_label
import poskmp.shared.generated.resources.close
import poskmp.shared.generated.resources.close_button
import poskmp.shared.generated.resources.credit_charge_summary
import poskmp.shared.generated.resources.credit_limit_exceeded_warning
import poskmp.shared.generated.resources.credit_sale_requires_customer_error
import poskmp.shared.generated.resources.customer_balance_format
import poskmp.shared.generated.resources.default_quantity_placeholder
import poskmp.shared.generated.resources.header_product_name
import poskmp.shared.generated.resources.insufficient_amount_error
import poskmp.shared.generated.resources.kg_suffix
import poskmp.shared.generated.resources.mixed_card_payment_blocked_wholesale_warning
import poskmp.shared.generated.resources.mixed_credit_requires_customer_error
import poskmp.shared.generated.resources.money
import poskmp.shared.generated.resources.money_transfer
import poskmp.shared.generated.resources.not_registered
import poskmp.shared.generated.resources.payment_method_credito
import poskmp.shared.generated.resources.payment_method_efectivo
import poskmp.shared.generated.resources.payment_method_mixto
import poskmp.shared.generated.resources.payment_method_tarjeta
import poskmp.shared.generated.resources.payment_method_transferencia
import poskmp.shared.generated.resources.payments
import poskmp.shared.generated.resources.person
import poskmp.shared.generated.resources.pesos_currency_label
import poskmp.shared.generated.resources.price_per_kg_label
import poskmp.shared.generated.resources.quantity_prompt_title
import poskmp.shared.generated.resources.quantity_weight_label
import poskmp.shared.generated.resources.sad_face
import poskmp.shared.generated.resources.save_unregistered_to_db
import poskmp.shared.generated.resources.sell_unregistered_title
import poskmp.shared.generated.resources.total_to_pay_label
import poskmp.shared.generated.resources.unit_price_label
import poskmp.shared.generated.resources.unregistered_name_placeholder
import poskmp.shared.generated.resources.weight_kg_label
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

data class CartItem(
    val product: Products,
    var quantity: Double,
    val originalPrice: Double = product.precio
)

@Suppress("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3AdaptiveApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun VentaScreen(
    viewModel: VentaViewModel,
    isCompact: Boolean,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Si no hay un turno de caja abierto, mostrar la vista de apertura de turno en toda la pantalla
    if (uiState.activeShift == null) {
        com.dnavarro.poskmp.ui.turnos.OpenShiftView(
            cashiers = uiState.cashiers,
            isOpening = uiState.isOpeningShift,
            errorMessage = uiState.openShiftError,
            onOpenShift = { cashierId, pin, initialCash ->
                viewModel.openShift(cashierId, pin, initialCash)
            },
            onClearError = {
                viewModel.clearOpenShiftError()
            },
            modifier = modifier
        )
        return
    }

    LaunchedEffect(uiState.lastReceipt?.folio) {
        if (uiState.lastReceipt != null) {
            viewModel.printLastReceipt()
        }
    }
    val searchQuery = uiState.searchQuery
    val productsList = uiState.activeProducts
    val cartItems = uiState.cartItems
    var selectedIndex by remember { mutableIntStateOf(-1) }

    // Weight Dialog state
    var showWeightDialogForProduct by remember { mutableStateOf<Products?>(null) }
    var weightInput by remember { mutableStateOf("1.000") }

    // Checkout Dialog state
    var showCheckoutDialog by remember { mutableStateOf(false) }
    var selectedPaymentMethod by remember { mutableStateOf(PaymentMethod.EFECTIVO) }
    var paymentAmountInput by remember { mutableStateOf(TextFieldValue("")) }
    var mixedCashInput by remember { mutableStateOf("") }
    var mixedCardInput by remember { mutableStateOf("") }
    var mixedTransferInput by remember { mutableStateOf("") }
    var mixedCreditInput by remember { mutableStateOf("") }
    var lastSaleTotal by remember { mutableDoubleStateOf(0.0) }
    var lastSaleChange by remember { mutableDoubleStateOf(0.0) }
    var lastSaleFolio by remember { mutableLongStateOf(0L) }
    val checkoutFocusRequester = remember { FocusRequester() }

    // Unregistered Product Dialog state
    var showUnregisteredDialog by remember { mutableStateOf(false) }
    var unregisteredName by remember { mutableStateOf("") }
    var unregisteredPrice by remember { mutableStateOf("") }
    var unregisteredQuantity by remember { mutableStateOf("1") }
    var saveUnregisteredToDatabase by remember { mutableStateOf(false) }
    val unregisteredFocusRequester = remember { FocusRequester() }

    fun openUnregisteredDialog() {
        unregisteredName = ""
        unregisteredPrice = ""
        unregisteredQuantity = "1"
        saveUnregisteredToDatabase = false
        showUnregisteredDialog = true
    }

    // Edit Product Dialog state (from context menu)
    var showProductDialogFor by remember { mutableStateOf<Products?>(null) }

    // Camera Barcode Scanner state
    var showCameraScanner by remember { mutableStateOf(false) }
    var lastScannedProduct by remember { mutableStateOf<Products?>(null) }
    var cameraScannerFeedback by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(cameraScannerFeedback) {
        if (cameraScannerFeedback != null) {
            delay(2500.milliseconds)
            cameraScannerFeedback = null
        }
    }

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

    PlatformBackHandler(enabled = navigator.canNavigateBack()) {
        coroutineScope.launch {
            navigator.navigateBack()
        }
    }
    val paneExpansionState = rememberPaneExpansionState()
    var userCatalogWidthDp by remember { mutableStateOf<Dp?>(null) }

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
            SoundManager.playErrorSound()
            delay(3000.milliseconds)
            showBarcodeNotFoundQuery = null
        }
    }

    LaunchedEffect(showWeightDialogForProduct) {
        if (showWeightDialogForProduct != null) {
            SoundManager.playErrorSound()
        }
    }

    LaunchedEffect(showWeightDialogForProduct, showCheckoutDialog, showUnregisteredDialog, showProductDialogFor) {
        if (showWeightDialogForProduct == null && !showCheckoutDialog && !showUnregisteredDialog && showProductDialogFor == null) {
            reclaimSearchBarFocus()
        }
    }

    fun toggleProductFavorite(product: Products) {
        viewModel.toggleProductFavorite(product)
        reclaimSearchBarFocus()
    }



    // Active products are observed via viewModel.uiState

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val desktopFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        if (!isAndroid()) {
            delay(100.milliseconds)
            try {
                searchBarFocusRequester.requestFocus()
            } catch (_: Exception) {}
        } else {
            delay(100.milliseconds)
            focusManager.clearFocus(force = true)
            keyboardController?.hide()
        }
    }

    // Helper: Add/Update product quantity in cart
    fun addProductToCart(product: Products, qty: Double) {
        viewModel.addProductToCart(product, qty)
        reclaimSearchBarFocus()
    }

    fun setProductQuantityInCart(product: Products, qty: Double) {
        viewModel.setProductQuantityInCart(product, qty)
        reclaimSearchBarFocus()
    }

    fun toggleWholesalePrice() {
        viewModel.toggleWholesalePrice()
        reclaimSearchBarFocus()
    }

    fun removeCartItem(item: CartItem) {
        viewModel.removeCartItem(item)
        reclaimSearchBarFocus()
    }

    fun clearCart() {
        viewModel.clearCart()
        reclaimSearchBarFocus()
    }

    fun undoLastCartChange() {
        viewModel.undoLastCartChange()
        reclaimSearchBarFocus()
    }

    val barcodeScanCallback: (String) -> Unit = { barcode ->
        coroutineScope.launch {
            val trimmed = barcode.trim()
            val p = viewModel.findProductByBarcode(trimmed)
            if (p != null) {
                if (p.por_peso == 1L) {
                    weightInput = "1.000"
                    showWeightDialogForProduct = p
                } else {
                    addProductToCart(p, 1.0)
                }
                viewModel.onSearchQueryChanged("")
            } else {
                showBarcodeNotFoundQuery = trimmed
                viewModel.onSearchQueryChanged("")
                reclaimSearchBarFocus()
            }
        }
    }

    val cameraScanCallback: (String) -> Unit = { barcode ->
        coroutineScope.launch {
            val trimmed = barcode.trim()
            val p = viewModel.findProductByBarcode(trimmed)
            if (p != null) {
                if (p.por_peso == 1L) {
                    weightInput = "1.000"
                    showWeightDialogForProduct = p
                    lastScannedProduct = p
                    cameraScannerFeedback = null
                } else {
                    addProductToCart(p, 1.0)
                    lastScannedProduct = p
                    cameraScannerFeedback = null
                }
                viewModel.onSearchQueryChanged("")
            } else {
                lastScannedProduct = null
                cameraScannerFeedback = "Producto no encontrado: $trimmed"
                SoundManager.playErrorSound()
                viewModel.onSearchQueryChanged("")
            }
        }
    }

    val currentScannedQuantity = remember(cartItems, lastScannedProduct) {
        cartItems.find { it.product.id == lastScannedProduct?.id }?.quantity ?: 1.0
    }

    val handleUndoLastScan: () -> Unit = {
        lastScannedProduct?.let { product ->
            val cartItem = cartItems.find { it.product.id == product.id }
            if (cartItem != null) {
                removeCartItem(cartItem)
            }
        }
        lastScannedProduct = null
        cameraScannerFeedback = null
    }

    val handleQuantityChange: (Double) -> Unit = { delta ->
        lastScannedProduct?.let { product ->
            addProductToCart(product, delta)
            val updatedItem = cartItems.find { it.product.id == product.id }
            if (updatedItem == null || updatedItem.quantity <= 0.0) {
                lastScannedProduct = null
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

            val shouldIntercept = isUp || isDown || isDelete || isPlus || isMinus

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
                            removeCartItem(currentItem)
                            if (selectedIndex >= cartItems.size) {
                                selectedIndex = cartItems.size - 1
                            }
                        }
                    }
                }
                true
            } else false
        } else false
    }

    val total = uiState.total

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
                                        viewModel.toggleWholesalePrice()
                                        true
                                    } else {
                                        if (cartItems.isNotEmpty()) {
                                            val currentIndex = selectedIndex.coerceIn(0, cartItems.lastIndex)
                                            val currentItem = cartItems[currentIndex]
                                            viewModel.toggleWholesalePriceForItem(currentItem)
                                            true
                                        } else false
                                    }
                                }

                                Key.F12 -> {
                                    paymentAmountInput = TextFieldValue("")
                                    showCheckoutDialog = true
                                    true
                                }

                                else -> false
                            }
                        }
                } else Modifier
            )
    ) {
        PullToRefreshBox(
            isRefreshing = uiState.isSyncing,
            onRefresh = { viewModel.refreshSync() },
            modifier = Modifier.fillMaxSize()
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
            val totalWidth = maxWidth
            val minCatalogWidth = 320.dp
            val minTicketWidth = 280.dp
            val splitterWidth = 10.dp
            val isTwoPaneWide = totalWidth >= (minCatalogWidth + minTicketWidth + splitterWidth) && !isCompact

            if (isTwoPaneWide) {
                val maxCatalogAllowed = totalWidth - minTicketWidth - splitterWidth
                val defaultCatalogWidth = (totalWidth - splitterWidth) * 0.6f
                val catalogWidth = (userCatalogWidthDp ?: defaultCatalogWidth).coerceIn(
                    minCatalogWidth,
                    maxCatalogAllowed
                )

                Row(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.width(catalogWidth).fillMaxHeight()) {
                        CatalogSection(
                            searchQuery = searchQuery,
                            onSearchQueryChange = { viewModel.onSearchQueryChanged(it) },
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
                            isCompact = false,
                            onViewCartClick = null,
                            onOpenScanner = { showCameraScanner = true },
                            cartCount = cartItems.size,
                            cartTotal = total,
                            onSellUnregisteredClick = { openUnregisteredDialog() },
                            onApplyItemWholesaleClick = {
                                if (cartItems.isNotEmpty()) {
                                    val currentIndex = selectedIndex.coerceIn(0, cartItems.lastIndex)
                                    val currentItem = cartItems[currentIndex]
                                    viewModel.toggleWholesalePriceForItem(currentItem)
                                    reclaimSearchBarFocus()
                                }
                            },
                            onApplyWholesaleClick = { toggleWholesalePrice() },
                            onCheckoutClick = {
                                paymentAmountInput = TextFieldValue("")
                                showCheckoutDialog = true
                            },
                            searchFocusRequester = searchBarFocusRequester,
                            onBarcodeScan = barcodeScanCallback,
                            onSearchKeyIntercept = handleSearchKeyIntercept
                        )
                    }

                    // Draggable Splitter with strict min width bounds
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(splitterWidth)
                            .pointerHoverIcon(PointerIcon.Hand)
                            .pointerInput(totalWidth) {
                                detectHorizontalDragGestures { change, dragAmount ->
                                    change.consume()
                                    val dragAmountDp = dragAmount.toDp()
                                    val currentW = userCatalogWidthDp ?: defaultCatalogWidth
                                    val maxAllowed = totalWidth - minTicketWidth - splitterWidth
                                    userCatalogWidthDp = (currentW + dragAmountDp).coerceIn(minCatalogWidth, maxAllowed)
                                }
                            }
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

                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        TicketSection(
                            cartItems = cartItems,
                            total = total,
                            onClearCart = { clearCart() },
                            onUpdateQuantity = { item, delta -> addProductToCart(item.product, delta) },
                            onSetQuantity = { item, qty -> setProductQuantityInCart(item.product, qty) },
                            onRemoveItem = { item -> removeCartItem(item) },
                            onCheckout = {
                                paymentAmountInput = TextFieldValue("")
                                showCheckoutDialog = true
                            },
                            selectedIndex = selectedIndex,
                            onSelectedIndexChange = { selectedIndex = it },
                            onBackClick = null,
                            canUndo = uiState.canUndo,
                            onUndo = { undoLastCartChange() },
                            heldTickets = uiState.heldTickets,
                            onHoldTicket = {
                                viewModel.putCurrentTicketOnHold()
                                reclaimSearchBarFocus()
                            },
                            onResumeHeldTicket = { ticket ->
                                viewModel.resumeHeldTicket(ticket)
                                reclaimSearchBarFocus()
                            },
                            onDiscardHeldTicket = { ticket ->
                                viewModel.discardHeldTicket(ticket)
                                reclaimSearchBarFocus()
                            },
                            selectedCustomer = uiState.selectedCustomer,
                            onAssignCustomerClick = { viewModel.setShowCustomerDialog(true) },
                            onClearCustomerClick = { viewModel.clearSelectedCustomer() }
                        )
                    }
                }
            } else {
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
                                onSearchQueryChange = { viewModel.onSearchQueryChanged(it) },
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
                                onOpenScanner = { showCameraScanner = true },
                                cartCount = cartItems.size,
                                cartTotal = total,
                                onSellUnregisteredClick = { openUnregisteredDialog() },
                                onApplyItemWholesaleClick = {
                                    if (cartItems.isNotEmpty()) {
                                        val currentIndex = selectedIndex.coerceIn(0, cartItems.lastIndex)
                                        val currentItem = cartItems[currentIndex]
                                        viewModel.toggleWholesalePriceForItem(currentItem)
                                        reclaimSearchBarFocus()
                                    }
                                },
                                onApplyWholesaleClick = { toggleWholesalePrice() },
                                onCheckoutClick = {
                                    paymentAmountInput = TextFieldValue("")
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
                                onClearCart = { clearCart() },
                                onUpdateQuantity = { item, delta -> addProductToCart(item.product, delta) },
                                onSetQuantity = { item, qty -> setProductQuantityInCart(item.product, qty) },
                                onRemoveItem = { item -> removeCartItem(item) },
                                onCheckout = {
                                    paymentAmountInput = TextFieldValue("")
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
                                } else null,
                                canUndo = uiState.canUndo,
                                onUndo = { undoLastCartChange() },
                                heldTickets = uiState.heldTickets,
                                onHoldTicket = {
                                    viewModel.putCurrentTicketOnHold()
                                    reclaimSearchBarFocus()
                                },
                                onResumeHeldTicket = { ticket ->
                                    viewModel.resumeHeldTicket(ticket)
                                    reclaimSearchBarFocus()
                                },
                                onDiscardHeldTicket = { ticket ->
                                    viewModel.discardHeldTicket(ticket)
                                    reclaimSearchBarFocus()
                                },
                                selectedCustomer = uiState.selectedCustomer,
                                onAssignCustomerClick = { viewModel.setShowCustomerDialog(true) },
                                onClearCustomerClick = { viewModel.clearSelectedCustomer() }
                            )
                        }
                    }
                )
            }

            // Non-blocking Barcode Not Found Toast Notification
            AnimatedVisibility(
                visible = showBarcodeNotFoundQuery != null,
                enter = fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) + slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)
                ),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
                    .zIndex(100f)
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    shape = ShapeDefaults.cardShape,
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .widthIn(max = 480.dp)
                        .clickable { showBarcodeNotFoundQuery = null }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.sad_face),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(Res.string.barcode_not_found_title),
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = stringResource(
                                    Res.string.barcode_not_found_message,
                                    showBarcodeNotFoundQuery ?: ""
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.9f)
                            )
                        }
                        IconButton(
                            onClick = { showBarcodeNotFoundQuery = null },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.close),
                                contentDescription = stringResource(Res.string.close_button),
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
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
            if (newPrice.isEmpty() || newPrice.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
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
        }


        BasicAlertDialog(
            onDismissRequest = { showWeightDialogForProduct = null },
            modifier = Modifier.background(
                MaterialTheme.colorScheme.surfaceContainerLowest, ShapeDefaults.cardShape
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
                                    val text = newValue.text
                                    if (text.isEmpty() || text.matches(Regex("^\\d*\\.?\\d{0,3}$"))) {
                                        weightInputValue = newValue
                                        val weight = text.toDoubleOrNull()
                                        if (weight != null && weight >= 0.0) {
                                            val calcPrice = weight * product.precio
                                            priceInputValue = if (calcPrice % 1.0 == 0.0) calcPrice.toInt()
                                                .toString() else ((calcPrice * 100.0).roundToInt() / 100.0).toString()
                                        } else if (text.isEmpty()) {
                                            priceInputValue = ""
                                        }
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
                                prefix = { Text("$", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
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
        LaunchedEffect(Unit) {
            val formattedTotal = if (total % 1.0 == 0.0) total.toInt().toString() else ((total * 100.0).roundToInt() / 100.0).toString()
            paymentAmountInput = TextFieldValue(
                text = formattedTotal,
                selection = TextRange(0, formattedTotal.length)
            )
            mixedCashInput = ""
            mixedCardInput = ""
            mixedTransferInput = ""
            mixedCreditInput = ""
            delay(50.milliseconds)
            checkoutFocusRequester.requestFocus()
        }

        val paymentText = paymentAmountInput.text
        val paymentAmount = paymentText.toDoubleOrNull() ?: 0.0
        val change = if (paymentAmount >= total) paymentAmount - total else 0.0
        val selectedCustomer = uiState.selectedCustomer

        val mCash = mixedCashInput.toDoubleOrNull() ?: 0.0
        val mCard = mixedCardInput.toDoubleOrNull() ?: 0.0
        val mTransfer = mixedTransferInput.toDoubleOrNull() ?: 0.0
        val mCredit = mixedCreditInput.toDoubleOrNull() ?: 0.0

        val totalReceivedMixto = mCash + mCard + mTransfer + mCredit
        val nonCashImmediate = mCard + mTransfer
        val remainingNeededForCash = (total - nonCashImmediate - mCredit).coerceAtLeast(0.0)
        val cambioMixto = if (mCash > remainingNeededForCash && totalReceivedMixto >= total) {
            mCash - remainingNeededForCash
        } else 0.0

        val hasWholesaleProducts = cartItems.any { it.product.precio == it.product.precio_mayoreo && it.product.precio_mayoreo > 0.0 }
        val isCardBlockedByWholesale = uiState.disallowCardPaymentOnWholesale && hasWholesaleProducts

        val isCheckoutValid = when (selectedPaymentMethod) {
            PaymentMethod.EFECTIVO -> paymentAmount >= total || paymentText.isEmpty()
            PaymentMethod.TARJETA -> !isCardBlockedByWholesale
            PaymentMethod.TRANSFERENCIA -> true
            PaymentMethod.MIXTO -> totalReceivedMixto >= total && (mCredit == 0.0 || selectedCustomer != null) && (!isCardBlockedByWholesale || mCard == 0.0)
            PaymentMethod.CREDITO -> selectedCustomer != null
        }

        val performCheckout: (Boolean) -> Unit = { printReceipt ->
            val (finalPayment, finalChange) = when (selectedPaymentMethod) {
                PaymentMethod.EFECTIVO -> Pair(
                    if (paymentText.isEmpty()) total else paymentAmount,
                    if (paymentText.isEmpty()) 0.0 else change
                )
                PaymentMethod.TARJETA, PaymentMethod.TRANSFERENCIA -> Pair(total, 0.0)
                PaymentMethod.MIXTO -> Pair(
                    (total - mCredit).coerceAtLeast(0.0),
                    cambioMixto
                )
                PaymentMethod.CREDITO -> Pair(0.0, 0.0)
            }
            lastSaleTotal = total
            lastSaleChange = finalChange
            showCheckoutDialog = false
            coroutineScope.launch {
                val folio = viewModel.processCheckout(
                    pagoCon = finalPayment,
                    cambio = finalChange,
                    metodoPago = selectedPaymentMethod.name,
                    customerId = selectedCustomer?.id,
                    printReceipt = printReceipt
                )
                lastSaleFolio = folio
            }
        }

        AlertDialog(
            onDismissRequest = { showCheckoutDialog = false },
            modifier = Modifier.onPreviewKeyEvent { keyEvent ->
                keyEvent.type == KeyEventType.KeyDown &&
                        (keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter) && if (isCheckoutValid) {
                    performCheckout(true)
                    true
                } else false
            },
            shape = MaterialTheme.shapes.large,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            title = { Text(stringResource(Res.string.checkout_sale_title), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                    // Payment Method Choice Buttons (Row 1: Efectivo, Tarjeta, Transferencia | Row 2: Mixto, Crédito)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        val row1Methods = listOf(
                            PaymentMethod.EFECTIVO to (stringResource(Res.string.payment_method_efectivo) to Res.drawable.money),
                            PaymentMethod.TARJETA to (stringResource(Res.string.payment_method_tarjeta) to Res.drawable.card),
                            PaymentMethod.TRANSFERENCIA to (stringResource(Res.string.payment_method_transferencia) to Res.drawable.money_transfer)
                        )
                        val row2Methods = listOf(
                            PaymentMethod.MIXTO to (stringResource(Res.string.payment_method_mixto) to Res.drawable.payments),
                            PaymentMethod.CREDITO to (stringResource(Res.string.payment_method_credito) to Res.drawable.person)
                        )

                        listOf(row1Methods, row2Methods).forEach { rowMethods ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                rowMethods.forEachIndexed { index, (method, info) ->
                                    val (label, icon) = info
                                    val isSelected = selectedPaymentMethod == method
                                    ToggleButton(
                                        checked = isSelected,
                                        onCheckedChange = {
                                            selectedPaymentMethod = method
                                            if (method != PaymentMethod.EFECTIVO) {
                                                paymentAmountInput = TextFieldValue("")
                                            }
                                        },
                                        colors = ToggleButtonDefaults.toggleButtonColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            checkedContainerColor = MaterialTheme.colorScheme.primary,
                                            checkedContentColor = MaterialTheme.colorScheme.onPrimary
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .semantics { role = Role.RadioButton },
                                        shapes = when (index) {
                                            0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                            rowMethods.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                                        }
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center,
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        ) {
                                            Icon(
                                                painter = painterResource(icon),
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = label,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Active Customer Badge (if selected)
                    if (selectedCustomer != null) {
                        Surface(
                            shape = ShapeDefaults.cardShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(Res.drawable.person),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = selectedCustomer.nombre,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (selectedCustomer.siempreMayoreo) {
                                            Surface(
                                                shape = MaterialTheme.shapes.extraSmall,
                                                color = MaterialTheme.colorScheme.tertiaryContainer
                                            ) {
                                                Text(
                                                    text = stringResource(Res.string.badge_customer_always_wholesale),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }
                                    if (selectedCustomer.saldoDeudor > 0.0) {
                                        Text(
                                            text = stringResource(Res.string.customer_balance_format, selectedCustomer.saldoDeudor.toString().formatPrice()),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.error,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }

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

                    // Conditional payment fields based on PaymentMethod
                    when (selectedPaymentMethod) {
                        PaymentMethod.EFECTIVO -> {
                            OutlinedTextField(
                                value = paymentAmountInput,
                                onValueChange = { newValue ->
                                    val text = newValue.text
                                    if (text.isEmpty() || text.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                                        paymentAmountInput = newValue
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().focusRequester(checkoutFocusRequester),
                                prefix = { Text("$ ", fontWeight = FontWeight.Bold) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                label = { Text(stringResource(Res.string.cash_received_label)) },
                                placeholder = { Text("0.00") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                )
                            )

                            if (paymentText.isNotEmpty() && paymentAmount < total) {
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

                        PaymentMethod.MIXTO -> {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                MixedPaymentRow(
                                    iconRes = Res.drawable.money,
                                    label = stringResource(Res.string.checkout_field_efectivo),
                                    value = mixedCashInput,
                                    onValueChange = { mixedCashInput = it },
                                    placeholder = "0.00"
                                )
                                MixedPaymentRow(
                                    iconRes = Res.drawable.card,
                                    label = stringResource(Res.string.checkout_field_tarjeta),
                                    value = mixedCardInput,
                                    onValueChange = { mixedCardInput = it },
                                    placeholder = "0.00"
                                )
                                MixedPaymentRow(
                                    iconRes = Res.drawable.money_transfer,
                                    label = stringResource(Res.string.checkout_field_transferencia),
                                    value = mixedTransferInput,
                                    onValueChange = { mixedTransferInput = it },
                                    placeholder = "0.00"
                                )
                                MixedPaymentRow(
                                    iconRes = Res.drawable.person,
                                    label = stringResource(Res.string.checkout_field_credito),
                                    value = mixedCreditInput,
                                    onValueChange = { mixedCreditInput = it },
                                    placeholder = "0.00"
                                )

                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(Res.string.checkout_total_received_label),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "$${totalReceivedMixto.toString().formatPrice()}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                val missingToCover = (total - totalReceivedMixto).coerceAtLeast(0.0)
                                if (missingToCover > 0.0) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = stringResource(Res.string.checkout_missing_to_cover_label),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                        Text(
                                            text = "$${missingToCover.toString().formatPrice()}",
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                } else if (cambioMixto > 0.0) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = stringResource(Res.string.checkout_change_label),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "$${cambioMixto.toString().formatPrice()}",
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                if (mCredit > 0.0) {
                                    if (selectedCustomer == null) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                                            shape = MaterialTheme.shapes.small,
                                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(10.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text(
                                                    text = stringResource(Res.string.mixed_credit_requires_customer_error),
                                                    color = MaterialTheme.colorScheme.error,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                OutlinedButton(
                                                    onClick = { showCheckoutDialog = false },
                                                    modifier = Modifier.align(Alignment.End),
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                                ) {
                                                    Text(stringResource(Res.string.btn_go_back_to_ticket), fontSize = 12.sp)
                                                }
                                            }
                                        }
                                    } else {
                                        val limit = selectedCustomer.limiteCredito
                                        val exceeds = limit > 0.0 && (selectedCustomer.saldoDeudor + mCredit) > limit
                                        if (exceeds) {
                                            Text(
                                                text = stringResource(Res.string.credit_limit_exceeded_warning, limit.toString().formatPrice()),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.error,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(top = 2.dp)
                                            )
                                        }
                                    }
                                }

                                if (mCard > 0.0 && isCardBlockedByWholesale) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                                        shape = MaterialTheme.shapes.small,
                                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                                    ) {
                                        Text(
                                            text = stringResource(Res.string.mixed_card_payment_blocked_wholesale_warning),
                                            color = MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }
                                }
                            }
                        }

                        PaymentMethod.CREDITO -> {
                            if (selectedCustomer == null) {
                                Surface(
                                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                                    shape = MaterialTheme.shapes.small,
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = stringResource(Res.string.credit_sale_requires_customer_error),
                                            color = MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        OutlinedButton(
                                            onClick = { showCheckoutDialog = false },
                                            modifier = Modifier.align(Alignment.End),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                        ) {
                                            Text(stringResource(Res.string.btn_go_back_to_ticket), fontSize = 12.sp)
                                        }
                                    }
                                }
                            } else {
                                val limit = selectedCustomer.limiteCredito
                                val exceeds = limit > 0.0 && (selectedCustomer.saldoDeudor + total) > limit
                                Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                                    Text(
                                        text = stringResource(Res.string.credit_charge_summary, total.toString().formatPrice()),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (exceeds) {
                                        Text(
                                            text = stringResource(Res.string.credit_limit_exceeded_warning, limit.toString().formatPrice()),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.error,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        PaymentMethod.TARJETA -> {
                            if (isCardBlockedByWholesale) {
                                Surface(
                                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                                    shape = MaterialTheme.shapes.small,
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            painter = painterResource(Res.drawable.card),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = stringResource(Res.string.card_payment_blocked_wholesale_warning),
                                            color = MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        PaymentMethod.TRANSFERENCIA -> {
                            // Exact payment for digital transactions
                        }
                    }
                }
            },
            confirmButton = {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    itemVerticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { performCheckout(false) },
                        enabled = isCheckoutValid,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(stringResource(Res.string.checkout_pay_without_print_button))
                    }
                    Button(
                        onClick = { performCheckout(true) },
                        enabled = isCheckoutValid,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(stringResource(Res.string.checkout_pay_and_print_button))
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showCheckoutDialog = false }) {
                    Text(stringResource(Res.string.cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    uiState.lastReceipt?.let { receipt ->
        ReceiptPreviewDialog(
            receipt = receipt,
            isPrinting = uiState.isPrintingReceipt,
            printSuccessful = uiState.receiptPrintSuccessful,
            printError = uiState.receiptPrintError,
            onPrint = { viewModel.printLastReceipt() },
            onDismiss = { viewModel.dismissLastReceipt() }
        )
    }

    if (uiState.showCustomerDialog) {
        CustomerSelectionDialog(
            customers = uiState.filteredCustomers,
            searchQuery = uiState.customerSearchQuery,
            selectedCustomer = uiState.selectedCustomer,
            onSearchQueryChange = { viewModel.onCustomerSearchQueryChange(it) },
            onSelectCustomer = { viewModel.selectCustomer(it) },
            onDismissRequest = { viewModel.setShowCustomerDialog(false) }
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
                            piezas = 1.0,
                            updated_at = currentTimeMillis(),
                            sync_state = "PENDING_INSERT"
                        )
                        if (saveUnregisteredToDatabase) {
                            viewModel.saveProduct(dummyProduct)
                        }
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
                            if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                                unregisteredPrice = input
                            }
                        },
                        label = { Text(stringResource(Res.string.unit_price_label)) },
                        placeholder = { Text("0.00") },
                        prefix = { Text("$", fontWeight = FontWeight.Bold) },
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
                            if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,3}$"))) {
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

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { saveUnregisteredToDatabase = !saveUnregisteredToDatabase },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = saveUnregisteredToDatabase,
                            onCheckedChange = { saveUnregisteredToDatabase = it }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(Res.string.save_unregistered_to_db),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
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
                            val effectivePrice = if (uiState.roundRetailPrice && saveUnregisteredToDatabase) roundPrice(priceVal) else priceVal
                            val dummyProduct = Products(
                                id = "UNREG-${generateUUID()}",
                                codigos = "[]",
                                nombre = unregisteredName.trim(),
                                precio = effectivePrice,
                                costo = 0.0,
                                categoria = notRegisteredCategory,
                                activo = 1L,
                                por_peso = if (qtyVal % 1.0 != 0.0) 1L else 0L,
                                precio_mayoreo = 0.0,
                                es_favorito = 0L,
                                piezas = 1.0,
                                updated_at = currentTimeMillis(),
                                sync_state = "PENDING_INSERT"
                            )
                            if (saveUnregisteredToDatabase) {
                                viewModel.saveProduct(dummyProduct)
                            }
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
        val categories = remember(uiState.activeProducts) {
            uiState.activeProducts.mapNotNull { it.categoria }.filter { it.isNotBlank() }.distinct().sorted()
        }
        ProductFormDialog(
            product = showProductDialogFor,
            onDismiss = { showProductDialogFor = null },
            onSave = { updatedProduct ->
                viewModel.updateProduct(updatedProduct)
                showProductDialogFor = null
            },
            existingCategories = categories,
            defaultRetailMarginPercentage = uiState.defaultRetailMargin,
            defaultWholesaleMarginPercentage = uiState.defaultWholesaleMargin,
            roundRetailPrice = uiState.roundRetailPrice,
            roundWholesalePrice = uiState.roundWholesalePrice
        )
    }

    // Camera Scanner Dialog
    if (showCameraScanner) {
        PlatformBarcodeScanner(
            onScanResult = { scannedBarcode -> cameraScanCallback(scannedBarcode) },
            onClose = {
                showCameraScanner = false
                lastScannedProduct = null
                cameraScannerFeedback = null
            },
            statusMessage = cameraScannerFeedback,
            lastScannedProduct = lastScannedProduct,
            lastScannedQuantity = currentScannedQuantity,
            onUndo = handleUndoLastScan,
            onQuantityChange = handleQuantityChange
        )
    }
}

@Composable
private fun MixedPaymentRow(
    iconRes: DrawableResource,
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "0.00"
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        OutlinedTextField(
            value = value,
            onValueChange = { text ->
                if (text.isEmpty() || text.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                    onValueChange(text)
                }
            },
            placeholder = {
                Text(
                    text = placeholder,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            },
            prefix = { Text("$ ") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            textStyle = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.End),
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.width(130.dp)
        )
    }
}

