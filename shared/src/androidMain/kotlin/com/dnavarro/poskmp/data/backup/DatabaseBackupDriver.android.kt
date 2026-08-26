package com.dnavarro.poskmp.data.backup

actual class DatabaseBackupDriver actual constructor() {
    actual fun getDefaultBackupDirectoryPath(): String = ""
    actual fun getBackupDirectoryPath(customPath: String?): String = ""

    actual suspend fun createBackup(customPath: String?): Result<String> =
        Result.failure(UnsupportedOperationException("Backups are desktop-only"))
}
