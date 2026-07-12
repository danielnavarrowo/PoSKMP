package com.dnavarro.poskmp.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import com.dnavarro.poskmp.db.Products
import com.dnavarro.poskmp.data.ProductRepository
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.roundToInt

data class CartItem(
    val product: Products,
    var quantity: Double
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VentaScreen(
    repository: ProductRepository,
    isCompact: Boolean,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var productsList by remember { mutableStateOf<List<Products>>(emptyList()) }
    val cartItems = remember { mutableStateListOf<CartItem>() }

    // Active tab in compact/mobile view
    var mobileSelectedTab by remember { mutableIntStateOf(0) }

    // Weight Dialog state
    var showWeightDialogForProduct by remember { mutableStateOf<Products?>(null) }
    var weightInput by remember { mutableStateOf("1.000") }

    // Checkout Dialog state
    var showCheckoutDialog by remember { mutableStateOf(false) }
    var paymentAmountInput by remember { mutableStateOf("") }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var lastSaleTotal by remember { mutableDoubleStateOf(0.0) }
    var lastSaleChange by remember { mutableDoubleStateOf(0.0) }

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
    }

    val subtotal = cartItems.sumOf { it.product.precio * it.quantity }
    val iva = subtotal * 0.16
    val total = subtotal + iva

    Scaffold(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
    ) { paddingValues ->
        if (isCompact) {
            // MOBILE COMPACT VIEW: TAB SYSTEM
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Tab Selection
                SecondaryTabRow(
                    selectedTabIndex = mobileSelectedTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Tab(
                        selected = mobileSelectedTab == 0,
                        onClick = { mobileSelectedTab = 0 },
                        text = { Text("Catálogo", fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) }
                    )
                    Tab(
                        selected = mobileSelectedTab == 1,
                        onClick = { mobileSelectedTab = 1 },
                        text = {
                            Text(
                                text = if (cartItems.isEmpty()) "Ticket" else "Ticket (${
                                    cartItems.sumOf { if (it.product.por_peso == 1L) 1.0 else it.quantity }.toInt()
                                })",
                                fontWeight = FontWeight.Bold
                            )
                        },
                        icon = { Icon(Icons.Default.ShoppingCart, contentDescription = null) }
                    )
                }

                // Render Active Tab View
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    if (mobileSelectedTab == 0) {
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
                            isCompact = true,
                            onViewCartClick = { mobileSelectedTab = 1 },
                            cartCount = cartItems.size,
                            cartTotal = total
                        )
                    } else {
                        TicketSection(
                            cartItems = cartItems,
                            subtotal = subtotal,
                            iva = iva,
                            total = total,
                            onClearCart = { cartItems.clear() },
                            onUpdateQuantity = { item, delta -> addProductToCart(item.product, delta) },
                            onRemoveItem = { item -> cartItems.remove(item) },
                            onCheckout = {
                                paymentAmountInput = ""
                                showCheckoutDialog = true
                            }
                        )
                    }
                }
            }
        } else {
            // DESKTOP WIDESCREEN VIEW: SPLIT ROW LAYOUT
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Left Column: Catalog
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
                    isCompact = false,
                    modifier = Modifier.weight(0.65f)
                )

                // Right Column: Ticket
                TicketSection(
                    cartItems = cartItems,
                    subtotal = subtotal,
                    iva = iva,
                    total = total,
                    onClearCart = { cartItems.clear() },
                    onUpdateQuantity = { item, delta -> addProductToCart(item.product, delta) },
                    onRemoveItem = { item -> cartItems.remove(item) },
                    onCheckout = {
                        paymentAmountInput = ""
                        showCheckoutDialog = true
                    },
                    modifier = Modifier
                        .weight(0.35f)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant)
                )
            }
        }
    }

    // Weight Dialog
    if (showWeightDialogForProduct != null) {
        val product = showWeightDialogForProduct!!

        var weightInputValue by remember(product.id) { mutableStateOf("1") }
        var priceInputValue by remember(product.id) {
            val initialPrice = product.precio
            mutableStateOf(if (initialPrice % 1.0 == 0.0) initialPrice.toInt().toString() else initialPrice.toString())
        }

        fun handleWeightChange(newWeight: String) {
            weightInputValue = newWeight
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
                weightInputValue = if (calcWeight % 1.0 == 0.0) calcWeight.toInt()
                    .toString() else ((calcWeight * 1000.0).roundToInt() / 1000.0).toString()
            } else if (newPrice.isEmpty()) {
                weightInputValue = ""
            }
        }


        BasicAlertDialog(
            onDismissRequest = { showWeightDialogForProduct = null },
            modifier = Modifier.background(
                MaterialTheme.colorScheme.surfaceContainerLowest, MaterialTheme.shapes.medium
            ).padding(20.dp),
            content = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "¿Cuánto de ${product.nombre}?",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Precio: $${product.precio.toString().formatPrice()} / Kg",
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
                                    val label =
                                        "${if (qty % 1.0 == 0.0) qty.toInt().toString() else qty.toString()}${" Kg"}"
                                    Box(
                                        modifier = Modifier
                                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.small)
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
                                text = "PESO (Kg)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )

                            OutlinedTextField(
                                value = weightInputValue,
                                onValueChange = { handleWeightChange(it) },
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
                                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.small)
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
                                text = "PESOS ($)",
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
                            Text("Cancelar", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Button(
                            onClick = {
                                val weight = weightInputValue.toDoubleOrNull() ?: 1.0
                                addProductToCart(product, weight)
                                showWeightDialogForProduct = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = MaterialTheme.shapes.small,
                        ) {
                            Text("Agregar al ticket", fontWeight = FontWeight.Bold, fontSize = 14.sp)
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
            shape = MaterialTheme.shapes.large,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            title = { Text("Cobro de Venta", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.small)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total a Pagar:", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
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
                        label = { Text("Efectivo Recibido") },
                        placeholder = { Text("0.00") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )

                    if (paymentAmountInput.isNotEmpty() && paymentAmount < total) {
                        Text(
                            "Monto insuficiente. Falta $${(total - paymentAmount).toString().formatPrice()}",
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
                            Text("Cambio a entregar:", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
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
                    Text("Registrar Venta")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCheckoutDialog = false }) {
                    Text("Cancelar", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    // Success Dialog
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
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
            title = { Text("¡Venta Exitosa!", fontWeight = FontWeight.Bold) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("La venta se ha registrado correctamente.", textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Total cobrado: $${lastSaleTotal.toString().formatPrice()}",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Cambio entregado: $${lastSaleChange.toString().formatPrice()}",
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
                    Text("Entendido")
                }
            }
        )
    }
}

// Helpers for catalog and cart segments
@Composable
fun CatalogSection(
    modifier: Modifier = Modifier,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    productsList: List<Products>,
    onProductClick: (Products) -> Unit,
    isCompact: Boolean,
    onViewCartClick: (() -> Unit)? = null,
    cartCount: Int = 0,
    cartTotal: Double = 0.0
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Search Bar & Fast Codes
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.medium),
            placeholder = { Text("Buscar por nombre, código de barra o categoría...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar") },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                    }
                }
            },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Catalog Header
        Text(
            text = if (searchQuery.isBlank()) "Productos Disponibles" else "Resultados de la Búsqueda",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (productsList.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "No se encontraron productos.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 16.sp
                    )
                }
            }
        } else {
            Box(modifier = Modifier.weight(1f)) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(productsList) { product ->
                        Card(
                            onClick = { onProductClick(product) },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth().height(140.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp).fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = product.categoria ?: "Sin categoría",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = product.nombre,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    Text(
                                        text = "$${
                                            product.precio.toString().formatPrice()
                                        }${if (product.precio_nota.isNullOrBlank()) "" else " / ${product.precio_nota}"}",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    if (product.por_peso == 1L) {
                                        Badge(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer) {
                                            Text(
                                                "Peso",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Mobile bottom overlay to access cart
                if (isCompact && cartCount > 0 && onViewCartClick != null) {
                    ExtendedFloatingActionButton(
                        onClick = onViewCartClick,
                        modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Ver Ticket ($cartCount) • $${cartTotal.toString().formatPrice()}")
                    }
                }
            }
        }
    }
}

@Composable
fun TicketSection(
    cartItems: List<CartItem>,
    subtotal: Double,
    iva: Double,
    total: Double,
    onClearCart: () -> Unit,
    onUpdateQuantity: (CartItem, Double) -> Unit,
    onRemoveItem: (CartItem) -> Unit,
    onCheckout: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        ) {
            Icon(
                Icons.Default.ShoppingCart,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Ticket Actual",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.weight(1f))
            if (cartItems.isNotEmpty()) {
                TextButton(onClick = onClearCart) {
                    Text("Limpiar", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        // Cart Items List
        if (cartItems.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.ShoppingCart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "El ticket está vacío",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 15.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(cartItems) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.product.nombre,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "$${
                                    item.product.precio.toString().formatPrice()
                                } x ${item.quantity.formatQuantity(item.product.por_peso == 1L)}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Decrease quantity
                            IconButton(
                                onClick = {
                                    val decrement = if (item.product.por_peso == 1L) 0.1 else 1.0
                                    onUpdateQuantity(item, -decrement)
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Disminuir",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            Text(
                                text = item.quantity.formatQuantity(item.product.por_peso == 1L),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.widthIn(min = 28.dp),
                                textAlign = TextAlign.Center
                            )

                            // Increase quantity
                            IconButton(
                                onClick = {
                                    val increment = if (item.product.por_peso == 1L) 0.1 else 1.0
                                    onUpdateQuantity(item, increment)
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "Aumentar",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            Text(
                                text = "$${(item.product.precio * item.quantity).toString().formatPrice()}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.widthIn(min = 55.dp),
                                textAlign = TextAlign.End
                            )

                            IconButton(
                                onClick = { onRemoveItem(item) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Quitar",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        // Totals and Checkout Button
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Subtotal", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                Text("$${subtotal.toString().formatPrice()}", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("IVA (16%)", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                Text("$${iva.toString().formatPrice()}", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Total", color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                Text(
                    "$${total.toString().formatPrice()}",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onCheckout,
                enabled = cartItems.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                ),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Icon(Icons.Default.Done, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cobrar", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// Helpers for formatted string displays
fun String.formatPrice(): String {
    val doubleVal = this.toDoubleOrNull() ?: return this
    return try {
        val parts = this.split(".")
        if (parts.size == 1) {
            "${parts[0]}.00"
        } else {
            val decimals = parts[1]
            if (decimals.length >= 2) {
                "${parts[0]}.${decimals.substring(0, 2)}"
            } else {
                "${parts[0]}.${decimals}0"
            }
        }
    } catch (_: Exception) {
        this
    }
}

fun Double.formatQuantity(isWeight: Boolean): String {
    return if (isWeight) {
        val parts = this.toString().split(".")
        if (parts.size == 1) {
            "${parts[0]}.000"
        } else {
            val decimals = parts[1]
            if (decimals.length >= 3) {
                "${parts[0]}.${decimals.substring(0, 3)}"
            } else {
                "${parts[0]}.${decimals.padEnd(3, '0')}"
            }
        }
    } else {
        this.toInt().toString()
    }
}
