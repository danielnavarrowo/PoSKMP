package com.dnavarro.poskmp.ui.venta

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dnavarro.poskmp.data.ProductRepository
import com.dnavarro.poskmp.data.SettingsRepository
import com.dnavarro.poskmp.db.Products
import com.dnavarro.poskmp.domain.model.Customer
import com.dnavarro.poskmp.domain.model.ReceiptDocument
import com.dnavarro.poskmp.domain.model.ReceiptItem
import com.dnavarro.poskmp.domain.receipt.ReceiptFormatter
import com.dnavarro.poskmp.domain.usecase.FindProductByBarcodeUseCase
import com.dnavarro.poskmp.domain.usecase.GetCustomersUseCase
import com.dnavarro.poskmp.domain.usecase.GetProductsUseCase
import com.dnavarro.poskmp.domain.usecase.PrintReceiptUseCase
import com.dnavarro.poskmp.domain.usecase.RecordSaleUseCase
import com.dnavarro.poskmp.domain.usecase.SaveProductUseCase
import com.dnavarro.poskmp.ui.CartItem
import com.dnavarro.poskmp.util.currentTimeMillis
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

import com.dnavarro.poskmp.data.sync.SyncRepository
import com.dnavarro.poskmp.data.sync.SyncStateEnum
import kotlinx.coroutines.Dispatchers

/**
 * ViewModel for Venta screen managing product flow, barcode search, customers, and cart updates.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class VentaViewModel(
    private val repository: ProductRepository,
    settingsRepository: SettingsRepository,
    private val getProductsUseCase: GetProductsUseCase = GetProductsUseCase(repository),
    private val findProductByBarcodeUseCase: FindProductByBarcodeUseCase = FindProductByBarcodeUseCase(repository),
    private val saveProductUseCase: SaveProductUseCase = SaveProductUseCase(repository),
    getCustomersUseCase: GetCustomersUseCase,
    private val recordSaleUseCase: RecordSaleUseCase,
    private val syncRepository: SyncRepository,
    private val printReceiptUseCase: PrintReceiptUseCase,
    getActiveShiftUseCase: com.dnavarro.poskmp.domain.usecase.GetActiveShiftUseCase,
    getCashiersUseCase: com.dnavarro.poskmp.domain.usecase.GetCashiersUseCase,
    private val openShiftUseCase: com.dnavarro.poskmp.domain.usecase.OpenShiftUseCase
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategory = MutableStateFlow<String?>(null)
    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    private val _heldTickets = MutableStateFlow<List<HeldTicket>>(emptyList())
    private val _cartHistory = mutableListOf<List<CartItem>>()
    private val _canUndo = MutableStateFlow(false)

    private val _selectedCustomer = MutableStateFlow<Customer?>(null)
    private val _customerSearchQuery = MutableStateFlow("")
    private val _showCustomerDialog = MutableStateFlow(false)
    private val _lastReceipt = MutableStateFlow<ReceiptDocument?>(null)
    private val _printState = MutableStateFlow(ReceiptPrintInternalState())

    private val _isOpeningShift = MutableStateFlow(false)
    private val _openShiftError = MutableStateFlow<String?>(null)

    private data class ReceiptDialogState(
        val customerSearchQuery: String,
        val showCustomerDialog: Boolean,
        val lastReceipt: ReceiptDocument?,
        val printState: ReceiptPrintInternalState
    )

    private data class ShiftState(
        val activeShift: com.dnavarro.poskmp.domain.model.CashierShift?,
        val cashiers: List<com.dnavarro.poskmp.domain.model.Cashier>,
        val isOpeningShift: Boolean,
        val openShiftError: String?
    )

    private data class ReceiptPrintInternalState(
        val isPrinting: Boolean = false,
        val hasError: Boolean = false,
        val successful: Boolean = false
    )

    private val _productsFlow = _searchQuery.flatMapLatest { query ->
        getProductsUseCase(query = query, activeOnly = true)
    }

    private val _shiftFlow = combine(
        getActiveShiftUseCase(),
        getCashiersUseCase(),
        _isOpeningShift,
        _openShiftError
    ) { activeShift, cashiers, isOpening, error ->
        ShiftState(activeShift, cashiers, isOpening, error)
    }

    val uiState: StateFlow<VentaUiState> = combine(
        combine(
            _searchQuery,
            _productsFlow,
            _selectedCategory,
            _cartItems,
            _heldTickets
        ) { q, products, cat, cart, held ->
            Tuple5(q, products, cat, cart, held)
        },
        combine(
            _canUndo,
            settingsRepository.defaultRetailMarginFlow,
            settingsRepository.defaultWholesaleMarginFlow,
            getCustomersUseCase(),
            _selectedCustomer
        ) { canUndo, retailMargin, wholesaleMargin, customers, selectedCustomer ->
            Tuple5(canUndo, retailMargin, wholesaleMargin, customers, selectedCustomer)
        },
        combine(
            _customerSearchQuery,
            _showCustomerDialog,
            _lastReceipt,
            _printState
        ) { cQuery, showDialog, lastReceipt, printState ->
            ReceiptDialogState(cQuery, showDialog, lastReceipt, printState)
        },
        combine(
            combine(
                settingsRepository.isRoundingEnabledFlow,
                settingsRepository.roundRetailPriceFlow,
                settingsRepository.roundWholesalePriceFlow,
                settingsRepository.roundTicketTotalFlow
            ) { isRoundingEnabled, roundRetailPrice, roundWholesalePrice, roundTicketTotal ->
                Tuple4(isRoundingEnabled, roundRetailPrice, roundWholesalePrice, roundTicketTotal)
            },
            settingsRepository.receiptSettingsFlow,
            _shiftFlow
        ) { roundingSettings, receiptSettings, shiftState ->
            Triple(roundingSettings, receiptSettings, shiftState)
        },
        syncRepository.syncState
    ) { (q, products, cat, cart, held),
        (canUndo, retailMargin, wholesaleMargin, customers, selectedCust),
        receiptDialogState,
        (roundingSettings, receiptSettings, shiftState),
        syncState ->
        val (cQuery, showDialog, lastReceipt, printState) = receiptDialogState
        val (isRoundingEnabled, roundRetailPrice, roundWholesalePrice, roundTicketTotal) = roundingSettings
        val filteredCust = if (cQuery.isBlank()) {
            customers
        } else {
            val query = cQuery.trim().lowercase()
            customers.filter {
                it.nombre.lowercase().contains(query) ||
                it.telefono.lowercase().contains(query) ||
                it.direccion.lowercase().contains(query)
            }
        }

        VentaUiState(
            searchQuery = q,
            activeProducts = products,
            selectedCategory = cat,
            cartItems = cart,
            heldTickets = held,
            canUndo = canUndo,
            defaultRetailMargin = retailMargin,
            defaultWholesaleMargin = wholesaleMargin,
            isRoundingEnabled = isRoundingEnabled,
            roundRetailPrice = isRoundingEnabled && roundRetailPrice,
            roundWholesalePrice = isRoundingEnabled && roundWholesalePrice,
            roundTicketTotal = isRoundingEnabled && roundTicketTotal,
            customers = customers,
            filteredCustomers = filteredCust,
            selectedCustomer = selectedCust,
            customerSearchQuery = cQuery,
            showCustomerDialog = showDialog,
            isSyncing = syncState == SyncStateEnum.SYNCING,
            receiptSettings = receiptSettings,
            lastReceipt = lastReceipt,
            isPrintingReceipt = printState.isPrinting,
            receiptPrintError = printState.hasError,
            receiptPrintSuccessful = printState.successful,
            activeShift = shiftState.activeShift,
            cashiers = shiftState.cashiers,
            isOpeningShift = shiftState.isOpeningShift,
            openShiftError = shiftState.openShiftError
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = VentaUiState()
    )

    fun openShift(cashierId: String, pin: String, initialCash: Double, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isOpeningShift.value = true
            _openShiftError.value = null
            val result = openShiftUseCase(cashierId, pin, initialCash)
            _isOpeningShift.value = false
            if (result.isSuccess) {
                onSuccess()
            } else {
                _openShiftError.value = result.exceptionOrNull()?.message ?: "Error al abrir turno"
            }
        }
    }

    fun clearOpenShiftError() {
        _openShiftError.value = null
    }

    private data class Tuple4<A, B, C, D>(
        val a: A,
        val b: B,
        val c: C,
        val d: D
    )

    private data class Tuple5<A, B, C, D, E>(
        val a: A,
        val b: B,
        val c: C,
        val d: D,
        val e: E
    )

    private fun pushCartHistory() {
        val current = _cartItems.value
        if (_cartHistory.isNotEmpty() && _cartHistory.last() == current) {
            return
        }
        _cartHistory.add(current)
        if (_cartHistory.size > 50) {
            _cartHistory.removeAt(0)
        }
        _canUndo.value = true
    }

    fun undoLastCartChange() {
        if (_cartHistory.isNotEmpty()) {
            val previous = _cartHistory.removeAt(_cartHistory.lastIndex)
            _cartItems.value = previous
            _canUndo.value = _cartHistory.isNotEmpty()
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun selectCustomer(customer: Customer?) {
        _selectedCustomer.value = customer
        _showCustomerDialog.value = false
    }

    fun clearSelectedCustomer() {
        _selectedCustomer.value = null
    }

    fun onCustomerSearchQueryChange(query: String) {
        _customerSearchQuery.value = query
    }

    fun setShowCustomerDialog(show: Boolean) {
        _showCustomerDialog.value = show
        if (show) {
            _customerSearchQuery.value = ""
        }
    }

    fun addProductToCart(product: Products, quantity: Double = 1.0) {
        val currentList = _cartItems.value.toMutableList()
        val existingIndex = currentList.indexOfFirst { it.product.id == product.id }
        if (existingIndex != -1) {
            val currentItem = currentList[existingIndex]
            val newQty = (currentItem.quantity + quantity)
            val roundedQty = (newQty * 100.0).roundToInt() / 100.0
            pushCartHistory()
            currentList[existingIndex] = currentItem.copy(
                quantity = roundedQty
            )
        } else if (quantity > 0.0) {
            pushCartHistory()
            val roundedQty = (quantity * 100.0).roundToInt() / 100.0
            currentList.add(
                CartItem(
                    product = product,
                    quantity = roundedQty,
                    originalPrice = product.precio
                )
            )
        }
        _cartItems.value = currentList
    }

    fun setProductQuantityInCart(product: Products, qty: Double) {
        val currentList = _cartItems.value.toMutableList()
        val existingIndex = currentList.indexOfFirst { it.product.id == product.id }
        if (existingIndex != -1) {
            val roundedQty = (qty * 100.0).roundToInt() / 100.0
            if (currentList[existingIndex].quantity == roundedQty) return
            pushCartHistory()
            if (qty <= 0.0) {
                currentList.removeAt(existingIndex)
            } else {
                currentList[existingIndex] = currentList[existingIndex].copy(quantity = roundedQty)
            }
        } else if (qty > 0.0) {
            pushCartHistory()
            val roundedQty = (qty * 100.0).roundToInt() / 100.0
            currentList.add(CartItem(product, roundedQty))
        }
        _cartItems.value = currentList
    }

    fun removeCartItem(item: CartItem) {
        pushCartHistory()
        _cartItems.value = _cartItems.value.filterNot { it.product.id == item.product.id }
    }

    fun clearCart() {
        if (_cartItems.value.isNotEmpty()) {
            pushCartHistory()
            _cartItems.value = emptyList()
            _selectedCustomer.value = null
        }
    }

    fun toggleWholesalePrice() {
        val currentList = _cartItems.value
        val eligibleItems = currentList.filter { it.product.precio_mayoreo > 0.0 }
        if (eligibleItems.isEmpty()) return

        pushCartHistory()
        val allWholesale = eligibleItems.all { it.product.precio == it.product.precio_mayoreo }
        _cartItems.value = currentList.map { item ->
            if (item.product.precio_mayoreo > 0.0) {
                val targetPrice = if (allWholesale) item.originalPrice else item.product.precio_mayoreo
                val newProduct = item.product.copy(precio = targetPrice)
                item.copy(product = newProduct)
            } else {
                item
            }
        }
    }

    fun toggleWholesalePriceForItem(item: CartItem) {
        if (item.product.precio_mayoreo <= 0.0) return
        val currentList = _cartItems.value
        val index = currentList.indexOfFirst { it.product.id == item.product.id }
        if (index < 0) return

        pushCartHistory()
        val targetItem = currentList[index]
        val isCurrentlyWholesale = targetItem.product.precio == targetItem.product.precio_mayoreo
        val targetPrice = if (isCurrentlyWholesale) targetItem.originalPrice else targetItem.product.precio_mayoreo

        val updatedProduct = targetItem.product.copy(precio = targetPrice)
        val updatedList = currentList.toMutableList()
        updatedList[index] = targetItem.copy(product = updatedProduct)
        _cartItems.value = updatedList
    }

    fun toggleProductFavorite(product: Products) {
        viewModelScope.launch {
            val updated = product.copy(
                es_favorito = if (product.es_favorito == 1L) 0L else 1L,
                updated_at = currentTimeMillis(),
                sync_state = "PENDING_UPDATE"
            )
            repository.updateProduct(updated)
        }
    }

    fun updateProduct(product: Products) {
        viewModelScope.launch {
            repository.updateProduct(product)
        }
    }

    fun saveProduct(product: Products) {
        viewModelScope.launch {
            saveProductUseCase(product)
            launch(Dispatchers.IO) {
                syncRepository.syncAll()
            }
        }
    }

    fun refreshSync() {
        viewModelScope.launch(Dispatchers.IO) {
            syncRepository.syncAll(isManual = true)
        }
    }

    suspend fun findProductByBarcode(barcode: String): Products? {
        return findProductByBarcodeUseCase(barcode)
    }

    fun putCurrentTicketOnHold() {
        val currentCart = _cartItems.value
        if (currentCart.isEmpty()) return
        val held = HeldTicket(items = currentCart)
        _heldTickets.value += held
        _cartItems.value = emptyList()
        _cartHistory.clear()
        _canUndo.value = false
    }

    fun resumeHeldTicket(heldTicket: HeldTicket) {
        val currentCart = _cartItems.value
        val updatedHeldList = _heldTickets.value.filterNot { it.id == heldTicket.id }.toMutableList()
        if (currentCart.isNotEmpty()) {
            updatedHeldList.add(0, HeldTicket(items = currentCart))
        }
        _heldTickets.value = updatedHeldList
        _cartItems.value = heldTicket.items
        _cartHistory.clear()
        _canUndo.value = false
    }

    fun discardHeldTicket(heldTicket: HeldTicket) {
        _heldTickets.value = _heldTickets.value.filterNot { it.id == heldTicket.id }
    }

    suspend fun processCheckout(
        pagoCon: Double,
        cambio: Double,
        metodoPago: String = "EFECTIVO",
        customerId: String? = null,
        printReceipt: Boolean = true
    ): Long {
        val currentItems = _cartItems.value
        if (currentItems.isEmpty()) return 0L
        val effectiveCustomerId = customerId ?: _selectedCustomer.value?.id
        val folio = recordSaleUseCase(
            cartItems = currentItems,
            pagoCon = pagoCon,
            cambio = cambio,
            metodoPago = metodoPago,
            customerId = effectiveCustomerId,
            roundTicketTotal = uiState.value.roundTicketTotal
        )
        if (printReceipt) {
            val receiptSettings = uiState.value.receiptSettings
            val receipt = ReceiptFormatter.create(
                folio = folio,
                createdAt = currentTimeMillis(),
                items = currentItems.map { item ->
                    val isWholesale = item.product.precio == item.product.precio_mayoreo && item.product.precio_mayoreo > 0.0
                    ReceiptItem(
                        name = item.product.nombre,
                        quantity = item.quantity,
                        unitPrice = item.product.precio,
                        subtotal = item.product.precio * item.quantity,
                        isWeightBased = item.product.por_peso == 1L,
                        originalUnitPrice = item.originalPrice,
                        isWholesale = isWholesale
                    )
                },
                total = currentItems.sumOf { it.product.precio * it.quantity }
                    .let { if (uiState.value.roundTicketTotal) com.dnavarro.poskmp.util.roundPrice(it) else it },
                paid = pagoCon,
                change = cambio,
                paymentMethod = metodoPago,
                customerName = _selectedCustomer.value?.nombre,
                settings = receiptSettings
            )
            _printState.value = ReceiptPrintInternalState()
            _lastReceipt.value = receipt
        }
        clearCart()
        viewModelScope.launch(Dispatchers.IO) {
            syncRepository.syncAll()
        }
        return folio
    }

    fun printLastReceipt() {
        val receipt = _lastReceipt.value ?: return
        if (_printState.value.isPrinting) return
        viewModelScope.launch {
            _printState.value = ReceiptPrintInternalState(isPrinting = true)
            val result = printReceiptUseCase(receipt)
            _printState.value = ReceiptPrintInternalState(
                hasError = result.isFailure,
                successful = result.isSuccess
            )
        }
    }

    fun dismissLastReceipt() {
        _lastReceipt.value = null
        _printState.value = ReceiptPrintInternalState()
    }
}
