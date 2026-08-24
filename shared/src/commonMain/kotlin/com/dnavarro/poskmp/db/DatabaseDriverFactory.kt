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
    } catch (_: Exception) {}

    try {
        driver.execute(null, "ALTER TABLE products ADD COLUMN updated_at INTEGER NOT NULL DEFAULT 0;", 0)
    } catch (_: Exception) {}

    try {
        driver.execute(null, "ALTER TABLE products ADD COLUMN sync_state TEXT NOT NULL DEFAULT 'SYNCED';", 0)
    } catch (_: Exception) {}

    try {
        driver.execute(
            null,
            """
            CREATE TABLE IF NOT EXISTS deleted_sync_records (
                id          TEXT    PRIMARY KEY NOT NULL,
                entity_type TEXT    NOT NULL,
                deleted_at  INTEGER NOT NULL
            );
            """.trimIndent(),
            0
        )
    } catch (e: Exception) {
        e.printStackTrace()
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
        driver.execute(null, "ALTER TABLE sales ADD COLUMN shift_id TEXT DEFAULT NULL;", 0)
    } catch (_: Exception) {
    }

    try {
        driver.execute(null, "ALTER TABLE sales ADD COLUMN cashier_id TEXT DEFAULT NULL;", 0)
    } catch (_: Exception) {
    }

    try {
        driver.execute(null, "ALTER TABLE sales ADD COLUMN cashier_name TEXT DEFAULT NULL;", 0)
    } catch (_: Exception) {
    }

    try {
        driver.execute(null, "CREATE INDEX IF NOT EXISTS idx_sales_customer_id ON sales(customer_id);", 0)
    } catch (_: Exception) {
    }

    try {
        driver.execute(null, "CREATE INDEX IF NOT EXISTS idx_sales_shift_id ON sales(shift_id);", 0)
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

    try {
        driver.execute(
            null,
            """
            CREATE TABLE IF NOT EXISTS cashiers (
                id         TEXT    PRIMARY KEY NOT NULL,
                nombre     TEXT    NOT NULL,
                pin        TEXT    NOT NULL DEFAULT '0000',
                activo     INTEGER NOT NULL DEFAULT 1,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL,
                sync_state TEXT    NOT NULL DEFAULT 'PENDING_INSERT'
            );
            """.trimIndent(),
            0
        )
        driver.execute(null, "CREATE INDEX IF NOT EXISTS idx_cashiers_activo ON cashiers(activo);", 0)

        driver.execute(
            null,
            """
            INSERT INTO cashiers (id, nombre, pin, activo, created_at, updated_at, sync_state)
            SELECT 'default-cashier-001', 'Cajero Principal', '0000', 1, 0, 0, 'PENDING_INSERT'
            WHERE NOT EXISTS (SELECT 1 FROM cashiers);
            """.trimIndent(),
            0
        )
    } catch (e: Exception) {
        e.printStackTrace()
    }

    try {
        driver.execute(
            null,
            """
            CREATE TABLE IF NOT EXISTS shifts (
                id                   TEXT    PRIMARY KEY NOT NULL,
                cashier_id           TEXT    NOT NULL,
                cashier_name         TEXT    NOT NULL,
                start_time           INTEGER NOT NULL,
                end_time             INTEGER DEFAULT NULL,
                initial_cash         REAL    NOT NULL DEFAULT 0.0,
                final_cash_expected  REAL    DEFAULT NULL,
                final_cash_counted   REAL    DEFAULT NULL,
                difference           REAL    DEFAULT NULL,
                notes                TEXT    DEFAULT NULL,
                is_closed            INTEGER NOT NULL DEFAULT 0,
                sync_state           TEXT    NOT NULL DEFAULT 'PENDING_INSERT',
                FOREIGN KEY (cashier_id) REFERENCES cashiers(id) ON DELETE RESTRICT
            );
            """.trimIndent(),
            0
        )
        driver.execute(null, "CREATE INDEX IF NOT EXISTS idx_shifts_is_closed ON shifts(is_closed);", 0)
        driver.execute(null, "CREATE INDEX IF NOT EXISTS idx_shifts_start_time ON shifts(start_time);", 0)
    } catch (e: Exception) {
        e.printStackTrace()
    }

    try {
        driver.execute(
            null,
            """
            CREATE TABLE IF NOT EXISTS cash_movements (
                id          TEXT    PRIMARY KEY NOT NULL,
                shift_id    TEXT    NOT NULL,
                cashier_id  TEXT    NOT NULL,
                tipo        TEXT    NOT NULL,
                monto       REAL    NOT NULL,
                motivo      TEXT    NOT NULL,
                created_at  INTEGER NOT NULL,
                sync_state  TEXT    NOT NULL DEFAULT 'PENDING_INSERT',
                FOREIGN KEY (shift_id) REFERENCES shifts(id) ON DELETE CASCADE,
                FOREIGN KEY (cashier_id) REFERENCES cashiers(id) ON DELETE RESTRICT
            );
            """.trimIndent(),
            0
        )
        driver.execute(null, "CREATE INDEX IF NOT EXISTS idx_cash_movements_shift_id ON cash_movements(shift_id);", 0)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
