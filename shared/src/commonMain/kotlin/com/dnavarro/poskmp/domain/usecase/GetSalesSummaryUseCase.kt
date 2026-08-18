package com.dnavarro.poskmp.domain.usecase

import com.dnavarro.poskmp.data.SaleRepository
import com.dnavarro.poskmp.domain.model.CategorySalesMetric
import com.dnavarro.poskmp.domain.model.PaymentMethodMetric
import com.dnavarro.poskmp.domain.model.ProductSalesMetric
import com.dnavarro.poskmp.domain.model.SalesSummary

class GetSalesSummaryUseCase(
    private val saleRepository: SaleRepository
) {
    suspend fun getSummary(startTime: Long, endTime: Long): SalesSummary {
        return saleRepository.getSalesSummaryBetween(startTime, endTime)
    }

    suspend fun getTopSellers(startTime: Long, endTime: Long, limit: Long = 10): List<ProductSalesMetric> {
        return saleRepository.getTopSellingProductsBetween(startTime, endTime, limit)
    }

    suspend fun getLeastSellers(startTime: Long, endTime: Long, limit: Long = 10): List<ProductSalesMetric> {
        return saleRepository.getLeastSellingProductsBetween(startTime, endTime, limit)
    }

    suspend fun getPaymentMethodMetrics(startTime: Long, endTime: Long, totalSales: Double): List<PaymentMethodMetric> {
        val rows = saleRepository.getPaymentMethodSalesBetween(startTime, endTime)
        return rows.map { row ->
            val pct = if (totalSales > 0) (row.totalRecaudado / totalSales) * 100.0 else 0.0
            row.copy(porcentaje = pct)
        }
    }

    suspend fun getCategorySalesMetrics(startTime: Long, endTime: Long, totalSales: Double): List<CategorySalesMetric> {
        val rows = saleRepository.getCategorySalesBetween(startTime, endTime)
        return rows.map { row ->
            val pct = if (totalSales > 0) (row.totalRecaudado / totalSales) * 100.0 else 0.0
            row.copy(porcentaje = pct)
        }
    }
}

