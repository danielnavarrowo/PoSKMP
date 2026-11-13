package com.dnavarro.poskmp.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.dnavarro.poskmp.data.source.local.SqlDelightCustomerDataSource
import com.dnavarro.poskmp.data.source.local.SqlDelightShiftDataSource
import com.dnavarro.poskmp.data.sync.TestSettingsRepository
import com.dnavarro.poskmp.db.AppDatabase
import com.dnavarro.poskmp.db.DatabaseMigrator
import com.dnavarro.poskmp.domain.model.CashMovementType
import com.dnavarro.poskmp.domain.usecase.RecordCustomerPaymentUseCase
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ShiftRepositoryCashPaymentsTest {

    private lateinit var driver: JdbcSqliteDriver
    private lateinit var database: AppDatabase
    private lateinit var shiftRepository: ShiftRepositoryImpl
    private lateinit var customerRepository: CustomerRepositoryImpl
    private lateinit var recordCustomerPaymentUseCase: RecordCustomerPaymentUseCase

    @BeforeTest
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        AppDatabase.Schema.create(driver)
        DatabaseMigrator.migrate(driver)
        database = AppDatabase(driver)

        val settingsRepository = TestSettingsRepository()
        val shiftLocalDataSource = SqlDelightShiftDataSource(database)
        shiftRepository = ShiftRepositoryImpl(
            localDataSource = shiftLocalDataSource,
            settingsRepository = settingsRepository
        )

        val customerLocalDataSource = SqlDelightCustomerDataSource(database)
        customerRepository = CustomerRepositoryImpl(customerLocalDataSource)
        recordCustomerPaymentUseCase = RecordCustomerPaymentUseCase(customerRepository)
    }

    @AfterTest
    fun tearDown() {
        driver.close()
    }

    private fun seedCashierAndCustomer() {
        val queries = database.appDatabaseQueries
        queries.insertCashier(
            id = "cashier-1",
            nombre = "Ana Cajera",
            pin = "1234",
            activo = 1L,
            created_at = 100L,
            updated_at = 100L,
            sync_state = "PENDING_INSERT",
            device_id = "device-test"
        )
        queries.insertCustomer(
            id = "cust-1",
            nombre = "Juan Perez",
            telefono = "555-0001",
            direccion = "Centro",
            notas = "",
            limite_credito = 5000.0,
            siempre_mayoreo = 0L,
            activo = 1L,
            created_at = 100L,
            updated_at = 100L,
            sync_state = "PENDING_INSERT",
            tipo_descuento = "NONE",
            porcentaje_descuento = 0.0
        )
    }

    private fun seedSale(
        id: String,
        folio: Long,
        total: Double,
        metodoPago: String,
        customerId: String?,
        shiftId: String?,
        createdAt: Long,
        montoEfectivo: Double = if (metodoPago == "EFECTIVO") total else 0.0,
        montoCredito: Double = if (metodoPago == "CREDITO") total else 0.0
    ) {
        database.appDatabaseQueries.insertSale(
            id = id,
            folio = folio,
            total = total,
            total_original = total,
            total_costo = total * 0.6,
            ganancia = total * 0.4,
            pago_con = if (metodoPago == "CREDITO") 0.0 else total,
            cambio = 0.0,
            metodo_pago = metodoPago,
            total_items = 1.0,
            customer_id = customerId,
            created_at = createdAt,
            sync_state = "PENDING_INSERT",
            shift_id = shiftId,
            cashier_id = "cashier-1",
            cashier_name = "Ana Cajera",
            estado = "COMPLETADA",
            es_foranea = 0L,
            monto_efectivo = montoEfectivo,
            monto_tarjeta = 0.0,
            monto_transferencia = 0.0,
            monto_credito = montoCredito,
            updated_at = createdAt
        )
    }

    @Test
    fun cashCustomerPaymentDuringShift_reducesDebtAndAddsToExpectedCashAndCloseShift() = runBlocking {
        seedCashierAndCustomer()

        // Prior credit sale creates $500 debt on customer
        seedSale(
            id = "sale-credit-1",
            folio = 1L,
            total = 500.0,
            metodoPago = "CREDITO",
            customerId = "cust-1",
            shiftId = null,
            createdAt = 500L
        )
        assertEquals(500.0, customerRepository.getCustomerById("cust-1")?.saldoDeudor)

        val openShiftResult = shiftRepository.openShift(cashierId = "cashier-1", initialCash = 1000.0)
        assertTrue(openShiftResult.isSuccess)
        val shift = openShiftResult.getOrThrow()

        // Cash sale + inflow + outflow during shift
        seedSale(
            id = "sale-cash-1",
            folio = 2L,
            total = 250.0,
            metodoPago = "EFECTIVO",
            customerId = null,
            shiftId = shift.id,
            createdAt = shift.startTime + 1L
        )
        assertTrue(
            shiftRepository.recordCashMovement(
                shiftId = shift.id,
                cashierId = "cashier-1",
                type = CashMovementType.ENTRADA,
                amount = 100.0,
                reason = "Cambio"
            ).isSuccess
        )
        assertTrue(
            shiftRepository.recordCashMovement(
                shiftId = shift.id,
                cashierId = "cashier-1",
                type = CashMovementType.SALIDA,
                amount = 50.0,
                reason = "Proveedor"
            ).isSuccess
        )

        // Record cash customer payment (abono en efectivo) during open shift
        val paymentResult = recordCustomerPaymentUseCase(
            customerId = "cust-1",
            monto = 150.0,
            metodoPago = "EFECTIVO",
            notas = "Abono parcial en efectivo"
        )
        assertTrue(paymentResult.isSuccess)
        assertEquals(350.0, customerRepository.getCustomerById("cust-1")?.saldoDeudor)

        // Expected cash = 1000 (initial) + 250 (cash sales) + 150 (cash abono) + 100 (inflow) - 50 (outflow) = 1450.0
        val summary = shiftRepository.getShiftSummary(shift.id).getOrThrow()
        assertEquals(250.0, summary.totalVentas, 0.001)
        assertEquals(250.0, summary.ventasEfectivo, 0.001)
        assertEquals(150.0, summary.abonosEfectivo, 0.001)
        assertEquals(100.0, summary.totalEntradas, 0.001)
        assertEquals(50.0, summary.totalSalidas, 0.001)
        assertEquals(1450.0, summary.efectivoEsperado, 0.001)

        // Closing shift persists the expected cash including cash abonos
        val closedShift = shiftRepository.closeShift(shift.id, countedCash = 1450.0, notes = null).getOrThrow()
        assertEquals(1450.0, closedShift.finalCashExpected)
        assertEquals(0.0, closedShift.difference)
    }

    @Test
    fun nonCashCustomerPayments_doNotIncreaseExpectedCash() = runBlocking {
        seedCashierAndCustomer()
        seedSale(
            id = "sale-credit-2",
            folio = 1L,
            total = 800.0,
            metodoPago = "CREDITO",
            customerId = "cust-1",
            shiftId = null,
            createdAt = 500L
        )

        val shift = shiftRepository.openShift(cashierId = "cashier-1", initialCash = 300.0).getOrThrow()

        assertTrue(recordCustomerPaymentUseCase("cust-1", 200.0, "TARJETA").isSuccess)
        assertTrue(recordCustomerPaymentUseCase("cust-1", 100.0, "TRANSFERENCIA").isSuccess)
        assertEquals(500.0, customerRepository.getCustomerById("cust-1")?.saldoDeudor)

        val summary = shiftRepository.getShiftSummary(shift.id).getOrThrow()
        assertEquals(0.0, summary.abonosEfectivo, 0.001)
        assertEquals(300.0, summary.efectivoEsperado, 0.001)
    }

    @Test
    fun cashCustomerPayments_respectShiftTimeBoundsAndDeletion() = runBlocking {
        seedCashierAndCustomer()
        val queries = database.appDatabaseQueries

        queries.insertShift(
            id = "shift-bounded",
            cashier_id = "cashier-1",
            cashier_name = "Ana Cajera",
            start_time = 2000L,
            end_time = 3000L,
            initial_cash = 500.0,
            final_cash_expected = null,
            final_cash_counted = null,
            difference = null,
            notes = null,
            is_closed = 1L,
            sync_state = "PENDING_INSERT",
            updated_at = 3000L
        )

        // Outside before start_time (excluded)
        queries.insertCustomerPayment("pay-before", "cust-1", 75.0, "EFECTIVO", "", 1999L, "PENDING_INSERT")
        // Inside inclusive boundaries (included)
        queries.insertCustomerPayment("pay-at-start", "cust-1", 40.0, "EFECTIVO", "", 2000L, "PENDING_INSERT")
        queries.insertCustomerPayment("pay-middle", "cust-1", 60.0, "efectivo", "", 2500L, "PENDING_INSERT")
        queries.insertCustomerPayment("pay-at-end", "cust-1", 25.0, "EFECTIVO", "", 3000L, "PENDING_INSERT")
        // Outside after end_time (excluded)
        queries.insertCustomerPayment("pay-after", "cust-1", 90.0, "EFECTIVO", "", 3001L, "PENDING_INSERT")

        val summaryBeforeDelete = shiftRepository.getShiftSummary("shift-bounded").getOrThrow()
        assertEquals(125.0, summaryBeforeDelete.abonosEfectivo, 0.001)
        assertEquals(625.0, summaryBeforeDelete.efectivoEsperado, 0.001)

        // Deleting a payment removes it from abonosEfectivo & efectivoEsperado
        recordCustomerPaymentUseCase.deletePayment("pay-middle")
        val summaryAfterDelete = shiftRepository.getShiftSummary("shift-bounded").getOrThrow()
        assertEquals(65.0, summaryAfterDelete.abonosEfectivo, 0.001)
        assertEquals(565.0, summaryAfterDelete.efectivoEsperado, 0.001)
    }
}
