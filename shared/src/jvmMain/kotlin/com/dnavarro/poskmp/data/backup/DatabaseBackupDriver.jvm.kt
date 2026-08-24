package com.dnavarro.poskmp.data.backup

import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual class DatabaseBackupDriver actual constructor() {
    private val dbFile = File(System.getProperty("user.home"), ".poskmp/pos_database.db")
    private val backupDir = File(System.getProperty("user.home"), ".poskmp/backups")

    actual fun getBackupDirectoryPath(): String = backupDir.absolutePath

    actual suspend fun createBackup(): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (!backupDir.exists()) {
                backupDir.mkdirs()
            }
            if (!dbFile.exists()) {
                dbFile.parentFile?.mkdirs()
                dbFile.createNewFile()
            }

            val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))
            val targetFile = File(backupDir, "pos_database_backup_$timestamp.db")
            dbFile.copyTo(targetFile, overwrite = true)

            val walFile = File(dbFile.parentFile, "pos_database.db-wal")
            if (walFile.exists()) {
                walFile.copyTo(File(backupDir, "pos_database_backup_$timestamp.db-wal"), overwrite = true)
            }
            val shmFile = File(dbFile.parentFile, "pos_database.db-shm")
            if (shmFile.exists()) {
                shmFile.copyTo(File(backupDir, "pos_database_backup_$timestamp.db-shm"), overwrite = true)
            }

            cleanupOldBackups(MAX_BACKUPS)

            Result.success(targetFile.absolutePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun cleanupOldBackups(maxKeep: Int) {
        try {
            val dbFiles = backupDir.listFiles { file ->
                file.isFile && file.name.startsWith("pos_database_backup_") && file.name.endsWith(".db")
            }?.sortedByDescending { it.name } ?: return

            if (dbFiles.size > maxKeep) {
                val filesToDelete = dbFiles.drop(maxKeep)
                for (oldDb in filesToDelete) {
                    oldDb.delete()
                    val baseName = oldDb.name
                    File(backupDir, "$baseName-wal").takeIf { it.exists() }?.delete()
                    File(backupDir, "$baseName-shm").takeIf { it.exists() }?.delete()
                    File(backupDir, "${oldDb.nameWithoutExtension}.db-wal").takeIf { it.exists() }?.delete()
                    File(backupDir, "${oldDb.nameWithoutExtension}.db-shm").takeIf { it.exists() }?.delete()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        const val MAX_BACKUPS = 7
    }
}
