package com.dnavarro.poskmp.data

import com.dnavarro.poskmp.data.source.local.ShiftLocalDataSource
import com.dnavarro.poskmp.db.Cash_movements
import com.dnavarro.poskmp.db.Cashiers
import com.dnavarro.poskmp.db.Shifts
import com.dnavarro.poskmp.domain.model.CashMovement
import com.dnavarro.poskmp.domain.model.CashMovementType
import com.dnavarro.poskmp.domain.model.Cashier
import com.dnavarro.poskmp.domain.model.CashierShift
import com.dnavarro.poskmp.domain.model.Sale
import com.dnavarro.poskmp.domain.model.ShiftSummary
import com.dnavarro.poskmp.util.currentTimeMillis
import com.dnavarro.poskmp.util.generateUUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

interface ShiftRepository {
    val activeShiftFlow: Flow<CashierShift?>
    suspend fun getActiveShift(): CashierShift?
    suspend fun getShiftById(id: String): CashierShift?
    suspend fun getShiftsBetween(startTime: Long, endTime: Long): List<CashierShift>
    val activeCashiersFlow: Flow<List<Cashier>>
    suspend fun getAllActiveCashiers(): List<Cashier>
    suspend fun getCashierById(id: String): Cashier?

    suspend fun openShift(cashierId: String, initialCash: Double): Result<CashierShift>
    suspend fun closeShift(shiftId: String, countedCash: Double, notes: String?): Result<CashierShift>
    suspend fun recordCashMovement(
        shiftId: String,
        cashierId: String,
        type: CashMovementType,
        amount: Double,
        reason: String
    ): Result<CashMovement>
    suspend fun saveCashier(id: String?, nombre: String, pin: String): Result<Cashier>
    suspend fun deleteCashier(id: String): Result<Unit>
    suspend fun countActiveCashiers(): Long
    fun getMovementsForShiftFlow(shiftId: String): Flow<List<CashMovement>>
    suspend fun getShiftSummary(shiftId: String): Result<ShiftSummary>
}

class ShiftRepositoryImpl(
    private val localDataSource: ShiftLocalDataSource
) : ShiftRepository {

    override val activeShiftFlow: Flow<CashierShift?> =
        localDataSource.getActiveShiftFlow().map { it?.toDomain() }

    override suspend fun getActiveShift(): CashierShift? =
        localDataSource.getActiveShift()?.toDomain()

    override suspend fun getShiftById(id: String): CashierShift? =
        localDataSource.getShiftById(id)?.toDomain()

    override suspend fun getShiftsBetween(startTime: Long, endTime: Long): List<CashierShift> =
        localDataSource.getShiftsBetween(startTime, endTime).map { it.toDomain() }

    override val activeCashiersFlow: Flow<List<Cashier>> =
        localDataSource.getAllActiveCashiersFlow().map { list -> list.map { it.toDomain() } }

    override suspend fun getAllActiveCashiers(): List<Cashier> = withContext(Dispatchers.IO) {
        localDataSource.getAllActiveCashiers().map { it.toDomain() }
    }

    override suspend fun getCashierById(id: String): Cashier? =
        localDataSource.getCashierById(id)?.toDomain()

    override suspend fun openShift(cashierId: String, initialCash: Double): Result<CashierShift> = withContext(Dispatchers.IO) {
        try {
            val existing = localDataSource.getActiveShift()
            if (existing != null) {
                return@withContext Result.failure(IllegalStateException("Ya existe un turno activo."))
            }

            val cashier = localDataSource.getCashierById(cashierId)
                ?: return@withContext Result.failure(IllegalArgumentException("Cajero no encontrado."))

            val now = currentTimeMillis()
            val newShift = Shifts(
                id = generateUUID(),
                cashier_id = cashier.id,
                cashier_name = cashier.nombre,
                start_time = now,
                end_time = null,
                initial_cash = initialCash,
                final_cash_expected = null,
                final_cash_counted = null,
                difference = null,
                notes = null,
                is_closed = 0L,
                sync_state = "PENDING_INSERT"
            )
            localDataSource.insertShift(newShift)
            Result.success(newShift.toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun closeShift(
        shiftId: String,
        countedCash: Double,
        notes: String?
    ): Result<CashierShift> = withContext(Dispatchers.IO) {
        try {
            val shiftDb = localDataSource.getShiftById(shiftId)
                ?: return@withContext Result.failure(IllegalArgumentException("Turno no encontrado."))

            if (shiftDb.is_closed == 1L) {
                return@withContext Result.failure(IllegalStateException("El turno ya se encuentra cerrado."))
            }

            val now = currentTimeMillis()
            val summaryResult = getShiftSummary(shiftId)
            val summary = summaryResult.getOrThrow()

            val expectedCash = summary.efectivoEsperado
            val difference = countedCash - expectedCash

            localDataSource.closeShift(
                id = shiftId,
                endTime = now,
                expectedCash = expectedCash,
                countedCash = countedCash,
                difference = difference,
                notes = notes
            )

            val updated = localDataSource.getShiftById(shiftId)?.toDomain()
                ?: return@withContext Result.failure(IllegalStateException("Error al recuperar el turno cerrado."))

            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun recordCashMovement(
        shiftId: String,
        cashierId: String,
        type: CashMovementType,
        amount: Double,
        reason: String
    ): Result<CashMovement> = withContext(Dispatchers.IO) {
        try {
            if (amount <= 0.0) {
                return@withContext Result.failure(IllegalArgumentException("El monto debe ser mayor a 0."))
            }

            val now = currentTimeMillis()
            val movement = Cash_movements(
                id = generateUUID(),
                shift_id = shiftId,
                cashier_id = cashierId,
                tipo = type.name,
                monto = amount,
                motivo = reason.trim(),
                created_at = now,
                sync_state = "PENDING_INSERT"
            )
            localDataSource.insertCashMovement(movement)
            Result.success(movement.toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getMovementsForShiftFlow(shiftId: String): Flow<List<CashMovement>> =
        localDataSource.getMovementsByShiftIdFlow(shiftId).map { list -> list.map { it.toDomain() } }

    override suspend fun getShiftSummary(shiftId: String): Result<ShiftSummary> = withContext(Dispatchers.IO) {
        try {
            val shiftDb = localDataSource.getShiftById(shiftId)
                ?: return@withContext Result.failure(IllegalArgumentException("Turno no encontrado."))

            val salesSummary = localDataSource.getSalesSummaryByShift(
                shiftId = shiftId,
                startTime = shiftDb.start_time,
                endTime = shiftDb.end_time
            )

            val totalEntradas = localDataSource.getSumMovementsByShiftAndType(shiftId, CashMovementType.ENTRADA.name)
            val totalSalidas = localDataSource.getSumMovementsByShiftAndType(shiftId, CashMovementType.SALIDA.name)
            val movements = localDataSource.getMovementsByShiftId(shiftId).map { it.toDomain() }

            val cancelledSalesSummary = localDataSource.getCancelledSalesSummaryByShift(
                shiftId = shiftId,
                startTime = shiftDb.start_time,
                endTime = shiftDb.end_time
            )
            val cancelledSales = localDataSource.getCancelledSalesByShift(
                shiftId = shiftId,
                startTime = shiftDb.start_time,
                endTime = shiftDb.end_time
            ).map { row ->
                Sale(
                    id = row.id,
                    folio = row.folio,
                    total = row.total,
                    totalOriginal = row.total_original,
                    totalCosto = row.total_costo,
                    ganancia = row.ganancia,
                    pagoCon = row.pago_con,
                    cambio = row.cambio,
                    metodoPago = row.metodo_pago,
                    totalItems = row.total_items,
                    customerId = row.customer_id,
                    createdAt = row.created_at,
                    syncState = row.sync_state,
                    shiftId = row.shift_id,
                    cashierId = row.cashier_id,
                    cashierName = row.cashier_name,
                    estado = row.estado
                )
            }

            val ventasEfectivo = salesSummary.ventas_efectivo
            val fondoInicial = shiftDb.initial_cash
            val efectivoEsperado = fondoInicial + ventasEfectivo + totalEntradas - totalSalidas

            val shiftSummary = ShiftSummary(
                shift = shiftDb.toDomain(),
                totalVentas = salesSummary.total_ventas,
                ventasEfectivo = ventasEfectivo,
                ventasTarjeta = salesSummary.ventas_tarjeta,
                ventasTransferencia = salesSummary.ventas_transferencia,
                ventasCredito = salesSummary.ventas_credito,
                ventasMixto = salesSummary.ventas_mixto,
                totalEntradas = totalEntradas,
                totalSalidas = totalSalidas,
                efectivoEsperado = efectivoEsperado,
                totalTransacciones = salesSummary.total_transacciones,
                totalVentasCanceladas = cancelledSalesSummary.total_cancelado,
                totalTicketsCancelados = cancelledSalesSummary.total_tickets_cancelados,
                cancelledSales = cancelledSales,
                movements = movements
            )
            Result.success(shiftSummary)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun countActiveCashiers(): Long = withContext(Dispatchers.IO) {
        localDataSource.countActiveCashiers()
    }

    override suspend fun saveCashier(
        id: String?,
        nombre: String,
        pin: String
    ): Result<Cashier> = withContext(Dispatchers.IO) {
        try {
            val now = currentTimeMillis()
            val cashierId = if (id.isNullOrBlank()) generateUUID() else id
            val existing = id?.let { localDataSource.getCashierById(it) }

            val cashier = Cashiers(
                id = cashierId,
                nombre = nombre.trim(),
                pin = pin.trim(),
                activo = 1L,
                created_at = existing?.created_at ?: now,
                updated_at = now,
                sync_state = if (existing != null) "PENDING_UPDATE" else "PENDING_INSERT"
            )
            localDataSource.insertCashier(cashier)
            Result.success(cashier.toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteCashier(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val activeShift = getActiveShift()
            if (activeShift != null && activeShift.cashierId == id) {
                return@withContext Result.failure(IllegalStateException("No se puede eliminar el cajero porque tiene un turno abierto actualmente."))
            }

            val count = localDataSource.countActiveCashiers()
            if (count <= 1L) {
                return@withContext Result.failure(IllegalStateException("Debe existir al menos un cajero activo en el sistema."))
            }

            localDataSource.deactivateCashier(id, currentTimeMillis())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun Shifts.toDomain(): CashierShift = CashierShift(
        id = id,
        cashierId = cashier_id,
        cashierName = cashier_name,
        startTime = start_time,
        endTime = end_time,
        initialCash = initial_cash,
        finalCashExpected = final_cash_expected,
        finalCashCounted = final_cash_counted,
        difference = difference,
        notes = notes,
        isClosed = is_closed == 1L
    )

    private fun Cashiers.toDomain(): Cashier = Cashier(
        id = id,
        nombre = nombre,
        pin = pin,
        activo = activo == 1L,
        createdAt = created_at,
        updatedAt = updated_at
    )

    private fun Cash_movements.toDomain(): CashMovement = CashMovement(
        id = id,
        shiftId = shift_id,
        cashierId = cashier_id,
        tipo = if (tipo == CashMovementType.SALIDA.name) CashMovementType.SALIDA else CashMovementType.ENTRADA,
        monto = monto,
        motivo = motivo,
        createdAt = created_at
    )
}
