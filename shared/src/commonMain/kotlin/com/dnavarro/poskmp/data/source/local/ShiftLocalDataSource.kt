package com.dnavarro.poskmp.data.source.local

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.dnavarro.poskmp.db.AppDatabase
import com.dnavarro.poskmp.db.Cash_movements
import com.dnavarro.poskmp.db.Cashiers
import com.dnavarro.poskmp.db.Sales
import com.dnavarro.poskmp.db.SelectCancelledSalesSummaryByShift
import com.dnavarro.poskmp.db.SelectSalesSummaryByShift
import com.dnavarro.poskmp.db.Shifts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

interface ShiftLocalDataSource {
    fun getActiveShiftFlow(): Flow<Shifts?>
    suspend fun getActiveShift(): Shifts?
    suspend fun getShiftById(id: String): Shifts?
    suspend fun getAllShifts(): List<Shifts>
    suspend fun getShiftsBetween(startTime: Long, endTime: Long): List<Shifts>
    suspend fun insertShift(shift: Shifts)
    suspend fun closeShift(
        id: String,
        endTime: Long,
        expectedCash: Double,
        countedCash: Double,
        difference: Double,
        notes: String?
    )

    fun getAllActiveCashiersFlow(): Flow<List<Cashiers>>
    suspend fun getAllActiveCashiers(): List<Cashiers>
    suspend fun getCashierById(id: String): Cashiers?
    suspend fun countCashiers(): Long
    suspend fun countActiveCashiers(): Long
    suspend fun insertCashier(cashier: Cashiers)
    suspend fun deactivateCashier(id: String, updatedAt: Long)

    suspend fun insertCashMovement(movement: Cash_movements)
    fun getMovementsByShiftIdFlow(shiftId: String): Flow<List<Cash_movements>>
    suspend fun getMovementsByShiftId(shiftId: String): List<Cash_movements>
    suspend fun getSumMovementsByShiftAndType(shiftId: String, tipo: String): Double
    suspend fun getSalesSummaryByShift(shiftId: String, startTime: Long, endTime: Long?): SelectSalesSummaryByShift
    suspend fun getCancelledSalesSummaryByShift(shiftId: String, startTime: Long, endTime: Long?): SelectCancelledSalesSummaryByShift
    suspend fun getCancelledSalesByShift(shiftId: String, startTime: Long, endTime: Long?): List<Sales>
}

class SqlDelightShiftDataSource(
    database: AppDatabase
) : ShiftLocalDataSource {
    private val queries = database.appDatabaseQueries

    override fun getActiveShiftFlow(): Flow<Shifts?> =
        queries.selectActiveShift().asFlow().mapToOneOrNull(Dispatchers.IO)

    override suspend fun getActiveShift(): Shifts? = withContext(Dispatchers.IO) {
        queries.selectActiveShift().executeAsOneOrNull()
    }

    override suspend fun getShiftById(id: String): Shifts? = withContext(Dispatchers.IO) {
        queries.selectShiftById(id).executeAsOneOrNull()
    }

    override suspend fun getAllShifts(): List<Shifts> = withContext(Dispatchers.IO) {
        queries.selectAllShifts().executeAsList()
    }

    override suspend fun getShiftsBetween(startTime: Long, endTime: Long): List<Shifts> = withContext(Dispatchers.IO) {
        queries.selectShiftsBetween(startTime, endTime).executeAsList()
    }

    override suspend fun insertShift(shift: Shifts): Unit = withContext(Dispatchers.IO) {
        queries.insertShift(
            id = shift.id,
            cashier_id = shift.cashier_id,
            cashier_name = shift.cashier_name,
            start_time = shift.start_time,
            end_time = shift.end_time,
            initial_cash = shift.initial_cash,
            final_cash_expected = shift.final_cash_expected,
            final_cash_counted = shift.final_cash_counted,
            difference = shift.difference,
            notes = shift.notes,
            is_closed = shift.is_closed,
            sync_state = shift.sync_state
        )
    }

    override suspend fun closeShift(
        id: String,
        endTime: Long,
        expectedCash: Double,
        countedCash: Double,
        difference: Double,
        notes: String?
    ): Unit = withContext(Dispatchers.IO) {
        queries.closeShift(
            end_time = endTime,
            final_cash_expected = expectedCash,
            final_cash_counted = countedCash,
            difference = difference,
            notes = notes,
            id = id
        )
    }

    override fun getAllActiveCashiersFlow(): Flow<List<Cashiers>> =
        queries.selectAllActiveCashiers().asFlow().mapToList(Dispatchers.IO)

    override suspend fun getAllActiveCashiers(): List<Cashiers> = withContext(Dispatchers.IO) {
        queries.selectAllActiveCashiers().executeAsList()
    }

    override suspend fun getCashierById(id: String): Cashiers? = withContext(Dispatchers.IO) {
        queries.selectCashierById(id).executeAsOneOrNull()
    }

    override suspend fun countCashiers(): Long = withContext(Dispatchers.IO) {
        queries.countCashiers().executeAsOne()
    }

    override suspend fun countActiveCashiers(): Long = withContext(Dispatchers.IO) {
        queries.countActiveCashiers().executeAsOne()
    }

    override suspend fun insertCashier(cashier: Cashiers): Unit = withContext(Dispatchers.IO) {
        queries.insertCashier(
            id = cashier.id,
            nombre = cashier.nombre,
            pin = cashier.pin,
            activo = cashier.activo,
            created_at = cashier.created_at,
            updated_at = cashier.updated_at,
            sync_state = cashier.sync_state
        )
    }

    override suspend fun deactivateCashier(id: String, updatedAt: Long): Unit = withContext(Dispatchers.IO) {
        queries.deactivateCashier(
            updated_at = updatedAt,
            id = id
        )
    }

    override suspend fun insertCashMovement(movement: Cash_movements): Unit = withContext(Dispatchers.IO) {
        queries.insertCashMovement(
            id = movement.id,
            shift_id = movement.shift_id,
            cashier_id = movement.cashier_id,
            tipo = movement.tipo,
            monto = movement.monto,
            motivo = movement.motivo,
            created_at = movement.created_at,
            sync_state = movement.sync_state
        )
    }

    override fun getMovementsByShiftIdFlow(shiftId: String): Flow<List<Cash_movements>> =
        queries.selectMovementsByShiftId(shiftId).asFlow().mapToList(Dispatchers.IO)

    override suspend fun getMovementsByShiftId(shiftId: String): List<Cash_movements> = withContext(Dispatchers.IO) {
        queries.selectMovementsByShiftId(shiftId).executeAsList()
    }

    override suspend fun getSumMovementsByShiftAndType(shiftId: String, tipo: String): Double = withContext(Dispatchers.IO) {
        queries.sumMovementsByShiftAndType(shiftId, tipo).executeAsOne()
    }

    override suspend fun getSalesSummaryByShift(
        shiftId: String,
        startTime: Long,
        endTime: Long?
    ): SelectSalesSummaryByShift = withContext(Dispatchers.IO) {
        queries.selectSalesSummaryByShift(
            shiftId = shiftId,
            startTime = startTime,
            endTime = endTime
        ).executeAsOne()
    }

    override suspend fun getCancelledSalesSummaryByShift(
        shiftId: String,
        startTime: Long,
        endTime: Long?
    ): SelectCancelledSalesSummaryByShift = withContext(Dispatchers.IO) {
        queries.selectCancelledSalesSummaryByShift(
            shiftId = shiftId,
            startTime = startTime,
            endTime = endTime
        ).executeAsOne()
    }

    override suspend fun getCancelledSalesByShift(
        shiftId: String,
        startTime: Long,
        endTime: Long?
    ): List<Sales> = withContext(Dispatchers.IO) {
        queries.selectCancelledSalesByShift(
            shiftId = shiftId,
            startTime = startTime,
            endTime = endTime
        ).executeAsList()
    }
}
