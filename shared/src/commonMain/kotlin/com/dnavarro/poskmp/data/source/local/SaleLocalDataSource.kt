package com.dnavarro.poskmp.data.source.local

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.dnavarro.poskmp.db.AppDatabase
import com.dnavarro.poskmp.db.Sales
import com.dnavarro.poskmp.db.Sale_items
import com.dnavarro.poskmp.domain.model.CategorySalesMetric
import com.dnavarro.poskmp.domain.model.DailySalesMetric
import com.dnavarro.poskmp.domain.model.PaymentMethodMetric
import com.dnavarro.poskmp.domain.model.ProductSalesMetric
import com.dnavarro.poskmp.domain.model.SalesSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

interface SaleLocalDataSource {
    suspend fun recordSale(sale: Sales, items: List<Sale_items>): Long
    suspend fun getNextFolio(): Long
    suspend fun getSalesSummaryBetween(startTime: Long, endTime: Long): SalesSummary
    suspend fun getTopSellingProductsBetween(startTime: Long, endTime: Long, limit: Long): List<ProductSalesMetric>
    suspend fun getLeastSellingProductsBetween(startTime: Long, endTime: Long, limit: Long): List<ProductSalesMetric>
    suspend fun getRecentSales(limit: Long, offset: Long): List<Sales>
    suspend fun getSalesBetween(startTime: Long, endTime: Long, limit: Long, offset: Long): List<Sales>
    suspend fun getPaymentMethodSalesBetween(startTime: Long, endTime: Long): List<PaymentMethodMetric>
    suspend fun getCategorySalesBetween(startTime: Long, endTime: Long): List<CategorySalesMetric>
    suspend fun getDailySalesBetween(startTime: Long, endTime: Long): List<DailySalesMetric>
    suspend fun getSaleById(id: String): Sales?
    suspend fun getItemsBySaleId(saleId: String): List<Sale_items>
    suspend fun getTotalSalesCount(): Long
    fun getLastSale(): Flow<Sales?>
}

class SqlDelightSaleDataSource(
    private val database: AppDatabase
) : SaleLocalDataSource {
    private val queries = database.appDatabaseQueries

    override suspend fun recordSale(sale: Sales, items: List<Sale_items>): Long = withContext(Dispatchers.IO) {
        database.transactionWithResult {
            queries.insertSale(
                id = sale.id,
                folio = sale.folio,
                total = sale.total,
                total_original = sale.total_original,
                total_costo = sale.total_costo,
                ganancia = sale.ganancia,
                pago_con = sale.pago_con,
                cambio = sale.cambio,
                metodo_pago = sale.metodo_pago,
                total_items = sale.total_items,
                customer_id = sale.customer_id,
                created_at = sale.created_at,
                sync_state = sale.sync_state,
                shift_id = sale.shift_id,
                cashier_id = sale.cashier_id,
                cashier_name = sale.cashier_name
            )

            items.forEach { item ->
                queries.insertSaleItem(
                    id = item.id,
                    sale_id = item.sale_id,
                    product_id = item.product_id,
                    product_nombre = item.product_nombre,
                    cantidad = item.cantidad,
                    precio_unitario = item.precio_unitario,
                    costo_unitario = item.costo_unitario,
                    subtotal = item.subtotal,
                    ganancia = item.ganancia,
                    es_mayoreo = item.es_mayoreo,
                    created_at = item.created_at
                )
            }
            sale.folio
        }
    }

    override suspend fun getNextFolio(): Long = withContext(Dispatchers.IO) {
        queries.getNextFolio().executeAsOne()
    }

    override suspend fun getSalesSummaryBetween(startTime: Long, endTime: Long): SalesSummary = withContext(Dispatchers.IO) {
        val result = queries.selectSalesSummaryBetween(startTime, endTime).executeAsOne()
        SalesSummary(
            totalVentas = result.total_ventas,
            totalSinDescuento = result.total_sin_descuento,
            totalCosto = result.total_costo,
            totalGanancia = result.total_ganancia,
            porcentajeGanancia = result.porcentaje_ganancia ?: 0.0,
            totalTicketCount = result.total_ticket_count,
            promedioTicket = result.promedio_ticket
        )
    }

    override suspend fun getTopSellingProductsBetween(
        startTime: Long,
        endTime: Long,
        limit: Long
    ): List<ProductSalesMetric> = withContext(Dispatchers.IO) {
        queries.selectTopSellingProductsBetween(startTime, endTime, limit).executeAsList().map { row ->
            ProductSalesMetric(
                productId = row.product_id,
                productNombre = row.product_nombre,
                totalUnidades = row.total_unidades ?: 0.0,
                totalRecaudado = row.total_recaudado ?: 0.0,
                gananciaGenerada = row.ganancia_generada ?: 0.0
            )
        }
    }

    override suspend fun getLeastSellingProductsBetween(
        startTime: Long,
        endTime: Long,
        limit: Long
    ): List<ProductSalesMetric> = withContext(Dispatchers.IO) {
        queries.selectLeastSellingProductsBetween(startTime, endTime, limit).executeAsList().map { row ->
            ProductSalesMetric(
                productId = row.product_id,
                productNombre = row.product_nombre,
                totalUnidades = row.total_unidades ?: 0.0,
                totalRecaudado = row.total_recaudado ?: 0.0,
                gananciaGenerada = row.ganancia_generada ?: 0.0
            )
        }
    }

    override suspend fun getRecentSales(limit: Long, offset: Long): List<Sales> = withContext(Dispatchers.IO) {
        queries.selectRecentSales(limit, offset).executeAsList()
    }

    override suspend fun getSalesBetween(
        startTime: Long,
        endTime: Long,
        limit: Long,
        offset: Long
    ): List<Sales> = withContext(Dispatchers.IO) {
        queries.selectSalesBetween(startTime, endTime, limit, offset).executeAsList()
    }

    override suspend fun getPaymentMethodSalesBetween(
        startTime: Long,
        endTime: Long
    ): List<PaymentMethodMetric> = withContext(Dispatchers.IO) {
        queries.selectPaymentMethodSalesBetween(startTime, endTime).executeAsList().map { row ->
            PaymentMethodMetric(
                metodoPago = row.metodo_pago,
                totalRecaudado = row.total_recaudado,
                transaccionesCount = row.total_transacciones
            )
        }
    }

    override suspend fun getCategorySalesBetween(
        startTime: Long,
        endTime: Long
    ): List<CategorySalesMetric> = withContext(Dispatchers.IO) {
        queries.selectCategorySalesBetween(startTime, endTime).executeAsList().map { row ->
            CategorySalesMetric(
                categoria = row.categoria,
                totalRecaudado = row.total_recaudado,
                totalUnidades = row.total_unidades
            )
        }
    }

    override suspend fun getDailySalesBetween(
        startTime: Long,
        endTime: Long
    ): List<DailySalesMetric> = withContext(Dispatchers.IO) {
        queries.selectDailySalesBetween(startTime, endTime).executeAsList().map { row ->
            DailySalesMetric(
                fecha = row.dia,
                diaLabel = row.dia,
                totalVentas = row.total_recaudado,
                totalGanancia = row.total_ganancia,
                transaccionesCount = row.total_transacciones
            )
        }
    }

    override suspend fun getSaleById(id: String): Sales? = withContext(Dispatchers.IO) {
        queries.selectSaleById(id).executeAsOneOrNull()
    }

    override suspend fun getItemsBySaleId(saleId: String): List<Sale_items> = withContext(Dispatchers.IO) {
        queries.selectItemsBySaleId(saleId).executeAsList()
    }

    override suspend fun getTotalSalesCount(): Long = withContext(Dispatchers.IO) {
        queries.selectAllSalesCount().executeAsOne()
    }

    override fun getLastSale(): Flow<Sales?> {
        return queries.selectRecentSales(1, 0).asFlow().mapToOneOrNull(Dispatchers.IO)
    }
}
