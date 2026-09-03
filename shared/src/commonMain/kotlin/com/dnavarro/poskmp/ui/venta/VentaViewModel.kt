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
import com.dnavarro.poskmp.domain.usecase.OpenCashDrawerUseCase
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
    private val openCashDrawerUseCase: OpenCashDrawerUseCase,
    getActiveShiftUseCase: com.dnavarro.poskmp.domain.usecase.GetActiveShiftUseCase,
    getCashiersUseCase: com.dnavarro.poskmp.domain.usecase.GetCashiersUseCase,
    private val openShiftUseCase: com.dnavarro.poskmp.domain.usecase.OpenShiftUseCase,
    private val recordCashMovementUseCase: com.dnavarro.poskmp.domain.usecase.RecordCashMovementUseCase,
    private val shiftRepository: com.dnavarro.poskmp.data.ShiftRepository
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
    private val _showInflowDialog = MutableStateFlow(false)
    private val _showOutflowDialog = MutableStateFlow(false)
    private val _isRecordingMovement = MutableStateFlow(false)
    private val _shiftActionError = MutableStateFlow<String?>(null)
    private val _shiftActionSuccess = MutableStateFlow<String?>(null)

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
        val openShiftError: String?,
        val showInflowDialog: Boolean,
        val showOutflowDialog: Boolean,
        val isRecordingMovement: Boolean,
        val shiftActionError: String?,
        val shiftActionSuccess: String?,
        val activeShiftMovements: List<com.dnavarro.poskmp.domain.model.CashMovement>
    )

    private data class ReceiptPrintInternalState(
        val isPrinting: Boolean = false,
        val hasError: Boolean = false,
        val successful: Boolean = false
    )

    private val _productsFlow = _searchQuery.flatMapLatest { query ->
        getProductsUseCase(query = query, activeOnly = true)
    }

    private val _activeShiftFlow = getActiveShiftUseCase()

    private val _activeShiftMovementsFlow = _activeShiftFlow.flatMapLatest { shift ->
        if (shift != null) {
            shiftRepository.getMovementsForShiftFlow(shift.id)
        } else {
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }

    private val _shiftFlow = combine(
        combine(
            _activeShiftFlow,
            getCashiersUseCase(),
            _isOpeningShift,
            _openShiftError
        ) { activeShift, cashiers, isOpening, error ->
            Tuple4(activeShift, cashiers, isOpening, error)
        },
        combine(
            _showInflowDialog,
            _showOutflowDialog,
            _isRecordingMovement,
            _shiftActionError,
            _shiftActionSuccess
        ) { inDialog, outDialog, recording, actError, actSuccess ->
            Tuple5(inDialog, outDialog, recording, actError, actSuccess)
        },
        _activeShiftMovementsFlow
    ) { (activeShift, cashiers, isOpening, error), (inDialog, outDialog, recording, actError, actSuccess), movements ->
        ShiftState(
            activeShift = activeShift,
            cashiers = cashiers,
            isOpeningShift = isOpening,
            openShiftError = error,
            showInflowDialog = inDialog,
            showOutflowDialog = outDialog,
            isRecordingMovement = recording,
            shiftActionError = actError,
            shiftActionSuccess = actSuccess,
            activeShiftMovements = movements
        )
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
            combine(
                _canUndo,
                settingsRepository.defaultRetailMarginFlow,
                settingsRepository.defaultWholesaleMarginFlow,
                settingsRepository.defaultDeliveryMarginFlow
            ) { canUndo, retailMargin, wholesaleMargin, deliveryMargin ->
                Tuple4(canUndo, retailMargin, wholesaleMargin, deliveryMargin)
            },
            combine(
                settingsRepository.useProductTableInCatalogFlow,
                getCustomersUseCase(),
                _selectedCustomer,
                settingsRepository.swapVentaLayoutOrderFlow
            ) { useProductTableInCatalog, customers, selectedCustomer, swapVentaLayoutOrder ->
                Tuple4(useProductTableInCatalog, customers, selectedCustomer, swapVentaLayoutOrder)
            }
        ) { (canUndo, retailMargin, wholesaleMargin, deliveryMargin), (useProductTableInCatalog, customers, selectedCustomer, swapVentaLayoutOrder) ->
            VentaCatalogConfig(
                canUndo = canUndo,
                defaultRetailMargin = retailMargin,
                defaultWholesaleMargin = wholesaleMargin,
                defaultDeliveryMargin = deliveryMargin,
                useProductTableInCatalog = useProductTableInCatalog,
                customers = customers,
                selectedCustomer = selectedCustomer,
                swapVentaLayoutOrder = swapVentaLayoutOrder
            )
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
                combine(
                    settingsRepository.isRoundingEnabledFlow,
                    settingsRepository.roundRetailPriceFlow,
                    settingsRepository.roundWholesalePriceFlow,
                    settingsRepository.roundDeliveryPriceFlow
                ) { isRoundingEnabled, roundRetailPrice, roundWholesalePrice, roundDeliveryPrice ->
                    Tuple4(isRoundingEnabled, roundRetailPrice, roundWholesalePrice, roundDeliveryPrice)
                },
                combine(
                    settingsRepository.roundTicketTotalFlow,
                    settingsRepository.disallowCardPaymentOnWholesaleFlow,
                    settingsRepository.prioritizeDeliveryPriceFlow
                ) { roundTicketTotal, disallowCardPaymentOnWholesale, prioritizeDeliveryPrice ->
                    Triple(roundTicketTotal, disallowCardPaymentOnWholesale, prioritizeDeliveryPrice)
                }
            ) { (isRoundingEnabled, roundRetailPrice, roundWholesalePrice, roundDeliveryPrice),
                (roundTicketTotal, disallowCardPaymentOnWholesale, prioritizeDeliveryPrice) ->
                VentaRoundingConfig(
                    isRoundingEnabled = isRoundingEnabled,
                    roundRetailPrice = roundRetailPrice,
                    roundWholesalePrice = roundWholesalePrice,
                    roundDeliveryPrice = roundDeliveryPrice,
                    roundTicketTotal = roundTicketTotal,
                    disallowCardPaymentOnWholesale = disallowCardPaymentOnWholesale,
                    prioritizeDeliveryPrice = prioritizeDeliveryPrice
                )
            },
            settingsRepository.receiptSettingsFlow,
            _shiftFlow
        ) { roundingSettings, receiptSettings, shiftState ->
            Triple(roundingSettings, receiptSettings, shiftState)
        },
        syncRepository.syncState
    ) { (q, products, cat, cart, held),
        catalogConfig,
        receiptDialogState,
        (roundingSettings, receiptSettings, shiftState),
        syncState ->
        val (cQuery, showDialog, lastReceipt, printState) = receiptDialogState
        val filteredCust = if (cQuery.isBlank()) {
            catalogConfig.customers
        } else {
            val query = cQuery.trim().lowercase()
            catalogConfig.customers.filter {
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
            canUndo = catalogConfig.canUndo,
            defaultRetailMargin = catalogConfig.defaultRetailMargin,
            defaultWholesaleMargin = catalogConfig.defaultWholesaleMargin,
            defaultDeliveryMargin = catalogConfig.defaultDeliveryMargin,
            isRoundingEnabled = roundingSettings.isRoundingEnabled,
            roundRetailPrice = roundingSettings.isRoundingEnabled && roundingSettings.roundRetailPrice,
            roundWholesalePrice = roundingSettings.isRoundingEnabled && roundingSettings.roundWholesalePrice,
            roundDeliveryPrice = roundingSettings.isRoundingEnabled && roundingSettings.roundDeliveryPrice,
            roundTicketTotal = roundingSettings.isRoundingEnabled && roundingSettings.roundTicketTotal,
            disallowCardPaymentOnWholesale = roundingSettings.disallowCardPaymentOnWholesale,
            prioritizeDeliveryPrice = roundingSettings.prioritizeDeliveryPrice,
            useProductTableInCatalog = catalogConfig.useProductTableInCatalog,
            swapVentaLayoutOrder = catalogConfig.swapVentaLayoutOrder,
            customers = catalogConfig.customers,
            filteredCustomers = filteredCust,
            selectedCustomer = catalogConfig.selectedCustomer,
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
            openShiftError = shiftState.openShiftError,
            showInflowDialog = shiftState.showInflowDialog,
            showOutflowDialog = shiftState.showOutflowDialog,
            isRecordingMovement = shiftState.isRecordingMovement,
            shiftActionError = shiftState.shiftActionError,
            shiftActionSuccess = shiftState.shiftActionSuccess,
            activeShiftMovements = shiftState.activeShiftMovements
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

    fun openInflowDialog() {
        if (uiState.value.activeShift == null) {
            _shiftActionError.value = "No hay ningún turno activo para registrar movimientos."
            return
        }
        _shiftActionError.value = null
        _showInflowDialog.value = true
        _showOutflowDialog.value = false
    }

    fun openOutflowDialog() {
        if (uiState.value.activeShift == null) {
            _shiftActionError.value = "No hay ningún turno activo para registrar movimientos."
            return
        }
        _shiftActionError.value = null
        _showInflowDialog.value = false
        _showOutflowDialog.value = true
    }

    fun dismissShiftDialogs() {
        _showInflowDialog.value = false
        _showOutflowDialog.value = false
        _shiftActionError.value = null
    }

    fun recordCashMovement(type: com.dnavarro.poskmp.domain.model.CashMovementType, amount: Double, reason: String) {
        val activeShift = uiState.value.activeShift
        if (activeShift == null) {
            _shiftActionError.value = "No hay ningún turno activo para registrar movimientos."
            return
        }
        viewModelScope.launch {
            _isRecordingMovement.value = true
            _shiftActionError.value = null
            val result = recordCashMovementUseCase(
                shiftId = activeShift.id,
                cashierId = activeShift.cashierId,
                type = type,
                amount = amount,
                reason = reason
            )
            _isRecordingMovement.value = false
            if (result.isSuccess) {
                _showInflowDialog.value = false
                _showOutflowDialog.value = false
                _shiftActionSuccess.value = if (type == com.dnavarro.poskmp.domain.model.CashMovementType.ENTRADA) {
                    "Entrada de efectivo registrada"
                } else {
                    "Salida de efectivo registrada"
                }
            } else {
                _shiftActionError.value = result.exceptionOrNull()?.message ?: "Error al registrar movimiento"
            }
        }
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
    private data class VentaCatalogConfig(
        val canUndo: Boolean = false,
        val defaultRetailMargin: Double = 0.0,
        val defaultWholesaleMargin: Double = 0.0,
        val defaultDeliveryMargin: Double = 0.0,
        val useProductTableInCatalog: Boolean = false,
        val customers: List<Customer> = emptyList(),
        val selectedCustomer: Customer? = null,
        val swapVentaLayoutOrder: Boolean = false
    )

    private data class VentaRoundingConfig(
        val isRoundingEnabled: Boolean = false,
        val roundRetailPrice: Boolean = false,
        val roundWholesalePrice: Boolean = false,
        val roundDeliveryPrice: Boolean = false,
        val roundTicketTotal: Boolean = false,
        val disallowCardPaymentOnWholesale: Boolean = false,
        val prioritizeDeliveryPrice: Boolean = false
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
        val previousCustomer = _selectedCustomer.value
        _selectedCustomer.value = customer
        _showCustomerDialog.value = false

        if (customer?.siempreMayoreo == true) {
            applyCustomerWholesalePricing(enable = true)
        } else if (previousCustomer?.siempreMayoreo == true && (customer == null || !customer.siempreMayoreo)) {
            applyCustomerWholesalePricing(enable = false)
        }
    }

    fun clearSelectedCustomer() {
        val previousCustomer = _selectedCustomer.value
        _selectedCustomer.value = null
        if (previousCustomer?.siempreMayoreo == true) {
            applyCustomerWholesalePricing(enable = false)
        }
    }

    private fun applyCustomerWholesalePricing(enable: Boolean) {
        val currentList = _cartItems.value
        val updated = currentList.map { item ->
            if (item.product.precio_mayoreo > 0.0) {
                val targetPrice = if (enable) item.product.precio_mayoreo else item.originalPrice
                item.copy(product = item.product.copy(precio = targetPrice))
            } else {
                item
            }
        }
        if (updated != currentList) {
            pushCartHistory()
            _cartItems.value = updated
        }
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
            val roundedQty = (newQty * 1000.0).roundToInt() / 1000.0
            pushCartHistory()
            if (roundedQty <= 0.0) {
                currentList.removeAt(existingIndex)
            } else {
                currentList[existingIndex] = currentItem.copy(
                    quantity = roundedQty
                )
            }
        } else if (quantity > 0.0) {
            pushCartHistory()
            val roundedQty = (quantity * 1000.0).roundToInt() / 1000.0
            val isWholesaleCustomer = _selectedCustomer.value?.siempreMayoreo == true
            val prioritizeDelivery = uiState.value.prioritizeDeliveryPrice
            val basePrice = if (prioritizeDelivery && product.precio_delivery > 0.0) {
                product.precio_delivery
            } else {
                product.precio
            }
            val effectivePrice = if (isWholesaleCustomer && product.precio_mayoreo > 0.0) {
                product.precio_mayoreo
            } else {
                basePrice
            }
            val productToCart = if (effectivePrice != product.precio) {
                product.copy(precio = effectivePrice)
            } else {
                product
            }
            currentList.add(
                CartItem(
                    product = productToCart,
                    quantity = roundedQty,
                    originalPrice = basePrice
                )
            )
        }
        _cartItems.value = currentList
    }

    fun setProductQuantityInCart(product: Products, qty: Double) {
        val currentList = _cartItems.value.toMutableList()
        val existingIndex = currentList.indexOfFirst { it.product.id == product.id }
        val roundedQty = (qty * 1000.0).roundToInt() / 1000.0
        if (existingIndex != -1) {
            if (currentList[existingIndex].quantity == roundedQty) return
            pushCartHistory()
            if (roundedQty <= 0.0) {
                currentList.removeAt(existingIndex)
            } else {
                currentList[existingIndex] = currentList[existingIndex].copy(quantity = roundedQty)
            }
        } else if (roundedQty > 0.0) {
            pushCartHistory()
            val isWholesaleCustomer = _selectedCustomer.value?.siempreMayoreo == true
            val effectivePrice = if (isWholesaleCustomer && product.precio_mayoreo > 0.0) {
                product.precio_mayoreo
            } else {
                product.precio
            }
            val productToCart = if (effectivePrice != product.precio) {
                product.copy(precio = effectivePrice)
            } else {
                product
            }
            currentList.add(
                CartItem(
                    product = productToCart,
                    quantity = roundedQty,
                    originalPrice = product.precio
                )
            )
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
        val isCashPayment = metodoPago == "EFECTIVO" || (metodoPago == "MIXTO" && pagoCon > 0.0)
        val shouldOpenDrawer = uiState.value.receiptSettings.openCashDrawerOnCashSale && isCashPayment

        if (printReceipt) {
            val receiptSettings = uiState.value.receiptSettings
            val receipt = ReceiptFormatter.create(
                folio = folio,
                createdAt = currentTimeMillis(),
                items = currentItems.map { item ->
                    val isWholesale = item.product.precio == item.product.precio_mayoreo && item.product.precio_mayoreo > 0.0
                    val isDelivery = !isWholesale && item.product.precio == item.product.precio_delivery && item.product.precio_delivery > 0.0
                    ReceiptItem(
                        name = item.product.nombre,
                        quantity = item.quantity,
                        unitPrice = item.product.precio,
                        subtotal = item.product.precio * item.quantity,
                        isWeightBased = item.product.por_peso == 1L,
                        originalUnitPrice = item.originalPrice,
                        isWholesale = isWholesale,
                        isDelivery = isDelivery
                    )
                },
                total = currentItems.sumOf { it.product.precio * it.quantity }
                    .let { if (uiState.value.roundTicketTotal) com.dnavarro.poskmp.util.roundPrice(it) else it },
                paid = pagoCon,
                change = cambio,
                paymentMethod = metodoPago,
                customerName = _selectedCustomer.value?.nombre,
                settings = receiptSettings.copy(openCashDrawerOnCashSale = shouldOpenDrawer)
            )
            _lastReceipt.value = receipt
            viewModelScope.launch {
                _printState.value = ReceiptPrintInternalState(isPrinting = true)
                val result = printReceiptUseCase(receipt)
                _printState.value = ReceiptPrintInternalState(
                    hasError = result.isFailure,
                    successful = result.isSuccess
                )
            }
        } else if (shouldOpenDrawer) {
            viewModelScope.launch {
                openCashDrawerUseCase()
            }
        }
        clearCart()
        viewModelScope.launch(Dispatchers.IO) {
            syncRepository.syncAll()
        }
        return folio
    }

}
