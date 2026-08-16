package com.dnavarro.poskmp.ui.venta

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dnavarro.poskmp.data.ProductRepository
import com.dnavarro.poskmp.data.SettingsRepository
import com.dnavarro.poskmp.db.Products
import com.dnavarro.poskmp.util.currentTimeMillis
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.dnavarro.poskmp.domain.usecase.FindProductByBarcodeUseCase
import com.dnavarro.poskmp.domain.usecase.GetProductsUseCase
import com.dnavarro.poskmp.domain.usecase.RecordSaleUseCase
import com.dnavarro.poskmp.domain.usecase.SaveProductUseCase

import com.dnavarro.poskmp.ui.CartItem
import kotlin.math.roundToInt

/**
 * ViewModel for Venta screen managing product flow, barcode search, and cart updates.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class VentaViewModel(
    private val repository: ProductRepository,
    settingsRepository: SettingsRepository,
    private val getProductsUseCase: GetProductsUseCase = GetProductsUseCase(repository),
    private val findProductByBarcodeUseCase: FindProductByBarcodeUseCase = FindProductByBarcodeUseCase(repository),
    private val saveProductUseCase: SaveProductUseCase = SaveProductUseCase(repository),
    private val recordSaleUseCase: RecordSaleUseCase
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategory = MutableStateFlow<String?>(null)
    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    private val _heldTickets = MutableStateFlow<List<HeldTicket>>(emptyList())
    private val _cartHistory = mutableListOf<List<CartItem>>()
    private val _canUndo = MutableStateFlow(false)

    private val _productsFlow = _searchQuery.flatMapLatest { query ->
        getProductsUseCase(query = query, activeOnly = true)
    }

    val uiState: StateFlow<VentaUiState> = combine(
        _searchQuery,
        _productsFlow,
        _selectedCategory,
        _cartItems,
        _heldTickets,
        _canUndo,
        settingsRepository.defaultRetailMarginFlow,
        settingsRepository.defaultWholesaleMarginFlow
    ) { flows: Array<Any?> ->
        val query = flows[0] as String
        @Suppress("UNCHECKED_CAST")
        val products = flows[1] as List<Products>
        val category = flows[2] as String?
        @Suppress("UNCHECKED_CAST")
        val cart = flows[3] as List<CartItem>
        @Suppress("UNCHECKED_CAST")
        val held = flows[4] as List<HeldTicket>
        val canUndo = flows[5] as Boolean
        val defaultRetailMargin = flows[6] as Double
        val defaultWholesaleMargin = flows[7] as Double

        VentaUiState(
            searchQuery = query,
            activeProducts = products,
            selectedCategory = category,
            cartItems = cart,
            heldTickets = held,
            canUndo = canUndo,
            defaultRetailMargin = defaultRetailMargin,
            defaultWholesaleMargin = defaultWholesaleMargin
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = VentaUiState()
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
            val previousState = _cartHistory.removeAt(_cartHistory.lastIndex)
            _cartItems.value = previousState
            _canUndo.value = _cartHistory.isNotEmpty()
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onCategorySelected(category: String?) {
        _selectedCategory.value = category
    }

    fun addProductToCart(product: Products, qty: Double) {
        pushCartHistory()
        val currentList = _cartItems.value.toMutableList()
        val existingIndex = currentList.indexOfFirst { it.product.id == product.id }
        if (existingIndex != -1) {
            val item = currentList[existingIndex]
            val newQty = ((item.quantity + qty) * 100.0).roundToInt() / 100.0
            if (newQty <= 0.0) {
                currentList.removeAt(existingIndex)
            } else {
                currentList[existingIndex] = item.copy(quantity = newQty)
            }
        } else if (qty > 0.0) {
            currentList.add(CartItem(product, qty))
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
        }
    }

    suspend fun findProductByBarcode(barcode: String): Products? {
        return findProductByBarcodeUseCase(barcode)
    }

    fun putCurrentTicketOnHold() {
        val currentCart = _cartItems.value
        if (currentCart.isEmpty()) return
        val held = HeldTicket(items = currentCart)
        _heldTickets.value = _heldTickets.value + held
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

    suspend fun processCheckout(pagoCon: Double, cambio: Double, metodoPago: String = "EFECTIVO"): Long {
        val currentItems = _cartItems.value
        if (currentItems.isEmpty()) return 0L
        val folio = recordSaleUseCase(
            cartItems = currentItems,
            pagoCon = pagoCon,
            cambio = cambio,
            metodoPago = metodoPago
        )
        clearCart()
        return folio
    }
}
