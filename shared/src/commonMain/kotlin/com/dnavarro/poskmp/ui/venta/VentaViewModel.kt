package com.dnavarro.poskmp.ui.venta

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dnavarro.poskmp.data.ProductRepository
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
import com.dnavarro.poskmp.domain.usecase.SaveProductUseCase

import com.dnavarro.poskmp.ui.CartItem
import kotlin.math.roundToInt

/**
 * ViewModel for Venta screen managing product flow, barcode search, and cart updates.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class VentaViewModel(
    private val repository: ProductRepository,
    private val getProductsUseCase: GetProductsUseCase = GetProductsUseCase(repository),
    private val findProductByBarcodeUseCase: FindProductByBarcodeUseCase = FindProductByBarcodeUseCase(repository),
    private val saveProductUseCase: SaveProductUseCase = SaveProductUseCase(repository)
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategory = MutableStateFlow<String?>(null)
    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())

    private val _productsFlow = _searchQuery.flatMapLatest { query ->
        getProductsUseCase(query = query, activeOnly = true)
    }

    val uiState: StateFlow<VentaUiState> = combine(
        _searchQuery,
        _productsFlow,
        _selectedCategory,
        _cartItems
    ) { query, products, category, cart ->
        VentaUiState(
            searchQuery = query,
            activeProducts = products,
            selectedCategory = category,
            cartItems = cart
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = VentaUiState()
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onCategorySelected(category: String?) {
        _selectedCategory.value = category
    }

    fun addProductToCart(product: Products, qty: Double) {
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
            if (qty <= 0.0) {
                currentList.removeAt(existingIndex)
            } else {
                val roundedQty = (qty * 100.0).roundToInt() / 100.0
                currentList[existingIndex] = currentList[existingIndex].copy(quantity = roundedQty)
            }
        } else if (qty > 0.0) {
            val roundedQty = (qty * 100.0).roundToInt() / 100.0
            currentList.add(CartItem(product, roundedQty))
        }
        _cartItems.value = currentList
    }

    fun removeCartItem(item: CartItem) {
        _cartItems.value = _cartItems.value.filterNot { it.product.id == item.product.id }
    }

    fun clearCart() {
        _cartItems.value = emptyList()
    }

    fun toggleWholesalePrice() {
        val currentList = _cartItems.value
        val eligibleItems = currentList.filter { it.product.precio_mayoreo > 0.0 }
        if (eligibleItems.isEmpty()) return

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
}
