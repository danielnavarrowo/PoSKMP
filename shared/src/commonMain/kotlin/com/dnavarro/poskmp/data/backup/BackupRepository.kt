package com.dnavarro.poskmp.data.backup

import com.dnavarro.poskmp.data.SettingsRepository
import com.dnavarro.poskmp.util.currentTimeMillis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.milliseconds

interface BackupRepository {
    val backupDirectoryPathFlow: Flow<String>
    suspend fun getBackupDirectoryPath(): String
    fun getDefaultBackupDirectoryPath(): String
    suspend fun setBackupDirectoryPath(path: String)
    suspend fun resetBackupDirectoryPathToDefault()
    suspend fun performBackup(): Result<String>
    fun performShutdownBackupIfNeeded(): Result<String?>
    suspend fun checkAndPerformAutoBackup(): Result<Boolean>
    suspend fun getAvailableBackups(): List<BackupFileInfo>
    suspend fun restoreBackup(filePath: String): Result<Unit>
}

class BackupRepositoryImpl(
    private val driver: DatabaseBackupDriver,
    private val settingsRepository: SettingsRepository
) : BackupRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var cachedAutoBackupEnabled: Boolean = true

    @Volatile
    private var cachedCustomBackupPath: String = ""

    init {
        scope.launch {
            settingsRepository.autoBackupEnabledFlow.collect { enabled ->
                cachedAutoBackupEnabled = enabled
            }
        }
        scope.launch {
            settingsRepository.backupDirectoryPathFlow.collect { customPath ->
                cachedCustomBackupPath = customPath
            }
        }
    }

    override val backupDirectoryPathFlow: Flow<String> =
        settingsRepository.backupDirectoryPathFlow.map { customPath ->
            driver.getBackupDirectoryPath(customPath)
        }

    override suspend fun getBackupDirectoryPath(): String {
        val customPath = settingsRepository.backupDirectoryPathFlow.first()
        cachedCustomBackupPath = customPath
        return driver.getBackupDirectoryPath(customPath)
    }

    override fun getDefaultBackupDirectoryPath(): String {
        return driver.getDefaultBackupDirectoryPath()
    }

    override suspend fun setBackupDirectoryPath(path: String) {
        val trimmed = path.trim()
        cachedCustomBackupPath = trimmed
        settingsRepository.setBackupDirectoryPath(trimmed)
    }

    override suspend fun resetBackupDirectoryPathToDefault() {
        cachedCustomBackupPath = ""
        settingsRepository.setBackupDirectoryPath("")
    }

    override suspend fun performBackup(): Result<String> = withContext(Dispatchers.IO) {
        val customPath = settingsRepository.backupDirectoryPathFlow.first()
        cachedCustomBackupPath = customPath
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

    override fun performShutdownBackupIfNeeded(): Result<String?> {
        if (!cachedAutoBackupEnabled) {
            return Result.success(null)
        }
        val customPath = cachedCustomBackupPath
        val path = driver.getBackupDirectoryPath(customPath)
        if (path.isEmpty()) {
            return Result.success(null)
        }
        val result = driver.createShutdownBackupIfNeeded(customPath)
        if (result.isSuccess && result.getOrNull() != null) {
            try {
                runBlocking {
                    withTimeoutOrNull(500.milliseconds) {
                        settingsRepository.setLastBackupTimestamp(currentTimeMillis())
                    }
                }
            } catch (_: Throwable) {
            }
        }
        return result
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

    override suspend fun getAvailableBackups(): List<BackupFileInfo> = withContext(Dispatchers.IO) {
        val customPath = settingsRepository.backupDirectoryPathFlow.first()
        driver.getAvailableBackups(customPath)
    }

    override suspend fun restoreBackup(filePath: String): Result<Unit> = withContext(Dispatchers.IO) {
        val customPath = settingsRepository.backupDirectoryPathFlow.first()
        try {
            driver.createBackup(customPath)
        } catch (_: Exception) {}

        driver.restoreBackup(filePath)
    }
}
