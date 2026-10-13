package com.dnavarro.poskmp.db

import app.cash.sqldelight.db.SqlDriver

expect class DatabaseDriverFactory() {
    fun createDriver(): SqlDriver
}

fun createDatabase(driverFactory: DatabaseDriverFactory): AppDatabase {
    val driver = driverFactory.createDriver()
    return createDatabase(driver)
}

fun createDatabase(driver: SqlDriver): AppDatabase {
    DatabaseMigrator.migrate(driver)
    return AppDatabase(driver)
}

