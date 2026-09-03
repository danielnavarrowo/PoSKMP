package com.dnavarro.poskmp.domain.usecase

import com.dnavarro.poskmp.data.SettingsRepository
import com.dnavarro.poskmp.db.AppDatabase
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
            }

            // 2. Clear all user preferences in DataStore back to defaults
            settingsRepository.clearAllSettings()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
