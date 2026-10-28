package com.dnavarro.poskmp.data.backup

import app.cash.sqldelight.db.SqlDriver
import com.dnavarro.poskmp.db.DatabaseMigrator
import java.io.File
import java.sql.DriverManager
import java.sql.Statement
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Properties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual class DatabaseBackupDriver actual constructor(private val driver: SqlDriver) {
    private val dbFile = File(System.getProperty("user.home"), ".poskmp/pos_database.db")
    private val defaultBackupDir = File(System.getProperty("user.home"), ".poskmp/backups")
    private val backupLock = Any()

    @Volatile
    private var lastBackupCompletedAtMillis: Long = 0L

    actual fun getDefaultBackupDirectoryPath(): String = defaultBackupDir.absolutePath

    actual fun getBackupDirectoryPath(customPath: String?): String {
        if (!customPath.isNullOrBlank()) {
            return File(customPath.trim()).absolutePath
        }
        return defaultBackupDir.absolutePath
    }

    actual suspend fun createBackup(customPath: String?): Result<String> = withContext(Dispatchers.IO) {
        createBackupSync(customPath)
    }

    /**
     * Executes a synchronous backup safe to call from a JVM shutdown hook or external close handler.
     * Acquires [backupLock] so that if an asynchronous backup is already running on [Dispatchers.IO],
     * the shutdown thread waits for it to finish cleanly before the JVM exits. If a backup already
     * completed within the last 15 seconds (e.g., during the normal exit flow), it skips creating a duplicate.
     */
    actual fun createShutdownBackupIfNeeded(customPath: String?): Result<String?> = synchronized(backupLock) {
        val elapsed = System.currentTimeMillis() - lastBackupCompletedAtMillis
        if (lastBackupCompletedAtMillis > 0L && elapsed in 0..15_000L) {
            return Result.success(null)
        }
        createBackupSync(customPath).map { it }
    }

    private fun createBackupSync(customPath: String?): Result<String> = synchronized(backupLock) {
        try {
            val targetDir = if (!customPath.isNullOrBlank()) File(customPath.trim()) else defaultBackupDir
            if (!targetDir.exists()) {
                targetDir.mkdirs()
            }
            if (!dbFile.exists()) {
                dbFile.parentFile?.mkdirs()
                dbFile.createNewFile()
            }

            try {
                driver.execute(null, "PRAGMA wal_checkpoint(TRUNCATE);", 0)
            } catch (_: Exception) {
                try {
                    DriverManager.getConnection("jdbc:sqlite:${dbFile.absolutePath}").use { conn ->
                        conn.createStatement().use { stmt ->
                            stmt.execute("PRAGMA wal_checkpoint(TRUNCATE);")
                        }
                    }
                } catch (_: Exception) {}
            }

            val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))
            val targetFile = File(targetDir, "pos_database_backup_$timestamp.db")
            dbFile.copyTo(targetFile, overwrite = true)

            val walFile = File(dbFile.parentFile, "pos_database.db-wal")
            if (walFile.exists()) {
                walFile.copyTo(File(targetDir, "pos_database_backup_$timestamp.db-wal"), overwrite = true)
            }
            val shmFile = File(dbFile.parentFile, "pos_database.db-shm")
            if (shmFile.exists()) {
                shmFile.copyTo(File(targetDir, "pos_database_backup_$timestamp.db-shm"), overwrite = true)
            }

            cleanupOldBackups(targetDir, MAX_BACKUPS)
            lastBackupCompletedAtMillis = System.currentTimeMillis()

            Result.success(targetFile.absolutePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    actual suspend fun getAvailableBackups(customPath: String?): List<BackupFileInfo> = withContext(Dispatchers.IO) {
        try {
            val targetDir = if (!customPath.isNullOrBlank()) File(customPath.trim()) else defaultBackupDir
            if (!targetDir.exists() || !targetDir.isDirectory) {
                return@withContext emptyList()
            }

            val dbFiles = targetDir.listFiles { file ->
                file.isFile && file.name.endsWith(".db")
            } ?: return@withContext emptyList()

            dbFiles
                .sortedByDescending { it.lastModified() }
                .map { file ->
                    BackupFileInfo(
                        fileName = file.name,
                        filePath = file.absolutePath,
                        sizeBytes = file.length(),
                        lastModifiedEpochMillis = file.lastModified()
                    )
                }
        } catch (_: Exception) {
            emptyList()
        }
    }

    actual suspend fun restoreBackup(backupFilePath: String): Result<Unit> = withContext(Dispatchers.IO) {
        val backupFile = File(backupFilePath)
        if (!backupFile.exists() || !backupFile.isFile) {
            return@withContext Result.failure(IllegalArgumentException("El archivo de respaldo no existe: $backupFilePath"))
        }
        if (!backupFile.name.endsWith(".db")) {
            return@withContext Result.failure(IllegalArgumentException("El archivo no es una base de datos válida (.db)"))
        }

        try {
            val properties = Properties().apply {
                put("journal_mode", "WAL")
                put("busy_timeout", "30000")
                put("synchronous", "NORMAL")
            }

            // Perform in-place restore using a dedicated JDBC connection to ensure
            // ATTACH, transactions, and table updates run within the same persistent connection session.
            DriverManager.getConnection("jdbc:sqlite:${dbFile.absolutePath}", properties).use { conn ->
                conn.autoCommit = true

                // 1. Truncate WAL & disable foreign keys
                conn.createStatement().use { stmt ->
                    stmt.execute("PRAGMA wal_checkpoint(TRUNCATE);")
                    stmt.execute("PRAGMA foreign_keys = OFF;")
                    val escapedPath = backupFile.absolutePath.replace("'", "''")
                    stmt.execute("ATTACH DATABASE '$escapedPath' AS backup_db;")
                }

                try {
                    val tablesToRestore = listOf(
                        "products",
                        "customers",
                        "customer_payments",
                        "cashiers",
                        "shifts",
                        "cash_movements",
                        "sales",
                        "sale_items",
                        "product_barcodes",
                        "deleted_sync_records"
                    )

                    // 2. Discover column intersection for each table
                    val plan = mutableListOf<Pair<String, String>>()
                    conn.createStatement().use { stmt ->
                        for (table in tablesToRestore) {
                            val mainCols = getTableColumns(stmt, "main", table)
                            val backupCols = getTableColumns(stmt, "backup_db", table)
                            if (mainCols.isNotEmpty() && backupCols.isNotEmpty()) {
                                val commonCols = mainCols.intersect(backupCols)
                                if (commonCols.isNotEmpty()) {
                                    plan.add(table to commonCols.joinToString(", "))
                                }
                            }
                        }
                    }

                    // 3. Atomically clear and restore tables in a transaction
                    conn.autoCommit = false
                    try {
                        conn.createStatement().use { stmt ->
                            // Delete in reverse order (children before parents)
                            for (table in tablesToRestore.reversed()) {
                                stmt.execute("DELETE FROM main.$table;")
                            }
                            // Insert in order (parents before children)
                            for ((table, colsList) in plan) {
                                stmt.execute("INSERT INTO main.$table ($colsList) SELECT $colsList FROM backup_db.$table;")
                            }
                        }
                        conn.commit()
                    } catch (t: Throwable) {
                        try {
                            conn.rollback()
                        } catch (_: Exception) {}
                        throw t
                    } finally {
                        conn.autoCommit = true
                    }
                } finally {
                    // 4. Detach backup database, re-enable foreign keys, and checkpoint WAL
                    try {
                        conn.createStatement().use { stmt ->
                            try {
                                stmt.execute("DETACH DATABASE backup_db;")
                            } catch (_: Exception) {}
                            try {
                                stmt.execute("PRAGMA foreign_keys = ON;")
                            } catch (_: Exception) {}
                            try {
                                stmt.execute("PRAGMA wal_checkpoint(TRUNCATE);")
                            } catch (_: Exception) {}
                        }
                    } catch (_: Exception) {}
                }
            }

            // 5. Reconcile schema and indexes (in case the restored backup was from an older version)
            DatabaseMigrator.migrate(driver)

            // 6. Notify SqlDelight listeners so active Compose UI flows refresh immediately
            driver.notifyListeners(
                "products",
                "product_barcodes",
                "customers",
                "customer_payments",
                "sales",
                "sale_items",
                "deleted_sync_records",
                "cashiers",
                "shifts",
                "cash_movements"
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun getTableColumns(stmt: Statement, schema: String, tableName: String): Set<String> {
        val columns = mutableSetOf<String>()
        try {
            stmt.executeQuery("PRAGMA $schema.table_info($tableName);").use { rs ->
                while (rs.next()) {
                    val colName = rs.getString(2)
                    if (colName != null) {
                        columns.add(colName.lowercase())
                    }
                }
            }
        } catch (_: Exception) {}
        return columns
    }

    private fun cleanupOldBackups(backupDir: File, maxKeep: Int) {
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
