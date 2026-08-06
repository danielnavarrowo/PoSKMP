package com.dnavarro.poskmp.data

import com.dnavarro.poskmp.data.source.local.SaleLocalDataSource
import com.dnavarro.poskmp.db.Sale_items
import com.dnavarro.poskmp.db.Sales
import com.dnavarro.poskmp.domain.model.ProductSalesMetric
import com.dnavarro.poskmp.domain.model.Sale
import com.dnavarro.poskmp.domain.model.SaleItem
import com.dnavarro.poskmp.domain.model.SalesSummary

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
            created_at = sale.createdAt,
            sync_state = sale.syncState
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
                createdAt = row.created_at,
                syncState = row.sync_state
            )
        }
    }

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
            createdAt = row.created_at,
            syncState = row.sync_state
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
}
