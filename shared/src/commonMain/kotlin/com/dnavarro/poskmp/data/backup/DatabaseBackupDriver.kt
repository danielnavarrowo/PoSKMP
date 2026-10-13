package com.dnavarro.poskmp.data.backup

import app.cash.sqldelight.db.SqlDriver

expect class DatabaseBackupDriver(driver: SqlDriver) {
    fun getDefaultBackupDirectoryPath(): String
    fun getBackupDirectoryPath(customPath: String? = null): String
    suspend fun createBackup(customPath: String? = null): Result<String>
    suspend fun getAvailableBackups(customPath: String? = null): List<BackupFileInfo>
    suspend fun restoreBackup(backupFilePath: String): Result<Unit>
}
