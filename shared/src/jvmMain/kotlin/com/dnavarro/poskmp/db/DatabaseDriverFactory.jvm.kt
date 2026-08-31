package com.dnavarro.poskmp.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File
import java.util.Properties

actual class DatabaseDriverFactory actual constructor() {
    actual fun createDriver(): SqlDriver {
        val databasePath = File(System.getProperty("user.home"), ".poskmp/pos_database.db")
        val dbFileExists = databasePath.exists()
        if (!dbFileExists) {
            databasePath.parentFile.mkdirs()
        }
        val properties = Properties().apply {
            put("journal_mode", "WAL")
            put("busy_timeout", "30000")
            put("synchronous", "NORMAL")
            put("foreign_keys", "ON")
        }
        val driver: SqlDriver = JdbcSqliteDriver(
            url = "jdbc:sqlite:${databasePath.absolutePath}",
            properties = properties
        )
        if (!dbFileExists) {
            AppDatabase.Schema.create(driver)
        }
        driver.execute(null, "PRAGMA journal_mode = WAL;", 0)
        driver.execute(null, "PRAGMA busy_timeout = 30000;", 0)
        driver.execute(null, "PRAGMA synchronous = NORMAL;", 0)
        driver.execute(null, "PRAGMA foreign_keys = ON;", 0)

        DatabaseMigrator.migrate(driver)
        return driver
    }
}
