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
            CREATE TABLE IF NOT EXISTS customers (
                id             TEXT    PRIMARY KEY NOT NULL,
                nombre         TEXT    NOT NULL,
                telefono       TEXT    NOT NULL DEFAULT '',
                direccion      TEXT    NOT NULL DEFAULT '',
                notas          TEXT    NOT NULL DEFAULT '',
                limite_credito REAL    NOT NULL DEFAULT 0.0,
                activo         INTEGER NOT NULL DEFAULT 1,
                created_at     INTEGER NOT NULL,
                updated_at     INTEGER NOT NULL,
                sync_state     TEXT    NOT NULL DEFAULT 'PENDING_INSERT'
            );
            """.trimIndent(),
            0
        )
        driver.execute(null, "CREATE INDEX IF NOT EXISTS idx_customers_nombre ON customers(nombre);", 0)
        driver.execute(null, "CREATE INDEX IF NOT EXISTS idx_customers_activo ON customers(activo);", 0)

        driver.execute(
            null,
            """
            CREATE TABLE IF NOT EXISTS customer_payments (
                id          TEXT    PRIMARY KEY NOT NULL,
                customer_id TEXT    NOT NULL,
                monto       REAL    NOT NULL,
                metodo_pago TEXT    NOT NULL DEFAULT 'EFECTIVO',
                notas       TEXT    NOT NULL DEFAULT '',
                created_at  INTEGER NOT NULL,
                sync_state  TEXT    NOT NULL DEFAULT 'PENDING_INSERT',
                FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE
            );
            """.trimIndent(),
            0
        )
        driver.execute(null, "CREATE INDEX IF NOT EXISTS idx_customer_payments_customer_id ON customer_payments(customer_id);", 0)
        driver.execute(null, "CREATE INDEX IF NOT EXISTS idx_customer_payments_created_at ON customer_payments(created_at);", 0)
    } catch (e: Exception) {
        e.printStackTrace()
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
                customer_id    TEXT    DEFAULT NULL,
                created_at     INTEGER NOT NULL,
                sync_state     TEXT    NOT NULL DEFAULT 'PENDING_INSERT',
                FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE SET NULL
            );
            """.trimIndent(),
            0
        )
        driver.execute(null, "CREATE INDEX IF NOT EXISTS idx_sales_created_at ON sales(created_at);", 0)
    } catch (e: Exception) {
        e.printStackTrace()
    }

    try {
        driver.execute(null, "ALTER TABLE sales ADD COLUMN customer_id TEXT DEFAULT NULL;", 0)
    } catch (_: Exception) {
        // Column already exists
    }

    try {
        driver.execute(null, "CREATE INDEX IF NOT EXISTS idx_sales_customer_id ON sales(customer_id);", 0)
    } catch (_: Exception) {
    }

    try {
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
