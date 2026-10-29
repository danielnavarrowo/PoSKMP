package com.dnavarro.poskmp.ui.venta

import com.dnavarro.poskmp.data.CustomerRepository
import com.dnavarro.poskmp.data.ProductRepository
import com.dnavarro.poskmp.data.SaleRepository
import com.dnavarro.poskmp.data.SettingsRepository
import com.dnavarro.poskmp.data.ShiftRepository
import com.dnavarro.poskmp.data.sync.SyncRepository
import com.dnavarro.poskmp.db.Products
import com.dnavarro.poskmp.domain.model.Customer
import com.dnavarro.poskmp.domain.model.CustomerDiscountType
import com.dnavarro.poskmp.domain.model.ReceiptFontFamily
import com.dnavarro.poskmp.domain.model.ReceiptSettings
import com.dnavarro.poskmp.printer.ReceiptPrinter
import com.dnavarro.poskmp.domain.usecase.FindProductByBarcodeUseCase
import com.dnavarro.poskmp.domain.usecase.GetActiveShiftUseCase
import com.dnavarro.poskmp.domain.usecase.GetCashiersUseCase
import com.dnavarro.poskmp.domain.usecase.GetCustomersUseCase
import com.dnavarro.poskmp.domain.usecase.GetProductsUseCase
import com.dnavarro.poskmp.domain.usecase.OpenCashDrawerUseCase
import com.dnavarro.poskmp.domain.usecase.OpenShiftUseCase
import com.dnavarro.poskmp.domain.usecase.PrintReceiptUseCase
import com.dnavarro.poskmp.domain.usecase.RecordCashMovementUseCase
import com.dnavarro.poskmp.domain.usecase.RecordSaleUseCase
import com.dnavarro.poskmp.domain.usecase.SaveProductUseCase
import com.dnavarro.poskmp.ui.CartItem
import com.dnavarro.poskmp.ui.PriceTier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import java.lang.reflect.Proxy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VentaViewModelPriceTierCyclingTest {

    private fun createTestProduct(
        id: String,
        nombre: String,
        precio: Double,
        costo: Double = 0.0,
        precioMayoreo: Double = 0.0,
        precioDelivery: Double = 0.0
    ): Products = Products(
        id = id,
        codigos = "[]",
        nombre = nombre,
        precio = precio,
        costo = costo,
        categoria = "General",
        activo = 1L,
        por_peso = 0L,
        precio_mayoreo = precioMayoreo,
        es_favorito = 0L,
        piezas = 10.0,
        precio_delivery = precioDelivery,
        created_at = 1000L,
        updated_at = 1000L,
        sync_state = "SYNCED"
    )

    private fun createVentaViewModel(): VentaViewModel {
        val dummyInvocationHandler = java.lang.reflect.InvocationHandler { _, method, _ ->
            val rt = method.returnType
            when {
                rt == Flow::class.java -> {
                    when (method.name) {
                        "getUseProductTableInCatalogFlow" -> flowOf(false)
                        "getDeviceIdFlow" -> flowOf("test-device")
                        "getSwapVentaLayoutOrderFlow" -> flowOf(false)
                        "getBeepOnBarcodeScanFlow" -> flowOf(false)
                        "getVentaSplitRatioFlow" -> flowOf(0.6f)
                        "getDefaultRetailMarginFlow" -> flowOf(0.0)
                        "getDefaultWholesaleMarginFlow" -> flowOf(0.0)
                        "getDefaultDeliveryMarginFlow" -> flowOf(0.0)
                        "getAutoWholesaleByQuantityFlow" -> flowOf(false)
                        "getAutoWholesaleQuantityThresholdFlow" -> flowOf(3)
                        "getAutoWholesaleByTicketTotalFlow" -> flowOf(false)
                        "getAutoWholesaleTicketTotalThresholdFlow" -> flowOf(0.0)
                        "getRoundRetailPriceFlow" -> flowOf(false)
                        "getRoundWholesalePriceFlow" -> flowOf(false)
                        "getRoundDeliveryPriceFlow" -> flowOf(false)
                        "getRoundTicketTotalFlow" -> flowOf(false)
                        "getPrioritizeDeliveryPriceFlow" -> flowOf(false)
                        "getDisallowCardPaymentOnWholesaleFlow" -> flowOf(false)
                        "getAutoLookupBarcodeProductsFlow" -> flowOf(true)
                        "getGeminiGroundingEnabledFlow" -> flowOf(false)
                        "getGeminiApiKeyFlow" -> flowOf("")
                        "getGoogleSearchEngineIdFlow" -> flowOf("")
                        "getGoogleSearchApiKeyFlow" -> flowOf("")
                        "getReceiptPaperWidthFlow" -> flowOf(80)
                        "getReceiptFontFamilyFlow" -> flowOf(ReceiptFontFamily.MONOSPACE)
                        "getReceiptSettingsFlow" -> flowOf(ReceiptSettings())
                        "getActiveShiftFlow" -> flowOf<com.dnavarro.poskmp.domain.model.CashierShift?>(null)
                        "getActiveCashiersFlow" -> flowOf(emptyList<com.dnavarro.poskmp.domain.model.Cashier>())
                        "getAllProducts", "getActiveProducts", "searchProducts" -> flowOf(emptyList<Products>())
                        "getActiveCustomers", "getAllCustomers" -> flowOf(emptyList<Customer>())
                        else -> flowOf<Any?>(null)
                    }
                }
                rt == StateFlow::class.java -> MutableStateFlow<Any?>(null)
                rt == Boolean::class.javaPrimitiveType -> false
                rt == Int::class.javaPrimitiveType -> 0
                rt == Long::class.javaPrimitiveType -> 0L
                rt == Double::class.javaPrimitiveType -> 0.0
                rt == Float::class.javaPrimitiveType -> 0.0f
                rt == String::class.java -> "test-device"
                rt == List::class.java -> emptyList<Any>()
                rt == Map::class.java -> emptyMap<Any, Any>()
                rt == Set::class.java -> emptySet<Any>()
                rt == Result::class.java -> Result.success(Unit)
                else -> null
            }
        }

        val productRepo = Proxy.newProxyInstance(
            ProductRepository::class.java.classLoader,
            arrayOf(ProductRepository::class.java),
            dummyInvocationHandler
        ) as ProductRepository

        val settingsRepo = Proxy.newProxyInstance(
            SettingsRepository::class.java.classLoader,
            arrayOf(SettingsRepository::class.java),
            dummyInvocationHandler
        ) as SettingsRepository

        val customerRepo = Proxy.newProxyInstance(
            CustomerRepository::class.java.classLoader,
            arrayOf(CustomerRepository::class.java),
            dummyInvocationHandler
        ) as CustomerRepository

        val saleRepo = Proxy.newProxyInstance(
            SaleRepository::class.java.classLoader,
            arrayOf(SaleRepository::class.java),
            dummyInvocationHandler
        ) as SaleRepository

        val syncRepo = Proxy.newProxyInstance(
            SyncRepository::class.java.classLoader,
            arrayOf(SyncRepository::class.java),
            dummyInvocationHandler
        ) as SyncRepository

        val receiptPrinter = Proxy.newProxyInstance(
            ReceiptPrinter::class.java.classLoader,
            arrayOf(ReceiptPrinter::class.java),
            dummyInvocationHandler
        ) as ReceiptPrinter

        val shiftRepo = Proxy.newProxyInstance(
            ShiftRepository::class.java.classLoader,
            arrayOf(ShiftRepository::class.java),
            dummyInvocationHandler
        ) as ShiftRepository

        return VentaViewModel(
            repository = productRepo,
            settingsRepository = settingsRepo,
            getProductsUseCase = GetProductsUseCase(productRepo),
            findProductByBarcodeUseCase = FindProductByBarcodeUseCase(productRepo),
            saveProductUseCase = SaveProductUseCase(productRepo),
            getCustomersUseCase = GetCustomersUseCase(customerRepo),
            recordSaleUseCase = RecordSaleUseCase(saleRepo, shiftRepo),
            syncRepository = syncRepo,
            printReceiptUseCase = PrintReceiptUseCase(receiptPrinter),
            openCashDrawerUseCase = OpenCashDrawerUseCase(receiptPrinter, settingsRepo),
            getActiveShiftUseCase = GetActiveShiftUseCase(shiftRepo),
            getCashiersUseCase = GetCashiersUseCase(shiftRepo),
            openShiftUseCase = OpenShiftUseCase(shiftRepo, settingsRepo),
            recordCashMovementUseCase = RecordCashMovementUseCase(shiftRepo),
            shiftRepository = shiftRepo
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun VentaViewModel.getCartItems(): List<CartItem> {
        val field = VentaViewModel::class.java.getDeclaredField("_cartItems")
        field.isAccessible = true
        val flow = field.get(this) as StateFlow<List<CartItem>>
        return flow.value
    }

    // =========================================================================
    // 1. Single Item Cycling: Custom Discount Customer
    // =========================================================================

    @Test
    fun testSingleItemCycling_customDiscount_withWholesalePrice() {
        val vm = createVentaViewModel()
        val customer = Customer(
            id = "c1",
            nombre = "Cliente Costo + 10%",
            tipoDescuento = CustomerDiscountType.COST_PLUS_PERCENTAGE,
            porcentajeDescuento = 10.0
        )
        vm.selectCustomer(customer)

        val product = createTestProduct(
            id = "p1",
            nombre = "Producto Con Mayoreo",
            precio = 100.0,
            costo = 50.0,
            precioMayoreo = 80.0
        )
        vm.addProductToCart(product, 1.0)

        // Initial state: CUSTOMER_DISCOUNT (50 * 1.10 = 55.0)
        var item = vm.getCartItems().first()
        assertEquals(PriceTier.CUSTOMER_DISCOUNT, item.appliedTier)
        assertEquals(55.0, item.product.precio)

        // 1st Cycle: CUSTOMER_DISCOUNT -> WHOLESALE (80.0)
        vm.cyclePriceTierForItem(item)
        item = vm.getCartItems().first()
        assertEquals(PriceTier.WHOLESALE, item.appliedTier)
        assertEquals(80.0, item.product.precio)

        // 2nd Cycle: WHOLESALE -> RETAIL (100.0)
        vm.cyclePriceTierForItem(item)
        item = vm.getCartItems().first()
        assertEquals(PriceTier.RETAIL, item.appliedTier)
        assertEquals(100.0, item.product.precio)

        // 3rd Cycle: RETAIL -> CUSTOMER_DISCOUNT (55.0)
        vm.cyclePriceTierForItem(item)
        item = vm.getCartItems().first()
        assertEquals(PriceTier.CUSTOMER_DISCOUNT, item.appliedTier)
        assertEquals(55.0, item.product.precio)

        // 4th Cycle: Repeats back to WHOLESALE (80.0)
        vm.cyclePriceTierForItem(item)
        item = vm.getCartItems().first()
        assertEquals(PriceTier.WHOLESALE, item.appliedTier)
        assertEquals(80.0, item.product.precio)
    }

    @Test
    fun testSingleItemCycling_customDiscount_withoutWholesalePrice() {
        val vm = createVentaViewModel()
        val customer = Customer(
            id = "c1",
            nombre = "Cliente Costo + 10%",
            tipoDescuento = CustomerDiscountType.COST_PLUS_PERCENTAGE,
            porcentajeDescuento = 10.0
        )
        vm.selectCustomer(customer)

        val product = createTestProduct(
            id = "p2",
            nombre = "Producto Sin Mayoreo",
            precio = 100.0,
            costo = 50.0,
            precioMayoreo = 0.0 // <= 0.0
        )
        vm.addProductToCart(product, 1.0)

        // Initial state: CUSTOMER_DISCOUNT (55.0)
        var item = vm.getCartItems().first()
        assertEquals(PriceTier.CUSTOMER_DISCOUNT, item.appliedTier)
        assertEquals(55.0, item.product.precio)

        // 1st Cycle: CUSTOMER_DISCOUNT -> RETAIL (100.0) (Wholesale skipped)
        vm.cyclePriceTierForItem(item)
        item = vm.getCartItems().first()
        assertEquals(PriceTier.RETAIL, item.appliedTier)
        assertEquals(100.0, item.product.precio)

        // 2nd Cycle: RETAIL -> CUSTOMER_DISCOUNT (55.0)
        vm.cyclePriceTierForItem(item)
        item = vm.getCartItems().first()
        assertEquals(PriceTier.CUSTOMER_DISCOUNT, item.appliedTier)
        assertEquals(55.0, item.product.precio)

        // 3rd Cycle: CUSTOMER_DISCOUNT -> RETAIL (100.0)
        vm.cyclePriceTierForItem(item)
        item = vm.getCartItems().first()
        assertEquals(PriceTier.RETAIL, item.appliedTier)
        assertEquals(100.0, item.product.precio)
    }

    @Test
    fun testSingleItemCycling_retailMinusPercentage() {
        val vm = createVentaViewModel()
        val customer = Customer(
            id = "c2",
            nombre = "Cliente Retail - 20%",
            tipoDescuento = CustomerDiscountType.RETAIL_MINUS_PERCENTAGE,
            porcentajeDescuento = 20.0
        )
        vm.selectCustomer(customer)

        val productWithWholesale = createTestProduct(
            id = "p3",
            nombre = "Producto Desc + Mayoreo",
            precio = 100.0,
            costo = 40.0,
            precioMayoreo = 85.0
        )
        vm.addProductToCart(productWithWholesale, 1.0)

        // Initial: 100 * 0.8 = 80.0
        var item = vm.getCartItems().first()
        assertEquals(PriceTier.CUSTOMER_DISCOUNT, item.appliedTier)
        assertEquals(80.0, item.product.precio)

        // Cycle 1: -> WHOLESALE (85.0)
        vm.cyclePriceTierForItem(item)
        item = vm.getCartItems().first()
        assertEquals(PriceTier.WHOLESALE, item.appliedTier)
        assertEquals(85.0, item.product.precio)

        // Cycle 2: -> RETAIL (100.0)
        vm.cyclePriceTierForItem(item)
        item = vm.getCartItems().first()
        assertEquals(PriceTier.RETAIL, item.appliedTier)
        assertEquals(100.0, item.product.precio)

        // Cycle 3: -> CUSTOMER_DISCOUNT (80.0)
        vm.cyclePriceTierForItem(item)
        item = vm.getCartItems().first()
        assertEquals(PriceTier.CUSTOMER_DISCOUNT, item.appliedTier)
        assertEquals(80.0, item.product.precio)
    }

    // =========================================================================
    // 2. Single Item Cycling: Customer without Custom Discount (or no customer)
    // =========================================================================

    @Test
    fun testSingleItemCycling_noCustomer_withWholesalePrice() {
        val vm = createVentaViewModel()
        // No customer selected

        val product = createTestProduct(
            id = "p1",
            nombre = "Refresco",
            precio = 20.0,
            costo = 12.0,
            precioMayoreo = 16.0
        )
        vm.addProductToCart(product, 1.0)

        // Initial: RETAIL (20.0)
        var item = vm.getCartItems().first()
        assertEquals(PriceTier.RETAIL, item.appliedTier)
        assertEquals(20.0, item.product.precio)

        // 1st Cycle: RETAIL -> WHOLESALE (16.0)
        vm.cyclePriceTierForItem(item)
        item = vm.getCartItems().first()
        assertEquals(PriceTier.WHOLESALE, item.appliedTier)
        assertEquals(16.0, item.product.precio)

        // 2nd Cycle: WHOLESALE -> RETAIL (20.0)
        vm.cyclePriceTierForItem(item)
        item = vm.getCartItems().first()
        assertEquals(PriceTier.RETAIL, item.appliedTier)
        assertEquals(20.0, item.product.precio)

        // 3rd Cycle: RETAIL -> WHOLESALE (16.0)
        vm.cyclePriceTierForItem(item)
        item = vm.getCartItems().first()
        assertEquals(PriceTier.WHOLESALE, item.appliedTier)
        assertEquals(16.0, item.product.precio)
    }

    @Test
    fun testSingleItemCycling_noCustomer_withoutWholesalePrice() {
        val vm = createVentaViewModel()

        val product = createTestProduct(
            id = "p2",
            nombre = "Chetos Sin Mayoreo",
            precio = 15.0,
            costo = 8.0,
            precioMayoreo = 0.0 // no wholesale
        )
        vm.addProductToCart(product, 1.0)

        // Initial: RETAIL (15.0)
        var item = vm.getCartItems().first()
        assertEquals(PriceTier.RETAIL, item.appliedTier)
        assertEquals(15.0, item.product.precio)

        // Cycling attempt: should do nothing because precio_mayoreo <= 0.0
        vm.cyclePriceTierForItem(item)
        item = vm.getCartItems().first()
        assertEquals(PriceTier.RETAIL, item.appliedTier)
        assertEquals(15.0, item.product.precio)

        // Second attempt
        vm.cyclePriceTierForItem(item)
        item = vm.getCartItems().first()
        assertEquals(PriceTier.RETAIL, item.appliedTier)
        assertEquals(15.0, item.product.precio)
    }

    @Test
    fun testSingleItemCycling_wholesaleCustomer() {
        val vm = createVentaViewModel()
        val customer = Customer(
            id = "c3",
            nombre = "Cliente Mayorista",
            siempreMayoreo = true,
            tipoDescuento = CustomerDiscountType.WHOLESALE
        )
        vm.selectCustomer(customer)

        val productWithWholesale = createTestProduct(
            id = "p1",
            nombre = "Producto Con Mayoreo",
            precio = 100.0,
            costo = 50.0,
            precioMayoreo = 75.0
        )
        val productWithoutWholesale = createTestProduct(
            id = "p2",
            nombre = "Producto Sin Mayoreo",
            precio = 50.0,
            costo = 30.0,
            precioMayoreo = 0.0
        )
        vm.addProductToCart(productWithWholesale, 1.0)
        vm.addProductToCart(productWithoutWholesale, 1.0)

        val items = vm.getCartItems()
        // Product 1 starts at WHOLESALE automatically
        assertEquals(PriceTier.WHOLESALE, items[0].appliedTier)
        assertEquals(75.0, items[0].product.precio)

        // Product 2 has no wholesale, starts at RETAIL
        assertEquals(PriceTier.RETAIL, items[1].appliedTier)
        assertEquals(50.0, items[1].product.precio)

        // Cycling Product 1 toggles to RETAIL
        vm.cyclePriceTierForItem(items[0])
        assertEquals(PriceTier.RETAIL, vm.getCartItems()[0].appliedTier)
        assertEquals(100.0, vm.getCartItems()[0].product.precio)

        // Cycling Product 1 again toggles to WHOLESALE
        vm.cyclePriceTierForItem(vm.getCartItems()[0])
        assertEquals(PriceTier.WHOLESALE, vm.getCartItems()[0].appliedTier)
        assertEquals(75.0, vm.getCartItems()[0].product.precio)

        // Cycling Product 2 does nothing
        vm.cyclePriceTierForItem(items[1])
        assertEquals(PriceTier.RETAIL, vm.getCartItems()[1].appliedTier)
        assertEquals(50.0, vm.getCartItems()[1].product.precio)
    }

    // =========================================================================
    // 3. Whole-Ticket Cycling: Regular Customer
    // =========================================================================

    @Test
    fun testWholeTicketCycling_regularCustomer_mixedItems() {
        val vm = createVentaViewModel()
        val prod1 = createTestProduct(id = "p1", nombre = "Mayoreo 1", precio = 100.0, precioMayoreo = 80.0)
        val prod2 = createTestProduct(id = "p2", nombre = "Sin Mayoreo", precio = 50.0, precioMayoreo = 0.0)
        val prod3 = createTestProduct(id = "p3", nombre = "Mayoreo 2", precio = 60.0, precioMayoreo = 45.0)

        vm.addProductToCart(prod1, 1.0)
        vm.addProductToCart(prod2, 1.0)
        vm.addProductToCart(prod3, 1.0)

        // Initial: all RETAIL
        var items = vm.getCartItems()
        assertEquals(PriceTier.RETAIL, items[0].appliedTier)
        assertEquals(PriceTier.RETAIL, items[1].appliedTier)
        assertEquals(PriceTier.RETAIL, items[2].appliedTier)

        // 1st Cycle: all eligible items switch to WHOLESALE, non-eligible remain RETAIL
        vm.cycleTicketPriceTier()
        items = vm.getCartItems()
        assertEquals(PriceTier.WHOLESALE, items[0].appliedTier)
        assertEquals(80.0, items[0].product.precio)
        assertEquals(PriceTier.RETAIL, items[1].appliedTier)
        assertEquals(50.0, items[1].product.precio)
        assertEquals(PriceTier.WHOLESALE, items[2].appliedTier)
        assertEquals(45.0, items[2].product.precio)

        // 2nd Cycle: eligible items switch back to RETAIL
        vm.cycleTicketPriceTier()
        items = vm.getCartItems()
        assertEquals(PriceTier.RETAIL, items[0].appliedTier)
        assertEquals(100.0, items[0].product.precio)
        assertEquals(PriceTier.RETAIL, items[1].appliedTier)
        assertEquals(50.0, items[1].product.precio)
        assertEquals(PriceTier.RETAIL, items[2].appliedTier)
        assertEquals(60.0, items[2].product.precio)

        // 3rd Cycle: eligible items switch back to WHOLESALE
        vm.cycleTicketPriceTier()
        items = vm.getCartItems()
        assertEquals(PriceTier.WHOLESALE, items[0].appliedTier)
        assertEquals(80.0, items[0].product.precio)
        assertEquals(PriceTier.RETAIL, items[1].appliedTier)
        assertEquals(PriceTier.WHOLESALE, items[2].appliedTier)
        assertEquals(45.0, items[2].product.precio)
    }

    @Test
    fun testWholeTicketCycling_regularCustomer_noEligibleItems() {
        val vm = createVentaViewModel()
        val prod1 = createTestProduct(id = "p1", nombre = "Sin Mayoreo 1", precio = 100.0, precioMayoreo = 0.0)
        val prod2 = createTestProduct(id = "p2", nombre = "Sin Mayoreo 2", precio = 50.0, precioMayoreo = -1.0)

        vm.addProductToCart(prod1, 1.0)
        vm.addProductToCart(prod2, 1.0)

        // Cycle ticket: should do nothing since no item has wholesale price
        vm.cycleTicketPriceTier()
        val items = vm.getCartItems()
        assertEquals(PriceTier.RETAIL, items[0].appliedTier)
        assertEquals(100.0, items[0].product.precio)
        assertEquals(PriceTier.RETAIL, items[1].appliedTier)
        assertEquals(50.0, items[1].product.precio)
    }

    // =========================================================================
    // 4. Whole-Ticket Cycling: Custom Discount Customer
    // =========================================================================

    @Test
    fun testWholeTicketCycling_customDiscount_allWithWholesalePrice() {
        val vm = createVentaViewModel()
        val customer = Customer(
            id = "c1",
            nombre = "Cliente Costo + 10%",
            tipoDescuento = CustomerDiscountType.COST_PLUS_PERCENTAGE,
            porcentajeDescuento = 10.0
        )
        vm.selectCustomer(customer)

        val prod1 = createTestProduct(id = "p1", nombre = "Prod 1", precio = 100.0, costo = 50.0, precioMayoreo = 80.0)
        val prod2 = createTestProduct(id = "p2", nombre = "Prod 2", precio = 60.0, costo = 30.0, precioMayoreo = 40.0)
        vm.addProductToCart(prod1, 1.0)
        vm.addProductToCart(prod2, 1.0)

        // Initial: both in CUSTOMER_DISCOUNT
        var items = vm.getCartItems()
        assertEquals(PriceTier.CUSTOMER_DISCOUNT, items[0].appliedTier)
        assertEquals(55.0, items[0].product.precio)
        assertEquals(PriceTier.CUSTOMER_DISCOUNT, items[1].appliedTier)
        assertEquals(33.0, items[1].product.precio)

        // Cycle 1: both transition to WHOLESALE
        vm.cycleTicketPriceTier()
        items = vm.getCartItems()
        assertEquals(PriceTier.WHOLESALE, items[0].appliedTier)
        assertEquals(80.0, items[0].product.precio)
        assertEquals(PriceTier.WHOLESALE, items[1].appliedTier)
        assertEquals(40.0, items[1].product.precio)

        // Cycle 2: both transition to RETAIL
        vm.cycleTicketPriceTier()
        items = vm.getCartItems()
        assertEquals(PriceTier.RETAIL, items[0].appliedTier)
        assertEquals(100.0, items[0].product.precio)
        assertEquals(PriceTier.RETAIL, items[1].appliedTier)
        assertEquals(60.0, items[1].product.precio)

        // Cycle 3: both transition back to CUSTOMER_DISCOUNT
        vm.cycleTicketPriceTier()
        items = vm.getCartItems()
        assertEquals(PriceTier.CUSTOMER_DISCOUNT, items[0].appliedTier)
        assertEquals(55.0, items[0].product.precio)
        assertEquals(PriceTier.CUSTOMER_DISCOUNT, items[1].appliedTier)
        assertEquals(33.0, items[1].product.precio)

        // Cycle 4: repeats to WHOLESALE
        vm.cycleTicketPriceTier()
        items = vm.getCartItems()
        assertEquals(PriceTier.WHOLESALE, items[0].appliedTier)
        assertEquals(80.0, items[0].product.precio)
        assertEquals(PriceTier.WHOLESALE, items[1].appliedTier)
        assertEquals(40.0, items[1].product.precio)
    }

    @Test
    fun testWholeTicketCycling_customDiscount_mixedItems() {
        val vm = createVentaViewModel()
        val customer = Customer(
            id = "c1",
            nombre = "Cliente Costo + 10%",
            tipoDescuento = CustomerDiscountType.COST_PLUS_PERCENTAGE,
            porcentajeDescuento = 10.0
        )
        vm.selectCustomer(customer)

        val prod1 = createTestProduct(id = "p1", nombre = "Prod Con Mayoreo", precio = 100.0, costo = 50.0, precioMayoreo = 80.0)
        val prod2 = createTestProduct(id = "p2", nombre = "Prod Sin Mayoreo", precio = 50.0, costo = 20.0, precioMayoreo = 0.0)
        vm.addProductToCart(prod1, 1.0)
        vm.addProductToCart(prod2, 1.0)

        // Initial: both CUSTOMER_DISCOUNT
        var items = vm.getCartItems()
        assertEquals(PriceTier.CUSTOMER_DISCOUNT, items[0].appliedTier)
        assertEquals(55.0, items[0].product.precio)
        assertEquals(PriceTier.CUSTOMER_DISCOUNT, items[1].appliedTier)
        assertEquals(22.0, items[1].product.precio)

        // Cycle 1: prod1 -> WHOLESALE (80.0); prod2 -> RETAIL (50.0)
        vm.cycleTicketPriceTier()
        items = vm.getCartItems()
        assertEquals(PriceTier.WHOLESALE, items[0].appliedTier)
        assertEquals(80.0, items[0].product.precio)
        assertEquals(PriceTier.RETAIL, items[1].appliedTier)
        assertEquals(50.0, items[1].product.precio)

        // Cycle 2: both in RETAIL
        vm.cycleTicketPriceTier()
        items = vm.getCartItems()
        assertEquals(PriceTier.RETAIL, items[0].appliedTier)
        assertEquals(100.0, items[0].product.precio)
        assertEquals(PriceTier.RETAIL, items[1].appliedTier)
        assertEquals(50.0, items[1].product.precio)

        // Cycle 3: both transition back to CUSTOMER_DISCOUNT
        vm.cycleTicketPriceTier()
        items = vm.getCartItems()
        assertEquals(PriceTier.CUSTOMER_DISCOUNT, items[0].appliedTier)
        assertEquals(55.0, items[0].product.precio)
        assertEquals(PriceTier.CUSTOMER_DISCOUNT, items[1].appliedTier)
        assertEquals(22.0, items[1].product.precio)

        // Cycle 4: prod1 -> WHOLESALE, prod2 -> RETAIL
        vm.cycleTicketPriceTier()
        items = vm.getCartItems()
        assertEquals(PriceTier.WHOLESALE, items[0].appliedTier)
        assertEquals(PriceTier.RETAIL, items[1].appliedTier)
    }

    // =========================================================================
    // 5. Adversarial Challenge / Failure Mode:
    // Whole-Ticket Cycling when ALL products have precio_mayoreo <= 0.0
    // =========================================================================

    /**
     * ADVERSARIAL DISCOVERY & REMEDIATION:
     * When all products in the ticket have precio_mayoreo <= 0.0,
     * cycleTicketPriceTier() transitions CUSTOMER_DISCOUNT -> RETAIL on Cycle 1,
     * and on Cycle 2 cleanly transitions back to CUSTOMER_DISCOUNT.
     * This test validates that the ticket cycles cleanly without getting stuck at RETAIL.
     */
    @Test
    fun testAdversarial_wholeTicketCycling_allProductsWithoutWholesale_cyclesCleanly() {
        val vm = createVentaViewModel()
        val customer = Customer(
            id = "c1",
            nombre = "Cliente Costo + 10%",
            tipoDescuento = CustomerDiscountType.COST_PLUS_PERCENTAGE,
            porcentajeDescuento = 10.0
        )
        vm.selectCustomer(customer)

        val prod1 = createTestProduct(id = "p1", nombre = "Prod Sin Mayoreo 1", precio = 100.0, costo = 50.0, precioMayoreo = 0.0)
        val prod2 = createTestProduct(id = "p2", nombre = "Prod Sin Mayoreo 2", precio = 40.0, costo = 20.0, precioMayoreo = 0.0)
        vm.addProductToCart(prod1, 1.0)
        vm.addProductToCart(prod2, 1.0)

        // Initial: CUSTOMER_DISCOUNT
        var items = vm.getCartItems()
        assertEquals(PriceTier.CUSTOMER_DISCOUNT, items[0].appliedTier)
        assertEquals(55.0, items[0].product.precio)
        assertEquals(PriceTier.CUSTOMER_DISCOUNT, items[1].appliedTier)
        assertEquals(22.0, items[1].product.precio)

        // Cycle 1: Transitions to RETAIL
        vm.cycleTicketPriceTier()
        items = vm.getCartItems()
        assertEquals(PriceTier.RETAIL, items[0].appliedTier)
        assertEquals(100.0, items[0].product.precio)
        assertEquals(PriceTier.RETAIL, items[1].appliedTier)
        assertEquals(40.0, items[1].product.precio)

        // Cycle 2: Returns cleanly to CUSTOMER_DISCOUNT
        vm.cycleTicketPriceTier()
        items = vm.getCartItems()
        assertEquals(PriceTier.CUSTOMER_DISCOUNT, items[0].appliedTier)
        assertEquals(55.0, items[0].product.precio)
        assertEquals(PriceTier.CUSTOMER_DISCOUNT, items[1].appliedTier)
        assertEquals(22.0, items[1].product.precio)

        // Cycle 3: Cycles back to RETAIL
        vm.cycleTicketPriceTier()
        items = vm.getCartItems()
        assertEquals(PriceTier.RETAIL, items[0].appliedTier)
        assertEquals(100.0, items[0].product.precio)
        assertEquals(PriceTier.RETAIL, items[1].appliedTier)
        assertEquals(40.0, items[1].product.precio)

        // Cycle 4: Cycles back to CUSTOMER_DISCOUNT (never latches at RETAIL)
        vm.cycleTicketPriceTier()
        items = vm.getCartItems()
        assertEquals(PriceTier.CUSTOMER_DISCOUNT, items[0].appliedTier)
        assertEquals(55.0, items[0].product.precio)
        assertEquals(PriceTier.CUSTOMER_DISCOUNT, items[1].appliedTier)
        assertEquals(22.0, items[1].product.precio)

        // Cycle 5: Cycles back to RETAIL cleanly
        vm.cycleTicketPriceTier()
        items = vm.getCartItems()
        assertEquals(PriceTier.RETAIL, items[0].appliedTier)
        assertEquals(100.0, items[0].product.precio)
        assertEquals(PriceTier.RETAIL, items[1].appliedTier)
        assertEquals(40.0, items[1].product.precio)
    }

    @Test
    fun testAdversarial_consecutiveWholeTicketCycling_fiveConsecutiveCycles_allConfigurations() {
        val customerCustom = Customer(
            id = "c1",
            nombre = "Cliente Costo + 20%",
            tipoDescuento = CustomerDiscountType.COST_PLUS_PERCENTAGE,
            porcentajeDescuento = 20.0
        )

        // 1. All items with wholesale (3-state cycle: CUSTOMER_DISCOUNT -> WHOLESALE -> RETAIL)
        val vmAllWholesale = createVentaViewModel()
        vmAllWholesale.selectCustomer(customerCustom)
        vmAllWholesale.addProductToCart(createTestProduct("w1", "W1", precio = 100.0, costo = 50.0, precioMayoreo = 80.0), 1.0)
        vmAllWholesale.addProductToCart(createTestProduct("w2", "W2", precio = 50.0, costo = 25.0, precioMayoreo = 40.0), 1.0)

        val expectedTiersAllWholesale = listOf(
            PriceTier.CUSTOMER_DISCOUNT, // initial
            PriceTier.WHOLESALE,         // cycle 1
            PriceTier.RETAIL,            // cycle 2
            PriceTier.CUSTOMER_DISCOUNT, // cycle 3
            PriceTier.WHOLESALE,         // cycle 4
            PriceTier.RETAIL             // cycle 5
        )
        for (cycle in 0..5) {
            if (cycle > 0) vmAllWholesale.cycleTicketPriceTier()
            val items = vmAllWholesale.getCartItems()
            val expectedTier = expectedTiersAllWholesale[cycle]
            assertEquals(expectedTier, items[0].appliedTier, "AllWholesale Cycle $cycle item 0 tier mismatch")
            assertEquals(expectedTier, items[1].appliedTier, "AllWholesale Cycle $cycle item 1 tier mismatch")
        }

        // 2. Mixed items (3-state cycle: CUSTOMER_DISCOUNT -> [WHOLESALE, RETAIL] -> RETAIL)
        val vmMixed = createVentaViewModel()
        vmMixed.selectCustomer(customerCustom)
        vmMixed.addProductToCart(createTestProduct("m1", "M1", precio = 100.0, costo = 50.0, precioMayoreo = 80.0), 1.0)
        vmMixed.addProductToCart(createTestProduct("m2", "M2", precio = 50.0, costo = 25.0, precioMayoreo = 0.0), 1.0)

        val expectedTiersMixed = listOf(
            Pair(PriceTier.CUSTOMER_DISCOUNT, PriceTier.CUSTOMER_DISCOUNT), // initial
            Pair(PriceTier.WHOLESALE, PriceTier.RETAIL),                    // cycle 1
            Pair(PriceTier.RETAIL, PriceTier.RETAIL),                       // cycle 2
            Pair(PriceTier.CUSTOMER_DISCOUNT, PriceTier.CUSTOMER_DISCOUNT), // cycle 3
            Pair(PriceTier.WHOLESALE, PriceTier.RETAIL),                    // cycle 4
            Pair(PriceTier.RETAIL, PriceTier.RETAIL)                        // cycle 5
        )
        for (cycle in 0..5) {
            if (cycle > 0) vmMixed.cycleTicketPriceTier()
            val items = vmMixed.getCartItems()
            val (exp0, exp1) = expectedTiersMixed[cycle]
            assertEquals(exp0, items[0].appliedTier, "Mixed Cycle $cycle item 0 tier mismatch")
            assertEquals(exp1, items[1].appliedTier, "Mixed Cycle $cycle item 1 tier mismatch")
        }

        // 3. Customer without custom discount (2-state toggle: RETAIL -> WHOLESALE -> RETAIL)
        val vmRegular = createVentaViewModel()
        vmRegular.addProductToCart(createTestProduct("r1", "R1", precio = 100.0, precioMayoreo = 80.0), 1.0)
        vmRegular.addProductToCart(createTestProduct("r2", "R2", precio = 50.0, precioMayoreo = 0.0), 1.0)

        val expectedTiersRegular = listOf(
            Pair(PriceTier.RETAIL, PriceTier.RETAIL),       // initial
            Pair(PriceTier.WHOLESALE, PriceTier.RETAIL),    // cycle 1
            Pair(PriceTier.RETAIL, PriceTier.RETAIL),       // cycle 2
            Pair(PriceTier.WHOLESALE, PriceTier.RETAIL),    // cycle 3
            Pair(PriceTier.RETAIL, PriceTier.RETAIL),       // cycle 4
            Pair(PriceTier.WHOLESALE, PriceTier.RETAIL)     // cycle 5
        )
        for (cycle in 0..5) {
            if (cycle > 0) vmRegular.cycleTicketPriceTier()
            val items = vmRegular.getCartItems()
            val (exp0, exp1) = expectedTiersRegular[cycle]
            assertEquals(exp0, items[0].appliedTier, "Regular Cycle $cycle item 0 tier mismatch")
            assertEquals(exp1, items[1].appliedTier, "Regular Cycle $cycle item 1 tier mismatch")
        }
    }

    // =========================================================================
    // 6. Resynchronization: Desynchronized items brought into lockstep
    // =========================================================================

    @Test
    fun testWholeTicketCycling_resynchronizesDesynchronizedTiers() {
        val vm = createVentaViewModel()
        val customer = Customer(
            id = "c1",
            nombre = "Cliente Costo + 10%",
            tipoDescuento = CustomerDiscountType.COST_PLUS_PERCENTAGE,
            porcentajeDescuento = 10.0
        )
        vm.selectCustomer(customer)

        val prod1 = createTestProduct(id = "p1", nombre = "Prod 1", precio = 100.0, costo = 50.0, precioMayoreo = 80.0)
        val prod2 = createTestProduct(id = "p2", nombre = "Prod 2", precio = 60.0, costo = 30.0, precioMayoreo = 40.0)
        vm.addProductToCart(prod1, 1.0)
        vm.addProductToCart(prod2, 1.0)

        // User manually cycles prod1 to WHOLESALE, leaving prod2 at CUSTOMER_DISCOUNT
        vm.cyclePriceTierForItem(vm.getCartItems()[0])
        assertEquals(PriceTier.WHOLESALE, vm.getCartItems()[0].appliedTier)
        assertEquals(PriceTier.CUSTOMER_DISCOUNT, vm.getCartItems()[1].appliedTier)

        // Now whole-ticket cycle: neither allCustom nor allWholesaleOrRetail is true -> falls into else: CUSTOMER_DISCOUNT
        vm.cycleTicketPriceTier()
        val resynced = vm.getCartItems()
        assertEquals(PriceTier.CUSTOMER_DISCOUNT, resynced[0].appliedTier)
        assertEquals(PriceTier.CUSTOMER_DISCOUNT, resynced[1].appliedTier)
        assertEquals(55.0, resynced[0].product.precio)
        assertEquals(33.0, resynced[1].product.precio)
    }
}
