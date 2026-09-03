package com.dnavarro.poskmp.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dnavarro.poskmp.domain.model.CashMovementType
import com.dnavarro.poskmp.domain.model.CashierShift
import com.dnavarro.poskmp.domain.model.CategorySalesMetric
import com.dnavarro.poskmp.domain.model.DailySalesMetric
import com.dnavarro.poskmp.domain.model.PaymentMethodMetric
import com.dnavarro.poskmp.domain.model.Sale
import com.dnavarro.poskmp.domain.model.SaleItem
import com.dnavarro.poskmp.theme.ShapeDefaults
import com.dnavarro.poskmp.ui.ventas.HistorialVentasScreen
import com.dnavarro.poskmp.ui.ventas.ProductosVendidosScreen
import com.dnavarro.poskmp.ui.ventas.SalesPeriodPreset
import com.dnavarro.poskmp.ui.ventas.VentasUiState
import com.dnavarro.poskmp.ui.ventas.VentasViewModel
import com.dnavarro.poskmp.util.formatEpochMillisToDateTime
import com.dnavarro.poskmp.util.formatPrice
import com.dnavarro.poskmp.util.formatShiftInterval
import com.dnavarro.poskmp.util.isAndroid
import ir.ehsannarmani.compose_charts.ColumnChart
import ir.ehsannarmani.compose_charts.PieChart
import ir.ehsannarmani.compose_charts.models.BarProperties
import ir.ehsannarmani.compose_charts.models.Bars
import ir.ehsannarmani.compose_charts.models.GridProperties
import ir.ehsannarmani.compose_charts.models.HorizontalIndicatorProperties
import ir.ehsannarmani.compose_charts.models.LabelHelperProperties
import ir.ehsannarmani.compose_charts.models.LabelProperties
import ir.ehsannarmani.compose_charts.models.Pie
import ir.ehsannarmani.compose_charts.models.PopupProperties
import ir.ehsannarmani.compose_charts.models.StrokeStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import poskmp.shared.generated.resources.Res
import poskmp.shared.generated.resources.accept_button
import poskmp.shared.generated.resources.active_shift_badge
import poskmp.shared.generated.resources.all_shifts_option
import poskmp.shared.generated.resources.arrow_up
import poskmp.shared.generated.resources.btn_cash_inflow
import poskmp.shared.generated.resources.btn_cash_inflow_desktop
import poskmp.shared.generated.resources.btn_cash_outflow
import poskmp.shared.generated.resources.btn_cash_outflow_desktop
import poskmp.shared.generated.resources.btn_close_shift
import poskmp.shared.generated.resources.btn_close_shift_desktop
import poskmp.shared.generated.resources.btn_view_sales_history
import poskmp.shared.generated.resources.btn_view_sold_products
import poskmp.shared.generated.resources.cancel
import poskmp.shared.generated.resources.cancel_sale_button
import poskmp.shared.generated.resources.cancel_sale_confirm_action
import poskmp.shared.generated.resources.back
import poskmp.shared.generated.resources.cancel_sale_confirm_message
import poskmp.shared.generated.resources.cancel_sale_confirm_title
import poskmp.shared.generated.resources.cancel_sale_keep_action
import poskmp.shared.generated.resources.card
import poskmp.shared.generated.resources.category_stat_format
import poskmp.shared.generated.resources.check
import poskmp.shared.generated.resources.close_button
import poskmp.shared.generated.resources.custom_range_active_format
import poskmp.shared.generated.resources.daily_sales_avg_format
import poskmp.shared.generated.resources.date_range_end_label
import poskmp.shared.generated.resources.delivery_mode_active_badge
import poskmp.shared.generated.resources.date_range_start_label
import poskmp.shared.generated.resources.empty_category_sales
import poskmp.shared.generated.resources.empty_daily_sales
import poskmp.shared.generated.resources.empty_payment_methods
import poskmp.shared.generated.resources.empty_recent_sales_history
import poskmp.shared.generated.resources.empty_sold_products
import poskmp.shared.generated.resources.kpi_average_ticket
import poskmp.shared.generated.resources.kpi_gross_total
import poskmp.shared.generated.resources.kpi_issued_tickets
import poskmp.shared.generated.resources.kpi_margin
import poskmp.shared.generated.resources.kpi_net_profit
import poskmp.shared.generated.resources.kpi_total_sales
import poskmp.shared.generated.resources.kpi_without_discount
import poskmp.shared.generated.resources.money
import poskmp.shared.generated.resources.money_transfer
import poskmp.shared.generated.resources.no_active_shift_badge
import poskmp.shared.generated.resources.payment_method_credito
import poskmp.shared.generated.resources.payment_method_efectivo
import poskmp.shared.generated.resources.payment_method_label
import poskmp.shared.generated.resources.payment_method_mixto
import poskmp.shared.generated.resources.payment_method_stat_format
import poskmp.shared.generated.resources.payment_method_tarjeta
import poskmp.shared.generated.resources.payment_method_transferencia
import poskmp.shared.generated.resources.payments
import poskmp.shared.generated.resources.period_range
import poskmp.shared.generated.resources.period_this_month
import poskmp.shared.generated.resources.period_this_week
import poskmp.shared.generated.resources.period_today
import poskmp.shared.generated.resources.period_yesterday
import poskmp.shared.generated.resources.person
import poskmp.shared.generated.resources.point_of_sale
import poskmp.shared.generated.resources.products
import poskmp.shared.generated.resources.reprint_receipt_button
import poskmp.shared.generated.resources.sale_status_cancelled
import poskmp.shared.generated.resources.select_date_range_title
import poskmp.shared.generated.resources.shift_filter_label
import poskmp.shared.generated.resources.shift_status_open
import poskmp.shared.generated.resources.ticket_cashier_format
import poskmp.shared.generated.resources.ticket_detail_profit
import poskmp.shared.generated.resources.ticket_detail_title
import poskmp.shared.generated.resources.ticket_detail_total
import poskmp.shared.generated.resources.title_category_sales
import poskmp.shared.generated.resources.title_daily_sales
import poskmp.shared.generated.resources.title_payment_methods
import poskmp.shared.generated.resources.uncategorized_label
import poskmp.shared.generated.resources.ventas_title
import kotlin.time.Duration.Companion.milliseconds

@Serializable
sealed interface VentasSubRoute : NavKey {
    @Serializable
    data object Main : VentasSubRoute

    @Serializable
    data object ProductosVendidos : VentasSubRoute

    @Serializable
    data object HistorialVentas : VentasSubRoute
}

val ventasNavSavedStateConfig = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(VentasSubRoute.Main::class, VentasSubRoute.Main.serializer())
            subclass(VentasSubRoute.ProductosVendidos::class, VentasSubRoute.ProductosVendidos.serializer())
            subclass(VentasSubRoute.HistorialVentas::class, VentasSubRoute.HistorialVentas.serializer())
        }
    }
}

@Composable
fun VentasScreen(
    viewModel: VentasViewModel,
    modifier: Modifier = Modifier,
    onNavigateBack: (() -> Unit)? = null
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Automatically refresh sales analytics whenever VentasScreen is displayed
    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    VentasScreen(
        state = state,
        onSelectPeriod = { viewModel.selectPeriod(it) },
        onSetCustomDateRange = { start, end -> viewModel.setCustomDateRange(start, end) },
        onDismissDateRangePicker = { viewModel.dismissDateRangePicker() },
        onOpenDateRangePicker = { viewModel.openDateRangePicker() },
        onSelectShiftFilter = { viewModel.selectShiftFilter(it) },
        onSelectSaleForDetail = { viewModel.selectSaleForDetail(it) },
        onOpenCancelSaleDialog = { viewModel.openCancelSaleDialog(it) },
        onDismissCancelSaleDialog = { viewModel.dismissCancelSaleDialog() },
        onConfirmCancelSale = { viewModel.cancelSale(it) },
        onOpenInflowDialog = { viewModel.openInflowDialog() },
        onOpenOutflowDialog = { viewModel.openOutflowDialog() },
        onOpenCloseShiftDialog = { viewModel.openCloseShiftDialog() },
        onDismissShiftDialogs = { viewModel.dismissShiftDialogs() },
        onRecordCashMovement = { type, amount, reason -> viewModel.recordCashMovement(type, amount, reason) },
        onCloseShift = { countedCash, notes -> viewModel.closeShift(countedCash, notes) },
        onClearShiftActionResult = { viewModel.clearShiftActionResult() },
        onReprintSaleReceipt = { sale, items -> viewModel.reprintSaleReceipt(sale, items) },
        onNavigateBack = onNavigateBack,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun VentasScreen(
    state: VentasUiState,
    onSelectPeriod: (SalesPeriodPreset) -> Unit,
    onSetCustomDateRange: (startDateMillis: Long, endDateMillis: Long) -> Unit,
    onDismissDateRangePicker: () -> Unit,
    onOpenDateRangePicker: () -> Unit,
    onSelectShiftFilter: (String?) -> Unit = {},
    onSelectSaleForDetail: (Sale?) -> Unit,
    onOpenCancelSaleDialog: (Sale) -> Unit = {},
    onDismissCancelSaleDialog: () -> Unit = {},
    onConfirmCancelSale: (Sale) -> Unit = {},
    onOpenInflowDialog: () -> Unit = {},
    onOpenOutflowDialog: () -> Unit = {},
    onOpenCloseShiftDialog: () -> Unit = {},
    onDismissShiftDialogs: () -> Unit = {},
    onRecordCashMovement: (CashMovementType, Double, String) -> Unit = { _, _, _ -> },
    onCloseShift: (Double, String?) -> Unit = { _, _ -> },
    onClearShiftActionResult: () -> Unit = {},
    onReprintSaleReceipt: (Sale, List<SaleItem>) -> Unit = { _, _ -> },
    onNavigateBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(state.shiftActionSuccess) {
        if (state.shiftActionSuccess != null) {
            delay(3000.milliseconds)
            onClearShiftActionResult()
        }
    }

    val desktopFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        if (!isAndroid()) {
            try {
                desktopFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    LaunchedEffect(
        state.showInflowDialog,
        state.showOutflowDialog,
        state.showCloseShiftDialog,
        state.showDateRangePicker,
        state.selectedSaleDetails,
        state.saleToCancel
    ) {
        if (!state.showInflowDialog &&
            !state.showOutflowDialog &&
            !state.showCloseShiftDialog &&
            !state.showDateRangePicker &&
            state.selectedSaleDetails == null &&
            state.saleToCancel == null &&
            !isAndroid()
        ) {
            try {
                desktopFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    val subBackStack = rememberNavBackStack(ventasNavSavedStateConfig, VentasSubRoute.Main)

    NavDisplay(
        backStack = subBackStack,
        onBack = {
            if (subBackStack.size > 1) {
                subBackStack.removeLastOrNull()
            }
        },
        transitionSpec = {
            (fadeIn(animationSpec = tween(220)) + scaleIn(initialScale = 0.98f, animationSpec = tween(220))) togetherWith
                    fadeOut(animationSpec = tween(180))
        },
        popTransitionSpec = {
            fadeIn(animationSpec = tween(180)) togetherWith
                    (fadeOut(animationSpec = tween(220)) + scaleOut(targetScale = 0.98f, animationSpec = tween(220)))
        },
        predictivePopTransitionSpec = {
            fadeIn(animationSpec = tween(180)) togetherWith
                    (fadeOut(animationSpec = tween(220)) + scaleOut(targetScale = 0.98f, animationSpec = tween(220)))
        },
        entryProvider = entryProvider {
            entry<VentasSubRoute.ProductosVendidos> {
                ProductosVendidosScreen(
                    soldProducts = state.soldProducts,
                    onNavigateBack = {
                        if (subBackStack.size > 1) {
                            subBackStack.removeLastOrNull()
                        }
                    },
                    modifier = modifier
                )
            }
            entry<VentasSubRoute.HistorialVentas> {
                HistorialVentasScreen(
                    sales = state.recentSales,
                    onSelectSale = onSelectSaleForDetail,
                    onCancelSale = onOpenCancelSaleDialog,
                    onNavigateBack = {
                        if (subBackStack.size > 1) {
                            subBackStack.removeLastOrNull()
                        }
                    },
                    modifier = modifier
                )
            }
            entry<VentasSubRoute.Main> {
                Scaffold(
                topBar = {
                TopAppBar(
                    title = {
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = stringResource(Res.string.ventas_title),
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.titleLarge,
                            textAlign = if (onNavigateBack != null) TextAlign.Start else TextAlign.Center
                        )
                    },
                    navigationIcon = {
                        if (onNavigateBack != null) {
                            IconButton(onClick = onNavigateBack) {
                                Icon(
                                    painter = painterResource(Res.drawable.back),
                                    contentDescription = stringResource(Res.string.cancel)
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
            .fillMaxSize()
            .then(
                if (!isAndroid()) {
                    Modifier
                        .focusRequester(desktopFocusRequester)
                        .focusable()
                        .onPreviewKeyEvent { keyEvent ->
                            keyEvent.type == KeyEventType.KeyDown && when (keyEvent.key) {
                                Key.F7 -> {
                                    if (state.activeShift != null) {
                                        onOpenInflowDialog()
                                        true
                                    } else false
                                }

                                Key.F8 -> {
                                    if (state.activeShift != null) {
                                        onOpenOutflowDialog()
                                        true
                                    } else false
                                }

                                Key.F9 -> {
                                    if (state.activeShift != null) {
                                        onOpenCloseShiftDialog()
                                        true
                                    } else false
                                }

                                else -> false
                            }
                        }
                } else Modifier
            )
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
                .padding(horizontal = 16.dp)
        ) {
            val isCompact = maxWidth < 720.dp

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                // Banner de Éxito / Feedback de Acciones de Turno
                if (state.shiftActionSuccess != null) {
                    item {
                        Surface(
                            color = Color(0xFF10B981).copy(alpha = 0.15f),
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(Res.drawable.check),
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = state.shiftActionSuccess,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF047857)
                                )
                            }
                        }
                    }
                }

                // Shift Actions Card (Entrada, Salida, Cerrar turno)
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(Res.drawable.person),
                                        contentDescription = null,
                                        tint = if (state.activeShift != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = if (state.activeShift != null) {
                                            stringResource(Res.string.active_shift_badge, state.activeShift.cashierName)
                                        } else {
                                            stringResource(Res.string.no_active_shift_badge)
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (state.activeShift != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // 3 Action Buttons: Entrada, Salida, Cerrar Turno
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                androidx.compose.material3.FilledTonalButton(
                                    onClick = onOpenInflowDialog,
                                    enabled = state.activeShift != null,
                                    shape = MaterialTheme.shapes.small,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = if (isAndroid()) stringResource(Res.string.btn_cash_inflow)
                                        else stringResource(Res.string.btn_cash_inflow_desktop),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }

                                androidx.compose.material3.FilledTonalButton(
                                    onClick = onOpenOutflowDialog,
                                    enabled = state.activeShift != null,
                                    shape = MaterialTheme.shapes.small,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = if (isAndroid()) stringResource(Res.string.btn_cash_outflow)
                                        else stringResource(Res.string.btn_cash_outflow_desktop),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }

                                androidx.compose.material3.Button(
                                    onClick = onOpenCloseShiftDialog,
                                    enabled = state.activeShift != null,
                                    shape = MaterialTheme.shapes.small,
                                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = if (isAndroid()) stringResource(Res.string.btn_close_shift)
                                        else stringResource(Res.string.btn_close_shift_desktop),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
                // Period Filter Selector
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val presets = SalesPeriodPreset.entries
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
                            ) {
                                presets.forEachIndexed { index, preset ->
                                    val isSelected = state.selectedPeriod == preset
                                    ToggleButton(
                                        checked = isSelected,
                                        onCheckedChange = { onSelectPeriod(preset) },
                                        colors = ToggleButtonDefaults.toggleButtonColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            checkedContainerColor = MaterialTheme.colorScheme.primary,
                                            checkedContentColor = MaterialTheme.colorScheme.onPrimary
                                        ),
                                        modifier = Modifier.semantics { role = Role.RadioButton },
                                        shapes = when (index) {
                                            0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                            presets.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                                        }
                                    ) {
                                        Text(
                                            text = when (preset) {
                                                SalesPeriodPreset.HOY -> stringResource(Res.string.period_today)
                                                SalesPeriodPreset.AYER -> stringResource(Res.string.period_yesterday)
                                                SalesPeriodPreset.ESTA_SEMANA -> stringResource(Res.string.period_this_week)
                                                SalesPeriodPreset.ESTE_MES -> stringResource(Res.string.period_this_month)
                                                SalesPeriodPreset.RANGO -> stringResource(Res.string.period_range)
                                            },
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }

                        if (state.selectedPeriod == SalesPeriodPreset.RANGO && state.customStartDate != null && state.customEndDate != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.clickable { onOpenDateRangePicker() }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = stringResource(
                                            Res.string.custom_range_active_format,
                                            formatDateDisplay(state.customStartDate),
                                            formatDateDisplay(state.customEndDate)
                                        ),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Dropdown Selector de Turnos de Caja
                        var shiftDropdownExpanded by remember { mutableStateOf(false) }
                        val selectedShift = state.shiftsForSelectedPeriod.firstOrNull { it.id == state.selectedShiftId }
                        val selectedShiftLabel = if (selectedShift != null) {
                            formatShiftDisplay(selectedShift)
                        } else {
                            stringResource(Res.string.all_shifts_option)
                        }

                        ExposedDropdownMenuBox(
                            expanded = shiftDropdownExpanded,
                            onExpandedChange = { shiftDropdownExpanded = it },
                            modifier = Modifier.fillMaxWidth(if (isCompact) 1f else 0.55f)
                        ) {
                            OutlinedTextField(
                                value = selectedShiftLabel,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(stringResource(Res.string.shift_filter_label)) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = shiftDropdownExpanded) },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(Res.drawable.person),
                                        contentDescription = null,
                                        tint = if (selectedShift != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true)
                            )

                            ExposedDropdownMenu(
                                expanded = shiftDropdownExpanded,
                                onDismissRequest = { shiftDropdownExpanded = false }
                            ) {
                                // Opción por defecto: Ver información de todos los turnos
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = stringResource(Res.string.all_shifts_option),
                                            fontWeight = if (state.selectedShiftId == null) FontWeight.Bold else FontWeight.Normal,
                                            color = if (state.selectedShiftId == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    onClick = {
                                        onSelectShiftFilter(null)
                                        shiftDropdownExpanded = false
                                    },
                                    leadingIcon = {
                                        if (state.selectedShiftId == null) {
                                            Icon(
                                                painter = painterResource(Res.drawable.check),
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                )

                                if (state.shiftsForSelectedPeriod.isNotEmpty()) {
                                    HorizontalDivider()
                                }

                                state.shiftsForSelectedPeriod.forEach { shift ->
                                    val isCurrent = shift.id == state.selectedShiftId
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(
                                                    text = formatShiftDisplay(shift),
                                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                )
                                                if (!shift.isClosed) {
                                                    Text(
                                                        text = stringResource(Res.string.shift_status_open),
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            onSelectShiftFilter(shift.id)
                                            shiftDropdownExpanded = false
                                        },
                                        leadingIcon = {
                                            if (isCurrent) {
                                                Icon(
                                                    painter = painterResource(Res.drawable.check),
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // KPI Summary Cards Grid
                item {
                    if (isCompact) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            KpiCard(
                                title = stringResource(Res.string.kpi_total_sales),
                                value = "$${state.summary.totalVentas.toString().formatPrice()}",
                                subtitle = if (state.summary.totalSinDescuento > state.summary.totalVentas) {
                                    stringResource(Res.string.kpi_without_discount, state.summary.totalSinDescuento.toString().formatPrice())
                                } else stringResource(Res.string.kpi_gross_total),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                KpiCard(
                                    title = stringResource(Res.string.kpi_net_profit),
                                    value = "$${state.summary.totalGanancia.toString().formatPrice()}",
                                    subtitle = stringResource(Res.string.kpi_margin, state.summary.porcentajeGanancia.toString().formatPrice()),
                                    modifier = Modifier.weight(1f)
                                )

                                KpiCard(
                                    title = stringResource(Res.string.kpi_issued_tickets),
                                    value = "${state.summary.totalTicketCount}",
                                    subtitle = stringResource(Res.string.kpi_average_ticket, state.summary.promedioTicket.toString().formatPrice()),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            KpiCard(
                                title = stringResource(Res.string.kpi_total_sales),
                                value = "$${state.summary.totalVentas.toString().formatPrice()}",
                                subtitle = if (state.summary.totalSinDescuento > state.summary.totalVentas) {
                                    stringResource(Res.string.kpi_without_discount, state.summary.totalSinDescuento.toString().formatPrice())
                                } else stringResource(Res.string.kpi_gross_total),
                                modifier = Modifier.weight(1f)
                            )

                            KpiCard(
                                title = stringResource(Res.string.kpi_net_profit),
                                value = "$${state.summary.totalGanancia.toString().formatPrice()}",
                                subtitle = stringResource(Res.string.kpi_margin, state.summary.porcentajeGanancia.toString().formatPrice()),
                                modifier = Modifier.weight(1f)
                            )

                            KpiCard(
                                title = stringResource(Res.string.kpi_issued_tickets),
                                value = "${state.summary.totalTicketCount}",
                                subtitle = stringResource(Res.string.kpi_average_ticket, state.summary.promedioTicket.toString().formatPrice()),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Daily Sales Bar Chart
                item {
                    DailySalesCard(
                        metrics = state.dailySalesMetrics,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Payment Methods & Category Sales
                item {
                    if (isCompact) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            PaymentMethodsCard(
                                metrics = state.paymentMethodMetrics,
                                modifier = Modifier.fillMaxWidth()
                            )
                            CategorySalesCard(
                                metrics = state.categorySalesMetrics,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            PaymentMethodsCard(
                                metrics = state.paymentMethodMetrics,
                                modifier = Modifier.weight(1f)
                            )
                            CategorySalesCard(
                                metrics = state.categorySalesMetrics,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Botones para abrir las pantallas completas de Productos Vendidos e Historial de Ventas
                item {
                    if (isCompact) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            SoldProductsNavButton(
                                soldProductsCount = state.soldProducts.size,
                                totalPieces = state.soldProducts.sumOf { it.totalUnidades },
                                onClick = {
                                    if (subBackStack.lastOrNull() != VentasSubRoute.ProductosVendidos) {
                                        subBackStack.add(VentasSubRoute.ProductosVendidos)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                            SalesHistoryNavButton(
                                salesCount = state.recentSales.size,
                                activeSalesCount = state.recentSales.count { !it.isCancelled },
                                onClick = {
                                    if (subBackStack.lastOrNull() != VentasSubRoute.HistorialVentas) {
                                        subBackStack.add(VentasSubRoute.HistorialVentas)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            SoldProductsNavButton(
                                soldProductsCount = state.soldProducts.size,
                                totalPieces = state.soldProducts.sumOf { it.totalUnidades },
                                onClick = {
                                    if (subBackStack.lastOrNull() != VentasSubRoute.ProductosVendidos) {
                                        subBackStack.add(VentasSubRoute.ProductosVendidos)
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                            SalesHistoryNavButton(
                                salesCount = state.recentSales.size,
                                activeSalesCount = state.recentSales.count { !it.isCancelled },
                                onClick = {
                                    if (subBackStack.lastOrNull() != VentasSubRoute.HistorialVentas) {
                                        subBackStack.add(VentasSubRoute.HistorialVentas)
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}
}
)

    // Date Range Picker Dialog
    if (state.showDateRangePicker) {
        DateRangePickerDialog(
            initialStartDateMillis = state.customStartDate,
            initialEndDateMillis = state.customEndDate,
            onDismissRequest = onDismissDateRangePicker,
            onDateRangeSelected = onSetCustomDateRange
        )
    }

    // Ticket Detail Dialog
    state.selectedSaleDetails?.let { (sale, items) ->
        SaleDetailDialog(
            sale = sale,
            items = items,
            onDismiss = { onSelectSaleForDetail(null) },
            onCancelSale = { onOpenCancelSaleDialog(sale) },
            onReprintReceipt = { onReprintSaleReceipt(sale, items) }
        )
    }

    // Cancel Sale Confirmation Dialog
    state.saleToCancel?.let { sale ->
        val cancelConfirmButtonFocusRequester = remember { FocusRequester() }

        LaunchedEffect(sale) {
            if (!isAndroid()) {
                delay(100.milliseconds)
                try {
                    cancelConfirmButtonFocusRequester.requestFocus()
                } catch (_: Exception) {}
            }
        }

        AlertDialog(
            onDismissRequest = {
                if (!state.isCancellingSale) onDismissCancelSaleDialog()
            },
            modifier = Modifier.then(
                if (!isAndroid()) {
                    Modifier
                        .focusable()
                        .onPreviewKeyEvent { keyEvent ->
                            keyEvent.type == KeyEventType.KeyDown && when (keyEvent.key) {
                                Key.Enter, Key.NumPadEnter -> {
                                    if (!state.isCancellingSale) {
                                        onConfirmCancelSale(sale)
                                        true
                                    } else false
                                }
                                else -> false
                            }
                        }
                } else Modifier
            ),
            title = {
                Text(
                    text = stringResource(Res.string.cancel_sale_confirm_title),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = stringResource(Res.string.cancel_sale_confirm_message, sale.folio),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                androidx.compose.material3.Button(
                    onClick = { onConfirmCancelSale(sale) },
                    enabled = !state.isCancellingSale,
                    modifier = if (!isAndroid()) Modifier.focusRequester(cancelConfirmButtonFocusRequester) else Modifier,
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text(
                        if (isAndroid()) stringResource(Res.string.cancel_sale_confirm_action)
                        else "${stringResource(Res.string.cancel_sale_confirm_action)} (Enter)",
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismissCancelSaleDialog,
                    enabled = !state.isCancellingSale
                ) {
                    Text(stringResource(Res.string.cancel_sale_keep_action))
                }
            }
        )
    }

    // Cash Movement Dialogs (Entrada / Salida)
    com.dnavarro.poskmp.ui.turnos.CashMovementDialogs(
        showInflowDialog = state.showInflowDialog,
        showOutflowDialog = state.showOutflowDialog,
        isLoading = state.isRecordingMovement,
        errorMessage = state.shiftActionError,
        movements = state.activeShiftMovements,
        onRecordMovement = onRecordCashMovement,
        onDismiss = onDismissShiftDialogs
    )

    // Close Shift Dialog
    if (state.showCloseShiftDialog && state.shiftSummary != null) {
        com.dnavarro.poskmp.ui.turnos.CloseShiftDialog(
            summary = state.shiftSummary,
            isClosing = state.isClosingShift,
            errorMessage = state.shiftActionError,
            onConfirmClose = onCloseShift,
            onDismiss = onDismissShiftDialogs
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DateRangePickerDialog(
    onDismissRequest: () -> Unit,
    onDateRangeSelected: (startDateMillis: Long, endDateMillis: Long) -> Unit,
    initialStartDateMillis: Long? = null,
    initialEndDateMillis: Long? = null
) {
    val todayUtcMillis = remember {
        java.time.LocalDate.now().atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
    }
    val currentYear = remember { java.time.LocalDate.now().year }

    val pastOrPresentSelectableDates = remember(todayUtcMillis, currentYear) {
        object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                return utcTimeMillis <= todayUtcMillis
            }

            override fun isSelectableYear(year: Int): Boolean {
                return year <= currentYear
            }
        }
    }

    val startDatePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialStartDateMillis?.let {
            val millis = java.time.Instant.ofEpochMilli(it)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate()
                .atStartOfDay(java.time.ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli()
            millis.coerceAtMost(todayUtcMillis)
        } ?: todayUtcMillis,
        selectableDates = pastOrPresentSelectableDates,
        yearRange = 2020..currentYear
    )

    val endDatePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialEndDateMillis?.let {
            val millis = java.time.Instant.ofEpochMilli(it)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate()
                .atStartOfDay(java.time.ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli()
            millis.coerceAtMost(todayUtcMillis)
        } ?: todayUtcMillis,
        selectableDates = pastOrPresentSelectableDates,
        yearRange = 2020..currentYear
    )

    var activeDateStep by remember { mutableIntStateOf(0) } // 0: Start Date, 1: End Date

    val acceptButtonFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        if (!isAndroid()) {
            delay(100.milliseconds)
            try {
                acceptButtonFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    DatePickerDialog(
        onDismissRequest = onDismissRequest,
        modifier = Modifier.then(
            if (!isAndroid()) {
                Modifier
                    .focusable()
                    .onPreviewKeyEvent { keyEvent ->
                        keyEvent.type == KeyEventType.KeyDown && when (keyEvent.key) {
                            Key.Enter, Key.NumPadEnter -> {
                                val startUtc = startDatePickerState.selectedDateMillis
                                val endUtc = endDatePickerState.selectedDateMillis ?: startUtc
                                if (startUtc != null && endUtc != null &&
                                    startUtc <= todayUtcMillis &&
                                    endUtc <= todayUtcMillis
                                ) {
                                    val (startLocal, endLocal) = convertUtcDatesToLocalMillis(startUtc, endUtc)
                                    onDateRangeSelected(startLocal, endLocal)
                                    true
                                } else false
                            }
                            else -> false
                        }
                    }
            } else Modifier
        ),
        confirmButton = {
            TextButton(
                onClick = {
                    val startUtc = startDatePickerState.selectedDateMillis
                    val endUtc = endDatePickerState.selectedDateMillis ?: startUtc
                    if (startUtc != null && endUtc != null) {
                        val (startLocal, endLocal) = convertUtcDatesToLocalMillis(startUtc, endUtc)
                        onDateRangeSelected(startLocal, endLocal)
                    }
                },
                modifier = if (!isAndroid()) Modifier.focusRequester(acceptButtonFocusRequester) else Modifier,
                enabled = startDatePickerState.selectedDateMillis != null &&
                        endDatePickerState.selectedDateMillis != null &&
                        startDatePickerState.selectedDateMillis!! <= todayUtcMillis &&
                        endDatePickerState.selectedDateMillis!! <= todayUtcMillis
            ) {
                Text(
                    if (isAndroid()) stringResource(Res.string.accept_button)
                    else "${stringResource(Res.string.accept_button)} (Enter)"
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(Res.string.cancel))
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(Res.string.select_date_range_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
            ) {
                ToggleButton(
                    checked = activeDateStep == 0,
                    onCheckedChange = { activeDateStep = 0 },
                    colors = ToggleButtonDefaults.toggleButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        checkedContainerColor = MaterialTheme.colorScheme.primary,
                        checkedContentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .semantics { role = Role.RadioButton },
                    shapes = ButtonGroupDefaults.connectedLeadingButtonShapes()
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(Res.string.date_range_start_label),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = startDatePickerState.selectedDateMillis?.let { formatDateDisplayUtc(it) } ?: "--/--/----",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                ToggleButton(
                    checked = activeDateStep == 1,
                    onCheckedChange = { activeDateStep = 1 },
                    colors = ToggleButtonDefaults.toggleButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        checkedContainerColor = MaterialTheme.colorScheme.primary,
                        checkedContentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .semantics { role = Role.RadioButton },
                    shapes = ButtonGroupDefaults.connectedTrailingButtonShapes()
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(Res.string.date_range_end_label),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = endDatePickerState.selectedDateMillis?.let { formatDateDisplayUtc(it) } ?: "--/--/----",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            DatePicker(
                state = if (activeDateStep == 0) startDatePickerState else endDatePickerState,
                title = null,
                headline = null,
                showModeToggle = true
            )
        }
    }
}

private fun convertUtcDatesToLocalMillis(utcStartMillis: Long, utcEndMillis: Long): Pair<Long, Long> {
    val startLocalDate = java.time.Instant.ofEpochMilli(utcStartMillis)
        .atZone(java.time.ZoneOffset.UTC)
        .toLocalDate()
    val endLocalDate = java.time.Instant.ofEpochMilli(utcEndMillis)
        .atZone(java.time.ZoneOffset.UTC)
        .toLocalDate()

    val (earlierDate, laterDate) = if (startLocalDate.isAfter(endLocalDate)) {
        Pair(endLocalDate, startLocalDate)
    } else {
        Pair(startLocalDate, endLocalDate)
    }

    val startMillis = earlierDate.atStartOfDay(java.time.ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()
    val endMillis = laterDate.atTime(23, 59, 59, 999_000_000)
        .atZone(java.time.ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()

    return Pair(startMillis, endMillis)
}

private fun formatDateDisplayUtc(utcEpochMillis: Long): String {
    val formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")
    return java.time.Instant.ofEpochMilli(utcEpochMillis)
        .atZone(java.time.ZoneOffset.UTC)
        .toLocalDate()
        .format(formatter)
}

private fun formatDateDisplay(epochMillis: Long): String {
    val formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")
    return java.time.Instant.ofEpochMilli(epochMillis)
        .atZone(java.time.ZoneId.systemDefault())
        .toLocalDate()
        .format(formatter)
}

private fun formatShiftDisplay(shift: CashierShift): String {
    return formatShiftInterval(
        startTime = shift.startTime,
        endTime = shift.endTime,
        isClosed = shift.isClosed,
        cashierName = shift.cashierName
    )
}

@Composable
private fun SoldProductsNavButton(
    soldProductsCount: Int,
    totalPieces: Double,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val piecesFormatted = if (totalPieces % 1.0 == 0.0) {
        totalPieces.toLong().toString()
    } else {
        totalPieces.toString().formatPrice()
    }
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        shape = MaterialTheme.shapes.medium,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(Res.drawable.products),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = stringResource(Res.string.btn_view_sold_products),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (soldProductsCount > 0) {
                            "$piecesFormatted piezas en total ($soldProductsCount productos)"
                        } else {
                            stringResource(Res.string.empty_sold_products)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                painter = painterResource(Res.drawable.arrow_up),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(20.dp)
                    .graphicsLayer { rotationZ = 90f }
            )
        }
    }
}

@Composable
private fun SalesHistoryNavButton(
    salesCount: Int,
    activeSalesCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        shape = MaterialTheme.shapes.medium,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(Res.drawable.point_of_sale),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = stringResource(Res.string.btn_view_sales_history),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (salesCount > 0) {
                            "$salesCount tickets emitidos ($activeSalesCount activos)"
                        } else {
                            stringResource(Res.string.empty_recent_sales_history)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                painter = painterResource(Res.drawable.arrow_up),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier
                    .size(20.dp)
                    .graphicsLayer { rotationZ = 90f }
            )
        }
    }
}

@Composable
private fun KpiCard(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SaleDetailDialog(
    sale: Sale,
    items: List<SaleItem>,
    onDismiss: () -> Unit,
    onCancelSale: () -> Unit,
    onReprintReceipt: () -> Unit
) {
    var isReprinting by remember { mutableStateOf(false) }
    val isCancelled = sale.isCancelled
    val closeButtonFocusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        if (!isAndroid()) {
            delay(100.milliseconds)
            try {
                closeButtonFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.then(
            if (!isAndroid()) {
                Modifier
                    .focusable()
                    .onPreviewKeyEvent { keyEvent ->
                        keyEvent.type == KeyEventType.KeyDown && when (keyEvent.key) {
                            Key.Enter, Key.NumPadEnter-> {
                                onDismiss()
                                true
                            }

                            else -> false
                        }
                    }
            } else Modifier
        ),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(Res.string.ticket_detail_title, sale.folio),
                    fontWeight = FontWeight.Bold
                )
                if (isCancelled) {
                    Surface(
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = stringResource(Res.string.sale_status_cancelled),
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                val dateAndCashier = if (!sale.cashierName.isNullOrBlank()) {
                    "${formatEpochMillisToDateTime(sale.createdAt)} • ${stringResource(Res.string.ticket_cashier_format, sale.cashierName)}"
                } else {
                    formatEpochMillisToDateTime(sale.createdAt)
                }
                Text(
                    text = dateAndCashier,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        stringResource(Res.string.ticket_detail_total, sale.total.toString().formatPrice()),
                        fontWeight = FontWeight.Bold,
                        style = if (isCancelled) TextStyle(textDecoration = TextDecoration.LineThrough) else TextStyle.Default
                    )
                    Text(
                        stringResource(Res.string.ticket_detail_profit, sale.ganancia.toString().formatPrice()),
                        color = if (isCancelled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(modifier = Modifier.height(240.dp)) {
                    items(items, key = { it.id }) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        item.productNombre,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (item.esMayoreo) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "Mayoreo",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    } else if (item.esDelivery) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.tertiaryContainer,
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = stringResource(Res.string.delivery_mode_active_badge),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    "${item.cantidad.toString().formatPrice()} x $${item.precioUnitario.toString().formatPrice()}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                "$${item.subtotal.toString().formatPrice()}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!isCancelled) {
                    androidx.compose.material3.OutlinedButton(
                        onClick = {
                            onDismiss()
                            onCancelSale()
                        },
                        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                    ) {
                        Text(stringResource(Res.string.cancel_sale_button))
                    }
                    androidx.compose.material3.FilledTonalButton(
                        onClick = {
                            if (!isReprinting) {
                                isReprinting = true
                                coroutineScope.launch {
                                    try {
                                        onReprintReceipt()
                                    } finally {
                                        delay(800.milliseconds)
                                        isReprinting = false
                                    }
                                }
                            }
                        },
                        enabled = !isReprinting
                    ) {
                        Text(stringResource(Res.string.reprint_receipt_button))
                    }
                }
                TextButton(
                    onClick = onDismiss,
                    modifier = if (!isAndroid()) Modifier.focusRequester(closeButtonFocusRequester) else Modifier
                ) {
                    Text(
                        if (isAndroid()) stringResource(Res.string.close_button)
                        else "${stringResource(Res.string.close_button)} (Enter)"
                    )
                }
            }
        }
    )
}

private val chartSliceColors = listOf(
    Color(0xFF2E7D32), // Verde
    Color(0xFF1976D2), // Azul
    Color(0xFFE65100), // Naranja
    Color(0xFF7B1FA2), // Morado
    Color(0xFF0097A7), // Turquesa
    Color(0xFFC2185B), // Rosa
    Color(0xFFFBC02D), // Amarillo
    Color(0xFF5D4037), // Café
    Color(0xFF455A64), // Gris Azulado
    Color(0xFFD32F2F)  // Rojo
)

private fun getChartSliceColor(index: Int): Color {
    return chartSliceColors[index % chartSliceColors.size]
}

@Composable
private fun DailySalesCard(
    metrics: List<DailySalesMetric>,
    modifier: Modifier = Modifier
) {
    val activeMetrics = remember(metrics) { metrics.filter { it.totalVentas > 0.0 } }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = ShapeDefaults.cardShape
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(Res.string.title_daily_sales),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (activeMetrics.isNotEmpty()) {
                    val avgDaily = activeMetrics.sumOf { it.totalVentas } / activeMetrics.size
                    Text(
                        stringResource(Res.string.daily_sales_avg_format, avgDaily.toString().formatPrice()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (activeMetrics.isEmpty()) {
                Text(
                    stringResource(Res.string.empty_daily_sales),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val primaryColor = MaterialTheme.colorScheme.primary
                val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant
                val outlineVariantColor = MaterialTheme.colorScheme.outlineVariant
                val inverseSurfaceColor = MaterialTheme.colorScheme.inverseSurface
                val inverseOnSurfaceColor = MaterialTheme.colorScheme.inverseOnSurface

                val barsData = remember(activeMetrics, primaryColor) {
                    activeMetrics.map { item ->
                        Bars(
                            label = item.diaLabel,
                            values = listOf(
                                Bars.Data(
                                    label = item.diaLabel,
                                    value = item.totalVentas,
                                    color = SolidColor(primaryColor),
                                    properties = BarProperties(
                                        cornerRadius = Bars.Data.Radius.Rectangle(
                                            topLeft = 4.dp,
                                            topRight = 4.dp
                                        )
                                    )
                                )
                            )
                        )
                    }
                }

                val barThickness = 22.dp
                val barSpacing = 10.dp
                val scrollState = rememberScrollState()

                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val minWidthForBars = (activeMetrics.size * 64).dp
                    val chartWidth = if (minWidthForBars > maxWidth) minWidthForBars else maxWidth

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(scrollState)
                    ) {
                        ColumnChart(
                            modifier = Modifier
                                .width(chartWidth)
                                .height(230.dp)
                                .padding(top = 8.dp, bottom = 4.dp),
                            data = barsData,
                            barProperties = BarProperties(
                                thickness = barThickness,
                                spacing = barSpacing,
                                cornerRadius = Bars.Data.Radius.Rectangle(
                                    topLeft = 4.dp,
                                    topRight = 4.dp
                                )
                            ),
                            labelProperties = LabelProperties(
                                enabled = true,
                                textStyle = TextStyle(
                                    color = onSurfaceVariantColor,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center
                                )
                            ),
                            indicatorProperties = HorizontalIndicatorProperties(
                                enabled = true,
                                textStyle = TextStyle(
                                    color = onSurfaceVariantColor,
                                    fontSize = 10.sp
                                ),
                                contentBuilder = { value ->
                                    if (value >= 1000) "$${(value / 1000).toString().formatPrice()}k" else "$${value.toInt()}"
                                }
                            ),
                            gridProperties = GridProperties(
                                enabled = true,
                                yAxisProperties = GridProperties.AxisProperties(
                                    enabled = true,
                                    style = StrokeStyle.Dashed(floatArrayOf(10f, 10f)),
                                    color = SolidColor(outlineVariantColor.copy(alpha = 0.4f))
                                )
                            ),
                            popupProperties = PopupProperties(
                                enabled = true,
                                containerColor = inverseSurfaceColor,
                                cornerRadius = 8.dp,
                                textStyle = TextStyle(
                                    color = inverseOnSurfaceColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                contentBuilder = { popup ->
                                    "$${popup.value.toString().formatPrice()}"
                                }
                            ),
                            labelHelperProperties = LabelHelperProperties(enabled = false)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PaymentMethodsCard(
    metrics: List<PaymentMethodMetric>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = ShapeDefaults.cardShape
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                stringResource(Res.string.title_payment_methods),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (metrics.isEmpty() || metrics.all { it.totalRecaudado <= 0.0 }) {
                Text(
                    stringResource(Res.string.empty_payment_methods),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                var selectedMethodName by remember { mutableStateOf<String?>(null) }
                val selectedMetric = metrics.find { it.metodoPago == selectedMethodName }
                val totalSales = remember(metrics) { metrics.sumOf { it.totalRecaudado } }

                val pieData = remember(metrics, selectedMethodName) {
                    metrics.filter { it.totalRecaudado > 0.0 }.mapIndexed { index, item ->
                        val color = getChartSliceColor(index)
                        Pie(
                            label = item.metodoPago,
                            data = item.totalRecaudado,
                            color = color,
                            selectedColor = color,
                            selected = (item.metodoPago == selectedMethodName)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    PieChart(
                        modifier = Modifier.size(240.dp),
                        data = pieData,
                        style = Pie.Style.Stroke(width = 28.dp),
                        spaceDegree = 2f,
                        selectedScale = 1.08f,
                        selectedPaddingDegree = 4f,
                        labelHelperProperties = LabelHelperProperties(enabled = false),
                        onPieClick = { clickedPie ->
                            selectedMethodName = if (selectedMethodName == clickedPie.label) null else clickedPie.label
                        }
                    )

                    // Center Details
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 40.dp)
                    ) {
                        if (selectedMetric != null) {
                            val displayName = getPaymentMethodDisplayName(selectedMetric.metodoPago)
                            Text(
                                text = stringResource(displayName),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${selectedMetric.porcentaje.toString().formatPrice()}%",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "$${selectedMetric.totalRecaudado.toString().formatPrice()}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        } else {
                            Text(
                                text = stringResource(Res.string.kpi_total_sales),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$${totalSales.toString().formatPrice()}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                metrics.forEachIndexed { index, metric ->
                    val color = getChartSliceColor(index)
                    val isSelected = metric.metodoPago == selectedMethodName
                    PaymentMethodMetricRow(
                        metric = metric,
                        color = color,
                        isSelected = isSelected,
                        onClick = {
                            selectedMethodName = if (selectedMethodName == metric.metodoPago) null else metric.metodoPago
                        }
                    )
                    if (index < metrics.lastIndex) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun PaymentMethodMetricRow(
    metric: PaymentMethodMetric,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val methodDisplayName = getPaymentMethodDisplayName(metric.metodoPago)
    val methodIcon = getPaymentMethodIcon(metric.metodoPago)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                color = color.copy(alpha = if (isSelected) 0.35f else 0.15f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(methodIcon),
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Column {
                Text(
                    text = stringResource(methodDisplayName),
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(
                        Res.string.payment_method_stat_format,
                        metric.transaccionesCount,
                        metric.porcentaje.toString().formatPrice()
                    ),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(
            text = "$${metric.totalRecaudado.toString().formatPrice()}",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun CategorySalesCard(
    metrics: List<CategorySalesMetric>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = ShapeDefaults.cardShape
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                stringResource(Res.string.title_category_sales),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (metrics.isEmpty() || metrics.all { it.totalRecaudado <= 0.0 }) {
                Text(
                    stringResource(Res.string.empty_category_sales),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                var selectedCategoryName by remember { mutableStateOf<String?>(null) }
                val selectedMetric = metrics.find { it.categoria == selectedCategoryName }
                val totalSales = remember(metrics) { metrics.sumOf { it.totalRecaudado } }

                val pieData = remember(metrics, selectedCategoryName) {
                    metrics.filter { it.totalRecaudado > 0.0 }.mapIndexed { index, item ->
                        val color = getChartSliceColor(index)
                        Pie(
                            label = item.categoria,
                            data = item.totalRecaudado,
                            color = color,
                            selectedColor = color,
                            selected = (item.categoria == selectedCategoryName)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    PieChart(
                        modifier = Modifier.size(240.dp),
                        data = pieData,
                        style = Pie.Style.Stroke(width = 28.dp),
                        spaceDegree = 2f,
                        selectedScale = 1.08f,
                        selectedPaddingDegree = 4f,
                        labelHelperProperties = LabelHelperProperties(enabled = false),
                        onPieClick = { clickedPie ->
                            selectedCategoryName = if (selectedCategoryName == clickedPie.label) null else clickedPie.label
                        }
                    )

                    // Center Details
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 40.dp)
                    ) {
                        if (selectedMetric != null) {
                            Text(
                                text = selectedMetric.categoria.ifBlank { stringResource(Res.string.uncategorized_label) },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${selectedMetric.porcentaje.toString().formatPrice()}%",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "$${selectedMetric.totalRecaudado.toString().formatPrice()}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        } else {
                            Text(
                                text = stringResource(Res.string.kpi_total_sales),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$${totalSales.toString().formatPrice()}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                metrics.forEachIndexed { index, metric ->
                    val color = getChartSliceColor(index)
                    val isSelected = metric.categoria == selectedCategoryName
                    CategorySalesMetricRow(
                        metric = metric,
                        color = color,
                        isSelected = isSelected,
                        onClick = {
                            selectedCategoryName = if (selectedCategoryName == metric.categoria) null else metric.categoria
                        }
                    )
                    if (index < metrics.lastIndex) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun CategorySalesMetricRow(
    metric: CategorySalesMetric,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                color = color,
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.size(14.dp)
            ) {}
            Column {
                Text(
                    text = metric.categoria.ifBlank { stringResource(Res.string.uncategorized_label) },
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(
                        Res.string.category_stat_format,
                        metric.totalUnidades.toString().formatPrice(),
                        metric.porcentaje.toString().formatPrice()
                    ),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(
            text = "$${metric.totalRecaudado.toString().formatPrice()}",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun getPaymentMethodDisplayName(methodName: String): StringResource {
    return when (methodName.uppercase()) {
        "EFECTIVO" -> Res.string.payment_method_efectivo
        "TARJETA" -> Res.string.payment_method_tarjeta
        "TRANSFERENCIA" -> Res.string.payment_method_transferencia
        "MIXTO" -> Res.string.payment_method_mixto
        "CREDITO" -> Res.string.payment_method_credito
        else -> Res.string.payment_method_label
    }
}

private fun getPaymentMethodIcon(methodName: String): DrawableResource {
    return when (methodName.uppercase()) {
        "EFECTIVO" -> Res.drawable.money
        "TARJETA" -> Res.drawable.card
        "TRANSFERENCIA" -> Res.drawable.money_transfer
        "MIXTO" -> Res.drawable.payments
        "CREDITO" -> Res.drawable.card
        else -> Res.drawable.payments
    }
}



