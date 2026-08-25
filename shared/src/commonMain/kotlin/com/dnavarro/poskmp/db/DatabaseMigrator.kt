package com.dnavarro.poskmp.db

import app.cash.sqldelight.db.SqlDriver

object DatabaseMigrator {
    fun migrate(driver: SqlDriver) {
        val createTableStatements = listOf(
            """
            CREATE TABLE IF NOT EXISTS products (
                id             TEXT    PRIMARY KEY NOT NULL,
                codigos        TEXT    NOT NULL DEFAULT '[]',
                nombre         TEXT    NOT NULL,
                precio         REAL    NOT NULL DEFAULT 0,
                costo          REAL    NOT NULL DEFAULT 0,
                categoria      TEXT    DEFAULT 'Sin categoria',
                activo         INTEGER NOT NULL DEFAULT 1,
                por_peso       INTEGER NOT NULL DEFAULT 0,
                precio_mayoreo REAL    NOT NULL DEFAULT 0,
                es_favorito    INTEGER NOT NULL DEFAULT 0,
                piezas         REAL    NOT NULL DEFAULT 1.0,
                updated_at     INTEGER NOT NULL,
                sync_state     TEXT    NOT NULL DEFAULT 'PENDING_INSERT'
            );
            """.trimIndent(),
            """
            CREATE TABLE IF NOT EXISTS customers (
                id              TEXT    PRIMARY KEY NOT NULL,
                nombre          TEXT    NOT NULL,
                telefono        TEXT    NOT NULL DEFAULT '',
                direccion       TEXT    NOT NULL DEFAULT '',
                notas           TEXT    NOT NULL DEFAULT '',
                limite_credito  REAL    NOT NULL DEFAULT 0.0,
                siempre_mayoreo INTEGER NOT NULL DEFAULT 0,
                activo          INTEGER NOT NULL DEFAULT 1,
                created_at      INTEGER NOT NULL,
                updated_at      INTEGER NOT NULL,
                sync_state      TEXT    NOT NULL DEFAULT 'PENDING_INSERT'
            );
            """.trimIndent(),
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
            """
            CREATE TABLE IF NOT EXISTS sales (
                id             TEXT    PRIMARY KEY NOT NULL,
                folio          INTEGER NOT NULL,
                total          REAL    NOT NULL,
                total_original REAL    NOT NULL DEFAULT 0,
                total_costo    REAL    NOT NULL DEFAULT 0,
                ganancia       REAL    NOT NULL DEFAULT 0,
                pago_con       REAL    NOT NULL DEFAULT 0,
                cambio         REAL    NOT NULL DEFAULT 0,
                metodo_pago    TEXT    NOT NULL DEFAULT 'EFECTIVO',
                total_items    REAL    NOT NULL DEFAULT 0,
                customer_id    TEXT    DEFAULT NULL,
                created_at     INTEGER NOT NULL,
                sync_state     TEXT    NOT NULL DEFAULT 'PENDING_INSERT',
                shift_id       TEXT    DEFAULT NULL,
                cashier_id     TEXT    DEFAULT NULL,
                cashier_name   TEXT    DEFAULT NULL,
                FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE SET NULL
            );
            """.trimIndent(),
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
            """
            CREATE TABLE IF NOT EXISTS deleted_sync_records (
                id          TEXT    PRIMARY KEY NOT NULL,
                table_name  TEXT    NOT NULL,
                record_id   TEXT    NOT NULL,
                deleted_at  INTEGER NOT NULL,
                sync_state  TEXT    NOT NULL DEFAULT 'PENDING_DELETE'
            );
            """.trimIndent(),
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
            """
            CREATE TABLE IF NOT EXISTS shifts (
                id                    TEXT    PRIMARY KEY NOT NULL,
                cashier_id            TEXT    NOT NULL,
                start_time            INTEGER NOT NULL,
                end_time              INTEGER DEFAULT NULL,
                fondo_inicial         REAL    NOT NULL DEFAULT 0.0,
                monto_ventas_efectivo REAL    NOT NULL DEFAULT 0.0,
                monto_ventas_otros    REAL    NOT NULL DEFAULT 0.0,
                entradas_efectivo     REAL    NOT NULL DEFAULT 0.0,
                salidas_efectivo      REAL    NOT NULL DEFAULT 0.0,
                efectivo_esperado     REAL    NOT NULL DEFAULT 0.0,
                efectivo_contado      REAL    DEFAULT NULL,
                diferencia            REAL    DEFAULT NULL,
                estado                TEXT    NOT NULL DEFAULT 'ABIERTO',
                notas                 TEXT    NOT NULL DEFAULT '',
                created_at            INTEGER NOT NULL,
                updated_at            INTEGER NOT NULL,
                sync_state            TEXT    NOT NULL DEFAULT 'PENDING_INSERT',
                FOREIGN KEY (cashier_id) REFERENCES cashiers(id)
            );
            """.trimIndent(),
            """
            CREATE TABLE IF NOT EXISTS cash_movements (
                id         TEXT    PRIMARY KEY NOT NULL,
                shift_id   TEXT    NOT NULL,
                tipo       TEXT    NOT NULL,
                monto      REAL    NOT NULL,
                motivo     TEXT    NOT NULL DEFAULT '',
                created_at INTEGER NOT NULL,
                sync_state TEXT    NOT NULL DEFAULT 'PENDING_INSERT',
                FOREIGN KEY (shift_id) REFERENCES shifts(id) ON DELETE CASCADE
            );
            """.trimIndent()
        )

        for (sql in createTableStatements) {
            try {
                driver.execute(null, sql, 0)
            } catch (_: Exception) {}
        }

        val alterStatements = listOf(
            "ALTER TABLE customers ADD COLUMN siempre_mayoreo INTEGER NOT NULL DEFAULT 0",
            "ALTER TABLE customers ADD COLUMN limite_credito REAL NOT NULL DEFAULT 0.0",
            "ALTER TABLE customers ADD COLUMN activo INTEGER NOT NULL DEFAULT 1",
            "ALTER TABLE customers ADD COLUMN telefono TEXT NOT NULL DEFAULT ''",
            "ALTER TABLE customers ADD COLUMN direccion TEXT NOT NULL DEFAULT ''",
            "ALTER TABLE customers ADD COLUMN notas TEXT NOT NULL DEFAULT ''",
            "ALTER TABLE sales ADD COLUMN shift_id TEXT DEFAULT NULL",
            "ALTER TABLE sales ADD COLUMN cashier_id TEXT DEFAULT NULL",
            "ALTER TABLE sales ADD COLUMN cashier_name TEXT DEFAULT NULL",
            "ALTER TABLE sales ADD COLUMN customer_id TEXT DEFAULT NULL",
            "ALTER TABLE products ADD COLUMN piezas REAL NOT NULL DEFAULT 1.0",
            "ALTER TABLE products ADD COLUMN precio_mayoreo REAL NOT NULL DEFAULT 0",
            "ALTER TABLE products ADD COLUMN es_favorito INTEGER NOT NULL DEFAULT 0",
            "ALTER TABLE products ADD COLUMN por_peso INTEGER NOT NULL DEFAULT 0",
            "ALTER TABLE products ADD COLUMN categoria TEXT DEFAULT 'Sin categoria'"
        )

        for (sql in alterStatements) {
            try {
                driver.execute(null, sql, 0)
            } catch (_: Exception) {}
        }

        val indexStatements = listOf(
            "CREATE INDEX IF NOT EXISTS idx_customers_activo ON customers(activo)",
            "CREATE INDEX IF NOT EXISTS idx_customer_payments_customer_id ON customer_payments(customer_id)",
            "CREATE INDEX IF NOT EXISTS idx_sales_created_at ON sales(created_at)",
            "CREATE INDEX IF NOT EXISTS idx_sales_customer_id ON sales(customer_id)",
            "CREATE INDEX IF NOT EXISTS idx_sales_shift_id ON sales(shift_id)",
            "CREATE INDEX IF NOT EXISTS idx_sale_items_sale_id ON sale_items(sale_id)",
            "CREATE INDEX IF NOT EXISTS idx_sale_items_product_id ON sale_items(product_id)",
            "CREATE INDEX IF NOT EXISTS idx_sale_items_created_at ON sale_items(created_at)",
            "CREATE INDEX IF NOT EXISTS idx_cash_movements_shift_id ON cash_movements(shift_id)",
            "CREATE INDEX IF NOT EXISTS idx_shifts_cashier_id ON shifts(cashier_id)",
            "CREATE INDEX IF NOT EXISTS idx_shifts_estado ON shifts(estado)",
            "CREATE INDEX IF NOT EXISTS idx_shifts_start_time ON shifts(start_time)"
        )

        for (sql in indexStatements) {
            try {
                driver.execute(null, sql, 0)
            } catch (_: Exception) {}
        }
    }
}
