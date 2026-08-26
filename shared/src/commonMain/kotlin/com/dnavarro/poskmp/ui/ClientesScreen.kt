package com.dnavarro.poskmp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dnavarro.poskmp.domain.model.Customer
import com.dnavarro.poskmp.theme.ShapeDefaults
import com.dnavarro.poskmp.ui.clientes.AccountStatementDialog
import com.dnavarro.poskmp.ui.clientes.ClientesUiState
import com.dnavarro.poskmp.ui.clientes.ClientesViewModel
import com.dnavarro.poskmp.ui.clientes.CustomerFormDialog
import com.dnavarro.poskmp.ui.clientes.RecordPaymentDialog
import com.dnavarro.poskmp.util.formatPrice
import com.dnavarro.poskmp.util.isAndroid
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import poskmp.shared.generated.resources.Res
import poskmp.shared.generated.resources.add
import poskmp.shared.generated.resources.add_customer_button
import poskmp.shared.generated.resources.add_customer_button_desktop
import poskmp.shared.generated.resources.cancel
import poskmp.shared.generated.resources.clientes_title
import poskmp.shared.generated.resources.close
import poskmp.shared.generated.resources.close_button
import poskmp.shared.generated.resources.customer_action_delete
import poskmp.shared.generated.resources.customer_action_edit
import poskmp.shared.generated.resources.customer_action_payment
import poskmp.shared.generated.resources.customer_action_statement
import poskmp.shared.generated.resources.customer_balance_format
import poskmp.shared.generated.resources.customer_credit_limit_format
import poskmp.shared.generated.resources.customer_no_debt
import poskmp.shared.generated.resources.customer_note_format
import poskmp.shared.generated.resources.customer_purchases_format
import poskmp.shared.generated.resources.delete
import poskmp.shared.generated.resources.delete_button
import poskmp.shared.generated.resources.badge_customer_always_wholesale
import poskmp.shared.generated.resources.delete_customer_confirm_format
import poskmp.shared.generated.resources.delete_customer_has_debt_warning
import poskmp.shared.generated.resources.delete_customer_title
import poskmp.shared.generated.resources.edit
import poskmp.shared.generated.resources.empty_customers_search
import poskmp.shared.generated.resources.empty_customers_subtitle
import poskmp.shared.generated.resources.empty_customers_title
import poskmp.shared.generated.resources.kpi_active_customers_count
import poskmp.shared.generated.resources.kpi_debt_accumulated
import poskmp.shared.generated.resources.kpi_debtors_count
import poskmp.shared.generated.resources.kpi_debtors_stat
import poskmp.shared.generated.resources.kpi_total_customers
import poskmp.shared.generated.resources.kpi_total_debt
import poskmp.shared.generated.resources.payments
import poskmp.shared.generated.resources.person
import poskmp.shared.generated.resources.search
import poskmp.shared.generated.resources.search_customer_placeholder

import androidx.compose.material3.pulltorefresh.PullToRefreshBox

@Composable
fun ClientesScreen(
    viewModel: ClientesViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ClientesContent(
        state = state,
        onRefresh = viewModel::refreshSync,
        onSearchQueryChange = viewModel::onSearchQueryChange,
        onOpenCreateCustomer = viewModel::openCreateCustomerDialog,
        onOpenEditCustomer = viewModel::openEditCustomerDialog,
        onDismissCustomerForm = viewModel::dismissCustomerFormDialog,
        onSaveCustomer = viewModel::saveCustomer,
        onOpenAccountStatement = viewModel::openAccountStatement,
        onDismissAccountStatement = viewModel::dismissAccountStatement,
        onOpenRecordPayment = viewModel::openRecordPaymentDialog,
        onDismissRecordPayment = viewModel::dismissRecordPaymentDialog,
        onRecordPayment = viewModel::recordPayment,
        onDeletePayment = { paymentId ->
            state.selectedCustomerForStatement?.let { customer ->
                viewModel.deletePayment(paymentId, customer.id)
            }
        },
        onOpenDeleteConfirm = viewModel::openDeleteConfirm,
        onDismissDeleteConfirm = viewModel::dismissDeleteConfirm,
        onDeleteCustomer = viewModel::deleteCustomer,
        modifier = modifier
    )
}

@Composable
fun ClientesContent(
    state: ClientesUiState,
    onRefresh: () -> Unit = {},
    onSearchQueryChange: (String) -> Unit,
    onOpenCreateCustomer: () -> Unit,
    onOpenEditCustomer: (Customer) -> Unit,
    onDismissCustomerForm: () -> Unit,
    onSaveCustomer: (id: String?, nombre: String, telefono: String, direccion: String, notas: String, limiteCredito: Double, siempreMayoreo: Boolean) -> Unit,
    onOpenAccountStatement: (Customer) -> Unit,
    onDismissAccountStatement: () -> Unit,
    onOpenRecordPayment: (Customer) -> Unit,
    onDismissRecordPayment: () -> Unit,
    onRecordPayment: (customerId: String, monto: Double, metodoPago: String, notas: String) -> Unit,
    onDeletePayment: (paymentId: String) -> Unit,
    onOpenDeleteConfirm: (Customer) -> Unit,
    onDismissDeleteConfirm: () -> Unit,
    onDeleteCustomer: (id: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val desktopFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        if (!isAndroid()) {
            try {
                desktopFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    LaunchedEffect(
        state.showCustomerForm,
        state.selectedCustomerForStatement,
        state.showPaymentDialogFor,
        state.showDeleteConfirmFor
    ) {
        if (!state.showCustomerForm &&
            state.selectedCustomerForStatement == null &&
            state.showPaymentDialogFor == null &&
            state.showDeleteConfirmFor == null &&
            !isAndroid()
        ) {
            try {
                desktopFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .then(
                if (!isAndroid()) {
                    Modifier
                        .focusRequester(desktopFocusRequester)
                        .focusable()
                        .onPreviewKeyEvent { keyEvent ->
                            if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.F10) {
                                onOpenCreateCustomer()
                                true
                            } else false
                        }
                } else Modifier
            ),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(Res.string.clientes_title),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onOpenCreateCustomer,
                icon = { Icon(painter = painterResource(Res.drawable.add), contentDescription = null) },
                text = {
                    Text(
                        if (isAndroid()) stringResource(Res.string.add_customer_button)
                        else stringResource(Res.string.add_customer_button_desktop)
                    )
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = state.isSyncing,
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize()
            ) {
            val isCompact = maxWidth < 700.dp

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                // Search Bar
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 12.dp),
                    placeholder = {
                        Text(
                            stringResource(Res.string.search_customer_placeholder),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(Res.drawable.search),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        if (state.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(
                                    painter = painterResource(Res.drawable.close),
                                    contentDescription = stringResource(Res.string.close_button),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.extraLarge
                )

                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 88.dp)
                ) {
                    // KPI Cards Grid
                    item {
                        if (isCompact) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CustomerKpiCard(
                                    title = stringResource(Res.string.kpi_total_customers),
                                    value = "${state.debtSummary.totalClientes}",
                                    subtitle = stringResource(Res.string.kpi_active_customers_count, state.debtSummary.totalClientes),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    CustomerKpiCard(
                                        title = stringResource(Res.string.kpi_total_debt),
                                        value = "$${state.debtSummary.totalDeudaAcumulada.toString().formatPrice()}",
                                        subtitle = stringResource(Res.string.kpi_debt_accumulated),
                                        isError = state.debtSummary.totalDeudaAcumulada > 0.0,
                                        modifier = Modifier.weight(1f)
                                    )
                                    CustomerKpiCard(
                                        title = stringResource(Res.string.kpi_debtors_count),
                                        value = "${state.debtSummary.clientesConDeuda}",
                                        subtitle = stringResource(Res.string.kpi_debtors_stat, state.debtSummary.clientesConDeuda),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CustomerKpiCard(
                                    title = stringResource(Res.string.kpi_total_customers),
                                    value = "${state.debtSummary.totalClientes}",
                                    subtitle = stringResource(Res.string.kpi_active_customers_count, state.debtSummary.totalClientes),
                                    modifier = Modifier.weight(1f)
                                )
                                CustomerKpiCard(
                                    title = stringResource(Res.string.kpi_total_debt),
                                    value = "$${state.debtSummary.totalDeudaAcumulada.toString().formatPrice()}",
                                    subtitle = stringResource(Res.string.kpi_debt_accumulated),
                                    isError = state.debtSummary.totalDeudaAcumulada > 0.0,
                                    modifier = Modifier.weight(1f)
                                )
                                CustomerKpiCard(
                                    title = stringResource(Res.string.kpi_debtors_count),
                                    value = "${state.debtSummary.clientesConDeuda}",
                                    subtitle = stringResource(Res.string.kpi_debtors_stat, state.debtSummary.clientesConDeuda),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Customers List Header
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    // Empty States or Customer Items
                    if (state.filteredClientes.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(horizontal = 24.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(Res.drawable.person),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.size(56.dp)
                                    )
                                    Text(
                                        text = if (state.searchQuery.isNotBlank()) {
                                            stringResource(Res.string.empty_customers_search, state.searchQuery)
                                        } else {
                                            stringResource(Res.string.empty_customers_title)
                                        },
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center
                                    )
                                    if (state.searchQuery.isBlank()) {
                                        Text(
                                            text = stringResource(Res.string.empty_customers_subtitle),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        items(state.filteredClientes, key = { it.id }) { customer ->
                            CustomerListItem(
                                customer = customer,
                                onOpenStatement = { onOpenAccountStatement(customer) },
                                onOpenPayment = { onOpenRecordPayment(customer) },
                                onEdit = { onOpenEditCustomer(customer) },
                                onDelete = { onOpenDeleteConfirm(customer) }
                            )
                        }
                    }
                }
            }
        }
    }
    }

    // Dialogs
    if (state.showCustomerForm) {
        CustomerFormDialog(
            customer = state.customerToEdit,
            onDismissRequest = onDismissCustomerForm,
            onSave = onSaveCustomer
        )
    }

    state.selectedCustomerForStatement?.let { customer ->
        AccountStatementDialog(
            customer = customer,
            statementItems = state.statementItems,
            isLoading = state.isLoadingStatement,
            onDismissRequest = onDismissAccountStatement,
            onOpenPaymentDialog = { onOpenRecordPayment(customer) },
            onDeletePayment = onDeletePayment
        )
    }

    state.showPaymentDialogFor?.let { customer ->
        RecordPaymentDialog(
            customer = customer,
            onDismissRequest = onDismissRecordPayment,
            onConfirm = { monto, metodoPago, notas ->
                onRecordPayment(customer.id, monto, metodoPago, notas)
            }
        )
    }

    state.showDeleteConfirmFor?.let { customer ->
        AlertDialog(
            onDismissRequest = onDismissDeleteConfirm,
            shape = MaterialTheme.shapes.large,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            title = {
                Text(
                    text = stringResource(Res.string.delete_customer_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(Res.string.delete_customer_confirm_format, customer.nombre),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (customer.saldoDeudor > 0.0) {
                        Text(
                            text = stringResource(Res.string.delete_customer_has_debt_warning, customer.saldoDeudor.toString().formatPrice()),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { onDeleteCustomer(customer.id) }
                ) {
                    Text(
                        stringResource(Res.string.delete_button),
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissDeleteConfirm) {
                    Text(stringResource(Res.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun CustomerKpiCard(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (isError) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            }
        ),
        shape = ShapeDefaults.cardShape
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CustomerListItem(
    customer: Customer,
    onOpenStatement: () -> Unit,
    onOpenPayment: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val hasDebt = customer.saldoDeudor > 0.0

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = ShapeDefaults.cardShape
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Avatar, Name, Balance Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar Circle
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(
                            color = if (hasDebt) {
                                MaterialTheme.colorScheme.errorContainer
                            } else {
                                MaterialTheme.colorScheme.primaryContainer
                            },
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = customer.nombre.take(1).uppercase(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = if (hasDebt) {
                            MaterialTheme.colorScheme.onErrorContainer
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        }
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Name & Purchases
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = customer.nombre,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (customer.telefono.isNotBlank()) {
                            Text(
                                text = customer.telefono,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "• " + stringResource(Res.string.customer_purchases_format, customer.totalCompras),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (customer.siempreMayoreo) {
                            Surface(
                                shape = MaterialTheme.shapes.extraSmall,
                                color = MaterialTheme.colorScheme.tertiaryContainer
                            ) {
                                Text(
                                    text = stringResource(Res.string.badge_customer_always_wholesale),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Debt Badge
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = if (hasDebt) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer
                    }
                ) {
                    Text(
                        text = if (hasDebt) {
                            stringResource(Res.string.customer_balance_format, customer.saldoDeudor.toString().formatPrice())
                        } else {
                            stringResource(Res.string.customer_no_debt)
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (hasDebt) {
                            MaterialTheme.colorScheme.onErrorContainer
                        } else {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Address & Notes (if available)
            if (customer.direccion.isNotBlank() || customer.notas.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (customer.direccion.isNotBlank()) {
                        Text(
                            text = customer.direccion,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (customer.notas.isNotBlank()) {
                        Text(
                            text = stringResource(Res.string.customer_note_format, customer.notas),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Credit Limit info
            if (customer.limiteCredito > 0.0) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(Res.string.customer_credit_limit_format, customer.limiteCredito.toString().formatPrice()),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.edit),
                        contentDescription = stringResource(Res.string.customer_action_edit),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.delete),
                        contentDescription = stringResource(Res.string.customer_action_delete),
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                OutlinedButton(
                    onClick = onOpenStatement,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        stringResource(Res.string.customer_action_statement),
                        fontSize = 12.sp
                    )
                }

                if (hasDebt) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onOpenPayment,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.payments),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            stringResource(Res.string.customer_action_payment),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}
