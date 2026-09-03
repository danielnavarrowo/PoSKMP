package com.dnavarro.poskmp.data.sync

import com.dnavarro.poskmp.data.SettingsRepository
import com.dnavarro.poskmp.data.source.remote.SupabaseRemoteDataSource
import com.dnavarro.poskmp.data.source.remote.dto.CashierDto
import com.dnavarro.poskmp.data.source.remote.dto.CustomerDto
import com.dnavarro.poskmp.data.source.remote.dto.CustomerPaymentDto
import com.dnavarro.poskmp.data.source.remote.dto.DeletedRecordDto
import com.dnavarro.poskmp.data.source.remote.dto.ProductDto
import com.dnavarro.poskmp.data.source.remote.dto.SaleDto
import com.dnavarro.poskmp.data.source.remote.dto.SaleItemDto
import com.dnavarro.poskmp.data.source.remote.dto.StoreSettingsDto
import com.dnavarro.poskmp.db.AppDatabase
import com.dnavarro.poskmp.util.currentTimeMillis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

enum class SyncStateEnum {
    IDLE,
    SYNCING,
    SUCCESS,
    ERROR
}

data class SyncReport(
    val success: Boolean,
    val message: String = "",
    val itemsPushed: Int = 0,
    val itemsPulled: Int = 0
)

interface SyncRepository {
    val syncState: StateFlow<SyncStateEnum>
    val lastError: StateFlow<String?>

    suspend fun testConnection(url: String, key: String): Result<Boolean>
    suspend fun syncAll(forceFullSync: Boolean = false, isManual: Boolean = false): Result<SyncReport>
}

class SyncRepositoryImpl(
    private val database: AppDatabase,
    private val remoteDataSource: SupabaseRemoteDataSource,
    private val settingsRepository: SettingsRepository
) : SyncRepository {

    private val _syncState = MutableStateFlow(SyncStateEnum.IDLE)
    override val syncState: StateFlow<SyncStateEnum> = _syncState.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    override val lastError: StateFlow<String?> = _lastError.asStateFlow()

    override suspend fun testConnection(url: String, key: String): Result<Boolean> = withContext(Dispatchers.IO) {
        remoteDataSource.testConnection(url, key)
    }

    override suspend fun syncAll(forceFullSync: Boolean, isManual: Boolean): Result<SyncReport> = withContext(Dispatchers.IO) {
        val autoSync = settingsRepository.autoSyncEnabledFlow.first()
        if (!isManual && !autoSync) {
            // Sincronización automática desactivada por el usuario en Ajustes
            return@withContext Result.success(SyncReport(success = true, message = "Auto sync disabled"))
        }

        val url = settingsRepository.supabaseUrlFlow.first()
        val key = settingsRepository.supabaseKeyFlow.first()

        if (url.isBlank() || key.isBlank()) {
            if (isManual) {
                val msg = "Credenciales de Supabase no configuradas"
                _lastError.value = msg
                _syncState.value = SyncStateEnum.ERROR
                return@withContext Result.failure(IllegalStateException(msg))
            } else {
                return@withContext Result.success(SyncReport(success = true, message = "Not configured"))
            }
        }

        _syncState.value = SyncStateEnum.SYNCING
        _lastError.value = null

        try {
            val queries = database.appDatabaseQueries
            var totalPushed = 0
            var totalPulled = 0

            // ----------------------------------------------------
            // 1. FASE PUSH (Subir datos locales pendientes)
            // ----------------------------------------------------

            // A) Productos
            val unsyncedProducts = queries.selectUnsyncedProducts().executeAsList()
            if (unsyncedProducts.isNotEmpty()) {
                val productDtos = unsyncedProducts.map { p ->
                    ProductDto(
                        id = p.id,
                        codigos = p.codigos,
                        nombre = p.nombre,
                        precio = p.precio,
                        costo = p.costo,
                        categoria = p.categoria,
                        activo = p.activo == 1L,
                        porPeso = p.por_peso == 1L,
                        precioMayoreo = p.precio_mayoreo,
                        precioDelivery = p.precio_delivery,
                        esFavorito = p.es_favorito == 1L,
                        piezas = p.piezas,
                        updatedAt = p.updated_at
                    )
                }
                val pushResult = remoteDataSource.pushProducts(url, key, productDtos)
                if (pushResult.isFailure) {
                    throw pushResult.exceptionOrNull() ?: Exception("Error al subir productos")
                }
                queries.transaction {
                    for ((id) in unsyncedProducts) {
                        queries.updateProductSyncState(sync_state = "SYNCED", id = id)
                    }
                }
                totalPushed += unsyncedProducts.size
            }

            // B) Clientes
            val unsyncedCustomers = queries.selectUnsyncedCustomers().executeAsList()
            if (unsyncedCustomers.isNotEmpty()) {
                val customerDtos = unsyncedCustomers.map { c ->
                    CustomerDto(
                        id = c.id,
                        nombre = c.nombre,
                        telefono = c.telefono,
                        direccion = c.direccion,
                        notas = c.notas,
                        limiteCredito = c.limite_credito,
                        siempreMayoreo = c.siempre_mayoreo == 1L,
                        activo = c.activo == 1L,
                        createdAt = c.created_at,
                        updatedAt = c.updated_at
                    )
                }
                val pushResult = remoteDataSource.pushCustomers(url, key, customerDtos)
                if (pushResult.isFailure) {
                    throw pushResult.exceptionOrNull() ?: Exception("Error al subir clientes")
                }
                queries.transaction {
                    for ((id) in unsyncedCustomers) {
                        queries.updateCustomerSyncState(sync_state = "SYNCED", id = id)
                    }
                }
                totalPushed += unsyncedCustomers.size
            }

            // C) Abonos de Clientes
            val unsyncedPayments = queries.selectUnsyncedCustomerPayments().executeAsList()
            if (unsyncedPayments.isNotEmpty()) {
                val paymentDtos = unsyncedPayments.map { cp ->
                    CustomerPaymentDto(
                        id = cp.id,
                        customerId = cp.customer_id,
                        monto = cp.monto,
                        metodoPago = cp.metodo_pago,
                        notas = cp.notas,
                        createdAt = cp.created_at
                    )
                }
                val pushResult = remoteDataSource.pushCustomerPayments(url, key, paymentDtos)
                if (pushResult.isFailure) {
                    throw pushResult.exceptionOrNull() ?: Exception("Error al subir abonos")
                }
                queries.transaction {
                    for ((id) in unsyncedPayments) {
                        queries.updateCustomerPaymentSyncState(sync_state = "SYNCED", id = id)
                    }
                }
                totalPushed += unsyncedPayments.size
            }

            // D) Cajeros
            val unsyncedCashiers = queries.selectUnsyncedCashiers().executeAsList()
            if (unsyncedCashiers.isNotEmpty()) {
                val cashierDtos = unsyncedCashiers.map { c ->
                    CashierDto(
                        id = c.id,
                        nombre = c.nombre,
                        pin = c.pin,
                        activo = c.activo == 1L,
                        createdAt = c.created_at,
                        updatedAt = c.updated_at
                    )
                }
                val pushCashierResult = remoteDataSource.pushCashiers(url, key, cashierDtos)
                if (pushCashierResult.isFailure) {
                    throw pushCashierResult.exceptionOrNull() ?: Exception("Error al subir cajeros")
                }
                queries.transaction {
                    for ((id) in unsyncedCashiers) {
                        queries.updateCashierSyncState(sync_state = "SYNCED", id = id)
                    }
                }
                totalPushed += unsyncedCashiers.size
            }

            // E) Ventas y Partidas de Ventas
            val unsyncedSales = queries.selectUnsyncedSales().executeAsList()
            if (unsyncedSales.isNotEmpty()) {
                val saleDtos = unsyncedSales.map { s ->
                    SaleDto(
                        id = s.id,
                        folio = s.folio,
                        total = s.total,
                        totalOriginal = s.total_original,
                        totalCosto = s.total_costo,
                        ganancia = s.ganancia,
                        pagoCon = s.pago_con,
                        cambio = s.cambio,
                        metodoPago = s.metodo_pago,
                        totalItems = s.total_items,
                        customerId = s.customer_id,
                        createdAt = s.created_at,
                        cashierName = s.cashier_name,
                        estado = s.estado
                    )
                }
                val pushSaleResult = remoteDataSource.pushSales(url, key, saleDtos)
                if (pushSaleResult.isFailure) {
                    throw pushSaleResult.exceptionOrNull() ?: Exception("Error al subir ventas")
                }

                // Subir las partidas asociadas
                val allSaleItems = mutableListOf<SaleItemDto>()
                for ((id) in unsyncedSales) {
                    val items = queries.selectItemsBySaleId(id).executeAsList()
                    allSaleItems.addAll(items.map { item ->
                        SaleItemDto(
                            id = item.id,
                            saleId = item.sale_id,
                            productId = item.product_id,
                            productNombre = item.product_nombre,
                            cantidad = item.cantidad,
                            precioUnitario = item.precio_unitario,
                            costoUnitario = item.costo_unitario,
                            subtotal = item.subtotal,
                            ganancia = item.ganancia,
                            esMayoreo = item.es_mayoreo == 1L,
                            esDelivery = item.es_delivery == 1L,
                            createdAt = item.created_at
                        )
                    })
                }

                if (allSaleItems.isNotEmpty()) {
                    val pushItemsResult = remoteDataSource.pushSaleItems(url, key, allSaleItems)
                    if (pushItemsResult.isFailure) {
                        throw pushItemsResult.exceptionOrNull() ?: Exception("Error al subir partidas de venta")
                    }
                }

                queries.transaction {
                    for ((id) in unsyncedSales) {
                        queries.updateSaleSyncState(sync_state = "SYNCED", id = id)
                    }
                }
                totalPushed += unsyncedSales.size
            }

            // F) Ajustes de Negocio (Márgenes, Redondeo, Datos de Tienda, Políticas)
            val localSettingsUpdatedAt = settingsRepository.businessSettingsUpdatedAtFlow.first()
            val lastSyncForPush = settingsRepository.lastSyncTimestampFlow.first()
            if (localSettingsUpdatedAt > lastSyncForPush || localSettingsUpdatedAt > 0L) {
                val receiptSettings = settingsRepository.receiptSettingsFlow.first()
                val storeSettingsDto = StoreSettingsDto(
                    id = "default",
                    storeName = receiptSettings.storeName,
                    storeAddress = receiptSettings.storeAddress,
                    storePhone = receiptSettings.storePhone,
                    receiptFooter = receiptSettings.footerMessage,
                    defaultRetailMargin = settingsRepository.defaultRetailMarginFlow.first(),
                    defaultWholesaleMargin = settingsRepository.defaultWholesaleMarginFlow.first(),
                    defaultDeliveryMargin = settingsRepository.defaultDeliveryMarginFlow.first(),
                    isRoundingEnabled = settingsRepository.isRoundingEnabledFlow.first(),
                    roundRetailPrice = settingsRepository.roundRetailPriceFlow.first(),
                    roundWholesalePrice = settingsRepository.roundWholesalePriceFlow.first(),
                    roundDeliveryPrice = settingsRepository.roundDeliveryPriceFlow.first(),
                    roundTicketTotal = settingsRepository.roundTicketTotalFlow.first(),
                    disallowCardPaymentOnWholesale = settingsRepository.disallowCardPaymentOnWholesaleFlow.first(),
                    updatedAt = if (localSettingsUpdatedAt > 0L) localSettingsUpdatedAt else currentTimeMillis()
                )
                val pushSettingsResult = remoteDataSource.pushStoreSettings(url, key, storeSettingsDto)
                if (pushSettingsResult.isFailure) {
                    throw pushSettingsResult.exceptionOrNull() ?: Exception("Error al subir ajustes de negocio")
                }
            }

            // G) Eliminaciones Locales (Tombstones)
            val pendingDeletes = queries.selectDeletedSyncRecords().executeAsList()
            if (pendingDeletes.isNotEmpty()) {
                val deleteDtos = pendingDeletes.map { DeletedRecordDto(it.id, it.entity_type, it.deleted_at) }
                for ((id, entity_type) in pendingDeletes) {
                    when (entity_type) {
                        "PRODUCT" -> remoteDataSource.deleteRemoteProduct(url, key, id)
                        "CUSTOMER" -> remoteDataSource.deleteRemoteCustomer(url, key, id)
                        "PAYMENT" -> remoteDataSource.deleteRemoteCustomerPayment(url, key, id)
                    }
                }
                val pushDeletesResult = remoteDataSource.pushDeletedRecords(url, key, deleteDtos)
                if (pushDeletesResult.isFailure) {
                    throw pushDeletesResult.exceptionOrNull() ?: Exception("Error al registrar eliminaciones remotas")
                }
                queries.transaction {
                    for ((id) in pendingDeletes) {
                        queries.deleteDeletedSyncRecord(id)
                    }
                }
                totalPushed += pendingDeletes.size
            }

            // ----------------------------------------------------
            // 2. FASE PULL (Descargar novedades remotas)
            // ----------------------------------------------------
            val lastSync = if (forceFullSync) 0L else settingsRepository.lastSyncTimestampFlow.first()

            // A) Productos Remotos
            val pulledProductsResult = remoteDataSource.pullProducts(url, key, lastSync)
            if (pulledProductsResult.isFailure) {
                throw pulledProductsResult.exceptionOrNull() ?: Exception("Error al descargar productos")
            }
            val remoteProducts = pulledProductsResult.getOrDefault(emptyList())
            queries.transaction {
                for ((id, codigos, nombre, precio, costo, categoria, activo, porPeso, precioMayoreo, precioDelivery, esFavorito, piezas, updatedAt) in remoteProducts) {
                    val local = queries.selectProductById(id).executeAsOneOrNull()
                    if (local == null || updatedAt >= local.updated_at || local.sync_state == "SYNCED") {
                        queries.upsertSyncedProduct(
                            id = id,
                            codigos = codigos,
                            nombre = nombre,
                            precio = precio,
                            costo = costo,
                            categoria = categoria ?: "Sin categoría",
                            activo = if (activo) 1L else 0L,
                            por_peso = if (porPeso) 1L else 0L,
                            precio_mayoreo = precioMayoreo,
                            precio_delivery = precioDelivery,
                            es_favorito = if (esFavorito) 1L else 0L,
                            piezas = piezas,
                            updated_at = updatedAt
                        )
                    }
                }
            }
            totalPulled += remoteProducts.size

            // B) Clientes Remotos
            val pulledCustomersResult = remoteDataSource.pullCustomers(url, key, lastSync)
            if (pulledCustomersResult.isFailure) {
                throw pulledCustomersResult.exceptionOrNull() ?: Exception("Error al descargar clientes")
            }
            val remoteCustomers = pulledCustomersResult.getOrDefault(emptyList())
            queries.transaction {
                for ((id, nombre, telefono, direccion, notas, limiteCredito, siempreMayoreo, activo, createdAt, updatedAt) in remoteCustomers) {
                    val local = queries.selectCustomerById(id).executeAsOneOrNull()
                    if (local == null || updatedAt >= local.updated_at || local.sync_state == "SYNCED") {
                        queries.upsertSyncedCustomer(
                            id = id,
                            nombre = nombre,
                            telefono = telefono,
                            direccion = direccion,
                            notas = notas,
                            limite_credito = limiteCredito,
                            siempre_mayoreo = if (siempreMayoreo) 1L else 0L,
                            activo = if (activo) 1L else 0L,
                            created_at = createdAt,
                            updated_at = updatedAt
                        )
                    }
                }
            }
            totalPulled += remoteCustomers.size

            // C) Abonos Remotos
            val pulledPaymentsResult = remoteDataSource.pullCustomerPayments(url, key, lastSync)
            if (pulledPaymentsResult.isFailure) {
                throw pulledPaymentsResult.exceptionOrNull() ?: Exception("Error al descargar abonos")
            }
            val remotePayments = pulledPaymentsResult.getOrDefault(emptyList())
            queries.transaction {
                for ((id, customerId, monto, metodoPago, notas, createdAt) in remotePayments) {
                    queries.upsertSyncedCustomerPayment(
                        id = id,
                        customer_id = customerId,
                        monto = monto,
                        metodo_pago = metodoPago,
                        notas = notas,
                        created_at = createdAt
                    )
                }
            }
            totalPulled += remotePayments.size

            // D) Cajeros Remotos
            val pulledCashiersResult = remoteDataSource.pullCashiers(url, key, lastSync)
            if (pulledCashiersResult.isFailure) {
                throw pulledCashiersResult.exceptionOrNull() ?: Exception("Error al descargar cajeros")
            }
            val remoteCashiers = pulledCashiersResult.getOrDefault(emptyList())
            queries.transaction {
                for ((id, nombre, pin, activo, createdAt, updatedAt) in remoteCashiers) {
                    val local = queries.selectCashierById(id).executeAsOneOrNull()
                    if (local == null || updatedAt >= local.updated_at || local.sync_state == "SYNCED") {
                        queries.upsertSyncedCashier(
                            id = id,
                            nombre = nombre,
                            pin = pin,
                            activo = if (activo) 1L else 0L,
                            created_at = createdAt,
                            updated_at = updatedAt
                        )
                    }
                }
            }
            totalPulled += remoteCashiers.size

            // E) Ventas y Partidas Remotas
            val pulledSalesResult = remoteDataSource.pullSales(url, key, lastSync)
            if (pulledSalesResult.isFailure) {
                throw pulledSalesResult.exceptionOrNull() ?: Exception("Error al descargar ventas")
            }
            val remoteSales = pulledSalesResult.getOrDefault(emptyList())
            queries.transaction {
                for ((id, folio, total, totalOriginal, totalCosto, ganancia, pagoCon, cambio, metodoPago, totalItems, customerId, createdAt, cashierName, estado) in remoteSales) {
                    queries.upsertSyncedSale(
                        id = id,
                        folio = folio,
                        total = total,
                        total_original = totalOriginal,
                        total_costo = totalCosto,
                        ganancia = ganancia,
                        pago_con = pagoCon,
                        cambio = cambio,
                        metodo_pago = metodoPago,
                        total_items = totalItems,
                        customer_id = customerId,
                        created_at = createdAt,
                        shift_id = null,
                        cashier_id = null,
                        cashier_name = cashierName,
                        estado = estado
                    )
                }
            }
            totalPulled += remoteSales.size

            val pulledItemsResult = remoteDataSource.pullSaleItems(url, key, lastSync)
            if (pulledItemsResult.isFailure) {
                throw pulledItemsResult.exceptionOrNull() ?: Exception("Error al descargar partidas de venta")
            }
            val remoteItems = pulledItemsResult.getOrDefault(emptyList())
            queries.transaction {
                for ((id, saleId, productId, productNombre, cantidad, precioUnitario, costoUnitario, subtotal, ganancia, esMayoreo, esDelivery, createdAt) in remoteItems) {
                    queries.upsertSyncedSaleItem(
                        id = id,
                        sale_id = saleId,
                        product_id = productId,
                        product_nombre = productNombre,
                        cantidad = cantidad,
                        precio_unitario = precioUnitario,
                        costo_unitario = costoUnitario,
                        subtotal = subtotal,
                        ganancia = ganancia,
                        es_mayoreo = if (esMayoreo) 1L else 0L,
                        es_delivery = if (esDelivery) 1L else 0L,
                        created_at = createdAt
                    )
                }
            }

            // F) Ajustes de Negocio Remotos
            val pulledSettingsResult = remoteDataSource.pullStoreSettings(url, key)
            if (pulledSettingsResult.isSuccess) {
                val remoteSettings = pulledSettingsResult.getOrNull()
                if (remoteSettings != null) {
                    val localSettingsUpdatedAt = settingsRepository.businessSettingsUpdatedAtFlow.first()
                    if (remoteSettings.updatedAt > localSettingsUpdatedAt) {
                        settingsRepository.setBusinessSettings(
                            defaultRetailMargin = remoteSettings.defaultRetailMargin,
                            defaultWholesaleMargin = remoteSettings.defaultWholesaleMargin,
                            defaultDeliveryMargin = remoteSettings.defaultDeliveryMargin,
                            isRoundingEnabled = remoteSettings.isRoundingEnabled,
                            roundRetailPrice = remoteSettings.roundRetailPrice,
                            roundWholesalePrice = remoteSettings.roundWholesalePrice,
                            roundDeliveryPrice = remoteSettings.roundDeliveryPrice,
                            roundTicketTotal = remoteSettings.roundTicketTotal,
                            disallowCardPaymentOnWholesale = remoteSettings.disallowCardPaymentOnWholesale,
                            storeName = remoteSettings.storeName,
                            storeAddress = remoteSettings.storeAddress,
                            storePhone = remoteSettings.storePhone,
                            receiptFooter = remoteSettings.receiptFooter,
                            updatedAt = remoteSettings.updatedAt
                        )
                        totalPulled++
                    }
                }
            }

            // G) Eliminaciones Remotas (Tombstones)
            val pulledDeletesResult = remoteDataSource.pullDeletedRecords(url, key, lastSync)
            if (pulledDeletesResult.isSuccess) {
                val remoteDeletes = pulledDeletesResult.getOrDefault(emptyList())
                if (remoteDeletes.isNotEmpty()) {
                    queries.transaction {
                        for ((id, entityType) in remoteDeletes) {
                            when (entityType) {
                                "PRODUCT" -> queries.deleteProductHard(id)
                                "CUSTOMER" -> queries.deleteCustomerHard(id)
                                "PAYMENT" -> queries.deleteCustomerPayment(id)
                            }
                        }
                    }
                    totalPulled += remoteDeletes.size
                }
            }

            // Guardar marca de tiempo de sincronización exitosa
            val now = currentTimeMillis()
            settingsRepository.setLastSyncTimestamp(now)

            _syncState.value = SyncStateEnum.SUCCESS
            val report = SyncReport(
                success = true,
                message = "Sincronización exitosa",
                itemsPushed = totalPushed,
                itemsPulled = totalPulled
            )
            Result.success(report)
        } catch (e: Exception) {
            val msg = e.message ?: "Error desconocido durante la sincronización"
            _lastError.value = msg
            _syncState.value = SyncStateEnum.ERROR
            Result.failure(e)
        }
    }
}
