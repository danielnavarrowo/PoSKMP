package com.dnavarro.poskmp.domain.usecase

import com.dnavarro.poskmp.data.SettingsRepository
import com.dnavarro.poskmp.db.AppDatabase
import com.dnavarro.poskmp.util.currentTimeMillis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ResetAppToFactoryDefaultsUseCase(
    private val settingsRepository: SettingsRepository,
    private val database: AppDatabase
) {
    suspend operator fun invoke(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // 1. Wipe all database tables within a transaction
            database.transaction {
                val queries = database.appDatabaseQueries
                queries.deleteAllCashMovements()
                queries.deleteAllShifts()
                queries.deleteAllCashiers()
                queries.deleteAllSaleItems()
                queries.deleteAllSales()
                queries.deleteAllCustomerPayments()
                queries.deleteAllCustomers()
                queries.deleteAllProducts()
                queries.deleteAllDeletedSyncRecords()

                // Re-seed default admin cashier
                val now = currentTimeMillis()
                queries.insertCashier(
                    id = "default-admin-cashier",
                    nombre = "Administrador",
                    pin = "0000",
                    activo = 1L,
                    created_at = now,
                    updated_at = now,
                    sync_state = "PENDING_INSERT"
                )
            }

            // 2. Clear all user preferences in DataStore back to defaults
            settingsRepository.clearAllSettings()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
