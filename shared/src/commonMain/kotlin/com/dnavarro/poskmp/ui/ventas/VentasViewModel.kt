package com.dnavarro.poskmp.ui.ventas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dnavarro.poskmp.data.SaleRepository
import com.dnavarro.poskmp.domain.model.CashMovement
import com.dnavarro.poskmp.domain.model.CashMovementType
import com.dnavarro.poskmp.domain.model.CashierShift
import com.dnavarro.poskmp.domain.model.CategorySalesMetric
import com.dnavarro.poskmp.domain.model.DailySalesMetric
import com.dnavarro.poskmp.domain.model.PaymentMethodMetric
import com.dnavarro.poskmp.domain.model.ProductSalesMetric
import com.dnavarro.poskmp.domain.model.Sale
import com.dnavarro.poskmp.domain.model.SaleItem
import com.dnavarro.poskmp.domain.model.SalesSummary
import com.dnavarro.poskmp.domain.model.ShiftSummary
import com.dnavarro.poskmp.domain.usecase.CancelSaleUseCase
import com.dnavarro.poskmp.domain.usecase.CloseShiftUseCase
import com.dnavarro.poskmp.domain.usecase.GetActiveShiftUseCase
import com.dnavarro.poskmp.domain.usecase.GetSalesSummaryUseCase
import com.dnavarro.poskmp.domain.usecase.GetShiftSummaryUseCase
import com.dnavarro.poskmp.domain.usecase.RecordCashMovementUseCase
import com.dnavarro.poskmp.util.currentTimeMillis
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import com.dnavarro.poskmp.data.ShiftRepository

enum class SalesPeriodPreset {
    HOY, AYER, ESTA_SEMANA, ESTE_MES, RANGO
}

data class VentasUiState(
    val selectedPeriod: SalesPeriodPreset = SalesPeriodPreset.HOY,
    val customStartDate: Long? = null,
    val customEndDate: Long? = null,
    val showDateRangePicker: Boolean = false,
    val shiftsForSelectedPeriod: List<CashierShift> = emptyList(),
    val selectedShiftId: String? = null,
    val isLoading: Boolean = false,
    val summary: SalesSummary = SalesSummary(0.0, 0.0, 0.0, 0.0, 0.0, 0L, 0.0),
    val topSellers: List<ProductSalesMetric> = emptyList(),
    val leastSellers: List<ProductSalesMetric> = emptyList(),
    val paymentMethodMetrics: List<PaymentMethodMetric> = emptyList(),
    val categorySalesMetrics: List<CategorySalesMetric> = emptyList(),
    val dailySalesMetrics: List<DailySalesMetric> = emptyList(),
    val recentSales: List<Sale> = emptyList(),
    val selectedSaleDetails: Pair<Sale, List<SaleItem>>? = null,
    val saleToCancel: Sale? = null,
    val isCancellingSale: Boolean = false,
    val activeShift: CashierShift? = null,
    val activeShiftMovements: List<CashMovement> = emptyList(),
    val showInflowDialog: Boolean = false,
    val showOutflowDialog: Boolean = false,
    val showCloseShiftDialog: Boolean = false,
    val shiftSummary: ShiftSummary? = null,
    val isRecordingMovement: Boolean = false,
    val isClosingShift: Boolean = false,
    val shiftActionError: String? = null,
    val shiftActionSuccess: String? = null
)

class VentasViewModel(
    private val getSalesSummaryUseCase: GetSalesSummaryUseCase,
    private val saleRepository: SaleRepository,
    private val shiftRepository: ShiftRepository,
    private val getActiveShiftUseCase: GetActiveShiftUseCase,
    private val recordCashMovementUseCase: RecordCashMovementUseCase,
    private val getShiftSummaryUseCase: GetShiftSummaryUseCase,
    private val closeShiftUseCase: CloseShiftUseCase,
    private val cancelSaleUseCase: CancelSaleUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(VentasUiState())
    val uiState: StateFlow<VentasUiState> = _uiState.asStateFlow()
    private var movementsJob: Job? = null

    init {
        loadDataForPeriod(SalesPeriodPreset.HOY)
        observeActiveShift()
    }

    private fun observeActiveShift() {
        viewModelScope.launch {
            getActiveShiftUseCase().collect { shift ->
                _uiState.update { it.copy(activeShift = shift) }
                movementsJob?.cancel()
                if (shift != null) {
                    movementsJob = viewModelScope.launch {
                        shiftRepository.getMovementsForShiftFlow(shift.id).collect { movements ->
                            _uiState.update { it.copy(activeShiftMovements = movements) }
                        }
                    }
                } else {
                    _uiState.update { it.copy(activeShiftMovements = emptyList()) }
                }
            }
        }
    }

    fun selectShiftFilter(shiftId: String?) {
        _uiState.update { it.copy(selectedShiftId = shiftId) }
        val (startTime, endTime) = if (_uiState.value.selectedPeriod == SalesPeriodPreset.RANGO &&
            _uiState.value.customStartDate != null && _uiState.value.customEndDate != null
        ) {
            Pair(_uiState.value.customStartDate!!, _uiState.value.customEndDate!!)
        } else {
            getPeriodTimeRange(_uiState.value.selectedPeriod)
        }
        loadDataForRange(startTime, endTime, shiftId = shiftId, preserveSelectedShift = true)
    }

    fun selectPeriod(preset: SalesPeriodPreset) {
        if (preset == SalesPeriodPreset.RANGO) {
            _uiState.update { it.copy(showDateRangePicker = true) }
        } else {
            _uiState.update { it.copy(selectedPeriod = preset, showDateRangePicker = false, selectedShiftId = null) }
            loadDataForPeriod(preset)
        }
    }

    fun setCustomDateRange(startDateMillis: Long, endDateMillis: Long) {
        _uiState.update {
            it.copy(
                selectedPeriod = SalesPeriodPreset.RANGO,
                customStartDate = startDateMillis,
                customEndDate = endDateMillis,
                showDateRangePicker = false,
                selectedShiftId = null
            )
        }
        loadDataForRange(startDateMillis, endDateMillis)
    }

    fun openDateRangePicker() {
        _uiState.update { it.copy(showDateRangePicker = true) }
    }

    fun dismissDateRangePicker() {
        _uiState.update { it.copy(showDateRangePicker = false) }
    }

    fun refresh() {
        val currentState = _uiState.value
        val (startTime, endTime) = if (currentState.selectedPeriod == SalesPeriodPreset.RANGO &&
            currentState.customStartDate != null && currentState.customEndDate != null
        ) {
            Pair(currentState.customStartDate, currentState.customEndDate)
        } else {
            getPeriodTimeRange(currentState.selectedPeriod)
        }
        loadDataForRange(startTime, endTime, shiftId = currentState.selectedShiftId, preserveSelectedShift = true)
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

    fun openCancelSaleDialog(sale: Sale) {
        _uiState.update { it.copy(saleToCancel = sale) }
    }

    fun dismissCancelSaleDialog() {
        _uiState.update { it.copy(saleToCancel = null) }
    }

    fun cancelSale(sale: Sale) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCancellingSale = true) }
            val result = cancelSaleUseCase(sale.id)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        isCancellingSale = false,
                        saleToCancel = null,
                        selectedSaleDetails = null,
                        shiftActionSuccess = "Venta #${sale.folio} cancelada exitosamente"
                    )
                }
                refresh()
            } else {
                _uiState.update {
                    it.copy(
                        isCancellingSale = false,
                        shiftActionError = result.exceptionOrNull()?.message ?: "Error al cancelar la venta"
                    )
                }
            }
        }
    }

    // Acciones de Turno y Movimientos
    fun openInflowDialog() {
        _uiState.update {
            it.copy(
                showInflowDialog = true,
                showOutflowDialog = false,
                showCloseShiftDialog = false,
                shiftActionError = null
            )
        }
    }

    fun openOutflowDialog() {
        _uiState.update {
            it.copy(
                showInflowDialog = false,
                showOutflowDialog = true,
                showCloseShiftDialog = false,
                shiftActionError = null
            )
        }
    }

    fun openCloseShiftDialog() {
        val activeShift = _uiState.value.activeShift
        if (activeShift == null) {
            _uiState.update { it.copy(shiftActionError = "No hay ningún turno activo.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, shiftActionError = null) }
            val summaryResult = getShiftSummaryUseCase(activeShift.id)
            if (summaryResult.isSuccess) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        showCloseShiftDialog = true,
                        shiftSummary = summaryResult.getOrNull()
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        shiftActionError = summaryResult.exceptionOrNull()?.message ?: "Error al obtener resumen de turno"
                    )
                }
            }
        }
    }

    fun dismissShiftDialogs() {
        _uiState.update {
            it.copy(
                showInflowDialog = false,
                showOutflowDialog = false,
                showCloseShiftDialog = false,
                shiftActionError = null
            )
        }
    }

    fun recordCashMovement(type: CashMovementType, amount: Double, reason: String) {
        val activeShift = _uiState.value.activeShift
        if (activeShift == null) {
            _uiState.update { it.copy(shiftActionError = "No hay ningún turno activo para registrar movimientos.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isRecordingMovement = true, shiftActionError = null) }
            val result = recordCashMovementUseCase(
                shiftId = activeShift.id,
                cashierId = activeShift.cashierId,
                type = type,
                amount = amount,
                reason = reason
            )
            _uiState.update {
                if (result.isSuccess) {
                    it.copy(
                        isRecordingMovement = false,
                        showInflowDialog = false,
                        showOutflowDialog = false,
                        shiftActionSuccess = if (type == CashMovementType.ENTRADA) "Entrada de efectivo registrada" else "Salida de efectivo registrada"
                    )
                } else {
                    it.copy(
                        isRecordingMovement = false,
                        shiftActionError = result.exceptionOrNull()?.message ?: "Error al registrar movimiento"
                    )
                }
            }
        }
    }

    fun closeShift(countedCash: Double, notes: String?) {
        val activeShift = _uiState.value.activeShift
        if (activeShift == null) {
            _uiState.update { it.copy(shiftActionError = "No hay ningún turno activo.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isClosingShift = true, shiftActionError = null) }
            val result = closeShiftUseCase(activeShift.id, countedCash, notes)
            _uiState.update {
                if (result.isSuccess) {
                    it.copy(
                        isClosingShift = false,
                        showCloseShiftDialog = false,
                        shiftSummary = null,
                        shiftActionSuccess = "Turno cerrado exitosamente. Corte de caja guardado."
                    )
                } else {
                    it.copy(
                        isClosingShift = false,
                        shiftActionError = result.exceptionOrNull()?.message ?: "Error al cerrar turno"
                    )
                }
            }
        }
    }

    fun clearShiftActionResult() {
        _uiState.update { it.copy(shiftActionError = null, shiftActionSuccess = null) }
    }

    private fun loadDataForPeriod(preset: SalesPeriodPreset) {
        val (startTime, endTime) = getPeriodTimeRange(preset)
        loadDataForRange(startTime, endTime)
    }

    private fun loadDataForRange(
        startTime: Long,
        endTime: Long,
        shiftId: String? = null,
        preserveSelectedShift: Boolean = false
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val shifts = shiftRepository.getShiftsBetween(startTime, endTime)
            val effectiveShiftId = if (preserveSelectedShift && shifts.any { it.id == shiftId }) {
                shiftId
            } else if (preserveSelectedShift && shiftId == null) {
                null
            } else {
                null
            }

            val summary = getSalesSummaryUseCase.getSummary(startTime, endTime, effectiveShiftId)
            val topSellers = getSalesSummaryUseCase.getTopSellers(startTime, endTime, limit = 10, shiftId = effectiveShiftId)
            val leastSellers = getSalesSummaryUseCase.getLeastSellers(startTime, endTime, limit = 10, shiftId = effectiveShiftId)
            val paymentMethodMetrics = getSalesSummaryUseCase.getPaymentMethodMetrics(startTime, endTime, summary.totalVentas, shiftId = effectiveShiftId)
            val categorySalesMetrics = getSalesSummaryUseCase.getCategorySalesMetrics(startTime, endTime, summary.totalVentas, shiftId = effectiveShiftId)
            val dailySalesMetrics = getSalesSummaryUseCase.getDailySalesMetrics(startTime, endTime, shiftId = effectiveShiftId)
            val recentSales = saleRepository.getSalesBetween(startTime, endTime, limit = 50, offset = 0, shiftId = effectiveShiftId)

            _uiState.update {
                it.copy(
                    isLoading = false,
                    shiftsForSelectedPeriod = shifts,
                    selectedShiftId = effectiveShiftId,
                    summary = summary,
                    topSellers = topSellers,
                    leastSellers = leastSellers,
                    paymentMethodMetrics = paymentMethodMetrics,
                    categorySalesMetrics = categorySalesMetrics,
                    dailySalesMetrics = dailySalesMetrics,
                    recentSales = recentSales
                )
            }
        }
    }

    private fun getPeriodTimeRange(preset: SalesPeriodPreset): Pair<Long, Long> {
        val zoneId = java.time.ZoneId.systemDefault()
        val today = java.time.LocalDate.now(zoneId)
        val nowMillis = currentTimeMillis()

        val startOfToday = today.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val endOfToday = today.atTime(23, 59, 59, 999_000_000).atZone(zoneId).toInstant().toEpochMilli()

        return when (preset) {
            SalesPeriodPreset.HOY -> Pair(startOfToday, nowMillis)
            SalesPeriodPreset.AYER -> {
                val yesterday = today.minusDays(1)
                val startOfYesterday = yesterday.atStartOfDay(zoneId).toInstant().toEpochMilli()
                val endOfYesterday = yesterday.atTime(23, 59, 59, 999_000_000).atZone(zoneId).toInstant().toEpochMilli()
                Pair(startOfYesterday, endOfYesterday)
            }
            SalesPeriodPreset.ESTA_SEMANA -> {
                val startOfWeek = today.with(java.time.DayOfWeek.MONDAY).atStartOfDay(zoneId).toInstant().toEpochMilli()
                Pair(startOfWeek, nowMillis)
            }
            SalesPeriodPreset.ESTE_MES -> {
                val startOfMonth = today.withDayOfMonth(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
                Pair(startOfMonth, nowMillis)
            }
            SalesPeriodPreset.RANGO -> {
                val start = _uiState.value.customStartDate ?: startOfToday
                val end = _uiState.value.customEndDate ?: endOfToday
                Pair(start, end)
            }
        }
    }
}
