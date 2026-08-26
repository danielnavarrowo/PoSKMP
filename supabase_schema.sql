-- =========================================================================
-- PoSKMP - ESQUEMA DE BASE DE DATOS PARA SUPABASE (POSTGRESQL)
-- =========================================================================
-- Ejecuta este script en el Editor SQL de tu proyecto en Supabase (SQL Editor).

-- 1. TABLA: products (Catálogo de Productos)
CREATE TABLE IF NOT EXISTS public.products (
    id             TEXT PRIMARY KEY,
    codigos        TEXT NOT NULL DEFAULT '[]',
    nombre         TEXT NOT NULL,
    precio         NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    costo          NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    categoria      TEXT DEFAULT 'Sin categoría',
    activo         BOOLEAN NOT NULL DEFAULT true,
    por_peso       BOOLEAN NOT NULL DEFAULT false,
    precio_mayoreo NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    es_favorito    BOOLEAN NOT NULL DEFAULT false,
    piezas         NUMERIC(10, 3) NOT NULL DEFAULT 1.000,
    updated_at     BIGINT NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

CREATE INDEX IF NOT EXISTS idx_products_updated_at ON public.products(updated_at);
CREATE INDEX IF NOT EXISTS idx_products_activo ON public.products(activo);

-- 2. TABLA: customers (Directorio de Clientes)
CREATE TABLE IF NOT EXISTS public.customers (
    id              TEXT PRIMARY KEY,
    nombre          TEXT NOT NULL,
    telefono        TEXT NOT NULL DEFAULT '',
    direccion       TEXT NOT NULL DEFAULT '',
    notas           TEXT NOT NULL DEFAULT '',
    limite_credito  NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    siempre_mayoreo BOOLEAN NOT NULL DEFAULT false,
    activo          BOOLEAN NOT NULL DEFAULT true,
    created_at      BIGINT NOT NULL,
    updated_at      BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_customers_updated_at ON public.customers(updated_at);

-- 3. TABLA: customer_payments (Abonos a Deuda de Clientes)
CREATE TABLE IF NOT EXISTS public.customer_payments (
    id          TEXT PRIMARY KEY,
    customer_id TEXT NOT NULL REFERENCES public.customers(id) ON DELETE CASCADE,
    monto       NUMERIC(12, 2) NOT NULL,
    metodo_pago TEXT NOT NULL DEFAULT 'EFECTIVO',
    notas       TEXT NOT NULL DEFAULT '',
    created_at  BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_customer_payments_created_at ON public.customer_payments(created_at);
CREATE INDEX IF NOT EXISTS idx_customer_payments_customer_id ON public.customer_payments(customer_id);

-- 4. TABLA: cashiers (Directorio de Cajeros)
CREATE TABLE IF NOT EXISTS public.cashiers (
    id         TEXT PRIMARY KEY,
    nombre     TEXT NOT NULL,
    pin        TEXT NOT NULL DEFAULT '0000',
    activo     BOOLEAN NOT NULL DEFAULT true,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_cashiers_activo ON public.cashiers(activo);

-- 5. TABLA: shifts (Turnos de Caja y Cortes)
CREATE TABLE IF NOT EXISTS public.shifts (
    id                  TEXT PRIMARY KEY,
    cashier_id          TEXT NOT NULL REFERENCES public.cashiers(id) ON DELETE RESTRICT,
    cashier_name        TEXT NOT NULL,
    start_time          BIGINT NOT NULL,
    end_time            BIGINT,
    initial_cash        NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    final_cash_expected NUMERIC(12, 2),
    final_cash_counted  NUMERIC(12, 2),
    difference          NUMERIC(12, 2),
    notes               TEXT,
    is_closed           BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX IF NOT EXISTS idx_shifts_is_closed ON public.shifts(is_closed);
CREATE INDEX IF NOT EXISTS idx_shifts_start_time ON public.shifts(start_time);

-- 6. TABLA: cash_movements (Entradas y Salidas de Efectivo de Caja)
CREATE TABLE IF NOT EXISTS public.cash_movements (
    id         TEXT PRIMARY KEY,
    shift_id   TEXT NOT NULL REFERENCES public.shifts(id) ON DELETE CASCADE,
    cashier_id TEXT NOT NULL REFERENCES public.cashiers(id) ON DELETE RESTRICT,
    tipo       TEXT NOT NULL, -- 'ENTRADA', 'SALIDA'
    monto      NUMERIC(12, 2) NOT NULL,
    motivo     TEXT NOT NULL,
    created_at BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_cash_movements_shift_id ON public.cash_movements(shift_id);

-- 7. TABLA: sales (Cabecera de Ventas / Tickets)
CREATE TABLE IF NOT EXISTS public.sales (
    id             TEXT PRIMARY KEY,
    folio          BIGINT NOT NULL,
    total          NUMERIC(12, 2) NOT NULL,
    total_original NUMERIC(12, 2) NOT NULL,
    total_costo    NUMERIC(12, 2) NOT NULL,
    ganancia       NUMERIC(12, 2) NOT NULL,
    pago_con       NUMERIC(12, 2) NOT NULL,
    cambio         NUMERIC(12, 2) NOT NULL,
    metodo_pago    TEXT NOT NULL DEFAULT 'EFECTIVO',
    total_items    NUMERIC(10, 3) NOT NULL DEFAULT 0.000,
    customer_id    TEXT REFERENCES public.customers(id) ON DELETE SET NULL,
    created_at     BIGINT NOT NULL,
    shift_id       TEXT REFERENCES public.shifts(id) ON DELETE SET NULL,
    cashier_id     TEXT REFERENCES public.cashiers(id) ON DELETE SET NULL,
    cashier_name   TEXT,
    estado         TEXT NOT NULL DEFAULT 'COMPLETADA'
);

CREATE INDEX IF NOT EXISTS idx_sales_created_at ON public.sales(created_at);
CREATE INDEX IF NOT EXISTS idx_sales_customer_id ON public.sales(customer_id);
CREATE INDEX IF NOT EXISTS idx_sales_shift_id ON public.sales(shift_id);
CREATE INDEX IF NOT EXISTS idx_sales_estado ON public.sales(estado);

-- 8. TABLA: sale_items (Partidas / Renglones de Ventas)
CREATE TABLE IF NOT EXISTS public.sale_items (
    id              TEXT PRIMARY KEY,
    sale_id         TEXT NOT NULL REFERENCES public.sales(id) ON DELETE CASCADE,
    product_id      TEXT REFERENCES public.products(id) ON DELETE SET NULL,
    product_nombre  TEXT NOT NULL,
    cantidad        NUMERIC(10, 3) NOT NULL,
    precio_unitario NUMERIC(12, 2) NOT NULL,
    costo_unitario  NUMERIC(12, 2) NOT NULL,
    subtotal        NUMERIC(12, 2) NOT NULL,
    ganancia        NUMERIC(12, 2) NOT NULL,
    es_mayoreo      BOOLEAN NOT NULL DEFAULT false,
    created_at      BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_sale_items_sale_id ON public.sale_items(sale_id);

-- 9. TABLA: store_settings (Ajustes de Negocio, Datos de la Tienda y Reglas)
CREATE TABLE IF NOT EXISTS public.store_settings (
    id                                 TEXT PRIMARY KEY DEFAULT 'default',
    store_name                         TEXT NOT NULL DEFAULT '',
    store_address                      TEXT NOT NULL DEFAULT '',
    store_phone                        TEXT NOT NULL DEFAULT '',
    receipt_footer                     TEXT NOT NULL DEFAULT '',
    default_retail_margin              NUMERIC(10, 4) NOT NULL DEFAULT 0.0,
    default_wholesale_margin           NUMERIC(10, 4) NOT NULL DEFAULT 0.0,
    is_rounding_enabled                BOOLEAN NOT NULL DEFAULT false,
    round_retail_price                 BOOLEAN NOT NULL DEFAULT false,
    round_wholesale_price              BOOLEAN NOT NULL DEFAULT false,
    round_ticket_total                 BOOLEAN NOT NULL DEFAULT false,
    disallow_card_payment_on_wholesale BOOLEAN NOT NULL DEFAULT false,
    updated_at                         BIGINT NOT NULL
);

-- 10. TABLA: deleted_records (Registro de Eliminaciones / Tombstones para Sincronización)
CREATE TABLE IF NOT EXISTS public.deleted_records (
    id          TEXT PRIMARY KEY,
    entity_type TEXT NOT NULL, -- 'PRODUCT', 'CUSTOMER', 'PAYMENT', 'CASHIER'
    deleted_at  BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_deleted_records_deleted_at ON public.deleted_records(deleted_at);

-- =========================================================================
-- SCRIPT DE MIGRACIÓN PARA BASES DE DATOS SUPABASE EXISTENTES
-- =========================================================================
-- Si ya tenías creada tu base de datos en Supabase, ejecuta este bloque para actualizarla:
/*
CREATE TABLE IF NOT EXISTS public.cashiers (
    id         TEXT PRIMARY KEY,
    nombre     TEXT NOT NULL,
    pin        TEXT NOT NULL DEFAULT '0000',
    activo     BOOLEAN NOT NULL DEFAULT true,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_cashiers_activo ON public.cashiers(activo);

CREATE TABLE IF NOT EXISTS public.shifts (
    id                  TEXT PRIMARY KEY,
    cashier_id          TEXT NOT NULL REFERENCES public.cashiers(id) ON DELETE RESTRICT,
    cashier_name        TEXT NOT NULL,
    start_time          BIGINT NOT NULL,
    end_time            BIGINT,
    initial_cash        NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    final_cash_expected NUMERIC(12, 2),
    final_cash_counted  NUMERIC(12, 2),
    difference          NUMERIC(12, 2),
    notes               TEXT,
    is_closed           BOOLEAN NOT NULL DEFAULT false
);
CREATE INDEX IF NOT EXISTS idx_shifts_is_closed ON public.shifts(is_closed);
CREATE INDEX IF NOT EXISTS idx_shifts_start_time ON public.shifts(start_time);

CREATE TABLE IF NOT EXISTS public.cash_movements (
    id         TEXT PRIMARY KEY,
    shift_id   TEXT NOT NULL REFERENCES public.shifts(id) ON DELETE CASCADE,
    cashier_id TEXT NOT NULL REFERENCES public.cashiers(id) ON DELETE RESTRICT,
    tipo       TEXT NOT NULL,
    monto      NUMERIC(12, 2) NOT NULL,
    motivo     TEXT NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_cash_movements_shift_id ON public.cash_movements(shift_id);

ALTER TABLE public.sales ADD COLUMN IF NOT EXISTS shift_id TEXT REFERENCES public.shifts(id) ON DELETE SET NULL;
ALTER TABLE public.sales ADD COLUMN IF NOT EXISTS cashier_id TEXT REFERENCES public.cashiers(id) ON DELETE SET NULL;
ALTER TABLE public.sales ADD COLUMN IF NOT EXISTS cashier_name TEXT;
ALTER TABLE public.sales ADD COLUMN IF NOT EXISTS estado TEXT NOT NULL DEFAULT 'COMPLETADA';
CREATE INDEX IF NOT EXISTS idx_sales_shift_id ON public.sales(shift_id);
CREATE INDEX IF NOT EXISTS idx_sales_estado ON public.sales(estado);

ALTER TABLE public.customers ADD COLUMN IF NOT EXISTS siempre_mayoreo BOOLEAN NOT NULL DEFAULT false;

ALTER TABLE public.store_settings ADD COLUMN IF NOT EXISTS store_name TEXT NOT NULL DEFAULT '';
ALTER TABLE public.store_settings ADD COLUMN IF NOT EXISTS store_address TEXT NOT NULL DEFAULT '';
ALTER TABLE public.store_settings ADD COLUMN IF NOT EXISTS store_phone TEXT NOT NULL DEFAULT '';
ALTER TABLE public.store_settings ADD COLUMN IF NOT EXISTS receipt_footer TEXT NOT NULL DEFAULT '';
ALTER TABLE public.store_settings ADD COLUMN IF NOT EXISTS disallow_card_payment_on_wholesale BOOLEAN NOT NULL DEFAULT false;
*/
