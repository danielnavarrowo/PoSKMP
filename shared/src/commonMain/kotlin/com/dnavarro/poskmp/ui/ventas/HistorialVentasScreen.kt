package com.dnavarro.poskmp.ui.ventas

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Badge
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dnavarro.poskmp.domain.model.Sale
import com.dnavarro.poskmp.theme.ShapeDefaults
import com.dnavarro.poskmp.ui.components.AppVerticalScrollbar
import com.dnavarro.poskmp.ui.getPaymentMethodColor
import com.dnavarro.poskmp.ui.getPaymentMethodIcon
import com.dnavarro.poskmp.util.PlatformBackHandler
import com.dnavarro.poskmp.util.formatEpochMillisToDateTime
import com.dnavarro.poskmp.util.formatPrice
import com.dnavarro.poskmp.util.isAndroid
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import poskmp.shared.generated.resources.Res
import poskmp.shared.generated.resources.apply_filters_button
import poskmp.shared.generated.resources.back
import poskmp.shared.generated.resources.btn_back
import poskmp.shared.generated.resources.check
import poskmp.shared.generated.resources.delivery
import poskmp.shared.generated.resources.empty_recent_sales_history
import poskmp.shared.generated.resources.filter
import poskmp.shared.generated.resources.filter_and_sort_title
import poskmp.shared.generated.resources.filter_off
import poskmp.shared.generated.resources.filter_on
import poskmp.shared.generated.resources.filter_section_title
import poskmp.shared.generated.resources.person
import poskmp.shared.generated.resources.reset_filters
import poskmp.shared.generated.resources.sale_badge_delivery
import poskmp.shared.generated.resources.sale_status_cancelled
import poskmp.shared.generated.resources.sales_history_total_summary
import poskmp.shared.generated.resources.sort_order_asc
import poskmp.shared.generated.resources.sort_order_desc
import poskmp.shared.generated.resources.sort_order_section_title
import poskmp.shared.generated.resources.sort_section_title
import poskmp.shared.generated.resources.ticket_cashier_format
import poskmp.shared.generated.resources.ticket_folio_format
import poskmp.shared.generated.resources.ticket_items_and_method
import poskmp.shared.generated.resources.ticket_profit_label
import poskmp.shared.generated.resources.title_recent_sales_history

enum class SaleSortField(val label: String) {
    TIME("Fecha y hora"),
    TOTAL("Monto total"),
    PIECES("Piezas")
}

enum class SaleTypeFilterOption {
    ALL,
    LOCAL,
    DELIVERY
}

enum class SaleStatusFilterOption {
    ALL,
    COMPLETED,
    CANCELLED
}

internal fun sortSales(
    sales: List<Sale>,
    sortField: SaleSortField,
    isAscending: Boolean
): List<Sale> {
    val comparator = when (sortField) {
        SaleSortField.TIME -> compareBy<Sale> { it.createdAt }.thenBy { it.folio }
        SaleSortField.TOTAL -> compareBy<Sale> { it.total }.thenBy { it.createdAt }
        SaleSortField.PIECES -> compareBy<Sale> { it.totalItems }.thenBy { it.createdAt }
    }
    return if (isAscending) sales.sortedWith(comparator) else sales.sortedWith(comparator.reversed())
}

@Composable
fun HistorialVentasScreen(
    sales: List<Sale>,
    isSyncing: Boolean = false,
    onRefresh: () -> Unit = {},
    onSelectSale: (Sale) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showFilterBottomSheet by remember { mutableStateOf(false) }
    var sortField by rememberSaveable { mutableStateOf(SaleSortField.TIME) }
    var isAscending by rememberSaveable { mutableStateOf(false) }
    var typeFilter by rememberSaveable { mutableStateOf(SaleTypeFilterOption.ALL) }
    var statusFilter by rememberSaveable { mutableStateOf(SaleStatusFilterOption.ALL) }
    var selectedPaymentMethod by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedCashier by rememberSaveable { mutableStateOf<String?>(null) }

    val availableCashiers = remember(sales) {
        sales.mapNotNull { it.cashierName }.filter { it.isNotBlank() }.distinct().sorted()
    }

    val hasActiveFilters = sortField != SaleSortField.TIME ||
            isAscending ||
            typeFilter != SaleTypeFilterOption.ALL ||
            statusFilter != SaleStatusFilterOption.ALL ||
            selectedPaymentMethod != null ||
            selectedCashier != null

    val resetFilters = {
        sortField = SaleSortField.TIME
        isAscending = false
        typeFilter = SaleTypeFilterOption.ALL
        statusFilter = SaleStatusFilterOption.ALL
        selectedPaymentMethod = null
        selectedCashier = null
    }

    val listState = rememberLazyListState()

    val filteredSales = remember(
        sales,
        sortField,
        isAscending,
        typeFilter,
        statusFilter,
        selectedPaymentMethod,
        selectedCashier
    ) {
        val filtered = sales.filter { sale ->
            val matchesType = when (typeFilter) {
                SaleTypeFilterOption.ALL -> true
                SaleTypeFilterOption.LOCAL -> !sale.esForanea
                SaleTypeFilterOption.DELIVERY -> sale.esForanea
            }
            val matchesStatus = when (statusFilter) {
                SaleStatusFilterOption.ALL -> true
                SaleStatusFilterOption.COMPLETED -> !sale.isCancelled
                SaleStatusFilterOption.CANCELLED -> sale.isCancelled
            }
            val matchesPayment = selectedPaymentMethod == null || sale.metodoPago.equals(selectedPaymentMethod, ignoreCase = true)
            val matchesCashier = selectedCashier == null || sale.cashierName == selectedCashier
            matchesType && matchesStatus && matchesPayment && matchesCashier
        }
        sortSales(filtered, sortField, isAscending)
    }

    LaunchedEffect(sortField, isAscending, typeFilter, statusFilter, selectedPaymentMethod, selectedCashier) {
        try {
            listState.scrollToItem(0)
        } catch (_: Exception) {}
    }

    val activeSalesCount = remember(filteredSales) { filteredSales.count { !it.isCancelled } }
    val totalRevenue = remember(filteredSales) { filteredSales.filter { !it.isCancelled }.sumOf { it.total } }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(Res.string.title_recent_sales_history),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                        if (filteredSales.isNotEmpty()) {
                            Text(
                                text = stringResource(
                                    Res.string.sales_history_total_summary,
                                    filteredSales.size,
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
                actions = {
                    Box(modifier = Modifier.padding(end = 8.dp)) {
                        IconButton(
                            onClick = { showFilterBottomSheet = true },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(MaterialShapes.Cookie4Sided.toShape())
                                .background(
                                    if (hasActiveFilters) MaterialTheme.colorScheme.tertiaryContainer
                                    else MaterialTheme.colorScheme.secondaryContainer
                                )
                        ) {
                            Icon(
                                painter = if (hasActiveFilters) painterResource(Res.drawable.filter_on)
                                else painterResource(Res.drawable.filter),
                                contentDescription = stringResource(Res.string.filter_section_title),
                                tint = if (hasActiveFilters) MaterialTheme.colorScheme.onTertiaryContainer
                                else MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        if (hasActiveFilters) {
                            Badge(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = (-2).dp, y = 2.dp)
                                    .size(10.dp),
                                containerColor = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                ),
                scrollBehavior = scrollBehavior
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
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "No hay tickets que coincidan con los filtros seleccionados.",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            if (hasActiveFilters) {
                                OutlinedButton(onClick = resetFilters) {
                                    Icon(
                                        painter = painterResource(Res.drawable.filter_off),
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(stringResource(Res.string.reset_filters))
                                }
                            }
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            itemsIndexed(
                                filteredSales,
                                key = { _, sale -> sale.id },
                                contentType = { _, _ -> "sale_ticket_card" }
                            ) { index, sale ->
                                val shape = if (filteredSales.size == 1) {
                                    ShapeDefaults.cardShape
                                } else if (index == 0) {
                                    ShapeDefaults.topListItemShape
                                } else if (index == filteredSales.lastIndex) {
                                    ShapeDefaults.bottomListItemShape
                                } else {
                                    ShapeDefaults.middleListItemShape
                                }
                                SaleTicketCard(
                                    sale = sale,
                                    onClick = { onSelectSale(sale) },
                                    shape = shape
                                )
                            }
                        }

                        AppVerticalScrollbar(
                            state = listState,
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .fillMaxHeight()
                                .padding(vertical = 4.dp, horizontal = 8.dp)
                        )
                    }
                }
            }
        }

        if (showFilterBottomSheet) {
            SaleFilterBottomSheet(
                sortField = sortField,
                isAscending = isAscending,
                typeFilter = typeFilter,
                statusFilter = statusFilter,
                selectedPaymentMethod = selectedPaymentMethod,
                selectedCashier = selectedCashier,
                availableCashiers = availableCashiers,
                onSortFieldSelected = { sortField = it },
                onSortOrderSelected = { isAscending = it },
                onTypeFilterSelected = { typeFilter = it },
                onStatusFilterSelected = { statusFilter = it },
                onPaymentMethodSelected = { selectedPaymentMethod = it },
                onCashierSelected = { selectedCashier = it },
                onResetFilters = resetFilters,
                onDismissRequest = { showFilterBottomSheet = false }
            )
        }
    }
}

@Composable
fun SaleTicketCard(
    sale: Sale,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = ShapeDefaults.cardShape
) {
    val isCancelled = sale.isCancelled
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isCancelled) MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        shape = shape
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
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = MaterialShapes.Pill.toShape(),
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(getPaymentMethodIcon(sale.metodoPago)),
                            contentDescription = sale.metodoPago,
                            tint = getPaymentMethodColor(sale.metodoPago),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))

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
                        stringResource(Res.string.ticket_items_and_method, sale.totalItems.toString().formatPrice()),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaleFilterBottomSheet(
    sortField: SaleSortField,
    isAscending: Boolean,
    typeFilter: SaleTypeFilterOption,
    statusFilter: SaleStatusFilterOption,
    selectedPaymentMethod: String?,
    selectedCashier: String?,
    availableCashiers: List<String>,
    onSortFieldSelected: (SaleSortField) -> Unit,
    onSortOrderSelected: (Boolean) -> Unit,
    onTypeFilterSelected: (SaleTypeFilterOption) -> Unit,
    onStatusFilterSelected: (SaleStatusFilterOption) -> Unit,
    onPaymentMethodSelected: (String?) -> Unit,
    onCashierSelected: (String?) -> Unit,
    onResetFilters: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
    )
    val coroutineScope = rememberCoroutineScope()
    var isDismissing by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier.then(
            if (!isAndroid()) {
                Modifier.onPreviewKeyEvent { keyEvent ->
                    keyEvent.type == KeyEventType.KeyDown && when (keyEvent.key) {
                        Key.Enter, Key.NumPadEnter -> {
                            onDismissRequest()
                            true
                        }
                        else -> false
                    }
                }
            } else Modifier
        )
    ) {
        PlatformBackHandler(enabled = sheetState.isVisible && !isDismissing) {
            isDismissing = true
            coroutineScope.launch {
                try {
                    sheetState.hide()
                } finally {
                    if (!sheetState.isVisible) {
                        onDismissRequest()
                    } else {
                        isDismissing = false
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(Res.string.filter_and_sort_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(
                    onClick = onResetFilters,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(MaterialShapes.Ghostish.toShape())
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.filter_off),
                        contentDescription = stringResource(Res.string.reset_filters)
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Scrollable Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section 1: Ordenar por
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(Res.string.sort_section_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        SaleSortField.entries.forEach { field ->
                            FilterChip(
                                selected = sortField == field,
                                onClick = { onSortFieldSelected(field) },
                                label = { Text(field.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                }

                // Section 2: Dirección del orden
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(Res.string.sort_order_section_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val orderOptions = listOf(
                            false to "▼ " + stringResource(Res.string.sort_order_desc),
                            true to "▲ " + stringResource(Res.string.sort_order_asc)
                        )
                        orderOptions.forEachIndexed { index, (order, label) ->
                            val isSelected = isAscending == order
                            ToggleButton(
                                checked = isSelected,
                                onCheckedChange = { onSortOrderSelected(order) },
                                colors = ToggleButtonDefaults.colors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    checkedContainerColor = MaterialTheme.colorScheme.primary,
                                    checkedContentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .semantics { role = Role.RadioButton },
                                shapes = when (index) {
                                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                    orderOptions.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                                }
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // Section 3: Tipo de venta
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Tipo de venta",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val typeOptions = listOf(
                            SaleTypeFilterOption.ALL to "Todos",
                            SaleTypeFilterOption.LOCAL to "En Tienda",
                            SaleTypeFilterOption.DELIVERY to "A Domicilio"
                        )
                        typeOptions.forEachIndexed { index, (option, label) ->
                            val isSelected = typeFilter == option
                            ToggleButton(
                                checked = isSelected,
                                onCheckedChange = { onTypeFilterSelected(option) },
                                colors = ToggleButtonDefaults.colors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    checkedContainerColor = MaterialTheme.colorScheme.primary,
                                    checkedContentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .semantics { role = Role.RadioButton },
                                shapes = when (index) {
                                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                    typeOptions.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                                }
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // Section 4: Estado del Ticket
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Estado del ticket",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val statusOptions = listOf(
                            SaleStatusFilterOption.ALL to "Todos",
                            SaleStatusFilterOption.COMPLETED to "Activos",
                            SaleStatusFilterOption.CANCELLED to "Cancelados"
                        )
                        statusOptions.forEachIndexed { index, (option, label) ->
                            val isSelected = statusFilter == option
                            ToggleButton(
                                checked = isSelected,
                                onCheckedChange = { onStatusFilterSelected(option) },
                                colors = ToggleButtonDefaults.colors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    checkedContainerColor = MaterialTheme.colorScheme.primary,
                                    checkedContentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .semantics { role = Role.RadioButton },
                                shapes = when (index) {
                                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                    statusOptions.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                                }
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // Section 5: Método de Pago
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Método de pago",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        val methods = listOf(
                            null to "Todos",
                            "EFECTIVO" to "Efectivo",
                            "TARJETA" to "Tarjeta",
                            "TRANSFERENCIA" to "Transferencia",
                            "MIXTO" to "Mixto",
                            "CREDITO" to "Crédito"
                        )
                        methods.forEach { (method, label) ->
                            val isSelected = selectedPaymentMethod == method
                            FilterChip(
                                selected = isSelected,
                                onClick = { onPaymentMethodSelected(method) },
                                label = { Text(label) },
                                leadingIcon = if (method != null) {
                                    {
                                        Icon(
                                            painter = painterResource(getPaymentMethodIcon(method)),
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                }

                // Section 6: Cajero (if available)
                if (availableCashiers.size > 1) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Cajero",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            FilterChip(
                                selected = selectedCashier == null,
                                onClick = { onCashierSelected(null) },
                                label = { Text("Todos") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                            availableCashiers.forEach { cashier ->
                                FilterChip(
                                    selected = selectedCashier == cashier,
                                    onClick = { onCashierSelected(cashier) },
                                    label = { Text(cashier) },
                                    leadingIcon = {
                                        Icon(
                                            painter = painterResource(Res.drawable.person),
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Apply Button
            ElevatedButton(
                onClick = onDismissRequest,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.elevatedButtonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary
                )
            ) {
                Icon(
                    painter = painterResource(Res.drawable.check),
                    contentDescription = stringResource(Res.string.apply_filters_button),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(Res.string.apply_filters_button),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
