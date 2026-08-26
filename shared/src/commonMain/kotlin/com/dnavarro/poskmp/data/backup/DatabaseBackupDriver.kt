package com.dnavarro.poskmp.data.backup

expect class DatabaseBackupDriver() {
    fun getDefaultBackupDirectoryPath(): String
    fun getBackupDirectoryPath(customPath: String? = null): String
    suspend fun createBackup(customPath: String? = null): Result<String>
}
