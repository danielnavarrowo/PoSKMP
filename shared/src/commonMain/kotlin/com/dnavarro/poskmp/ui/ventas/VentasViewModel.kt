package com.dnavarro.poskmp.ui.ventas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dnavarro.poskmp.data.SaleRepository
import com.dnavarro.poskmp.domain.model.ProductSalesMetric
import com.dnavarro.poskmp.domain.model.Sale
import com.dnavarro.poskmp.domain.model.SaleItem
import com.dnavarro.poskmp.domain.model.SalesSummary
import com.dnavarro.poskmp.domain.usecase.GetSalesSummaryUseCase
import com.dnavarro.poskmp.util.currentTimeMillis
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SalesPeriodPreset {
    HOY, AYER, ESTA_SEMANA, ESTE_MES
}

data class VentasUiState(
    val selectedPeriod: SalesPeriodPreset = SalesPeriodPreset.HOY,
    val isLoading: Boolean = false,
    val summary: SalesSummary = SalesSummary(0.0, 0.0, 0.0, 0.0, 0.0, 0L, 0.0),
    val topSellers: List<ProductSalesMetric> = emptyList(),
    val leastSellers: List<ProductSalesMetric> = emptyList(),
    val recentSales: List<Sale> = emptyList(),
    val selectedSaleDetails: Pair<Sale, List<SaleItem>>? = null
)

class VentasViewModel(
    private val getSalesSummaryUseCase: GetSalesSummaryUseCase,
    private val saleRepository: SaleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(VentasUiState())
    val uiState: StateFlow<VentasUiState> = _uiState.asStateFlow()

    init {
        loadDataForPeriod(SalesPeriodPreset.HOY)
    }

    fun selectPeriod(preset: SalesPeriodPreset) {
        _uiState.update { it.copy(selectedPeriod = preset) }
        loadDataForPeriod(preset)
    }

    fun refresh() {
        loadDataForPeriod(_uiState.value.selectedPeriod)
    }

    fun selectSaleForDetail(sale: Sale?) {
        if (sale == null) {
            _uiState.update { it.copy(selectedSaleDetails = null) }
            return
        }
        viewModelScope.launch {
            val items = saleRepository.getItemsBySaleId(sale.id)
            _uiState.update { it.copy(selectedSaleDetails = Pair(sale, items)) }
        }
    }

    private fun loadDataForPeriod(preset: SalesPeriodPreset) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val (startTime, endTime) = getPeriodTimeRange(preset)

            val summary = getSalesSummaryUseCase.getSummary(startTime, endTime)
            val topSellers = getSalesSummaryUseCase.getTopSellers(startTime, endTime, limit = 10)
            val leastSellers = getSalesSummaryUseCase.getLeastSellers(startTime, endTime, limit = 10)
            val recentSales = saleRepository.getRecentSales(limit = 30, offset = 0)

            _uiState.update {
                it.copy(
                    isLoading = false,
                    summary = summary,
                    topSellers = topSellers,
                    leastSellers = leastSellers,
                    recentSales = recentSales
                )
            }
        }
    }

    private fun getPeriodTimeRange(preset: SalesPeriodPreset): Pair<Long, Long> {
        val now = currentTimeMillis()
        val millisInDay = 86_400_000L
        val startOfToday = (now / millisInDay) * millisInDay

        return when (preset) {
            SalesPeriodPreset.HOY -> Pair(startOfToday, now)
            SalesPeriodPreset.AYER -> Pair(startOfToday - millisInDay, startOfToday - 1)
            SalesPeriodPreset.ESTA_SEMANA -> Pair(startOfToday - (7 * millisInDay), now)
            SalesPeriodPreset.ESTE_MES -> Pair(startOfToday - (30 * millisInDay), now)
        }
    }
}
