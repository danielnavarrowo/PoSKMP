package com.dnavarro.poskmp.ui.preview

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.dnavarro.poskmp.domain.model.Cashier
import com.dnavarro.poskmp.domain.model.Customer
import com.dnavarro.poskmp.domain.model.SalesSummary
import com.dnavarro.poskmp.theme.AppTheme
import com.dnavarro.poskmp.ui.AjustesScreen
import com.dnavarro.poskmp.ui.ClientesContent
import com.dnavarro.poskmp.ui.VentasScreen
import com.dnavarro.poskmp.ui.clientes.ClientesUiState
import com.dnavarro.poskmp.ui.turnos.OpenShiftView
import com.dnavarro.poskmp.ui.ventas.VentasUiState

@FormFactorPreviews
@Composable
fun ClientesScreenPreview() {
    AppTheme {
        ClientesContent(
            state = ClientesUiState(
                clientes = listOf(
                    Customer(id = "1", nombre = "Juan Pérez", telefono = "555-1234", direccion = "Av. Reforma 100", saldoDeudor = 350.0),
                    Customer(id = "2", nombre = "María González", telefono = "555-5678", direccion = "Calle Juárez 200", saldoDeudor = 0.0),
                    Customer(id = "3", nombre = "Abarrotes Don Pepe", telefono = "555-9012", direccion = "Hidalgo 50", saldoDeudor = 1250.0)
                ),
                filteredClientes = listOf(
                    Customer(id = "1", nombre = "Juan Pérez", telefono = "555-1234", direccion = "Av. Reforma 100", saldoDeudor = 350.0),
                    Customer(id = "2", nombre = "María González", telefono = "555-5678", direccion = "Calle Juárez 200", saldoDeudor = 0.0),
                    Customer(id = "3", nombre = "Abarrotes Don Pepe", telefono = "555-9012", direccion = "Hidalgo 50", saldoDeudor = 1250.0)
                )
            ),
            onSearchQueryChange = {},
            onOpenCreateCustomer = {},
            onOpenEditCustomer = {},
            onDismissCustomerForm = {},
            onSaveCustomer = { _, _, _, _, _, _, _ -> },
            onOpenAccountStatement = {},
            onDismissAccountStatement = {},
            onOpenRecordPayment = {},
            onDismissRecordPayment = {},
            onRecordPayment = { _, _, _, _ -> },
            onDeletePayment = {},
            onOpenDeleteConfirm = {},
            onDismissDeleteConfirm = {},
            onDeleteCustomer = {}
        )
    }
}

@FormFactorPreviews
@Composable
fun VentasScreenPreview() {
    AppTheme {
        VentasScreen(
            state = VentasUiState(
                summary = SalesSummary(
                    totalVentas = 15420.0,
                    totalSinDescuento = 15420.0,
                    totalCosto = 10800.0,
                    totalGanancia = 4620.0,
                    porcentajeGanancia = 29.96,
                    totalTicketCount = 42L,
                    promedioTicket = 367.14
                )
            ),
            onSelectPeriod = {},
            onSetCustomDateRange = { _, _ -> },
            onDismissDateRangePicker = {},
            onOpenDateRangePicker = {},
            onSelectSaleForDetail = {}
        )
    }
}

@FormFactorPreviews
@Composable
fun AjustesScreenPreview() {
    AppTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            AjustesScreen()
        }
    }
}

@FormFactorPreviews
@Composable
fun OpenShiftPreview() {
    AppTheme {
        OpenShiftView(
            cashiers = listOf(
                Cashier(id = "1", nombre = "Cajero Principal", pin = "1234", activo = true, createdAt = 0L, updatedAt = 0L),
                Cashier(id = "2", nombre = "Cajero Turno Tarde", pin = "5678", activo = true, createdAt = 0L, updatedAt = 0L)
            ),
            isOpening = false,
            errorMessage = null,
            onOpenShift = { _, _, _ -> },
            onClearError = {}
        )
    }
}
