package com.dnavarro.poskmp.ui

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dnavarro.poskmp.domain.model.ProductSalesMetric
import com.dnavarro.poskmp.domain.model.Sale
import com.dnavarro.poskmp.domain.model.SaleItem
import com.dnavarro.poskmp.ui.ventas.SalesPeriodPreset
import com.dnavarro.poskmp.ui.ventas.VentasUiState
import com.dnavarro.poskmp.ui.ventas.VentasViewModel
import com.dnavarro.poskmp.util.formatPrice
import org.jetbrains.compose.resources.stringResource
import poskmp.shared.generated.resources.Res
import poskmp.shared.generated.resources.accept_button
import poskmp.shared.generated.resources.cancel
import poskmp.shared.generated.resources.close_button
import poskmp.shared.generated.resources.custom_range_active_format
import poskmp.shared.generated.resources.empty_period_sales
import poskmp.shared.generated.resources.empty_recent_sales_history
import poskmp.shared.generated.resources.kpi_average_ticket
import poskmp.shared.generated.resources.kpi_gross_total
import poskmp.shared.generated.resources.kpi_issued_tickets
import poskmp.shared.generated.resources.kpi_margin
import poskmp.shared.generated.resources.kpi_net_profit
import poskmp.shared.generated.resources.kpi_total_sales
import poskmp.shared.generated.resources.kpi_without_discount
import poskmp.shared.generated.resources.period_range
import poskmp.shared.generated.resources.period_this_month
import poskmp.shared.generated.resources.period_this_week
import poskmp.shared.generated.resources.period_today
import poskmp.shared.generated.resources.period_yesterday
import poskmp.shared.generated.resources.select_date_range_title
import poskmp.shared.generated.resources.ticket_detail_profit
import poskmp.shared.generated.resources.ticket_detail_title
import poskmp.shared.generated.resources.ticket_detail_total
import poskmp.shared.generated.resources.ticket_folio_format
import poskmp.shared.generated.resources.ticket_items_and_method
import poskmp.shared.generated.resources.ticket_profit_label
import poskmp.shared.generated.resources.title_least_sellers
import poskmp.shared.generated.resources.title_recent_sales_history
import poskmp.shared.generated.resources.title_top_sellers
import poskmp.shared.generated.resources.units_sold_count
import poskmp.shared.generated.resources.ventas_title

@Composable
fun VentasScreen(
    viewModel: VentasViewModel,
    modifier: Modifier = Modifier
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
        onSelectSaleForDetail = { viewModel.selectSaleForDetail(it) },
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
    onSelectSaleForDetail: (Sale?) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(Res.string.ventas_title),
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
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

                // Products Performance Tables (Top & Least Sold)
                item {
                    if (isCompact) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            TopSellersCard(
                                topSellers = state.topSellers,
                                modifier = Modifier.fillMaxWidth()
                            )
                            LeastSellersCard(
                                leastSellers = state.leastSellers,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            TopSellersCard(
                                topSellers = state.topSellers,
                                modifier = Modifier.weight(1f)
                            )
                            LeastSellersCard(
                                leastSellers = state.leastSellers,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Recent Ticket History List Header
                item {
                    Text(
                        stringResource(Res.string.title_recent_sales_history),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                if (state.recentSales.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    stringResource(Res.string.empty_recent_sales_history),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                } else {
                    items(state.recentSales, key = { it.id }) { sale ->
                        SaleTicketCard(
                            sale = sale,
                            onClick = { onSelectSaleForDetail(sale) }
                        )
                    }
                }
            }
        }
    }

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
            onDismiss = { onSelectSaleForDetail(null) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateRangePickerDialog(
    onDismissRequest: () -> Unit,
    onDateRangeSelected: (startDateMillis: Long, endDateMillis: Long) -> Unit,
    initialStartDateMillis: Long? = null,
    initialEndDateMillis: Long? = null
) {
    val dateRangePickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = initialStartDateMillis?.let {
            java.time.Instant.ofEpochMilli(it)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate()
                .atStartOfDay(java.time.ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli()
        },
        initialSelectedEndDateMillis = initialEndDateMillis?.let {
            java.time.Instant.ofEpochMilli(it)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate()
                .atStartOfDay(java.time.ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli()
        }
    )

    DatePickerDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                onClick = {
                    val startUtc = dateRangePickerState.selectedStartDateMillis
                    val endUtc = dateRangePickerState.selectedEndDateMillis ?: startUtc
                    if (startUtc != null && endUtc != null) {
                        val (startLocal, endLocal) = convertUtcRangeToLocalMillis(startUtc, endUtc)
                        onDateRangeSelected(startLocal, endLocal)
                    }
                },
                enabled = dateRangePickerState.selectedStartDateMillis != null
            ) {
                Text(stringResource(Res.string.accept_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(Res.string.cancel))
            }
        }
    ) {
        DateRangePicker(
            state = dateRangePickerState,
            title = {
                Text(
                    text = stringResource(Res.string.select_date_range_title),
                    modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            showModeToggle = false,
            modifier = Modifier.weight(1f)
        )
    }
}

private fun convertUtcRangeToLocalMillis(utcStartMillis: Long, utcEndMillis: Long): Pair<Long, Long> {
    val startLocalDate = java.time.Instant.ofEpochMilli(utcStartMillis)
        .atZone(java.time.ZoneOffset.UTC)
        .toLocalDate()
    val endLocalDate = java.time.Instant.ofEpochMilli(utcEndMillis)
        .atZone(java.time.ZoneOffset.UTC)
        .toLocalDate()

    val startMillis = startLocalDate.atStartOfDay(java.time.ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()
    val endMillis = endLocalDate.atTime(23, 59, 59, 999_000_000)
        .atZone(java.time.ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()

    return Pair(startMillis, endMillis)
}

private fun formatDateDisplay(epochMillis: Long): String {
    val formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")
    return java.time.Instant.ofEpochMilli(epochMillis)
        .atZone(java.time.ZoneId.systemDefault())
        .toLocalDate()
        .format(formatter)
}

@Composable
private fun TopSellersCard(
    topSellers: List<ProductSalesMetric>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                stringResource(Res.string.title_top_sellers),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (topSellers.isEmpty()) {
                Text(
                    stringResource(Res.string.empty_period_sales),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                topSellers.take(5).forEachIndexed { index, metric ->
                    ProductMetricRow(metric)
                    if (index < topSellers.take(5).lastIndex) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun LeastSellersCard(
    leastSellers: List<ProductSalesMetric>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                stringResource(Res.string.title_least_sellers),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (leastSellers.isEmpty()) {
                Text(
                    stringResource(Res.string.empty_period_sales),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                leastSellers.take(5).forEachIndexed { index, metric ->
                    ProductMetricRow(metric)
                    if (index < leastSellers.take(5).lastIndex) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                    }
                }
            }
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
private fun ProductMetricRow(metric: ProductSalesMetric) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = metric.productNombre,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = stringResource(Res.string.units_sold_count, metric.totalUnidades.toString().formatPrice()),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "$${metric.totalRecaudado.toString().formatPrice()}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(Res.string.ticket_profit_label, metric.gananciaGenerada.toString().formatPrice()),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SaleTicketCard(
    sale: Sale,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f, fill = false),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            "#${sale.folio}",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontSize = 14.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        stringResource(Res.string.ticket_folio_format, sale.folio),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        stringResource(Res.string.ticket_items_and_method, sale.totalItems.toString().formatPrice(), sale.metodoPago),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "$${sale.total.toString().formatPrice()}",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1
                )
                Text(
                    stringResource(Res.string.ticket_profit_label, sale.ganancia.toString().formatPrice()),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun SaleDetailDialog(
    sale: Sale,
    items: List<SaleItem>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.ticket_detail_title, sale.folio), fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(Res.string.ticket_detail_total, sale.total.toString().formatPrice()), fontWeight = FontWeight.Bold)
                    Text(stringResource(Res.string.ticket_detail_profit, sale.ganancia.toString().formatPrice()), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
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
                                Text(
                                    item.productNombre,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
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
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.close_button))
            }
        }
    )
}


