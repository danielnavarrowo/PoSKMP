package com.dnavarro.poskmp.domain.usecase

import com.dnavarro.poskmp.data.SaleRepository
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
}
