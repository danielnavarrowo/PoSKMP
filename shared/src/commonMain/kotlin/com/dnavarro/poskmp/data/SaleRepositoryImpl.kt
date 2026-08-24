package com.dnavarro.poskmp.data

import com.dnavarro.poskmp.data.source.local.SaleLocalDataSource
import com.dnavarro.poskmp.db.Sale_items
import com.dnavarro.poskmp.db.Sales
import com.dnavarro.poskmp.domain.model.CategorySalesMetric
import com.dnavarro.poskmp.domain.model.DailySalesMetric
import com.dnavarro.poskmp.domain.model.PaymentMethodMetric
import com.dnavarro.poskmp.domain.model.ProductSalesMetric
import com.dnavarro.poskmp.domain.model.Sale
import com.dnavarro.poskmp.domain.model.SaleItem
import com.dnavarro.poskmp.domain.model.SalesSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SaleRepositoryImpl(
    private val localDataSource: SaleLocalDataSource
) : SaleRepository {

    override suspend fun recordSale(sale: Sale, items: List<SaleItem>): Long {
        val dbSale = Sales(
            id = sale.id,
            folio = sale.folio,
            total = sale.total,
            total_original = sale.totalOriginal,
            total_costo = sale.totalCosto,
            ganancia = sale.ganancia,
            pago_con = sale.pagoCon,
            cambio = sale.cambio,
            metodo_pago = sale.metodoPago,
            total_items = sale.totalItems,
            customer_id = sale.customerId,
            created_at = sale.createdAt,
            sync_state = sale.syncState,
            shift_id = sale.shiftId,
            cashier_id = sale.cashierId,
            cashier_name = sale.cashierName
        )
        val dbItems = items.map { item ->
            Sale_items(
                id = item.id,
                sale_id = item.saleId,
                product_id = item.productId,
                product_nombre = item.productNombre,
                cantidad = item.cantidad,
                precio_unitario = item.precioUnitario,
                costo_unitario = item.costoUnitario,
                subtotal = item.subtotal,
                ganancia = item.ganancia,
                es_mayoreo = if (item.esMayoreo) 1L else 0L,
                created_at = item.createdAt
            )
        }
        return localDataSource.recordSale(dbSale, dbItems)
    }

    override suspend fun getNextFolio(): Long = localDataSource.getNextFolio()

    override suspend fun getSalesSummaryBetween(startTime: Long, endTime: Long): SalesSummary =
        localDataSource.getSalesSummaryBetween(startTime, endTime)

    override suspend fun getTopSellingProductsBetween(
        startTime: Long,
        endTime: Long,
        limit: Long
    ): List<ProductSalesMetric> =
        localDataSource.getTopSellingProductsBetween(startTime, endTime, limit)

    override suspend fun getLeastSellingProductsBetween(
        startTime: Long,
        endTime: Long,
        limit: Long
    ): List<ProductSalesMetric> =
        localDataSource.getLeastSellingProductsBetween(startTime, endTime, limit)

    override suspend fun getRecentSales(limit: Long, offset: Long): List<Sale> {
        return localDataSource.getRecentSales(limit, offset).map { row ->
            Sale(
                id = row.id,
                folio = row.folio,
                total = row.total,
                totalOriginal = row.total_original,
                totalCosto = row.total_costo,
                ganancia = row.ganancia,
                pagoCon = row.pago_con,
                cambio = row.cambio,
                metodoPago = row.metodo_pago,
                totalItems = row.total_items,
                customerId = row.customer_id,
                createdAt = row.created_at,
                syncState = row.sync_state,
                shiftId = row.shift_id,
                cashierId = row.cashier_id,
                cashierName = row.cashier_name
            )
        }
    }

    override suspend fun getSalesBetween(
        startTime: Long,
        endTime: Long,
        limit: Long,
        offset: Long
    ): List<Sale> {
        return localDataSource.getSalesBetween(startTime, endTime, limit, offset).map { row ->
            Sale(
                id = row.id,
                folio = row.folio,
                total = row.total,
                totalOriginal = row.total_original,
                totalCosto = row.total_costo,
                ganancia = row.ganancia,
                pagoCon = row.pago_con,
                cambio = row.cambio,
                metodoPago = row.metodo_pago,
                totalItems = row.total_items,
                customerId = row.customer_id,
                createdAt = row.created_at,
                syncState = row.sync_state,
                shiftId = row.shift_id,
                cashierId = row.cashier_id,
                cashierName = row.cashier_name
            )
        }
    }

    override suspend fun getPaymentMethodSalesBetween(
        startTime: Long,
        endTime: Long
    ): List<PaymentMethodMetric> =
        localDataSource.getPaymentMethodSalesBetween(startTime, endTime)

    override suspend fun getCategorySalesBetween(
        startTime: Long,
        endTime: Long
    ): List<CategorySalesMetric> =
        localDataSource.getCategorySalesBetween(startTime, endTime)

    override suspend fun getDailySalesBetween(
        startTime: Long,
        endTime: Long
    ): List<DailySalesMetric> =
        localDataSource.getDailySalesBetween(startTime, endTime)

    override suspend fun getSaleById(id: String): Sale? {
        val row = localDataSource.getSaleById(id) ?: return null
        return Sale(
            id = row.id,
            folio = row.folio,
            total = row.total,
            totalOriginal = row.total_original,
            totalCosto = row.total_costo,
            ganancia = row.ganancia,
            pagoCon = row.pago_con,
            cambio = row.cambio,
            metodoPago = row.metodo_pago,
            totalItems = row.total_items,
            customerId = row.customer_id,
            createdAt = row.created_at,
            syncState = row.sync_state,
            shiftId = row.shift_id,
            cashierId = row.cashier_id,
            cashierName = row.cashier_name
        )
    }

    override suspend fun getItemsBySaleId(saleId: String): List<SaleItem> {
        return localDataSource.getItemsBySaleId(saleId).map { row ->
            SaleItem(
                id = row.id,
                saleId = row.sale_id,
                productId = row.product_id,
                productNombre = row.product_nombre,
                cantidad = row.cantidad,
                precioUnitario = row.precio_unitario,
                costoUnitario = row.costo_unitario,
                subtotal = row.subtotal,
                ganancia = row.ganancia,
                esMayoreo = row.es_mayoreo == 1L,
                createdAt = row.created_at
            )
        }
    }

    override suspend fun getTotalSalesCount(): Long = localDataSource.getTotalSalesCount()

    override fun getLastSale(): Flow<Sale?> {
        return localDataSource.getLastSale().map { row ->
            row?.let {
                Sale(
                    id = it.id,
                    folio = it.folio,
                    total = it.total,
                    totalOriginal = it.total_original,
                    totalCosto = it.total_costo,
                    ganancia = it.ganancia,
                    pagoCon = it.pago_con,
                    cambio = it.cambio,
                    metodoPago = it.metodo_pago,
                    totalItems = it.total_items,
                    customerId = it.customer_id,
                    createdAt = it.created_at,
                    syncState = it.sync_state,
                    shiftId = it.shift_id,
                    cashierId = it.cashier_id,
                    cashierName = it.cashier_name
                )
            }
        }
    }
}
