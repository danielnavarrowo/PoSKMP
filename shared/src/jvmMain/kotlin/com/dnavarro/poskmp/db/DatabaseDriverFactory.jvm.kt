package com.dnavarro.poskmp.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File

actual class DatabaseDriverFactory actual constructor() {
    actual fun createDriver(): SqlDriver {
        val databasePath = File(System.getProperty("user.home"), ".poskmp/pos_database.db")
        val dbFileExists = databasePath.exists()
        if (!dbFileExists) {
            databasePath.parentFile.mkdirs()
        }
        val driver: SqlDriver = JdbcSqliteDriver("jdbc:sqlite:${databasePath.absolutePath}")
        if (!dbFileExists) {
            AppDatabase.Schema.create(driver)
        }
        DatabaseMigrator.migrate(driver)
        return driver
    }
}
