package com.dnavarro.poskmp.domain.usecase

import com.dnavarro.poskmp.data.SaleRepository
import com.dnavarro.poskmp.domain.model.CategorySalesMetric
import com.dnavarro.poskmp.domain.model.DailySalesMetric
import com.dnavarro.poskmp.domain.model.PaymentMethodMetric
import com.dnavarro.poskmp.domain.model.ProductSalesMetric
import com.dnavarro.poskmp.domain.model.SalesSummary

class GetSalesSummaryUseCase(
    private val saleRepository: SaleRepository
) {
    suspend fun getSummary(startTime: Long, endTime: Long, shiftId: String? = null): SalesSummary {
        return saleRepository.getSalesSummaryBetween(startTime, endTime, shiftId)
    }

    suspend fun getSoldProducts(startTime: Long, endTime: Long, shiftId: String? = null): List<ProductSalesMetric> {
        return saleRepository.getSoldProductsBetween(startTime, endTime, shiftId)
    }

    suspend fun getTopSellers(startTime: Long, endTime: Long, limit: Long = 10, shiftId: String? = null): List<ProductSalesMetric> {
        return saleRepository.getTopSellingProductsBetween(startTime, endTime, limit, shiftId)
    }

    suspend fun getLeastSellers(startTime: Long, endTime: Long, limit: Long = 10, shiftId: String? = null): List<ProductSalesMetric> {
        return saleRepository.getLeastSellingProductsBetween(startTime, endTime, limit, shiftId)
    }

    suspend fun getPaymentMethodMetrics(startTime: Long, endTime: Long, totalSales: Double, shiftId: String? = null): List<PaymentMethodMetric> {
        val rows = saleRepository.getPaymentMethodSalesBetween(startTime, endTime, shiftId)
        return rows.map { row ->
            val pct = if (totalSales > 0) (row.totalRecaudado / totalSales) * 100.0 else 0.0
            row.copy(porcentaje = pct)
        }
    }

    suspend fun getCategorySalesMetrics(startTime: Long, endTime: Long, totalSales: Double, shiftId: String? = null): List<CategorySalesMetric> {
        val rows = saleRepository.getCategorySalesBetween(startTime, endTime, shiftId)
        return rows.map { row ->
            val pct = if (totalSales > 0) (row.totalRecaudado / totalSales) * 100.0 else 0.0
            row.copy(porcentaje = pct)
        }
    }

    suspend fun getDailySalesMetrics(startTime: Long, endTime: Long, shiftId: String? = null): List<DailySalesMetric> {
        val dbRows = saleRepository.getDailySalesBetween(startTime, endTime, shiftId)
        val rowsByDate = dbRows.associateBy { it.fecha }

        val zoneId = java.time.ZoneId.systemDefault()
        val startDate = java.time.Instant.ofEpochMilli(startTime).atZone(zoneId).toLocalDate()
        val endDate = java.time.Instant.ofEpochMilli(endTime).atZone(zoneId).toLocalDate()

        val totalDays = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1
        val result = mutableListOf<DailySalesMetric>()

        var currentDate = startDate
        val dateFormatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")

        while (!currentDate.isAfter(endDate)) {
            val dateIso = currentDate.format(dateFormatter)
            val dbEntry = rowsByDate[dateIso]

            val dayName = when (currentDate.dayOfWeek) {
                java.time.DayOfWeek.MONDAY -> "Lun"
                java.time.DayOfWeek.TUESDAY -> "Mar"
                java.time.DayOfWeek.WEDNESDAY -> "Mié"
                java.time.DayOfWeek.THURSDAY -> "Jue"
                java.time.DayOfWeek.FRIDAY -> "Vie"
                java.time.DayOfWeek.SATURDAY -> "Sáb"
                java.time.DayOfWeek.SUNDAY -> "Dom"
            }
            val monthName = when (currentDate.month) {
                java.time.Month.JANUARY -> "Ene"
                java.time.Month.FEBRUARY -> "Feb"
                java.time.Month.MARCH -> "Mar"
                java.time.Month.APRIL -> "Abr"
                java.time.Month.MAY -> "May"
                java.time.Month.JUNE -> "Jun"
                java.time.Month.JULY -> "Jul"
                java.time.Month.AUGUST -> "Ago"
                java.time.Month.SEPTEMBER -> "Sep"
                java.time.Month.OCTOBER -> "Oct"
                java.time.Month.NOVEMBER -> "Nov"
                java.time.Month.DECEMBER -> "Dic"
            }

            val diaLabel = if (totalDays <= 7) {
                "$dayName ${currentDate.dayOfMonth}"
            } else {
                "${currentDate.dayOfMonth} $monthName"
            }

            result.add(
                DailySalesMetric(
                    fecha = dateIso,
                    diaLabel = diaLabel,
                    totalVentas = dbEntry?.totalVentas ?: 0.0,
                    totalGanancia = dbEntry?.totalGanancia ?: 0.0,
                    transaccionesCount = dbEntry?.transaccionesCount ?: 0L
                )
            )
            currentDate = currentDate.plusDays(1)
        }

        return result
    }
}


