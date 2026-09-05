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
    precio_delivery NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
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
    shift_id       TEXT,
    cashier_id     TEXT,
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
    es_delivery     BOOLEAN NOT NULL DEFAULT false,
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
    default_delivery_margin            NUMERIC(10, 4) NOT NULL DEFAULT 0.0,
    is_rounding_enabled                BOOLEAN NOT NULL DEFAULT false,
    round_product_prices               BOOLEAN NOT NULL DEFAULT false,
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

-- 11. TABLA: remote_audit_logs (Bitácora de Operaciones de Escritura/Actualización Remotas)
CREATE TABLE IF NOT EXISTS public.remote_audit_logs (
    id          BIGSERIAL PRIMARY KEY,
    table_name  TEXT NOT NULL,
    operation   TEXT NOT NULL, -- 'INSERT', 'UPDATE', 'DELETE'
    record_id   TEXT NOT NULL,
    summary     TEXT,
    device_id   TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

CREATE INDEX IF NOT EXISTS idx_remote_audit_logs_created_at ON public.remote_audit_logs(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_remote_audit_logs_table ON public.remote_audit_logs(table_name);

-- Función de Trigger para registrar operaciones de escritura y actualización automáticamente
CREATE OR REPLACE FUNCTION public.fn_log_remote_audit()
RETURNS TRIGGER AS $$
DECLARE
    v_table_name TEXT := TG_TABLE_NAME;
    v_op TEXT := TG_OP;
    v_record_id TEXT;
    v_summary TEXT;
BEGIN
    IF (v_op = 'DELETE') THEN
        v_record_id := OLD.id::text;
        IF v_table_name = 'products' THEN
            v_summary := 'Producto eliminado: ' || COALESCE(OLD.nombre, OLD.id);
        ELSIF v_table_name = 'customers' THEN
            v_summary := 'Cliente eliminado: ' || COALESCE(OLD.nombre, OLD.id);
        ELSIF v_table_name = 'customer_payments' THEN
            v_summary := 'Abono eliminado: $' || COALESCE(OLD.monto::text, '0');
        ELSIF v_table_name = 'cashiers' THEN
            v_summary := 'Cajero eliminado: ' || COALESCE(OLD.nombre, OLD.id);
        ELSIF v_table_name = 'sales' THEN
            v_summary := 'Venta eliminada: Folio #' || COALESCE(OLD.folio::text, OLD.id) || ' ($' || COALESCE(OLD.total::text, '0') || ')';
        ELSE
            v_summary := 'Registro eliminado en ' || v_table_name;
        END IF;
    ELSIF (v_op = 'INSERT') THEN
        v_record_id := NEW.id::text;
        IF v_table_name = 'products' THEN
            v_summary := 'Producto creado: ' || NEW.nombre || ' ($' || NEW.precio || ')';
        ELSIF v_table_name = 'customers' THEN
            v_summary := 'Cliente registrado: ' || NEW.nombre;
        ELSIF v_table_name = 'customer_payments' THEN
            v_summary := 'Abono registrado: $' || NEW.monto || ' (' || NEW.metodo_pago || ')';
        ELSIF v_table_name = 'cashiers' THEN
            v_summary := 'Cajero registrado: ' || NEW.nombre;
        ELSIF v_table_name = 'sales' THEN
            v_summary := 'Venta creada: Folio #' || NEW.folio || ' ($' || NEW.total || ' - ' || NEW.metodo_pago || ')' || CASE WHEN NEW.cashier_name IS NOT NULL AND NEW.cashier_name != '' THEN ' • ' || NEW.cashier_name ELSE '' END;
        ELSIF v_table_name = 'store_settings' THEN
            v_summary := 'Ajustes de tienda guardados';
        ELSIF v_table_name = 'deleted_records' THEN
            v_summary := 'Baja registrada: ' || NEW.entity_type || ' (' || NEW.id || ')';
        ELSE
            v_summary := 'Nuevo registro en ' || v_table_name;
        END IF;
    ELSIF (v_op = 'UPDATE') THEN
        v_record_id := NEW.id::text;
        IF v_table_name = 'products' THEN
            v_summary := 'Producto modificado: ' || NEW.nombre || ' ($' || NEW.precio || ', stock: ' || NEW.piezas || ')';
        ELSIF v_table_name = 'customers' THEN
            v_summary := 'Cliente modificado: ' || NEW.nombre || ' (Límite: $' || NEW.limite_credito || ')';
        ELSIF v_table_name = 'customer_payments' THEN
            v_summary := 'Abono modificado: $' || NEW.monto;
        ELSIF v_table_name = 'cashiers' THEN
            v_summary := 'Cajero modificado: ' || NEW.nombre || ' (Activo: ' || CASE WHEN NEW.activo THEN 'Sí' ELSE 'No' END || ')';
        ELSIF v_table_name = 'sales' THEN
            v_summary := 'Venta modificada: Folio #' || NEW.folio || ' (Estado: ' || NEW.estado || ')';
        ELSIF v_table_name = 'store_settings' THEN
            v_summary := 'Ajustes de tienda actualizados';
        ELSE
            v_summary := 'Registro actualizado en ' || v_table_name;
        END IF;
    END IF;

    INSERT INTO public.remote_audit_logs (table_name, operation, record_id, summary, created_at)
    VALUES (v_table_name, v_op, v_record_id, v_summary, now());

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Triggers automáticos
DROP TRIGGER IF EXISTS trg_audit_products ON public.products;
CREATE TRIGGER trg_audit_products
AFTER INSERT OR UPDATE OR DELETE ON public.products
FOR EACH ROW EXECUTE FUNCTION public.fn_log_remote_audit();

DROP TRIGGER IF EXISTS trg_audit_customers ON public.customers;
CREATE TRIGGER trg_audit_customers
AFTER INSERT OR UPDATE OR DELETE ON public.customers
FOR EACH ROW EXECUTE FUNCTION public.fn_log_remote_audit();

DROP TRIGGER IF EXISTS trg_audit_customer_payments ON public.customer_payments;
CREATE TRIGGER trg_audit_customer_payments
AFTER INSERT OR UPDATE OR DELETE ON public.customer_payments
FOR EACH ROW EXECUTE FUNCTION public.fn_log_remote_audit();

DROP TRIGGER IF EXISTS trg_audit_cashiers ON public.cashiers;
CREATE TRIGGER trg_audit_cashiers
AFTER INSERT OR UPDATE OR DELETE ON public.cashiers
FOR EACH ROW EXECUTE FUNCTION public.fn_log_remote_audit();

DROP TRIGGER IF EXISTS trg_audit_sales ON public.sales;
CREATE TRIGGER trg_audit_sales
AFTER INSERT OR UPDATE OR DELETE ON public.sales
FOR EACH ROW EXECUTE FUNCTION public.fn_log_remote_audit();

DROP TRIGGER IF EXISTS trg_audit_store_settings ON public.store_settings;
CREATE TRIGGER trg_audit_store_settings
AFTER INSERT OR UPDATE OR DELETE ON public.store_settings
FOR EACH ROW EXECUTE FUNCTION public.fn_log_remote_audit();

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

ALTER TABLE public.products ADD COLUMN IF NOT EXISTS precio_delivery NUMERIC(12, 2) NOT NULL DEFAULT 0.00;
ALTER TABLE public.sale_items ADD COLUMN IF NOT EXISTS es_delivery BOOLEAN NOT NULL DEFAULT false;

ALTER TABLE public.store_settings ADD COLUMN IF NOT EXISTS store_name TEXT NOT NULL DEFAULT '';
ALTER TABLE public.store_settings ADD COLUMN IF NOT EXISTS store_address TEXT NOT NULL DEFAULT '';
ALTER TABLE public.store_settings ADD COLUMN IF NOT EXISTS store_phone TEXT NOT NULL DEFAULT '';
ALTER TABLE public.store_settings ADD COLUMN IF NOT EXISTS receipt_footer TEXT NOT NULL DEFAULT '';
ALTER TABLE public.store_settings ADD COLUMN IF NOT EXISTS default_delivery_margin NUMERIC(10, 4) NOT NULL DEFAULT 0.0;
ALTER TABLE public.store_settings ADD COLUMN IF NOT EXISTS round_product_prices BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE public.store_settings ADD COLUMN IF NOT EXISTS disallow_card_payment_on_wholesale BOOLEAN NOT NULL DEFAULT false;

-- Si existían columnas de redondeo previas, migrar valores hacia round_product_prices:
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_schema = 'public' AND table_name = 'store_settings' AND column_name = 'round_retail_price'
    ) THEN
        UPDATE public.store_settings 
        SET round_product_prices = (
            COALESCE(round_retail_price, false) OR 
            COALESCE(round_wholesale_price, false) OR 
            COALESCE(round_delivery_price, false)
        );
    END IF;
END $$;

-- Bitácora de operaciones remotas y triggers:
CREATE TABLE IF NOT EXISTS public.remote_audit_logs (
    id          BIGSERIAL PRIMARY KEY,
    table_name  TEXT NOT NULL,
    operation   TEXT NOT NULL,
    record_id   TEXT NOT NULL,
    summary     TEXT,
    device_id   TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);
CREATE INDEX IF NOT EXISTS idx_remote_audit_logs_created_at ON public.remote_audit_logs(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_remote_audit_logs_table ON public.remote_audit_logs(table_name);

CREATE OR REPLACE FUNCTION public.fn_log_remote_audit()
RETURNS TRIGGER AS $$
DECLARE
    v_table_name TEXT := TG_TABLE_NAME;
    v_op TEXT := TG_OP;
    v_record_id TEXT;
    v_summary TEXT;
BEGIN
    IF (v_op = 'DELETE') THEN
        v_record_id := OLD.id::text;
        IF v_table_name = 'products' THEN
            v_summary := 'Producto eliminado: ' || COALESCE(OLD.nombre, OLD.id);
        ELSIF v_table_name = 'customers' THEN
            v_summary := 'Cliente eliminado: ' || COALESCE(OLD.nombre, OLD.id);
        ELSIF v_table_name = 'customer_payments' THEN
            v_summary := 'Abono eliminado: $' || COALESCE(OLD.monto::text, '0');
        ELSIF v_table_name = 'cashiers' THEN
            v_summary := 'Cajero eliminado: ' || COALESCE(OLD.nombre, OLD.id);
        ELSIF v_table_name = 'sales' THEN
            v_summary := 'Venta eliminada: Folio #' || COALESCE(OLD.folio::text, OLD.id) || ' ($' || COALESCE(OLD.total::text, '0') || ')';
        ELSE
            v_summary := 'Registro eliminado en ' || v_table_name;
        END IF;
    ELSIF (v_op = 'INSERT') THEN
        v_record_id := NEW.id::text;
        IF v_table_name = 'products' THEN
            v_summary := 'Producto creado: ' || NEW.nombre || ' ($' || NEW.precio || ')';
        ELSIF v_table_name = 'customers' THEN
            v_summary := 'Cliente registrado: ' || NEW.nombre;
        ELSIF v_table_name = 'customer_payments' THEN
            v_summary := 'Abono registrado: $' || NEW.monto || ' (' || NEW.metodo_pago || ')';
        ELSIF v_table_name = 'cashiers' THEN
            v_summary := 'Cajero registrado: ' || NEW.nombre;
        ELSIF v_table_name = 'sales' THEN
            v_summary := 'Venta creada: Folio #' || NEW.folio || ' ($' || NEW.total || ' - ' || NEW.metodo_pago || ')' || CASE WHEN NEW.cashier_name IS NOT NULL AND NEW.cashier_name != '' THEN ' • ' || NEW.cashier_name ELSE '' END;
        ELSIF v_table_name = 'store_settings' THEN
            v_summary := 'Ajustes de tienda guardados';
        ELSIF v_table_name = 'deleted_records' THEN
            v_summary := 'Baja registrada: ' || NEW.entity_type || ' (' || NEW.id || ')';
        ELSE
            v_summary := 'Nuevo registro en ' || v_table_name;
        END IF;
    ELSIF (v_op = 'UPDATE') THEN
        v_record_id := NEW.id::text;
        IF v_table_name = 'products' THEN
            v_summary := 'Producto modificado: ' || NEW.nombre || ' ($' || NEW.precio || ', stock: ' || NEW.piezas || ')';
        ELSIF v_table_name = 'customers' THEN
            v_summary := 'Cliente modificado: ' || NEW.nombre || ' (Límite: $' || NEW.limite_credito || ')';
        ELSIF v_table_name = 'customer_payments' THEN
            v_summary := 'Abono modificado: $' || NEW.monto;
        ELSIF v_table_name = 'cashiers' THEN
            v_summary := 'Cajero modificado: ' || NEW.nombre || ' (Activo: ' || CASE WHEN NEW.activo THEN 'Sí' ELSE 'No' END || ')';
        ELSIF v_table_name = 'sales' THEN
            v_summary := 'Venta modificada: Folio #' || NEW.folio || ' (Estado: ' || NEW.estado || ')';
        ELSIF v_table_name = 'store_settings' THEN
            v_summary := 'Ajustes de tienda actualizados';
        ELSE
            v_summary := 'Registro actualizado en ' || v_table_name;
        END IF;
    END IF;

    INSERT INTO public.remote_audit_logs (table_name, operation, record_id, summary, created_at)
    VALUES (v_table_name, v_op, v_record_id, v_summary, now());

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_audit_products ON public.products;
CREATE TRIGGER trg_audit_products AFTER INSERT OR UPDATE OR DELETE ON public.products FOR EACH ROW EXECUTE FUNCTION public.fn_log_remote_audit();

DROP TRIGGER IF EXISTS trg_audit_customers ON public.customers;
CREATE TRIGGER trg_audit_customers AFTER INSERT OR UPDATE OR DELETE ON public.customers FOR EACH ROW EXECUTE FUNCTION public.fn_log_remote_audit();

DROP TRIGGER IF EXISTS trg_audit_customer_payments ON public.customer_payments;
CREATE TRIGGER trg_audit_customer_payments AFTER INSERT OR UPDATE OR DELETE ON public.customer_payments FOR EACH ROW EXECUTE FUNCTION public.fn_log_remote_audit();

DROP TRIGGER IF EXISTS trg_audit_cashiers ON public.cashiers;
CREATE TRIGGER trg_audit_cashiers AFTER INSERT OR UPDATE OR DELETE ON public.cashiers FOR EACH ROW EXECUTE FUNCTION public.fn_log_remote_audit();

DROP TRIGGER IF EXISTS trg_audit_sales ON public.sales;
CREATE TRIGGER trg_audit_sales AFTER INSERT OR UPDATE OR DELETE ON public.sales FOR EACH ROW EXECUTE FUNCTION public.fn_log_remote_audit();

DROP TRIGGER IF EXISTS trg_audit_store_settings ON public.store_settings;
CREATE TRIGGER trg_audit_store_settings AFTER INSERT OR UPDATE OR DELETE ON public.store_settings FOR EACH ROW EXECUTE FUNCTION public.fn_log_remote_audit();
*/
