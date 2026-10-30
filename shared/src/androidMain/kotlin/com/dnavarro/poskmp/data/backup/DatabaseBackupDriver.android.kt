package com.dnavarro.poskmp.data.backup

import app.cash.sqldelight.db.SqlDriver

actual class DatabaseBackupDriver actual constructor(private val driver: SqlDriver) {
    @Suppress("SameReturnValue")
    actual fun getDefaultBackupDirectoryPath(): String = ""

    @Suppress("SameReturnValue")
    actual fun getBackupDirectoryPath(customPath: String?): String = ""

    actual suspend fun createBackup(customPath: String?): Result<String> =
        Result.failure(UnsupportedOperationException("Backups are desktop-only"))

    actual fun createShutdownBackupIfNeeded(customPath: String?): Result<String?> =
        Result.success(null)

    actual suspend fun getAvailableBackups(customPath: String?): List<BackupFileInfo> = emptyList()

    actual suspend fun restoreBackup(backupFilePath: String): Result<Unit> =
        Result.failure(UnsupportedOperationException("Restores are desktop-only"))
}
