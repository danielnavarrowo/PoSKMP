package com.dnavarro.poskmp.db

import app.cash.sqldelight.db.SqlDriver

expect class DatabaseDriverFactory() {
    fun createDriver(): SqlDriver
}

fun createDatabase(driverFactory: DatabaseDriverFactory): AppDatabase {
    val driver = driverFactory.createDriver()
    ensureTablesExist(driver)
    return AppDatabase(driver)
}

private fun ensureTablesExist(driver: SqlDriver) {
    try {
        driver.execute(null, "ALTER TABLE products ADD COLUMN piezas REAL NOT NULL DEFAULT 1.0;", 0)
    } catch (_: Exception) {
        // Column already exists
    }

    try {
        driver.execute(
            null,
            """
            CREATE TABLE IF NOT EXISTS sales (
                id             TEXT    PRIMARY KEY NOT NULL,
                folio          INTEGER NOT NULL,
                total          REAL    NOT NULL,
                total_original REAL    NOT NULL,
                total_costo    REAL    NOT NULL,
                ganancia       REAL    NOT NULL,
                pago_con       REAL    NOT NULL,
                cambio         REAL    NOT NULL,
                metodo_pago    TEXT    NOT NULL DEFAULT 'EFECTIVO',
                total_items    REAL    NOT NULL DEFAULT 0,
                created_at     INTEGER NOT NULL,
                sync_state     TEXT    NOT NULL DEFAULT 'PENDING_INSERT'
            );
            """.trimIndent(),
            0
        )
        driver.execute(null, "CREATE INDEX IF NOT EXISTS idx_sales_created_at ON sales(created_at);", 0)

        driver.execute(
            null,
            """
            CREATE TABLE IF NOT EXISTS sale_items (
                id              TEXT    PRIMARY KEY NOT NULL,
                sale_id         TEXT    NOT NULL,
                product_id      TEXT,
                product_nombre  TEXT    NOT NULL,
                cantidad        REAL    NOT NULL,
                precio_unitario REAL    NOT NULL,
                costo_unitario  REAL    NOT NULL,
                subtotal        REAL    NOT NULL,
                ganancia        REAL    NOT NULL,
                es_mayoreo      INTEGER NOT NULL DEFAULT 0,
                created_at      INTEGER NOT NULL,
                FOREIGN KEY (sale_id) REFERENCES sales(id) ON DELETE CASCADE
            );
            """.trimIndent(),
            0
        )
        driver.execute(null, "CREATE INDEX IF NOT EXISTS idx_sale_items_sale_id ON sale_items(sale_id);", 0)
        driver.execute(null, "CREATE INDEX IF NOT EXISTS idx_sale_items_product_id ON sale_items(product_id);", 0)
        driver.execute(null, "CREATE INDEX IF NOT EXISTS idx_sale_items_created_at ON sale_items(created_at);", 0)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
