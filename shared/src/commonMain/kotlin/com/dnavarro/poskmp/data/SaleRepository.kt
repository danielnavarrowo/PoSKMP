package com.dnavarro.poskmp.data

import com.dnavarro.poskmp.domain.model.CategorySalesMetric
import com.dnavarro.poskmp.domain.model.DailySalesMetric
import com.dnavarro.poskmp.domain.model.PaymentMethodMetric
import com.dnavarro.poskmp.domain.model.ProductSalesMetric
import com.dnavarro.poskmp.domain.model.Sale
import com.dnavarro.poskmp.domain.model.SaleItem
import com.dnavarro.poskmp.domain.model.SalesSummary
import kotlinx.coroutines.flow.Flow

interface SaleRepository {
    suspend fun recordSale(sale: Sale, items: List<SaleItem>): Long
    suspend fun getNextFolio(): Long
    suspend fun getSalesSummaryBetween(startTime: Long, endTime: Long, shiftId: String? = null): SalesSummary
    suspend fun getTopSellingProductsBetween(startTime: Long, endTime: Long, limit: Long = 10, shiftId: String? = null): List<ProductSalesMetric>
    suspend fun getLeastSellingProductsBetween(startTime: Long, endTime: Long, limit: Long = 10, shiftId: String? = null): List<ProductSalesMetric>
    suspend fun getRecentSales(limit: Long = 20, offset: Long = 0): List<Sale>
    suspend fun getSalesBetween(startTime: Long, endTime: Long, limit: Long = 50, offset: Long = 0, shiftId: String? = null): List<Sale>
    suspend fun getPaymentMethodSalesBetween(startTime: Long, endTime: Long, shiftId: String? = null): List<PaymentMethodMetric>
    suspend fun getCategorySalesBetween(startTime: Long, endTime: Long, shiftId: String? = null): List<CategorySalesMetric>
    suspend fun getDailySalesBetween(startTime: Long, endTime: Long, shiftId: String? = null): List<DailySalesMetric>
    suspend fun getSaleById(id: String): Sale?
    suspend fun getItemsBySaleId(saleId: String): List<SaleItem>
    suspend fun getTotalSalesCount(): Long
    fun getLastSale(): Flow<Sale?>
}

