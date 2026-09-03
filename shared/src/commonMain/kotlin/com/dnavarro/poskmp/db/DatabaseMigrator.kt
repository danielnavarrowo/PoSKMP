package com.dnavarro.poskmp.db

import app.cash.sqldelight.db.QueryResult
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
                precio_delivery REAL   NOT NULL DEFAULT 0,
                updated_at     INTEGER NOT NULL DEFAULT 0,
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
                created_at      INTEGER NOT NULL DEFAULT 0,
                updated_at      INTEGER NOT NULL DEFAULT 0,
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
                estado         TEXT    NOT NULL DEFAULT 'COMPLETADA',
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
                es_delivery     INTEGER NOT NULL DEFAULT 0,
                created_at      INTEGER NOT NULL,
                FOREIGN KEY (sale_id) REFERENCES sales(id) ON DELETE CASCADE
            );
            """.trimIndent(),
            """
            CREATE TABLE IF NOT EXISTS deleted_sync_records (
                id          TEXT    PRIMARY KEY NOT NULL,
                entity_type TEXT    NOT NULL,
                deleted_at  INTEGER NOT NULL
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
            """
            CREATE TABLE IF NOT EXISTS cash_movements (
                id          TEXT    PRIMARY KEY NOT NULL,
                shift_id    TEXT    NOT NULL,
                cashier_id  TEXT    NOT NULL,
                tipo        TEXT    NOT NULL,
                monto       REAL    NOT NULL,
                motivo      TEXT    NOT NULL DEFAULT '',
                created_at  INTEGER NOT NULL,
                sync_state  TEXT    NOT NULL DEFAULT 'PENDING_INSERT',
                FOREIGN KEY (shift_id) REFERENCES shifts(id) ON DELETE CASCADE,
                FOREIGN KEY (cashier_id) REFERENCES cashiers(id) ON DELETE RESTRICT
            );
            """.trimIndent()
        )

        for (sql in createTableStatements) {
            try {
                driver.execute(null, sql, 0)
            } catch (_: Exception) {}
        }

        // Migrate products columns
        ensureColumnExists(driver, "products", "piezas", "REAL NOT NULL DEFAULT 1.0")
        ensureColumnExists(driver, "products", "precio_mayoreo", "REAL NOT NULL DEFAULT 0")
        ensureColumnExists(driver, "products", "precio_delivery", "REAL NOT NULL DEFAULT 0")
        ensureColumnExists(driver, "products", "es_favorito", "INTEGER NOT NULL DEFAULT 0")
        ensureColumnExists(driver, "products", "por_peso", "INTEGER NOT NULL DEFAULT 0")
        ensureColumnExists(driver, "products", "categoria", "TEXT DEFAULT 'Sin categoria'")
        ensureColumnExists(driver, "products", "updated_at", "INTEGER NOT NULL DEFAULT 0")
        ensureColumnExists(driver, "products", "sync_state", "TEXT NOT NULL DEFAULT 'SYNCED'")

        // Migrate customers columns
        ensureColumnExists(driver, "customers", "siempre_mayoreo", "INTEGER NOT NULL DEFAULT 0")
        ensureColumnExists(driver, "customers", "limite_credito", "REAL NOT NULL DEFAULT 0.0")
        ensureColumnExists(driver, "customers", "activo", "INTEGER NOT NULL DEFAULT 1")
        ensureColumnExists(driver, "customers", "telefono", "TEXT NOT NULL DEFAULT ''")
        ensureColumnExists(driver, "customers", "direccion", "TEXT NOT NULL DEFAULT ''")
        ensureColumnExists(driver, "customers", "notas", "TEXT NOT NULL DEFAULT ''")
        ensureColumnExists(driver, "customers", "created_at", "INTEGER NOT NULL DEFAULT 0")
        ensureColumnExists(driver, "customers", "updated_at", "INTEGER NOT NULL DEFAULT 0")
        ensureColumnExists(driver, "customers", "sync_state", "TEXT NOT NULL DEFAULT 'PENDING_INSERT'")

        // Migrate sales columns
        ensureColumnExists(driver, "sales", "shift_id", "TEXT DEFAULT NULL")
        ensureColumnExists(driver, "sales", "cashier_id", "TEXT DEFAULT NULL")
        ensureColumnExists(driver, "sales", "cashier_name", "TEXT DEFAULT NULL")
        ensureColumnExists(driver, "sales", "customer_id", "TEXT DEFAULT NULL")
        ensureColumnExists(driver, "sales", "estado", "TEXT NOT NULL DEFAULT 'COMPLETADA'")
        ensureColumnExists(driver, "sales", "sync_state", "TEXT NOT NULL DEFAULT 'PENDING_INSERT'")

        // Migrate sale_items columns
        ensureColumnExists(driver, "sale_items", "es_delivery", "INTEGER NOT NULL DEFAULT 0")

        // Ensure default cashier
        try {
            driver.execute(
                null,
                """
                INSERT INTO cashiers (id, nombre, pin, activo, created_at, updated_at, sync_state)
                SELECT 'default-cashier-001', 'Cajero Principal', '0000', 1, 0, 0, 'PENDING_INSERT'
                WHERE NOT EXISTS (SELECT 1 FROM cashiers);
                """.trimIndent(),
                0
            )
        } catch (_: Exception) {}

        // Indexes
        val indexStatements = listOf(
            "CREATE INDEX IF NOT EXISTS idx_products_activo ON products(activo)",
            "CREATE INDEX IF NOT EXISTS idx_products_es_favorito ON products(es_favorito)",
            "CREATE INDEX IF NOT EXISTS idx_products_categoria ON products(categoria)",
            "CREATE INDEX IF NOT EXISTS idx_customers_activo ON customers(activo)",
            "CREATE INDEX IF NOT EXISTS idx_customers_nombre ON customers(nombre)",
            "CREATE INDEX IF NOT EXISTS idx_customer_payments_customer_id ON customer_payments(customer_id)",
            "CREATE INDEX IF NOT EXISTS idx_customer_payments_created_at ON customer_payments(created_at)",
            "CREATE INDEX IF NOT EXISTS idx_sales_created_at ON sales(created_at)",
            "CREATE INDEX IF NOT EXISTS idx_sales_customer_id ON sales(customer_id)",
            "CREATE INDEX IF NOT EXISTS idx_sales_shift_id ON sales(shift_id)",
            "CREATE INDEX IF NOT EXISTS idx_sales_estado ON sales(estado)",
            "CREATE INDEX IF NOT EXISTS idx_sale_items_sale_id ON sale_items(sale_id)",
            "CREATE INDEX IF NOT EXISTS idx_sale_items_product_id ON sale_items(product_id)",
            "CREATE INDEX IF NOT EXISTS idx_sale_items_created_at ON sale_items(created_at)",
            "CREATE INDEX IF NOT EXISTS idx_cashiers_activo ON cashiers(activo)",
            "CREATE INDEX IF NOT EXISTS idx_shifts_is_closed ON shifts(is_closed)",
            "CREATE INDEX IF NOT EXISTS idx_shifts_start_time ON shifts(start_time)",
            "CREATE INDEX IF NOT EXISTS idx_cash_movements_shift_id ON cash_movements(shift_id)"
        )

        for (sql in indexStatements) {
            try {
                driver.execute(null, sql, 0)
            } catch (_: Exception) {}
        }
    }

    private fun getTableColumns(driver: SqlDriver, tableName: String): Set<String> {
        return try {
            val queryResult = driver.executeQuery(
                identifier = null,
                sql = "PRAGMA table_info($tableName);",
                mapper = { cursor ->
                    val columns = mutableSetOf<String>()
                    while (cursor.next().value) {
                        val colName = cursor.getString(1)
                        if (colName != null) {
                            columns.add(colName.lowercase())
                        }
                    }
                    QueryResult.Value(columns)
                },
                parameters = 0
            )
            queryResult.value
        } catch (_: Exception) {
            emptySet()
        }
    }

    private fun ensureColumnExists(
        driver: SqlDriver,
        tableName: String,
        columnName: String,
        columnDefinition: String
    ) {
        val existingColumns = getTableColumns(driver, tableName)
        if (existingColumns.isNotEmpty() && !existingColumns.contains(columnName.lowercase())) {
            try {
                driver.execute(null, "ALTER TABLE $tableName ADD COLUMN $columnName $columnDefinition;", 0)
            } catch (_: Exception) {}
        }
    }
}
