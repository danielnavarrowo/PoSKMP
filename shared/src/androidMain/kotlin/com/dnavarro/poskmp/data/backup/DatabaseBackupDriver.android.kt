package com.dnavarro.poskmp.data.backup

actual class DatabaseBackupDriver actual constructor() {
    actual fun getBackupDirectoryPath(): String = ""

    actual suspend fun createBackup(): Result<String> =
        Result.failure(UnsupportedOperationException("Backups are desktop-only"))
}
