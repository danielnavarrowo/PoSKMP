package com.dnavarro.poskmp.db

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

actual class DatabaseDriverFactory actual constructor() {
    actual fun createDriver(): SqlDriver {
        val context = appContext ?: throw IllegalStateException("DatabaseDriverFactory.appContext must be initialized on Android")
        return AndroidSqliteDriver(AppDatabase.Schema, context, "pos_database.db")
    }

    companion object {
        var appContext: Context? = null
    }
}
