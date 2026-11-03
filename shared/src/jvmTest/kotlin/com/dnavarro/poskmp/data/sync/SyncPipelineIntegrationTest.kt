package com.dnavarro.poskmp.data.sync

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.dnavarro.poskmp.data.source.remote.dto.CashierDto
import com.dnavarro.poskmp.data.source.remote.dto.ProductDto
import com.dnavarro.poskmp.data.source.remote.dto.SaleDto
import com.dnavarro.poskmp.data.source.remote.dto.SaleItemDto
import com.dnavarro.poskmp.data.source.remote.dto.ShiftDto
import com.dnavarro.poskmp.db.AppDatabase
import com.dnavarro.poskmp.db.DatabaseMigrator
import com.dnavarro.poskmp.domain.model.DeviceRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SyncPipelineIntegrationTest {

    private lateinit var driver: JdbcSqliteDriver
    private lateinit var database: AppDatabase
    private lateinit var fakeRemoteDataSource: FakeSupabaseRemoteDataSource
    private lateinit var settingsRepository: TestSettingsRepository
    private lateinit var syncRepository: SyncRepositoryImpl

    @BeforeTest
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        AppDatabase.Schema.create(driver)
        DatabaseMigrator.migrate(driver)
        database = AppDatabase(driver)

        fakeRemoteDataSource = FakeSupabaseRemoteDataSource()
        settingsRepository = TestSettingsRepository(
            initialSupabaseUrl = "https://test.supabase.co",
            initialSupabaseKey = "test-anon-key",
            initialLastSyncTimestamp = 0L,
            initialAutoSyncEnabled = true,
            initialDeviceRole = DeviceRole.ADMIN
        )

        syncRepository = SyncRepositoryImpl(
            database = database,
            remoteDataSource = fakeRemoteDataSource,
            settingsRepository = settingsRepository
        )
    }

    @AfterTest
    fun tearDown() {
        driver.close()
    }

    // =========================================================================
    // 1. OFFLINE-TO-ONLINE SYNC RECOVERY
    // =========================================================================

    @Test
    fun testOfflineInsertRecovery_pushesAllEntitiesAndMarksThemSynced(): Unit = runBlocking {
        val queries = database.appDatabaseQueries

        // 1. Product created offline
        queries.insertProduct(
            id = "prod-offline-1",
            codigos = "[\"111111\"]",
            nombre = "Sabritas Clasicas 45g",
            precio = 20.0,
            costo = 14.0,
            categoria = "Botanas",
            activo = 1L,
            por_peso = 0L,
            precio_mayoreo = 17.0,
            es_favorito = 1L,
            piezas = 1.0,
            precio_delivery = 22.0,
            created_at = 1000L,
            updated_at = 1000L,
            sync_state = "PENDING_INSERT"
        )
        queries.insertProductBarcode(barcode = "111111", product_id = "prod-offline-1")

        // 2. Customer created offline with special pricing
        queries.insertCustomer(
            id = "cust-offline-1",
            nombre = "Doña Maria",
            telefono = "555-0100",
            direccion = "Calle Luna 12",
            notas = "Cliente frecuente",
            limite_credito = 2000.0,
            siempre_mayoreo = 1L,
            activo = 1L,
            created_at = 1050L,
            updated_at = 1050L,
            sync_state = "PENDING_INSERT",
            tipo_descuento = "WHOLESALE",
            porcentaje_descuento = 0.0
        )

        // 3. Cashier created offline
        queries.insertCashier(
            id = "cashier-offline-1",
            nombre = "Cajero Turno Matutino",
            pin = "1234",
            activo = 1L,
            created_at = 1100L,
            updated_at = 1100L,
            sync_state = "PENDING_INSERT",
            device_id = "device-pos-1"
        )

        // 4. Shift created offline
        queries.insertShift(
            id = "shift-offline-1",
            cashier_id = "cashier-offline-1",
            cashier_name = "Cajero Turno Matutino",
            start_time = 1200L,
            end_time = null,
            initial_cash = 300.0,
            final_cash_expected = null,
            final_cash_counted = null,
            difference = null,
            notes = "Apertura normal",
            is_closed = 0L,
            sync_state = "PENDING_INSERT",
            updated_at = 1200L
        )

        // 5. Cash movement created offline
        queries.insertCashMovement(
            id = "mov-offline-1",
            shift_id = "shift-offline-1",
            cashier_id = "cashier-offline-1",
            tipo = "INGRESO",
            monto = 150.0,
            motivo = "Fondo de monedas",
            created_at = 1250L,
            sync_state = "PENDING_INSERT"
        )

        // 6. Sale with normal item and unregistered ("UNREG-") item created offline
        queries.insertSale(
            id = "sale-offline-1",
            folio = 101L,
            total = 50.0,
            total_original = 50.0,
            total_costo = 30.0,
            ganancia = 20.0,
            pago_con = 100.0,
            cambio = 50.0,
            metodo_pago = "EFECTIVO",
            total_items = 2.0,
            customer_id = "cust-offline-1",
            created_at = 1300L,
            sync_state = "PENDING_INSERT",
            shift_id = "shift-offline-1",
            cashier_id = "cashier-offline-1",
            cashier_name = "Cajero Turno Matutino",
            estado = "COMPLETADA",
            es_foranea = 0L,
            monto_efectivo = 50.0,
            monto_tarjeta = 0.0,
            monto_transferencia = 0.0,
            monto_credito = 0.0,
            updated_at = 1300L
        )
        // Item 1: Known catalog product
        queries.insertSaleItem(
            id = "item-offline-1",
            sale_id = "sale-offline-1",
            product_id = "prod-offline-1",
            product_nombre = "Sabritas Clasicas 45g",
            cantidad = 1.0,
            precio_unitario = 20.0,
            costo_unitario = 14.0,
            subtotal = 20.0,
            ganancia = 6.0,
            es_mayoreo = 0L,
            es_delivery = 0L,
            es_descuento = 0L,
            created_at = 1310L
        )
        // Item 2: Unregistered product ("UNREG-...")
        queries.insertSaleItem(
            id = "item-offline-2",
            sale_id = "sale-offline-1",
            product_id = "UNREG-abc-123",
            product_nombre = "Articulo Varios",
            cantidad = 1.0,
            precio_unitario = 30.0,
            costo_unitario = 16.0,
            subtotal = 30.0,
            ganancia = 14.0,
            es_mayoreo = 0L,
            es_delivery = 0L,
            es_descuento = 0L,
            created_at = 1315L
        )

        // 7. Customer payment created offline
        queries.insertCustomerPayment(
            id = "pay-offline-1",
            customer_id = "cust-offline-1",
            monto = 50.0,
            metodo_pago = "EFECTIVO",
            notas = "Abono inicial",
            created_at = 1400L,
            sync_state = "PENDING_INSERT"
        )

        // Run sync (force = true ensures execution without cooldown delay)
        val syncResult = syncRepository.syncAll(isManual = true, force = true)
        assertTrue(syncResult.isSuccess, "Sync execution must succeed")
        val report = syncResult.getOrThrow()
        assertTrue(report.success, "Sync report should be successful")
        assertTrue(report.itemsPushed > 0, "Pushed count must be greater than zero")

        // Verify remote received all entities
        assertEquals(1, fakeRemoteDataSource.pushedProducts.size)
        assertEquals("prod-offline-1", fakeRemoteDataSource.pushedProducts.first().id)
        assertEquals(1000L, fakeRemoteDataSource.pushedProducts.first().createdAt)

        assertEquals(1, fakeRemoteDataSource.pushedCustomers.size)
        assertEquals("cust-offline-1", fakeRemoteDataSource.pushedCustomers.first().id)
        assertEquals("WHOLESALE", fakeRemoteDataSource.pushedCustomers.first().tipoDescuento)

        assertEquals(1, fakeRemoteDataSource.pushedCashiers.size)
        assertEquals("cashier-offline-1", fakeRemoteDataSource.pushedCashiers.first().id)

        assertEquals(1, fakeRemoteDataSource.pushedShifts.size)
        assertEquals("shift-offline-1", fakeRemoteDataSource.pushedShifts.first().id)

        assertEquals(1, fakeRemoteDataSource.pushedCashMovements.size)
        assertEquals("mov-offline-1", fakeRemoteDataSource.pushedCashMovements.first().id)

        assertEquals(1, fakeRemoteDataSource.pushedSales.size)
        val pushedSale = fakeRemoteDataSource.pushedSales.first()
        assertEquals("sale-offline-1", pushedSale.id)
        assertEquals("cashier-offline-1", pushedSale.cashierId, "Sale must include cashier_id")
        assertEquals(1300L, pushedSale.updatedAt, "Sale must include updated_at")

        assertEquals(2, fakeRemoteDataSource.pushedSaleItems.size)
        val unregItem = fakeRemoteDataSource.pushedSaleItems.first { it.id == "item-offline-2" }
        assertNull(unregItem.productId, "UNREG- product ID must be sanitized to null to avoid Postgres FK 23503 error")

        assertEquals(1, fakeRemoteDataSource.pushedCustomerPayments.size)
        assertEquals("pay-offline-1", fakeRemoteDataSource.pushedCustomerPayments.first().id)

        // Verify local SQLite sync_state has transitioned to SYNCED
        assertEquals("SYNCED", queries.selectProductById("prod-offline-1").executeAsOne().sync_state)
        assertEquals("SYNCED", queries.selectCustomerById("cust-offline-1").executeAsOne().sync_state)
        assertEquals("SYNCED", queries.selectCashierById("cashier-offline-1").executeAsOne().sync_state)
        assertEquals("SYNCED", queries.selectShiftById("shift-offline-1").executeAsOne().sync_state)
        assertEquals("SYNCED", queries.selectSaleById("sale-offline-1").executeAsOne().sync_state)
        assertTrue(queries.selectUnsyncedCashMovements().executeAsList().isEmpty(), "No cash movements should remain unsynced")
        assertTrue(queries.selectUnsyncedCustomerPayments().executeAsList().isEmpty(), "No customer payments should remain unsynced")
        assertTrue(queries.selectUnsyncedProducts().executeAsList().isEmpty(), "No products should remain unsynced")
        assertTrue(queries.selectUnsyncedCustomers().executeAsList().isEmpty(), "No customers should remain unsynced")
        assertTrue(queries.selectUnsyncedSales().executeAsList().isEmpty(), "No sales should remain unsynced")
        assertTrue(queries.selectUnsyncedShifts().executeAsList().isEmpty(), "No shifts should remain unsynced")
    }

    @Test
    fun testOfflineUpdateRecovery_pushesModifiedRecordsAndTransitionsToSynced(): Unit = runBlocking {
        val queries = database.appDatabaseQueries

        // Seed with SYNCED product
        queries.insertProduct(
            id = "prod-update-1",
            codigos = "[]",
            nombre = "Galletas Chokis",
            precio = 18.0,
            costo = 12.0,
            categoria = "Galletas",
            activo = 1L,
            por_peso = 0L,
            precio_mayoreo = 16.0,
            es_favorito = 0L,
            piezas = 1.0,
            precio_delivery = 20.0,
            created_at = 1000L,
            updated_at = 1000L,
            sync_state = "SYNCED"
        )

        // Offline update to price and sync_state = PENDING_UPDATE
        queries.updateProduct(
            codigos = "[]",
            nombre = "Galletas Chokis",
            precio = 22.0,
            costo = 15.0,
            categoria = "Galletas",
            activo = 1L,
            por_peso = 0L,
            precio_mayoreo = 19.0,
            es_favorito = 0L,
            piezas = 1.0,
            precio_delivery = 24.0,
            created_at = 1000L,
            updated_at = 2000L,
            sync_state = "PENDING_UPDATE",
            id = "prod-update-1"
        )

        val syncResult = syncRepository.syncAll(isManual = true, force = true)
        assertTrue(syncResult.isSuccess)

        // Remote must receive the update
        val remoteProduct = fakeRemoteDataSource.remoteProducts["prod-update-1"]
        assertNotNull(remoteProduct)
        assertEquals(22.0, remoteProduct.precio)
        assertEquals(2000L, remoteProduct.updatedAt)

        // Local product must be SYNCED
        assertEquals("SYNCED", queries.selectProductById("prod-update-1").executeAsOne().sync_state)
    }

    // =========================================================================
    // 2. PARTIAL FAILURE WATERMARK SAFETY
    // =========================================================================

    @Test
    fun testPartialFailure_networkErrorDoesNotAdvanceWatermark_andRetriesSafelyOnNextSync(): Unit = runBlocking {
        val queries = database.appDatabaseQueries

        // Set initial watermark
        val initialWatermark = 50_000L
        settingsRepository.setLastSyncTimestamp(initialWatermark)

        // Insert pending offline shift
        queries.insertCashier(
            id = "cashier-pf-1",
            nombre = "Cashier Test",
            pin = "1111",
            activo = 1L,
            created_at = 1000L,
            updated_at = 1000L,
            sync_state = "SYNCED",
            device_id = null
        )
        queries.insertShift(
            id = "shift-pf-1",
            cashier_id = "cashier-pf-1",
            cashier_name = "Cashier Test",
            start_time = 60_000L,
            end_time = null,
            initial_cash = 200.0,
            final_cash_expected = null,
            final_cash_counted = null,
            difference = null,
            notes = null,
            is_closed = 0L,
            sync_state = "PENDING_INSERT",
            updated_at = 60_000L
        )

        // Inject simulated mid-sync remote network failure on pullSales
        fakeRemoteDataSource.pullSalesError = RuntimeException("HTTP 500: Database connection pool exhausted")

        // First sync attempt: should fail mid-way
        val failedResult = syncRepository.syncAll(isManual = true, force = true)
        assertTrue(failedResult.isFailure, "Sync must return failure when remote call fails")
        assertEquals(SyncStateEnum.ERROR, syncRepository.syncState.value)

        // CRITICAL CHECK: Watermark MUST NOT advance after failure!
        val watermarkAfterFailure = settingsRepository.lastSyncTimestampFlow.first()
        assertEquals(
            initialWatermark,
            watermarkAfterFailure,
            "Watermark must NOT advance when a sync phase encounters an error"
        )

        // Pending shift was pushed during phase 1, but sync was interrupted before completion.
        // Now clear the error so subsequent sync succeeds.
        fakeRemoteDataSource.resetErrors()

        // Second sync attempt: should succeed and safely advance watermark
        val successResult = syncRepository.syncAll(isManual = true, force = true)
        assertTrue(successResult.isSuccess, "Retry sync must succeed after remote error is cleared")
        assertEquals(SyncStateEnum.SUCCESS, syncRepository.syncState.value)

        val newWatermark = settingsRepository.lastSyncTimestampFlow.first()
        assertTrue(
            newWatermark > initialWatermark,
            "Watermark should advance only after 100% of sync phases succeed (new: $newWatermark, old: $initialWatermark)"
        )
    }

    @Test
    fun testPartialFailure_orphanedChildRecordDefersWatermarkToPreventRecordLoss(): Unit = runBlocking {
        val initialWatermark = 100_000L
        settingsRepository.setLastSyncTimestamp(initialWatermark)

        // Remote has a child sale item whose parent sale is not in SQLite yet (e.g. out of order or paginated)
        val orphanedCreatedAt = 150_000L
        fakeRemoteDataSource.remoteSaleItems["orphaned-item-1"] = SaleItemDto(
            id = "orphaned-item-1",
            saleId = "missing-parent-sale-1",
            productId = null,
            productNombre = "Item Without Parent",
            cantidad = 1.0,
            precioUnitario = 50.0,
            costoUnitario = 30.0,
            subtotal = 50.0,
            ganancia = 20.0,
            esMayoreo = false,
            esDelivery = false,
            esDescuento = false,
            createdAt = orphanedCreatedAt
        )

        val syncResult = syncRepository.syncAll(isManual = true, force = true)
        assertTrue(syncResult.isSuccess)

        // Watermark must NOT advance past orphaned record timestamp - 1L
        val resultingWatermark = settingsRepository.lastSyncTimestampFlow.first()
        val expectedWatermarkCeiling = orphanedCreatedAt - 1L
        assertEquals(
            expectedWatermarkCeiling,
            resultingWatermark,
            "Watermark must be capped by earliestDeferredTimestamp - 1L to prevent skipping orphaned child on next pull"
        )
    }

    // =========================================================================
    // 3. OLD VS. NEW TIMESTAMP CONFLICT RESOLUTION
    // =========================================================================

    @Test
    fun testConflictResolution_localUnpushedCatalogChangesPreservedAgainstOlderRemoteRecord(): Unit = runBlocking {
        val queries = database.appDatabaseQueries

        // Admin edited catalog product offline with newer timestamp
        queries.insertProduct(
            id = "prod-conflict-1",
            codigos = "[]",
            nombre = "Aceite Nutrioli 1L",
            precio = 55.0,
            costo = 40.0,
            categoria = "Abarrotes",
            activo = 1L,
            por_peso = 0L,
            precio_mayoreo = 50.0,
            es_favorito = 1L,
            piezas = 1.0,
            precio_delivery = 58.0,
            created_at = 1000L,
            updated_at = 200_000L,
            sync_state = "PENDING_UPDATE"
        )

        // Remote database contains stale/older record (e.g. from device that was behind)
        fakeRemoteDataSource.remoteProducts["prod-conflict-1"] = ProductDto(
            id = "prod-conflict-1",
            codigos = "[]",
            nombre = "Aceite Nutrioli 1L Stale",
            precio = 45.0,
            costo = 35.0,
            categoria = "Abarrotes",
            activo = true,
            porPeso = false,
            precioMayoreo = 42.0,
            precioDelivery = 48.0,
            esFavorito = false,
            piezas = 1.0,
            createdAt = 1000L,
            updatedAt = 150_000L // Older than local 200_000L
        )

        val syncResult = syncRepository.syncAll(isManual = true, force = true)
        assertTrue(syncResult.isSuccess)

        // Local product must retain local changes and not be clobbered by remote pre-pull
        val localProduct = queries.selectProductById("prod-conflict-1").executeAsOne()
        assertEquals(55.0, localProduct.precio, "Local newer price must NOT be overwritten by older remote price")
        assertEquals("Aceite Nutrioli 1L", localProduct.nombre)
        assertEquals("SYNCED", localProduct.sync_state, "Local product should transition to SYNCED after push")

        // Remote must be updated with local data
        val remoteProduct = fakeRemoteDataSource.remoteProducts["prod-conflict-1"]
        assertNotNull(remoteProduct)
        assertEquals(55.0, remoteProduct.precio)
    }

    @Test
    fun testConflictResolution_offlineSaleCancellationNeverOverwrittenByStaleRemoteCompletada(): Unit = runBlocking {
        val queries = database.appDatabaseQueries

        // Local sale cancelled offline: estado = CANCELADA, sync_state = PENDING_UPDATE
        queries.insertSale(
            id = "sale-cancelled-1",
            folio = 501L,
            total = 120.0,
            total_original = 120.0,
            total_costo = 80.0,
            ganancia = 40.0,
            pago_con = 150.0,
            cambio = 30.0,
            metodo_pago = "EFECTIVO",
            total_items = 1.0,
            customer_id = null,
            created_at = 100_000L,
            sync_state = "PENDING_UPDATE",
            shift_id = null,
            cashier_id = null,
            cashier_name = null,
            estado = "CANCELADA",
            es_foranea = 0L,
            monto_efectivo = 120.0,
            monto_tarjeta = 0.0,
            monto_transferencia = 0.0,
            monto_credito = 0.0,
            updated_at = 250_000L
        )

        // Remote returns the sale in COMPLETADA state with older updatedAt
        fakeRemoteDataSource.remoteSales["sale-cancelled-1"] = SaleDto(
            id = "sale-cancelled-1",
            folio = 501L,
            total = 120.0,
            totalOriginal = 120.0,
            totalCosto = 80.0,
            ganancia = 40.0,
            pagoCon = 150.0,
            cambio = 30.0,
            metodoPago = "EFECTIVO",
            totalItems = 1.0,
            customerId = null,
            createdAt = 100_000L,
            cashierId = null,
            cashierName = null,
            estado = "COMPLETADA",
            esForanea = false,
            shiftId = null,
            montoEfectivo = 120.0,
            montoTarjeta = 0.0,
            montoTransferencia = 0.0,
            montoCredito = 0.0,
            updatedAt = 100_000L // Stale remote
        )

        val syncResult = syncRepository.syncAll(isManual = true, force = true)
        assertTrue(syncResult.isSuccess)

        // Local sale MUST preserve CANCELADA
        val localSale = queries.selectSaleById("sale-cancelled-1").executeAsOne()
        assertEquals("CANCELADA", localSale.estado, "Offline sale cancellation must NEVER be reverted to COMPLETADA by stale remote")
        assertEquals("SYNCED", localSale.sync_state)

        // Remote must be updated to CANCELADA
        assertEquals("CANCELADA", fakeRemoteDataSource.remoteSales["sale-cancelled-1"]?.estado)
    }

    @Test
    fun testConflictResolution_offlineClosedShiftNeverOverwrittenByStaleRemoteOpenShift(): Unit = runBlocking {
        val queries = database.appDatabaseQueries

        // Seed cashier
        queries.insertCashier(
            id = "c-shift-test",
            nombre = "Cajero Turno",
            pin = "0000",
            activo = 1L,
            created_at = 1000L,
            updated_at = 1000L,
            sync_state = "SYNCED",
            device_id = null
        )
        fakeRemoteDataSource.remoteCashiers["c-shift-test"] = CashierDto(
            id = "c-shift-test",
            nombre = "Cajero Turno",
            pin = "0000",
            activo = true,
            createdAt = 1000L,
            updatedAt = 1000L
        )

        // Local shift closed offline: is_closed = 1, sync_state = PENDING_UPDATE
        queries.insertShift(
            id = "shift-closed-1",
            cashier_id = "c-shift-test",
            cashier_name = "Cajero Turno",
            start_time = 100_000L,
            end_time = 120_000L,
            initial_cash = 500.0,
            final_cash_expected = 2000.0,
            final_cash_counted = 2000.0,
            difference = 0.0,
            notes = "Cierre puntual",
            is_closed = 1L,
            sync_state = "PENDING_UPDATE",
            updated_at = 120_000L
        )

        // Stale remote shift arrived unclosed (isClosed = false)
        fakeRemoteDataSource.remoteShifts["shift-closed-1"] = ShiftDto(
            id = "shift-closed-1",
            cashierId = "c-shift-test",
            cashierName = "Cajero Turno",
            startTime = 100_000L,
            endTime = null,
            initialCash = 500.0,
            finalCashExpected = null,
            finalCashCounted = null,
            difference = null,
            notes = null,
            isClosed = false,
            updatedAt = 100_000L // Older
        )

        val syncResult = syncRepository.syncAll(isManual = true, force = true)
        assertTrue(syncResult.isSuccess)

        // Local shift must stay closed with accurate cash counts
        val localShift = queries.selectShiftById("shift-closed-1").executeAsOne()
        assertEquals(1L, localShift.is_closed, "Offline closed shift must NOT be overwritten to open shift")
        assertEquals(2000.0, localShift.final_cash_counted)
        assertEquals("SYNCED", localShift.sync_state)

        // Remote shift must be updated to closed
        val remoteShift = fakeRemoteDataSource.remoteShifts["shift-closed-1"]
        assertNotNull(remoteShift)
        assertTrue(remoteShift.isClosed, "Remote shift must be updated to closed")
        assertEquals(2000.0, remoteShift.finalCashCounted)
    }

    @Test
    fun testConflictResolution_newerRemoteUpdatesOverwriteOlderSyncedLocalRecord(): Unit = runBlocking {
        val queries = database.appDatabaseQueries

        // Local product in SYNCED state with older updatedAt
        queries.insertProduct(
            id = "prod-synced-1",
            codigos = "[]",
            nombre = "Leche Entera 1L",
            precio = 24.0,
            costo = 18.0,
            categoria = "Lacteos",
            activo = 1L,
            por_peso = 0L,
            precio_mayoreo = 22.0,
            es_favorito = 0L,
            piezas = 1.0,
            precio_delivery = 26.0,
            created_at = 1000L,
            updated_at = 100_000L,
            sync_state = "SYNCED"
        )

        // Remote product updated on another terminal with newer updatedAt
        fakeRemoteDataSource.remoteProducts["prod-synced-1"] = ProductDto(
            id = "prod-synced-1",
            codigos = "[]",
            nombre = "Leche Entera 1L Nueva Presentacion",
            precio = 27.5,
            costo = 20.0,
            categoria = "Lacteos",
            activo = true,
            porPeso = false,
            precioMayoreo = 25.0,
            precioDelivery = 29.0,
            esFavorito = true,
            piezas = 1.0,
            createdAt = 1000L,
            updatedAt = 200_000L // Newer than local 100_000L
        )

        val syncResult = syncRepository.syncAll(isManual = true, force = true)
        assertTrue(syncResult.isSuccess)

        val localProduct = queries.selectProductById("prod-synced-1").executeAsOne()
        assertEquals("Leche Entera 1L Nueva Presentacion", localProduct.nombre)
        assertEquals(27.5, localProduct.precio)
        assertEquals(200_000L, localProduct.updated_at)
        assertEquals("SYNCED", localProduct.sync_state)
    }

    @Test
    fun testConflictResolution_olderRemoteUpdatesDoNotOverwriteNewerSyncedLocalRecord(): Unit = runBlocking {
        val queries = database.appDatabaseQueries

        // Local product in SYNCED state with newer updatedAt
        queries.insertProduct(
            id = "prod-synced-2",
            codigos = "[]",
            nombre = "Huevo 12 piezas",
            precio = 42.0,
            costo = 32.0,
            categoria = "Abarrotes",
            activo = 1L,
            por_peso = 0L,
            precio_mayoreo = 38.0,
            es_favorito = 0L,
            piezas = 1.0,
            precio_delivery = 45.0,
            created_at = 1000L,
            updated_at = 300_000L,
            sync_state = "SYNCED"
        )

        // Remote product has older timestamp
        fakeRemoteDataSource.remoteProducts["prod-synced-2"] = ProductDto(
            id = "prod-synced-2",
            codigos = "[]",
            nombre = "Huevo 12 piezas Old",
            precio = 35.0,
            costo = 28.0,
            categoria = "Abarrotes",
            activo = true,
            porPeso = false,
            precioMayoreo = 33.0,
            precioDelivery = 38.0,
            esFavorito = false,
            piezas = 1.0,
            createdAt = 1000L,
            updatedAt = 200_000L // Older than local 300_000L
        )

        val syncResult = syncRepository.syncAll(isManual = true, force = true)
        assertTrue(syncResult.isSuccess)

        val localProduct = queries.selectProductById("prod-synced-2").executeAsOne()
        assertEquals(42.0, localProduct.precio, "Older remote update must not overwrite newer local record")
        assertEquals("Huevo 12 piezas", localProduct.nombre)
    }

    // =========================================================================
    // 4. MULTI-PAGE DATASET SYNCHRONIZATION (>1,000 RECORDS)
    // =========================================================================

    @Test
    fun testMultiPageDatasetSynchronization_1500ProductsRetrievedWithoutTruncation(): Unit = runBlocking {
        val queries = database.appDatabaseQueries

        // Generate 1,500 remote products (exceeding standard 1,000 row PostgREST page limit)
        val totalCatalogSize = 1500
        for (i in 1..totalCatalogSize) {
            val paddedId = "prod-${i.toString().padStart(4, '0')}"
            fakeRemoteDataSource.remoteProducts[paddedId] = ProductDto(
                id = paddedId,
                codigos = "[\"CODE-$paddedId\"]",
                nombre = "Catalogo Producto $paddedId",
                precio = 10.0 + (i % 50),
                costo = 5.0 + (i % 20),
                categoria = if (i % 2 == 0) "Categoria A" else "Categoria B",
                activo = true,
                porPeso = (i % 10 == 0),
                precioMayoreo = 9.0 + (i % 50),
                precioDelivery = 12.0 + (i % 50),
                esFavorito = (i <= 10),
                piezas = 1.0,
                createdAt = 100_000L + i,
                updatedAt = 100_000L + i
            )
        }

        assertEquals(1500, fakeRemoteDataSource.remoteProducts.size)

        // Execute sync to download entire catalog into empty local SQLite database
        val syncResult = syncRepository.syncAll(isManual = true, force = true)
        assertTrue(syncResult.isSuccess, "Syncing 1,500 products must succeed")
        val report = syncResult.getOrThrow()
        assertEquals(1500, report.itemsPulled)

        // Assert all 1,500 records are persisted in local SQLite
        val allLocalProducts = queries.selectAllProducts().executeAsList()
        assertEquals(1500, allLocalProducts.size, "Local database must contain all 1,500 products without page truncation")

        // Assert boundary elements across pages (page 1, page 1 boundary, page 2 boundary, last element)
        val firstProduct = queries.selectProductById("prod-0001").executeAsOneOrNull()
        assertNotNull(firstProduct, "Product 1 must exist")
        assertEquals("Catalogo Producto prod-0001", firstProduct.nombre)
        assertEquals("SYNCED", firstProduct.sync_state)

        val midPage1 = queries.selectProductById("prod-0500").executeAsOneOrNull()
        assertNotNull(midPage1, "Product 500 must exist")
        assertEquals("SYNCED", midPage1.sync_state)

        val page1Boundary = queries.selectProductById("prod-1000").executeAsOneOrNull()
        assertNotNull(page1Boundary, "Product 1000 (1,000-row page boundary) must exist")
        assertEquals("Catalogo Producto prod-1000", page1Boundary.nombre)
        assertEquals("SYNCED", page1Boundary.sync_state)

        val page2First = queries.selectProductById("prod-1001").executeAsOneOrNull()
        assertNotNull(page2First, "Product 1001 (start of page 2) must exist")
        assertEquals("Catalogo Producto prod-1001", page2First.nombre)
        assertEquals("SYNCED", page2First.sync_state)

        val lastProduct = queries.selectProductById("prod-1500").executeAsOneOrNull()
        assertNotNull(lastProduct, "Product 1500 (end of page 2) must exist")
        assertEquals("Catalogo Producto prod-1500", lastProduct.nombre)
        assertEquals("SYNCED", lastProduct.sync_state)

        // Also verify barcode entries were created
        val barcodesCount = queries.selectProductByBarcode(barcode = "CODE-prod-1000", normalizedBarcode = "CODE-prod-1000").executeAsOneOrNull()
        assertNotNull(barcodesCount, "Barcode for product 1000 must be properly indexed and queryable")
    }

    // =========================================================================
    // 5. STORE SETTINGS BIDIRECTIONAL SYNC & PRICING RULES
    // =========================================================================

    @Test
    fun testStoreSettingsSync_bidirectionalParity(): Unit = runBlocking {
        // 1. Update business settings locally with the 5 new pricing rules
        settingsRepository.setBusinessSettings(
            defaultRetailMargin = 35.0,
            defaultWholesaleMargin = 20.0,
            defaultDeliveryMargin = 25.0,
            roundRetailPrice = true,
            roundWholesalePrice = false,
            roundDeliveryPrice = true,
            roundTicketTotal = false,
            disallowCardPaymentOnWholesale = true,
            prioritizeDeliveryPrice = true,
            autoWholesaleByQuantity = true,
            autoWholesaleQuantityThreshold = 5,
            autoWholesaleByTicketTotal = true,
            autoWholesaleTicketTotalThreshold = 850.0,
            storeName = "Super Tienda KMP",
            storeAddress = "Calle Central 100",
            storePhone = "555-9988",
            transferClabe = "123456789012345678",
            transferBeneficiary = "Empresa SA de CV",
            receiptFooter = "Visitenos de nuevo",
            updatedAt = 500_000L
        )

        // Sync pushes settings to remote
        val pushResult = syncRepository.syncAll(isManual = true, force = true)
        assertTrue(pushResult.isSuccess)

        val remoteSettings = fakeRemoteDataSource.remoteStoreSettings
        assertNotNull(remoteSettings)
        assertEquals("Super Tienda KMP", remoteSettings.storeName)
        assertTrue(remoteSettings.prioritizeDeliveryPrice)
        assertTrue(remoteSettings.autoWholesaleByQuantity)
        assertEquals(5, remoteSettings.autoWholesaleQuantityThreshold)
        assertTrue(remoteSettings.autoWholesaleByTicketTotal)
        assertEquals(850.0, remoteSettings.autoWholesaleTicketTotalThreshold)

        // 2. Remote updates settings with newer timestamp
        fakeRemoteDataSource.remoteStoreSettings = remoteSettings.copy(
            storeName = "Super Tienda KMP Sucursal 2",
            autoWholesaleQuantityThreshold = 8,
            autoWholesaleTicketTotalThreshold = 1200.0,
            updatedAt = 600_000L
        )

        // Next sync pulls updated settings
        val pullResult = syncRepository.syncAll(isManual = true, force = true)
        assertTrue(pullResult.isSuccess)

        assertEquals("Super Tienda KMP Sucursal 2", settingsRepository.receiptSettingsFlow.first().storeName)
        assertEquals(8, settingsRepository.autoWholesaleQuantityThresholdFlow.first())
        assertEquals(1200.0, settingsRepository.autoWholesaleTicketTotalThresholdFlow.first())
        assertEquals(600_000L, settingsRepository.businessSettingsUpdatedAtFlow.first())
    }

    // =========================================================================
    // 6. CASHIER TOMBSTONE SOFT DELETION
    // =========================================================================

    @Test
    fun testCashierTombstone_deactivatesCashierLocally(): Unit = runBlocking {
        val queries = database.appDatabaseQueries

        queries.insertCashier(
            id = "c-tombstone-1",
            nombre = "Cajero A Eliminar",
            pin = "5555",
            activo = 1L,
            created_at = 1000L,
            updated_at = 1000L,
            sync_state = "SYNCED",
            device_id = null
        )

        // Remote pushes tombstone for cashier
        fakeRemoteDataSource.remoteDeletedRecords.add(
            com.dnavarro.poskmp.data.source.remote.dto.DeletedRecordDto(
                id = "c-tombstone-1",
                entityType = "CASHIER",
                deletedAt = 2000L
            )
        )

        val syncResult = syncRepository.syncAll(isManual = true, force = true)
        assertTrue(syncResult.isSuccess)

        val cashier = queries.selectCashierById("c-tombstone-1").executeAsOneOrNull()
        assertNotNull(cashier)
        assertEquals(0L, cashier.activo, "Cashier should be deactivated upon receiving CASHIER tombstone")
        assertEquals("SYNCED", cashier.sync_state)
    }
}
