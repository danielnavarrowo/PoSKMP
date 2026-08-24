package com.dnavarro.poskmp.data.backup

expect class DatabaseBackupDriver() {
    fun getBackupDirectoryPath(): String
    suspend fun createBackup(): Result<String>
}
