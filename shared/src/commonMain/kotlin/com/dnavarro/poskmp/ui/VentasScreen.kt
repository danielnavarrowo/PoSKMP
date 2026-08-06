package com.dnavarro.poskmp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dnavarro.poskmp.domain.model.ProductSalesMetric
import com.dnavarro.poskmp.domain.model.Sale
import com.dnavarro.poskmp.domain.model.SaleItem
import com.dnavarro.poskmp.ui.ventas.SalesPeriodPreset
import com.dnavarro.poskmp.ui.ventas.VentasViewModel
import com.dnavarro.poskmp.util.formatPrice
import org.jetbrains.compose.resources.stringResource
import poskmp.shared.generated.resources.Res
import poskmp.shared.generated.resources.close_button
import poskmp.shared.generated.resources.empty_period_sales
import poskmp.shared.generated.resources.empty_recent_sales_history
import poskmp.shared.generated.resources.kpi_average_ticket
import poskmp.shared.generated.resources.kpi_gross_total
import poskmp.shared.generated.resources.kpi_issued_tickets
import poskmp.shared.generated.resources.kpi_margin
import poskmp.shared.generated.resources.kpi_net_profit
import poskmp.shared.generated.resources.kpi_total_sales
import poskmp.shared.generated.resources.kpi_without_discount
import poskmp.shared.generated.resources.period_this_month
import poskmp.shared.generated.resources.period_this_week
import poskmp.shared.generated.resources.period_today
import poskmp.shared.generated.resources.period_yesterday
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Header Title & Period Filter Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(Res.string.ventas_title),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
            ) {
                val presets = SalesPeriodPreset.entries
                presets.forEachIndexed { index, preset ->
                    val isSelected = state.selectedPeriod == preset
                    ToggleButton(
                        checked = isSelected,
                        onCheckedChange = { viewModel.selectPeriod(preset) },
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
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Main Scrollable Content
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // KPI Summary Cards Grid
            item {
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

            // Products Performance Tables Row (Top & Least Sold)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Top Sold
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                stringResource(Res.string.title_top_sellers),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            if (state.topSellers.isEmpty()) {
                                Text(
                                    stringResource(Res.string.empty_period_sales),
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                state.topSellers.take(5).forEach { metric ->
                                    ProductMetricRow(metric)
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                                }
                            }
                        }
                    }

                    // Least Sold
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                stringResource(Res.string.title_least_sellers),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            if (state.leastSellers.isEmpty()) {
                                Text(
                                    stringResource(Res.string.empty_period_sales),
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                state.leastSellers.take(5).forEach { metric ->
                                    ProductMetricRow(metric)
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Recent Ticket History List Header
            item {
                Text(
                    stringResource(Res.string.title_recent_sales_history),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            if (state.recentSales.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                    ) {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text(
                                stringResource(Res.string.empty_recent_sales_history),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            } else {
                items(state.recentSales) { sale ->
                    SaleTicketCard(
                        sale = sale,
                        onClick = { viewModel.selectSaleForDetail(sale) }
                    )
                }
            }
        }
    }

    // Ticket Detail Dialog
    state.selectedSaleDetails?.let { (sale, items) ->
        SaleDetailDialog(
            sale = sale,
            items = items,
            onDismiss = { viewModel.selectSaleForDetail(null) }
        )
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
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            Text(metric.productNombre, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text(stringResource(Res.string.units_sold_count, metric.totalUnidades.toString().formatPrice()), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("$${metric.totalRecaudado.toString().formatPrice()}", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(stringResource(Res.string.ticket_profit_label, metric.gananciaGenerada.toString().formatPrice()), fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SaleTicketCard(
    sale: Sale,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("#${sale.folio}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer, fontSize = 14.sp)
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(stringResource(Res.string.ticket_folio_format, sale.folio), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(stringResource(Res.string.ticket_items_and_method, sale.totalItems.toString().formatPrice(), sale.metodoPago), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text("$${sale.total.toString().formatPrice()}", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                Text(stringResource(Res.string.ticket_profit_label, sale.ganancia.toString().formatPrice()), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    items(items) { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.productNombre, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text("${item.cantidad.toString().formatPrice()} x $${item.precioUnitario.toString().formatPrice()}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text("$${item.subtotal.toString().formatPrice()}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
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
