package com.dnavarro.poskmp.db

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

actual class DatabaseDriverFactory actual constructor() {
    actual fun createDriver(): SqlDriver {
        val context = appContext ?: throw IllegalStateException("DatabaseDriverFactory.appContext must be initialized on Android")
        val driver = AndroidSqliteDriver(AppDatabase.Schema, context, "pos_database.db")
        DatabaseMigrator.migrate(driver)
        return driver
    }

    companion object {
        var appContext: Context? = null
    }
}
