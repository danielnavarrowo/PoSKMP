package com.dnavarro.poskmp.data.backup

import com.dnavarro.poskmp.data.SettingsRepository
import com.dnavarro.poskmp.util.currentTimeMillis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

interface BackupRepository {
    val backupDirectoryPathFlow: Flow<String>
    suspend fun getBackupDirectoryPath(): String
    fun getDefaultBackupDirectoryPath(): String
    suspend fun setBackupDirectoryPath(path: String)
    suspend fun resetBackupDirectoryPathToDefault()
    suspend fun performBackup(): Result<String>
    suspend fun checkAndPerformAutoBackup(): Result<Boolean>
}

class BackupRepositoryImpl(
    private val driver: DatabaseBackupDriver,
    private val settingsRepository: SettingsRepository
) : BackupRepository {

    override val backupDirectoryPathFlow: Flow<String> =
        settingsRepository.backupDirectoryPathFlow.map { customPath ->
            driver.getBackupDirectoryPath(customPath)
        }

    override suspend fun getBackupDirectoryPath(): String {
        val customPath = settingsRepository.backupDirectoryPathFlow.first()
        return driver.getBackupDirectoryPath(customPath)
    }

    override fun getDefaultBackupDirectoryPath(): String {
        return driver.getDefaultBackupDirectoryPath()
    }

    override suspend fun setBackupDirectoryPath(path: String) {
        settingsRepository.setBackupDirectoryPath(path.trim())
    }

    override suspend fun resetBackupDirectoryPathToDefault() {
        settingsRepository.setBackupDirectoryPath("")
    }

    override suspend fun performBackup(): Result<String> = withContext(Dispatchers.IO) {
        val customPath = settingsRepository.backupDirectoryPathFlow.first()
        val path = driver.getBackupDirectoryPath(customPath)
        if (path.isEmpty()) {
            return@withContext Result.failure(UnsupportedOperationException("Backups are desktop-only"))
        }
        val result = driver.createBackup(customPath)
        if (result.isSuccess) {
            settingsRepository.setLastBackupTimestamp(currentTimeMillis())
        }
        result
    }

    override suspend fun checkAndPerformAutoBackup(): Result<Boolean> = withContext(Dispatchers.IO) {
        val customPath = settingsRepository.backupDirectoryPathFlow.first()
        val path = driver.getBackupDirectoryPath(customPath)
        if (path.isEmpty()) {
            return@withContext Result.success(false)
        }
        val autoBackupEnabled = settingsRepository.autoBackupEnabledFlow.first()
        if (!autoBackupEnabled) {
            return@withContext Result.success(false)
        }
        val lastBackup = settingsRepository.lastBackupTimestampFlow.first()
        val now = currentTimeMillis()
        val dayInMillis = 86_400_000L
        if (lastBackup == 0L || (now - lastBackup) >= dayInMillis) {
            val backupResult = performBackup()
            if (backupResult.isSuccess) {
                Result.success(true)
            } else {
                Result.failure(backupResult.exceptionOrNull() ?: Exception("Error al crear la copia de seguridad automática"))
            }
        } else {
            Result.success(false)
        }
    }
}
