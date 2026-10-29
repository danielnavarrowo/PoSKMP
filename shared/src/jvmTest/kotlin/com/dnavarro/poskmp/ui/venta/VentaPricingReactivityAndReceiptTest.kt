package com.dnavarro.poskmp.ui.venta

import androidx.compose.ui.graphics.Color
import com.dnavarro.poskmp.data.CustomerRepository
import com.dnavarro.poskmp.data.DEFAULT_PRODUCT_TABLE_COLUMN_NAMES
import com.dnavarro.poskmp.data.ProductRepository
import com.dnavarro.poskmp.data.SaleRepository
import com.dnavarro.poskmp.data.SettingsRepository
import com.dnavarro.poskmp.data.ShiftRepository
import com.dnavarro.poskmp.data.source.remote.dto.RemoteAuditLogDto
import com.dnavarro.poskmp.data.sync.SyncReport
import com.dnavarro.poskmp.data.sync.SyncRepository
import com.dnavarro.poskmp.data.sync.SyncStateEnum
import com.dnavarro.poskmp.db.Products
import com.dnavarro.poskmp.domain.model.AccountStatementItem
import com.dnavarro.poskmp.domain.model.CashMovement
import com.dnavarro.poskmp.domain.model.CashMovementType
import com.dnavarro.poskmp.domain.model.Cashier
import com.dnavarro.poskmp.domain.model.CashierShift
import com.dnavarro.poskmp.domain.model.CategorySalesMetric
import com.dnavarro.poskmp.domain.model.Customer
import com.dnavarro.poskmp.domain.model.CustomerDebtSummary
import com.dnavarro.poskmp.domain.model.CustomerDiscountType
import com.dnavarro.poskmp.domain.model.CustomerPayment
import com.dnavarro.poskmp.domain.model.DailySalesMetric
import com.dnavarro.poskmp.domain.model.DeliveryComparisonMetric
import com.dnavarro.poskmp.domain.model.DeviceRole
import com.dnavarro.poskmp.domain.model.PaymentMethodMetric
import com.dnavarro.poskmp.domain.model.ProductSalesMetric
import com.dnavarro.poskmp.domain.model.ProductSalesStats
import com.dnavarro.poskmp.domain.model.ReceiptDocument
import com.dnavarro.poskmp.domain.model.ReceiptItem
import com.dnavarro.poskmp.domain.model.ReceiptSettings
import com.dnavarro.poskmp.domain.model.Sale
import com.dnavarro.poskmp.domain.model.SaleItem
import com.dnavarro.poskmp.domain.model.SalesSummary
import com.dnavarro.poskmp.domain.model.ShiftSummary
import com.dnavarro.poskmp.domain.receipt.ReceiptFormatter
import com.dnavarro.poskmp.domain.usecase.GetActiveShiftUseCase
import com.dnavarro.poskmp.domain.usecase.GetCashiersUseCase
import com.dnavarro.poskmp.domain.usecase.GetCustomersUseCase
import com.dnavarro.poskmp.domain.usecase.OpenCashDrawerUseCase
import com.dnavarro.poskmp.domain.usecase.OpenShiftUseCase
import com.dnavarro.poskmp.domain.usecase.PrintReceiptUseCase
import com.dnavarro.poskmp.domain.usecase.RecordCashMovementUseCase
import com.dnavarro.poskmp.domain.usecase.RecordSaleUseCase
import com.dnavarro.poskmp.printer.ReceiptPrinter
import com.dnavarro.poskmp.theme.DarkModeConfig
import com.dnavarro.poskmp.ui.CartItem
import com.dnavarro.poskmp.ui.PriceTier
import com.dnavarro.poskmp.ui.Screen
import com.materialkolor.PaletteStyle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class VentaPricingReactivityAndReceiptTest {

    // ==========================================
    // FAKES FOR TESTING
    // ==========================================

    private class FakeProductRepository : ProductRepository {
        val products = mutableListOf<Products>()
        override fun getAllProducts(): Flow<List<Products>> = flowOf(products)
        override fun getActiveProducts(): Flow<List<Products>> = flowOf(products)
        override fun searchProducts(query: String, activeOnly: Boolean): Flow<List<Products>> =
            flowOf(products.filter { it.nombre.contains(query, ignoreCase = true) })
        override fun getProductSalesStats(): Flow<Map<String, ProductSalesStats>> = flowOf(emptyMap())
        override suspend fun getProductById(id: String): Products? = products.find { it.id == id }
        override suspend fun insertProduct(product: Products) { products.add(product) }
        override suspend fun insertProducts(products: List<Products>) { this.products.addAll(products) }
        override suspend fun updateProduct(product: Products) {}
        override suspend fun updateProducts(products: List<Products>) {}
        override suspend fun deleteProductSoft(id: String, updatedAt: Long) {}
        override suspend fun deleteProductHard(id: String) {}
        override suspend fun deleteProductsHard(ids: List<String>) {}
        override suspend fun deleteAllProducts() {}
        override suspend fun getAllProductsList(): List<Products> = products
        override suspend fun getUnsyncedProducts(): List<Products> = emptyList()
        override suspend fun updateSyncStatus(id: String, syncState: String, updatedAt: Long) {}
        override suspend fun findProductByBarcode(barcode: String): Products? = null
        override suspend fun findConflictingProductForBarcodes(barcodes: List<String>, excludeProductId: String?): Pair<String, Products>? = null
        override suspend fun getProductByName(name: String): Products? = products.find { it.nombre.equals(name, ignoreCase = true) }
    }

    private class FakeCustomerRepository : CustomerRepository {
        val customers = mutableListOf<Customer>()
        override fun getActiveCustomers(): Flow<List<Customer>> = flowOf(customers)
        override fun getAllCustomers(): Flow<List<Customer>> = flowOf(customers)
        override suspend fun getCustomerById(id: String): Customer? = customers.find { it.id == id }
        override suspend fun searchActiveCustomers(query: String): List<Customer> =
            customers.filter { it.nombre.contains(query, ignoreCase = true) }
        override suspend fun saveCustomer(customer: Customer) {
            customers.removeAll { it.id == customer.id }
            customers.add(customer)
        }
        override suspend fun setCustomerActiveStatus(id: String, activo: Boolean) {}
        override suspend fun deleteCustomer(id: String) {}
        override suspend fun recordCustomerPayment(payment: CustomerPayment) {}
        override suspend fun deleteCustomerPayment(paymentId: String) {}
        override suspend fun getCustomerDebtSummary(): CustomerDebtSummary = CustomerDebtSummary(0, 0.0, 0)
        override suspend fun getCustomerAccountStatement(customerId: String): List<AccountStatementItem> = emptyList()
    }

    private class FakeSaleRepository : SaleRepository {
        var recordedSale: Sale? = null
        var recordedItems: List<SaleItem> = emptyList()
        private var nextFolio = 1001L

        override suspend fun recordSale(sale: Sale, items: List<SaleItem>): Long {
            recordedSale = sale
            recordedItems = items
            return 1L
        }
        override suspend fun getNextFolio(): Long = nextFolio++
        override suspend fun getSalesSummaryBetween(startTime: Long, endTime: Long, shiftId: String?): SalesSummary =
            SalesSummary(
                totalVentas = 0.0,
                totalSinDescuento = 0.0,
                totalCosto = 0.0,
                totalGanancia = 0.0,
                porcentajeGanancia = 0.0,
                totalTicketCount = 0L,
                promedioTicket = 0.0
            )
        override suspend fun getDeliveryComparisonBetween(startTime: Long, endTime: Long, shiftId: String?): DeliveryComparisonMetric =
            DeliveryComparisonMetric()
        override suspend fun getSoldProductsBetween(startTime: Long, endTime: Long, shiftId: String?): List<ProductSalesMetric> = emptyList()
        override suspend fun getTopSellingProductsBetween(startTime: Long, endTime: Long, limit: Long, shiftId: String?): List<ProductSalesMetric> = emptyList()
        override suspend fun getLeastSellingProductsBetween(startTime: Long, endTime: Long, limit: Long, shiftId: String?): List<ProductSalesMetric> = emptyList()
        override suspend fun getRecentSales(limit: Long, offset: Long): List<Sale> = emptyList()
        override suspend fun getSalesBetween(startTime: Long, endTime: Long, limit: Long, offset: Long, shiftId: String?): List<Sale> = emptyList()
        override suspend fun getPaymentMethodSalesBetween(startTime: Long, endTime: Long, shiftId: String?): List<PaymentMethodMetric> = emptyList()
        override suspend fun getCategorySalesBetween(startTime: Long, endTime: Long, shiftId: String?): List<CategorySalesMetric> = emptyList()
        override suspend fun getDailySalesBetween(startTime: Long, endTime: Long, shiftId: String?): List<DailySalesMetric> = emptyList()
        override suspend fun getSaleById(id: String): Sale? = recordedSale
        override suspend fun getItemsBySaleId(saleId: String): List<SaleItem> = recordedItems
        override suspend fun getTotalSalesCount(): Long = if (recordedSale != null) 1L else 0L
        override suspend fun cancelSale(saleId: String) {}
        override fun getLastSale(): Flow<Sale?> = flowOf(recordedSale)
    }

    private class FakeShiftRepository : ShiftRepository {
        override val activeShiftFlow: Flow<CashierShift?> = flowOf(null)
        override suspend fun getActiveShift(): CashierShift? = null
        override suspend fun getShiftById(id: String): CashierShift? = null
        override suspend fun getShiftsBetween(startTime: Long, endTime: Long): List<CashierShift> = emptyList()
        override val activeCashiersFlow: Flow<List<Cashier>> = flowOf(emptyList())
        override suspend fun getAllActiveCashiers(): List<Cashier> = emptyList()
        override suspend fun getCashierById(id: String): Cashier? = null
        override suspend fun openShift(cashierId: String, initialCash: Double): Result<CashierShift> =
            Result.failure(NotImplementedError())
        override suspend fun closeShift(shiftId: String, countedCash: Double, notes: String?): Result<CashierShift> =
            Result.failure(NotImplementedError())
        override suspend fun recordCashMovement(shiftId: String, cashierId: String, type: CashMovementType, amount: Double, reason: String): Result<CashMovement> =
            Result.failure(NotImplementedError())
        override suspend fun saveCashier(id: String?, nombre: String, pin: String): Result<Cashier> =
            Result.failure(NotImplementedError())
        override suspend fun deleteCashier(id: String): Result<Unit> = Result.success(Unit)
        override suspend fun countActiveCashiers(): Long = 0L
        override suspend fun claimCashiersWithoutDevice() {}
        override fun getMovementsForShiftFlow(shiftId: String): Flow<List<CashMovement>> = flowOf(emptyList())
        override suspend fun getShiftSummary(shiftId: String): Result<ShiftSummary> =
            Result.failure(NotImplementedError())
    }

    private class FakeSyncRepository : SyncRepository {
        private val _syncState = MutableStateFlow(SyncStateEnum.IDLE)
        override val syncState: StateFlow<SyncStateEnum> = _syncState.asStateFlow()
        private val _lastError = MutableStateFlow<String?>(null)
        override val lastError: StateFlow<String?> = _lastError.asStateFlow()

        override suspend fun testConnection(url: String, key: String): Result<Boolean> = Result.success(true)
        override suspend fun syncAll(forceFullSync: Boolean, isManual: Boolean, force: Boolean): Result<SyncReport> =
            Result.success(SyncReport(success = true))
        override suspend fun getRemoteAuditLogs(limit: Int): Result<List<RemoteAuditLogDto>> = Result.success(emptyList())
    }

    private class FakeReceiptPrinter : ReceiptPrinter {
        var lastPrintedDocument: ReceiptDocument? = null
        override suspend fun print(document: ReceiptDocument): Result<Unit> {
            lastPrintedDocument = document
            return Result.success(Unit)
        }
        override suspend fun openCashDrawer(printerId: String?): Result<Unit> = Result.success(Unit)
    }

    private class FakeSettingsRepository : SettingsRepository {
        override val useDynamicColorFlow: Flow<Boolean> = flowOf(false)
        override val seedColorFlow: Flow<Color> = flowOf(Color(0xFF6750A4))
        override val isAmoledFlow: Flow<Boolean> = flowOf(false)
        override val darkModeConfigFlow: Flow<DarkModeConfig> = flowOf(DarkModeConfig.SYSTEM)
        override val paletteStyleFlow: Flow<PaletteStyle> = flowOf(PaletteStyle.TonalSpot)
        override val appScaleFlow: Flow<Float> = flowOf(1.0f)
        override val defaultScreenFlow: Flow<Screen> = flowOf(Screen.VENTA)
        override val isChecadorDialogFlow: Flow<Boolean> = flowOf(false)
        override val showExtraPricesChecadorFlow: Flow<Boolean> = flowOf(false)
        override val lowResourceChecadorShaderFlow: Flow<Boolean> = flowOf(false)
        override val useProductTableInCatalogFlow: Flow<Boolean> = flowOf(false)
        override val swapVentaLayoutOrderFlow: Flow<Boolean> = flowOf(false)
        override val ventaSplitRatioFlow: Flow<Float> = flowOf(0.5f)
        override val beepOnBarcodeScanFlow: Flow<Boolean> = flowOf(false)
        override val defaultRetailMarginFlow: Flow<Double> = flowOf(30.0)
        override val defaultWholesaleMarginFlow: Flow<Double> = flowOf(15.0)
        override val defaultDeliveryMarginFlow: Flow<Double> = flowOf(20.0)
        override val roundRetailPriceFlow: Flow<Boolean> = flowOf(false)
        override val roundWholesalePriceFlow: Flow<Boolean> = flowOf(false)
        override val roundDeliveryPriceFlow: Flow<Boolean> = flowOf(false)
        override val roundTicketTotalFlow: Flow<Boolean> = flowOf(false)
        override val disallowCardPaymentOnWholesaleFlow: Flow<Boolean> = flowOf(false)
        override val prioritizeDeliveryPriceFlow: Flow<Boolean> = flowOf(false)
        override val autoWholesaleByQuantityFlow: Flow<Boolean> = flowOf(false)
        override val autoWholesaleQuantityThresholdFlow: Flow<Int> = flowOf(3)
        override val autoWholesaleByTicketTotalFlow: Flow<Boolean> = flowOf(false)
        override val autoWholesaleTicketTotalThresholdFlow: Flow<Double> = flowOf(1000.0)
        override val autoLookupBarcodeProductsFlow: Flow<Boolean> = flowOf(false)
        override val geminiGroundingEnabledFlow: Flow<Boolean> = flowOf(false)
        override val geminiApiKeyFlow: Flow<String> = flowOf("")
        override val googleSearchEngineIdFlow: Flow<String> = flowOf("")
        override val googleSearchApiKeyFlow: Flow<String> = flowOf("")
        override val productTableVisibleColumnsFlow: Flow<Set<String>> = flowOf(DEFAULT_PRODUCT_TABLE_COLUMN_NAMES)
        override val supabaseUrlFlow: Flow<String> = flowOf("")
        override val supabaseKeyFlow: Flow<String> = flowOf("")
        override val lastSyncTimestampFlow: Flow<Long> = flowOf(0L)
        override val autoSyncEnabledFlow: Flow<Boolean> = flowOf(false)
        override val autoBackupEnabledFlow: Flow<Boolean> = flowOf(false)
        override val lastBackupTimestampFlow: Flow<Long> = flowOf(0L)
        override val backupDirectoryPathFlow: Flow<String> = flowOf("")
        override val businessSettingsUpdatedAtFlow: Flow<Long> = flowOf(0L)
        override val receiptSettingsFlow: Flow<ReceiptSettings> = flowOf(ReceiptSettings())
        override val deviceRoleFlow: Flow<DeviceRole> = flowOf(DeviceRole.ADMIN)
        override val terminalPrefixFlow: Flow<String> = flowOf("")
        override val localActiveShiftIdFlow: Flow<String?> = flowOf(null)
        override val deviceIdFlow: Flow<String> = flowOf("test-device")

        override suspend fun getOrCreateDeviceId(): String = "test-device"
        override suspend fun setLocalActiveShiftId(shiftId: String?) {}
        override suspend fun setDeviceRole(role: DeviceRole) {}
        override suspend fun setTerminalPrefix(prefix: String) {}
        override suspend fun setUseDynamicColor(useDynamic: Boolean) {}
        override suspend fun setSeedColor(color: Color) {}
        override suspend fun setIsAmoled(isAmoled: Boolean) {}
        override suspend fun setDarkModeConfig(config: DarkModeConfig) {}
        override suspend fun setPaletteStyle(style: PaletteStyle) {}
        override suspend fun setAppScale(scale: Float) {}
        override suspend fun setDefaultScreen(screen: Screen) {}
        override suspend fun setIsChecadorDialog(isDialog: Boolean) {}
        override suspend fun setShowExtraPricesChecador(show: Boolean) {}
        override suspend fun setLowResourceChecadorShader(enabled: Boolean) {}
        override suspend fun setUseProductTableInCatalog(enabled: Boolean) {}
        override suspend fun setSwapVentaLayoutOrder(swap: Boolean) {}
        override suspend fun setVentaSplitRatio(ratio: Float) {}
        override suspend fun setBeepOnBarcodeScan(enabled: Boolean) {}
        override suspend fun setDefaultRetailMargin(margin: Double) {}
        override suspend fun setDefaultWholesaleMargin(margin: Double) {}
        override suspend fun setDefaultDeliveryMargin(margin: Double) {}
        override suspend fun setRoundRetailPrice(enabled: Boolean) {}
        override suspend fun setRoundWholesalePrice(enabled: Boolean) {}
        override suspend fun setRoundDeliveryPrice(enabled: Boolean) {}
        override suspend fun setRoundTicketTotal(enabled: Boolean) {}
        override suspend fun setDisallowCardPaymentOnWholesale(disallow: Boolean) {}
        override suspend fun setPrioritizeDeliveryPrice(prioritize: Boolean) {}
        override suspend fun setAutoWholesaleByQuantity(enabled: Boolean) {}
        override suspend fun setAutoWholesaleQuantityThreshold(threshold: Int) {}
        override suspend fun setAutoWholesaleByTicketTotal(enabled: Boolean) {}
        override suspend fun setAutoWholesaleTicketTotalThreshold(threshold: Double) {}
        override suspend fun setAutoLookupBarcodeProducts(enabled: Boolean) {}
        override suspend fun setGeminiGroundingEnabled(enabled: Boolean) {}
        override suspend fun setGeminiApiKey(apiKey: String) {}
        override suspend fun setGoogleSearchEngineId(id: String) {}
        override suspend fun setGoogleSearchApiKey(key: String) {}
        override suspend fun setProductTableVisibleColumns(columns: Set<String>) {}
        override suspend fun toggleProductTableColumn(columnName: String, defaultColumns: Set<String>) {}
        override suspend fun setBusinessSettings(
            defaultRetailMargin: Double,
            defaultWholesaleMargin: Double,
            defaultDeliveryMargin: Double,
            roundRetailPrice: Boolean,
            roundWholesalePrice: Boolean,
            roundDeliveryPrice: Boolean,
            roundTicketTotal: Boolean,
            disallowCardPaymentOnWholesale: Boolean,
            storeName: String,
            storeAddress: String,
            storePhone: String,
            transferClabe: String,
            transferBeneficiary: String,
            receiptFooter: String,
            updatedAt: Long
        ) {}
        override suspend fun setSupabaseUrl(url: String) {}
        override suspend fun setSupabaseKey(key: String) {}
        override suspend fun setLastSyncTimestamp(timestamp: Long) {}
        override suspend fun setAutoSyncEnabled(enabled: Boolean) {}
        override suspend fun setAutoBackupEnabled(enabled: Boolean) {}
        override suspend fun setLastBackupTimestamp(timestamp: Long) {}
        override suspend fun setBackupDirectoryPath(path: String) {}
        override suspend fun setReceiptSettings(settings: ReceiptSettings) {}
        override suspend fun clearAllSettings(preserveSupabaseCredentials: Boolean) {}
    }

    private fun createProduct(
        id: String,
        nombre: String,
        precio: Double,
        costo: Double,
        precioMayoreo: Double = 0.0,
        porPeso: Long = 0L
    ): Products = Products(
        id = id,
        codigos = "[]",
        nombre = nombre,
        precio = precio,
        costo = costo,
        categoria = "General",
        activo = 1L,
        por_peso = porPeso,
        precio_mayoreo = precioMayoreo,
        es_favorito = 0L,
        piezas = 1.0,
        precio_delivery = 0.0,
        created_at = 1000L,
        updated_at = 1000L,
        sync_state = "SYNCED"
    )

    private fun createCustomer(
        id: String,
        nombre: String,
        tipoDescuento: CustomerDiscountType,
        porcentajeDescuento: Double = 0.0,
        siempreMayoreo: Boolean = false
    ): Customer = Customer(
        id = id,
        nombre = nombre,
        telefono = "1234567890",
        direccion = "Av Test 123",
        notas = "",
        limiteCredito = 5000.0,
        saldoDeudor = 0.0,
        activo = true,
        siempreMayoreo = siempreMayoreo,
        tipoDescuento = tipoDescuento,
        porcentajeDescuento = porcentajeDescuento,
        createdAt = 1000L,
        updatedAt = 1000L,
        syncState = "SYNCED"
    )

    private fun createViewModel(
        productRepo: ProductRepository = FakeProductRepository(),
        customerRepo: CustomerRepository = FakeCustomerRepository(),
        saleRepo: SaleRepository = FakeSaleRepository(),
        shiftRepo: ShiftRepository = FakeShiftRepository(),
        syncRepo: SyncRepository = FakeSyncRepository(),
        settingsRepo: SettingsRepository = FakeSettingsRepository(),
        printer: ReceiptPrinter = FakeReceiptPrinter()
    ): VentaViewModel = VentaViewModel(
        repository = productRepo,
        settingsRepository = settingsRepo,
        getCustomersUseCase = GetCustomersUseCase(customerRepo),
        recordSaleUseCase = RecordSaleUseCase(saleRepo, shiftRepo),
        syncRepository = syncRepo,
        printReceiptUseCase = PrintReceiptUseCase(printer),
        openCashDrawerUseCase = OpenCashDrawerUseCase(printer, settingsRepo),
        getActiveShiftUseCase = GetActiveShiftUseCase(shiftRepo),
        getCashiersUseCase = GetCashiersUseCase(shiftRepo),
        openShiftUseCase = OpenShiftUseCase(shiftRepo, settingsRepo),
        recordCashMovementUseCase = RecordCashMovementUseCase(shiftRepo),
        shiftRepository = shiftRepo
    )

    @Suppress("UNCHECKED_CAST")
    private fun VentaViewModel.getCartItems(): List<CartItem> {
        val field = VentaViewModel::class.java.getDeclaredField("_cartItems")
        field.isAccessible = true
        val flow = field.get(this) as StateFlow<List<CartItem>>
        return flow.value
    }

    @Suppress("UNCHECKED_CAST")
    private fun VentaViewModel.getSelectedCustomer(): Customer? {
        val field = VentaViewModel::class.java.getDeclaredField("_selectedCustomer")
        field.isAccessible = true
        val flow = field.get(this) as StateFlow<Customer?>
        return flow.value
    }

    // ==========================================
    // 1. VentaViewModel Pricing Reactivity Tests
    // ==========================================

    @Test
    fun testCustomerReactivity_addItems_selectCostPlus_selectRetailMinus_clearCustomer() = runBlocking {
        val vm = createViewModel()

        // Setup items:
        // Item 1: costo = 50.0, precio = 100.0, precio_mayoreo = 80.0
        val p1 = createProduct("p1", "Producto Uno", precio = 100.0, costo = 50.0, precioMayoreo = 80.0)
        // Item 2: costo = 20.0, precio = 40.0, precio_mayoreo = 30.0
        val p2 = createProduct("p2", "Producto Dos", precio = 40.0, costo = 20.0, precioMayoreo = 30.0)
        // Item 3: zero cost, costo = 0.0, precio = 50.0, precio_mayoreo = 45.0
        val p3 = createProduct("p3", "Producto Costo Cero", precio = 50.0, costo = 0.0, precioMayoreo = 45.0)

        vm.addProductToCart(p1, 1.0)
        vm.addProductToCart(p2, 2.0)
        vm.addProductToCart(p3, 1.0)

        // Step 0: Initial state - No customer selected -> Standard Retail prices
        val initialCart = vm.getCartItems()
        assertEquals(3, initialCart.size)
        assertEquals(100.0, initialCart[0].product.precio)
        assertEquals(PriceTier.RETAIL, initialCart[0].appliedTier)
        assertEquals(40.0, initialCart[1].product.precio)
        assertEquals(PriceTier.RETAIL, initialCart[1].appliedTier)
        assertEquals(50.0, initialCart[2].product.precio)
        assertEquals(PriceTier.RETAIL, initialCart[2].appliedTier)

        // Step 1: Select customer with Cost + 20%
        // Cost + 20% formula: cost * (1 + 0.20)
        // p1: 50.0 * 1.20 = 60.00
        // p2: 20.0 * 1.20 = 24.00
        // p3: costo <= 0.0 -> fallback to wholesale (45.0)
        val costPlusCustomer = createCustomer(
            id = "c-cost-plus",
            nombre = "Cliente Costo + 20%",
            tipoDescuento = CustomerDiscountType.COST_PLUS_PERCENTAGE,
            porcentajeDescuento = 20.0
        )
        vm.selectCustomer(costPlusCustomer)

        val costPlusCart = vm.getCartItems()
        assertEquals(60.0, costPlusCart[0].product.precio, "p1 price should be cost + 20% = 60.0")
        assertEquals(100.0, costPlusCart[0].originalPrice, "p1 original retail price must be preserved")
        assertEquals(PriceTier.CUSTOMER_DISCOUNT, costPlusCart[0].appliedTier)

        assertEquals(24.0, costPlusCart[1].product.precio, "p2 price should be cost + 20% = 24.0")
        assertEquals(40.0, costPlusCart[1].originalPrice)
        assertEquals(PriceTier.CUSTOMER_DISCOUNT, costPlusCart[1].appliedTier)

        assertEquals(45.0, costPlusCart[2].product.precio, "p3 (cost 0) should fall back to wholesale = 45.0")
        assertEquals(50.0, costPlusCart[2].originalPrice)
        assertEquals(PriceTier.CUSTOMER_DISCOUNT, costPlusCart[2].appliedTier)

        // Step 2: Switch to customer with Retail - 15%
        // Retail - 15% formula: retail * (1 - 0.15)
        // p1: 100.0 * 0.85 = 85.00
        // p2: 40.0 * 0.85 = 34.00
        // p3: 50.0 * 0.85 = 42.50
        val retailMinusCustomer = createCustomer(
            id = "c-retail-minus",
            nombre = "Cliente Retail - 15%",
            tipoDescuento = CustomerDiscountType.RETAIL_MINUS_PERCENTAGE,
            porcentajeDescuento = 15.0
        )
        vm.selectCustomer(retailMinusCustomer)

        val retailMinusCart = vm.getCartItems()
        assertEquals(85.0, retailMinusCart[0].product.precio, "p1 price should be retail - 15% = 85.0")
        assertEquals(100.0, retailMinusCart[0].originalPrice)
        assertEquals(PriceTier.CUSTOMER_DISCOUNT, retailMinusCart[0].appliedTier)

        assertEquals(34.0, retailMinusCart[1].product.precio, "p2 price should be retail - 15% = 34.0")
        assertEquals(40.0, retailMinusCart[1].originalPrice)
        assertEquals(PriceTier.CUSTOMER_DISCOUNT, retailMinusCart[1].appliedTier)

        assertEquals(42.5, retailMinusCart[2].product.precio, "p3 price should be retail - 15% = 42.5")
        assertEquals(50.0, retailMinusCart[2].originalPrice)
        assertEquals(PriceTier.CUSTOMER_DISCOUNT, retailMinusCart[2].appliedTier)

        // Step 3: Clear customer -> all items must be restored to retail
        vm.clearSelectedCustomer()

        val clearedCart = vm.getCartItems()
        assertNull(vm.getSelectedCustomer(), "Selected customer should be null")
        assertEquals(100.0, clearedCart[0].product.precio, "p1 price should be restored to retail 100.0")
        assertEquals(PriceTier.RETAIL, clearedCart[0].appliedTier)

        assertEquals(40.0, clearedCart[1].product.precio, "p2 price should be restored to retail 40.0")
        assertEquals(PriceTier.RETAIL, clearedCart[1].appliedTier)

        assertEquals(50.0, clearedCart[2].product.precio, "p3 price should be restored to retail 50.0")
        assertEquals(PriceTier.RETAIL, clearedCart[2].appliedTier)
    }

    @Test
    fun testCustomerReactivity_addItemAfterCustomerSelection() = runBlocking {
        val vm = createViewModel()

        // Select customer with Cost + 20% first
        val costPlusCustomer = createCustomer(
            id = "c-cost-plus",
            nombre = "Cliente Costo + 20%",
            tipoDescuento = CustomerDiscountType.COST_PLUS_PERCENTAGE,
            porcentajeDescuento = 20.0
        )
        vm.selectCustomer(costPlusCustomer)

        // Now add an item
        val p = createProduct("p-late", "Producto Tardio", precio = 120.0, costo = 60.0, precioMayoreo = 90.0)
        vm.addProductToCart(p, 1.0)

        val cart = vm.getCartItems()
        assertEquals(1, cart.size)
        // 60.0 * 1.20 = 72.00
        assertEquals(72.0, cart[0].product.precio, "Newly added product should automatically inherit active customer discount")
        assertEquals(120.0, cart[0].originalPrice)
        assertEquals(PriceTier.CUSTOMER_DISCOUNT, cart[0].appliedTier)

        // Clear customer
        vm.clearSelectedCustomer()
        val restoredCart = vm.getCartItems()
        assertEquals(120.0, restoredCart[0].product.precio, "Should be restored to retail after clearing customer")
        assertEquals(PriceTier.RETAIL, restoredCart[0].appliedTier)
    }

    @Test
    fun testPriceTierCycling_individualItemAndTicketWide() = runBlocking {
        val vm = createViewModel()

        val p1 = createProduct("p1", "Prod Con Mayoreo", precio = 100.0, costo = 50.0, precioMayoreo = 80.0)
        val p2 = createProduct("p2", "Prod Sin Mayoreo", precio = 50.0, costo = 20.0, precioMayoreo = 0.0)

        vm.addProductToCart(p1, 1.0)
        vm.addProductToCart(p2, 1.0)

        // Select customer with Cost + 20%
        // p1: cost 50 -> 60.0, wholesale: 80.0, retail: 100.0
        // p2: cost 20 -> 24.0, wholesale: 0.0, retail: 50.0
        val customer = createCustomer("c1", "Cliente C+", CustomerDiscountType.COST_PLUS_PERCENTAGE, 20.0)
        vm.selectCustomer(customer)

        val cart0 = vm.getCartItems()
        assertEquals(60.0, cart0[0].product.precio)
        assertEquals(PriceTier.CUSTOMER_DISCOUNT, cart0[0].appliedTier)
        assertEquals(24.0, cart0[1].product.precio)
        assertEquals(PriceTier.CUSTOMER_DISCOUNT, cart0[1].appliedTier)

        // Test 1: Cycle p1 individually: Customer Discount (60.0) -> Wholesale (80.0) -> Retail (100.0) -> Customer Discount (60.0)
        vm.cyclePriceTierForItem(cart0[0])
        val cart1 = vm.getCartItems()
        assertEquals(PriceTier.WHOLESALE, cart1[0].appliedTier)
        assertEquals(80.0, cart1[0].product.precio)

        vm.cyclePriceTierForItem(cart1[0])
        val cart2 = vm.getCartItems()
        assertEquals(PriceTier.RETAIL, cart2[0].appliedTier)
        assertEquals(100.0, cart2[0].product.precio)

        vm.cyclePriceTierForItem(cart2[0])
        val cart3 = vm.getCartItems()
        assertEquals(PriceTier.CUSTOMER_DISCOUNT, cart3[0].appliedTier)
        assertEquals(60.0, cart3[0].product.precio)

        // Test 2: Cycle p2 individually (no wholesale price: precio_mayoreo <= 0):
        // Should transition directly: Customer Discount (24.0) -> Retail (50.0) -> Customer Discount (24.0)
        vm.cyclePriceTierForItem(cart3[1])
        val cart4 = vm.getCartItems()
        assertEquals(PriceTier.RETAIL, cart4[1].appliedTier, "Items without wholesale should skip Wholesale and cycle directly to Retail")
        assertEquals(50.0, cart4[1].product.precio)

        vm.cyclePriceTierForItem(cart4[1])
        val cart5 = vm.getCartItems()
        assertEquals(PriceTier.CUSTOMER_DISCOUNT, cart5[1].appliedTier)
        assertEquals(24.0, cart5[1].product.precio)

        // Test 3: Whole ticket tier cycling (cycleTicketPriceTier)
        // Both items at CUSTOMER_DISCOUNT -> cycle to WHOLESALE (p1 gets wholesale 80, p2 gets retail 50)
        vm.cycleTicketPriceTier()
        val ticketWholesaleCart = vm.getCartItems()
        assertEquals(PriceTier.WHOLESALE, ticketWholesaleCart[0].appliedTier)
        assertEquals(80.0, ticketWholesaleCart[0].product.precio)
        assertEquals(PriceTier.RETAIL, ticketWholesaleCart[1].appliedTier, "p2 has no wholesale, should fall back to retail")
        assertEquals(50.0, ticketWholesaleCart[1].product.precio)

        // Next cycle: all transition to RETAIL
        vm.cycleTicketPriceTier()
        val ticketRetailCart = vm.getCartItems()
        assertEquals(PriceTier.RETAIL, ticketRetailCart[0].appliedTier)
        assertEquals(100.0, ticketRetailCart[0].product.precio)
        assertEquals(PriceTier.RETAIL, ticketRetailCart[1].appliedTier)
        assertEquals(50.0, ticketRetailCart[1].product.precio)

        // Next cycle: all transition back to CUSTOMER_DISCOUNT
        vm.cycleTicketPriceTier()
        val ticketBackToCustomCart = vm.getCartItems()
        assertEquals(PriceTier.CUSTOMER_DISCOUNT, ticketBackToCustomCart[0].appliedTier)
        assertEquals(60.0, ticketBackToCustomCart[0].product.precio)
        assertEquals(PriceTier.CUSTOMER_DISCOUNT, ticketBackToCustomCart[1].appliedTier)
        assertEquals(24.0, ticketBackToCustomCart[1].product.precio)
    }

    // ==========================================
    // 2. Persistence in RecordSaleUseCase
    // ==========================================

    @Test
    fun testRecordSaleUseCase_persistsAccurateUnitPricesSubtotalsAndTotalOriginal() = runBlocking {
        val fakeSaleRepo = FakeSaleRepository()
        val recordSaleUseCase = RecordSaleUseCase(saleRepository = fakeSaleRepo)

        val p1 = createProduct("p1", "Item Descuento Cliente", precio = 60.0, costo = 50.0, precioMayoreo = 80.0)
        val p2 = createProduct("p2", "Item Mayoreo", precio = 30.0, costo = 20.0, precioMayoreo = 30.0)
        val p3 = createProduct("p3", "Item Retail Normal", precio = 50.0, costo = 30.0, precioMayoreo = 40.0)

        val cartItems = listOf(
            CartItem(
                product = p1,
                quantity = 2.0,
                originalPrice = 100.0,
                appliedTier = PriceTier.CUSTOMER_DISCOUNT
            ),
            CartItem(
                product = p2,
                quantity = 3.0,
                originalPrice = 40.0,
                appliedTier = PriceTier.WHOLESALE
            ),
            CartItem(
                product = p3,
                quantity = 1.0,
                originalPrice = 50.0,
                appliedTier = PriceTier.RETAIL
            )
        )

        // Totals calculation:
        // Item 1: 60.0 * 2 = 120.0 subtotal; original = 100.0 * 2 = 200.0; cost = 50.0 * 2 = 100.0; profit = 20.0
        // Item 2: 30.0 * 3 = 90.0 subtotal; original = 40.0 * 3 = 120.0; cost = 20.0 * 3 = 60.0; profit = 30.0
        // Item 3: 50.0 * 1 = 50.0 subtotal; original = 50.0 * 1 = 50.0; cost = 30.0 * 1 = 30.0; profit = 20.0
        // Total = 120.0 + 90.0 + 50.0 = 260.0
        // TotalOriginal = 200.0 + 120.0 + 50.0 = 370.0
        // TotalCosto = 100.0 + 60.0 + 30.0 = 190.0
        // Ganancia = 260.0 - 190.0 = 70.0

        val folio = recordSaleUseCase(
            cartItems = cartItems,
            pagoCon = 500.0,
            cambio = 240.0,
            metodoPago = "EFECTIVO",
            customerId = "cust-123"
        )

        assertEquals(1L, folio)
        val recordedSale = fakeSaleRepo.recordedSale
        assertNotNull(recordedSale, "Sale must be recorded in repository")
        assertEquals(260.0, recordedSale.total, "Recorded total must match discounted sum")
        assertEquals(370.0, recordedSale.totalOriginal, "Recorded totalOriginal must match baseline retail sum")
        assertEquals(190.0, recordedSale.totalCosto)
        assertEquals(70.0, recordedSale.ganancia)
        assertEquals("cust-123", recordedSale.customerId)

        val recordedItems = fakeSaleRepo.recordedItems
        assertEquals(3, recordedItems.size)

        // Item 1 assertions:
        assertEquals("p1", recordedItems[0].productId)
        assertEquals(60.0, recordedItems[0].precioUnitario, "Unit price must be discounted price")
        assertEquals(120.0, recordedItems[0].subtotal)
        assertEquals(50.0, recordedItems[0].costoUnitario)
        assertEquals(20.0, recordedItems[0].ganancia)
        assertFalse(recordedItems[0].esMayoreo, "Customer discount is not wholesale")

        // Item 2 assertions:
        assertEquals("p2", recordedItems[1].productId)
        assertEquals(30.0, recordedItems[1].precioUnitario)
        assertEquals(90.0, recordedItems[1].subtotal)
        assertEquals(20.0, recordedItems[1].costoUnitario)
        assertEquals(30.0, recordedItems[1].ganancia)
        assertTrue(recordedItems[1].esMayoreo, "Item 2 was applied at wholesale")

        // Item 3 assertions:
        assertEquals("p3", recordedItems[2].productId)
        assertEquals(50.0, recordedItems[2].precioUnitario)
        assertEquals(50.0, recordedItems[2].subtotal)
        assertEquals(30.0, recordedItems[2].costoUnitario)
        assertEquals(20.0, recordedItems[2].ganancia)
        assertFalse(recordedItems[2].esMayoreo)
    }

    // ==========================================
    // 3. Receipt Formatting in ReceiptFormatter
    // ==========================================

    @Test
    fun testReceiptFormatter_lineItemUnitPriceDetails_whenDiscounted() {
        val settings = ReceiptSettings(storeName = "Tienda PosKMP")

        // 3 items to format:
        // Item 1: quantity = 1.0, originalUnitPrice = 100.0, unitPrice = 85.0 (discounted) -> must show ($85.00 c/u - desc.)
        // Item 2: quantity = 2.0, originalUnitPrice = 50.0, unitPrice = 50.0 (not discounted, qty != 1) -> must show ($50.00 c/u)
        // Item 3: quantity = 1.0, originalUnitPrice = 30.0, unitPrice = 30.0 (not discounted, qty == 1) -> NO unit price detail
        val items = listOf(
            ReceiptItem(
                name = "Playera Algodon",
                quantity = 1.0,
                unitPrice = 85.0,
                subtotal = 85.0,
                originalUnitPrice = 100.0,
                isWeightBased = false
            ),
            ReceiptItem(
                name = "Calcetines",
                quantity = 2.0,
                unitPrice = 50.0,
                subtotal = 100.0,
                originalUnitPrice = 50.0,
                isWeightBased = false
            ),
            ReceiptItem(
                name = "Gorra Basica",
                quantity = 1.0,
                unitPrice = 30.0,
                subtotal = 30.0,
                originalUnitPrice = 30.0,
                isWeightBased = false
            )
        )

        val doc = ReceiptFormatter.create(
            folio = 101L,
            createdAt = 1700000000000L,
            items = items,
            total = 215.0,
            paid = 300.0,
            change = 85.0,
            paymentMethod = "EFECTIVO",
            customerName = "Juan Perez",
            settings = settings,
            overrideTotalOriginal = 230.0
        )

        val renderedLines = doc.lines.map { it.text }

        // Assert Item 1 shows unit price detail with "- desc." even though quantity is 1
        val item1DetailLine = renderedLines.find { it.contains("($85.00 c/u - desc.)") }
        assertNotNull(item1DetailLine, "Discounted line item must show ($85.00 c/u - desc.)")

        // Assert Item 2 shows regular ($50.00 c/u) without "- desc."
        val item2DetailLine = renderedLines.find { it.contains("($50.00 c/u)") && !it.contains("- desc.") }
        assertNotNull(item2DetailLine, "Non-discounted multi-qty line item must show ($50.00 c/u)")

        // Assert Item 3 does not show any unit detail line
        val item3DetailLine = renderedLines.find { it.contains("30.00 c/u") }
        assertNull(item3DetailLine, "Non-discounted single qty item should not show redundant unit price detail")

        // Assert discount summary section in receipt
        val totalSinDescLine = renderedLines.find { it.contains("Total sin desc:") }
        assertNotNull(totalSinDescLine, "Receipt must contain 'Total sin desc:'")
        assertTrue(totalSinDescLine.contains("230.00"), "Total sin desc must display \$230.00")

        val ahorroLine = renderedLines.find { it.contains("Ahorro:") }
        assertNotNull(ahorroLine, "Receipt must contain 'Ahorro:'")
        assertTrue(ahorroLine.contains("15.00"), "Ahorro must display \$15.00 (230.00 - 215.00)")
    }

    @Test
    fun testReceiptFormatter_createFromSale_properlyComputesTotalSinDescAndAhorro() {
        val settings = ReceiptSettings(storeName = "Tienda PosKMP")

        // Sale with total = 260.0 and totalOriginal = 370.0
        val sale = Sale(
            id = "s-100",
            folio = 1001L,
            total = 260.0,
            totalOriginal = 370.0,
            totalCosto = 190.0,
            ganancia = 70.0,
            pagoCon = 500.0,
            cambio = 240.0,
            metodoPago = "EFECTIVO",
            totalItems = 6.0,
            customerId = "cust-1",
            createdAt = 1700000000000L
        )

        // SaleItems stored in SQLite only have precioUnitario and subtotal
        val saleItems = listOf(
            SaleItem(
                id = "si-1",
                saleId = "s-100",
                productId = "p1",
                productNombre = "Item Descuento Cliente",
                cantidad = 2.0,
                precioUnitario = 60.0,
                costoUnitario = 50.0,
                subtotal = 120.0,
                ganancia = 20.0,
                esMayoreo = false,
                createdAt = 1700000000000L
            ),
            SaleItem(
                id = "si-2",
                saleId = "s-100",
                productId = "p2",
                productNombre = "Item Mayoreo",
                cantidad = 3.0,
                precioUnitario = 30.0,
                costoUnitario = 20.0,
                subtotal = 90.0,
                ganancia = 30.0,
                esMayoreo = true,
                createdAt = 1700000000000L
            ),
            SaleItem(
                id = "si-3",
                saleId = "s-100",
                productId = "p3",
                productNombre = "Item Retail Normal",
                cantidad = 1.0,
                precioUnitario = 50.0,
                costoUnitario = 30.0,
                subtotal = 50.0,
                ganancia = 20.0,
                esMayoreo = false,
                createdAt = 1700000000000L
            )
        )

        val doc = ReceiptFormatter.createFromSale(
            sale = sale,
            items = saleItems,
            customerName = "Maria Lopez",
            settings = settings
        )

        val renderedLines = doc.lines.map { it.text }

        // Verify "Total sin desc:" line
        val totalSinDescLine = renderedLines.find { it.contains("Total sin desc:") }
        assertNotNull(totalSinDescLine, "Reprinted receipt must contain 'Total sin desc:'")
        assertTrue(totalSinDescLine.contains("370.00"), "Total sin desc must display \$370.00 from sale.totalOriginal")

        // Verify "Ahorro:" line
        val ahorroLine = renderedLines.find { it.contains("Ahorro:") }
        assertNotNull(ahorroLine, "Reprinted receipt must contain 'Ahorro:'")
        assertTrue(ahorroLine.contains("110.00"), "Ahorro must display \$110.00 (370.00 - 260.00)")

        // Verify Total line
        val totalLine = renderedLines.find { it.contains("TOTAL") }
        assertNotNull(totalLine, "Receipt must contain 'TOTAL'")
        assertTrue(totalLine.contains("260.00"), "Total must display charged amount \$260.00")
    }

    @Test
    fun testReceiptFormatter_noDiscountSale_doesNotShowTotalSinDescOrAhorro() {
        val settings = ReceiptSettings(storeName = "Tienda PosKMP")

        val sale = Sale(
            id = "s-200",
            folio = 1002L,
            total = 100.0,
            totalOriginal = 100.0,
            totalCosto = 60.0,
            ganancia = 40.0,
            pagoCon = 100.0,
            cambio = 0.0,
            metodoPago = "EFECTIVO",
            totalItems = 1.0,
            createdAt = 1700000000000L
        )

        val saleItems = listOf(
            SaleItem(
                id = "si-4",
                saleId = "s-200",
                productId = "p4",
                productNombre = "Item Sin Descuento",
                cantidad = 1.0,
                precioUnitario = 100.0,
                costoUnitario = 60.0,
                subtotal = 100.0,
                ganancia = 40.0,
                esMayoreo = false,
                createdAt = 1700000000000L
            )
        )

        val doc = ReceiptFormatter.createFromSale(
            sale = sale,
            items = saleItems,
            settings = settings
        )

        val renderedLines = doc.lines.map { it.text }
        assertNull(renderedLines.find { it.contains("Total sin desc:") }, "Should not display 'Total sin desc:' when there is no discount")
        assertNull(renderedLines.find { it.contains("Ahorro:") }, "Should not display 'Ahorro:' when there are no savings")
    }
}
