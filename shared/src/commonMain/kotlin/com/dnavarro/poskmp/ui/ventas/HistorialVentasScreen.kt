package com.dnavarro.poskmp.ui.ventas

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dnavarro.poskmp.domain.model.Sale
import com.dnavarro.poskmp.theme.ShapeDefaults
import com.dnavarro.poskmp.ui.components.AppVerticalScrollbar
import com.dnavarro.poskmp.util.formatEpochMillisToDateTime
import com.dnavarro.poskmp.util.formatPrice
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import poskmp.shared.generated.resources.Res
import poskmp.shared.generated.resources.back
import poskmp.shared.generated.resources.btn_back
import poskmp.shared.generated.resources.delivery
import poskmp.shared.generated.resources.empty_recent_sales_history
import poskmp.shared.generated.resources.filter_sales_all
import poskmp.shared.generated.resources.filter_sales_delivery
import poskmp.shared.generated.resources.filter_sales_local
import poskmp.shared.generated.resources.person
import poskmp.shared.generated.resources.sale_badge_delivery
import poskmp.shared.generated.resources.sale_status_cancelled
import poskmp.shared.generated.resources.sales_history_total_summary
import poskmp.shared.generated.resources.ticket_cashier_format
import poskmp.shared.generated.resources.ticket_folio_format
import poskmp.shared.generated.resources.ticket_items_and_method
import poskmp.shared.generated.resources.ticket_profit_label
import poskmp.shared.generated.resources.title_recent_sales_history

@Composable
fun HistorialVentasScreen(
    sales: List<Sale>,
    isSyncing: Boolean = false,
    onRefresh: () -> Unit = {},
    onSelectSale: (Sale) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableIntStateOf(0) }
    val listState = rememberLazyListState()
    val activeSalesCount = remember(sales) { sales.count { !it.isCancelled } }
    val totalRevenue = remember(sales) { sales.filter { !it.isCancelled }.sumOf { it.total } }
    val localSalesCount = remember(sales) { sales.count { !it.esForanea } }
    val deliverySalesCount = remember(sales) { sales.count { it.esForanea } }

    val filteredSales = remember(sales, selectedFilter) {
        when (selectedFilter) {
            1 -> sales.filter { !it.esForanea }
            2 -> sales.filter { it.esForanea }
            else -> sales
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(Res.string.title_recent_sales_history),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                        if (sales.isNotEmpty()) {
                            Text(
                                text = stringResource(
                                    Res.string.sales_history_total_summary,
                                    sales.size,
                                    activeSalesCount,
                                    totalRevenue.toString().formatPrice()
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            painter = painterResource(Res.drawable.back),
                            contentDescription = stringResource(Res.string.btn_back),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        val pullToRefreshState = rememberPullToRefreshState()
        PullToRefreshBox(
            state = pullToRefreshState,
            isRefreshing = isSyncing,
            onRefresh = onRefresh,
            indicator = {
                PullToRefreshDefaults.LoadingIndicator(
                    state = pullToRefreshState,
                    isRefreshing = isSyncing,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = paddingValues.calculateBottomPadding())
            ) {
                // Quick Summary Cards
                if (sales.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedFilter == 0,
                            onClick = { selectedFilter = 0 },
                            label = { Text(stringResource(Res.string.filter_sales_all, sales.size)) }
                        )
                        FilterChip(
                            selected = selectedFilter == 1,
                            onClick = { selectedFilter = 1 },
                            label = { Text(stringResource(Res.string.filter_sales_local, localSalesCount)) }
                        )
                        FilterChip(
                            selected = selectedFilter == 2,
                            onClick = { selectedFilter = 2 },
                            label = { Text(stringResource(Res.string.filter_sales_delivery, deliverySalesCount)) },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(Res.drawable.delivery),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                }

                if (sales.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(Res.string.empty_recent_sales_history),
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                } else if (filteredSales.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No hay tickets que coincidan con el filtro seleccionado.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(
                                filteredSales,
                                key = { it.id },
                                contentType = { "sale_ticket_card" }
                            ) { sale ->
                                SaleTicketCard(
                                    sale = sale,
                                    onClick = { onSelectSale(sale) }
                                )
                            }
                        }

                        AppVerticalScrollbar(
                            state = listState,
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .fillMaxHeight()
                                .padding(vertical = 4.dp, horizontal = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SaleTicketCard(
    sale: Sale,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isCancelled = sale.isCancelled
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isCancelled) MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        shape = ShapeDefaults.cardShape
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

                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            stringResource(Res.string.ticket_folio_format, sale.folio),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
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
                        if (sale.esForanea) {
                            Surface(
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(Res.drawable.delivery),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = stringResource(Res.string.sale_badge_delivery),
                                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                    Text(
                        text = formatEpochMillisToDateTime(sale.createdAt),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    if (!sale.cashierName.isNullOrBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.person),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = stringResource(Res.string.ticket_cashier_format, sale.cashierName),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "$${sale.total.toString().formatPrice()}",
                       style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = if (isCancelled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                        maxLines = 1
                    )
                    Text(
                        stringResource(Res.string.ticket_profit_label, sale.ganancia.toString().formatPrice()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
