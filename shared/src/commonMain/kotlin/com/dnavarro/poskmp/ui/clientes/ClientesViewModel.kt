package com.dnavarro.poskmp.ui.clientes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dnavarro.poskmp.domain.model.Customer
import com.dnavarro.poskmp.domain.usecase.GetCustomerAccountStatementUseCase
import com.dnavarro.poskmp.domain.usecase.GetCustomersUseCase
import com.dnavarro.poskmp.domain.usecase.RecordCustomerPaymentUseCase
import com.dnavarro.poskmp.domain.usecase.SaveCustomerUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ClientesViewModel(
    private val getCustomersUseCase: GetCustomersUseCase,
    private val saveCustomerUseCase: SaveCustomerUseCase,
    private val recordCustomerPaymentUseCase: RecordCustomerPaymentUseCase,
    private val getCustomerAccountStatementUseCase: GetCustomerAccountStatementUseCase
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _internalState = MutableStateFlow(ClientesUiState())

    val uiState: StateFlow<ClientesUiState> = combine(
        getCustomersUseCase(),
        _searchQuery,
        _internalState
    ) { customerList, query, internal ->
        val filtered = if (query.isBlank()) {
            customerList
        } else {
            val q = query.trim().lowercase()
            customerList.filter {
                it.nombre.lowercase().contains(q) ||
                it.telefono.lowercase().contains(q) ||
                it.direccion.lowercase().contains(q)
            }
        }

        val totalDebt = customerList.filter { it.saldoDeudor > 0.0 }.sumOf { it.saldoDeudor }
        val debtorsCount = customerList.count { it.saldoDeudor > 0.0 }.toLong()
        val debtSummary = internal.debtSummary.copy(
            totalClientes = customerList.size.toLong(),
            totalDeudaAcumulada = totalDebt,
            clientesConDeuda = debtorsCount
        )

        internal.copy(
            clientes = customerList,
            filteredClientes = filtered,
            searchQuery = query,
            debtSummary = debtSummary
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ClientesUiState()
    )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun openCreateCustomerDialog() {
        _internalState.update {
            it.copy(showCustomerForm = true, customerToEdit = null)
        }
    }

    fun openEditCustomerDialog(customer: Customer) {
        _internalState.update {
            it.copy(showCustomerForm = true, customerToEdit = customer)
        }
    }

    fun dismissCustomerFormDialog() {
        _internalState.update {
            it.copy(showCustomerForm = false, customerToEdit = null)
        }
    }

    fun saveCustomer(
        id: String?,
        nombre: String,
        telefono: String,
        direccion: String,
        notas: String,
        limiteCredito: Double
    ) {
        viewModelScope.launch {
            val result = saveCustomerUseCase(
                id = id,
                nombre = nombre,
                telefono = telefono,
                direccion = direccion,
                notas = notas,
                limiteCredito = limiteCredito
            )
            result.onSuccess {
                dismissCustomerFormDialog()
            }.onFailure { error ->
                _internalState.update {
                    it.copy(errorMessage = error.message ?: "Error al guardar cliente")
                }
            }
        }
    }

    fun openAccountStatement(customer: Customer) {
        _internalState.update {
            it.copy(
                selectedCustomerForStatement = customer,
                isLoadingStatement = true,
                statementItems = emptyList()
            )
        }
        loadAccountStatement(customer.id)
    }

    private fun loadAccountStatement(customerId: String) {
        viewModelScope.launch {
            val items = getCustomerAccountStatementUseCase(customerId)
            val updatedCustomer = getCustomersUseCase.getById(customerId)
            _internalState.update {
                it.copy(
                    statementItems = items,
                    isLoadingStatement = false,
                    selectedCustomerForStatement = updatedCustomer ?: it.selectedCustomerForStatement
                )
            }
        }
    }

    fun dismissAccountStatement() {
        _internalState.update {
            it.copy(
                selectedCustomerForStatement = null,
                statementItems = emptyList(),
                isLoadingStatement = false
            )
        }
    }

    fun openRecordPaymentDialog(customer: Customer) {
        _internalState.update {
            it.copy(showPaymentDialogFor = customer)
        }
    }

    fun dismissRecordPaymentDialog() {
        _internalState.update {
            it.copy(showPaymentDialogFor = null)
        }
    }

    fun recordPayment(
        customerId: String,
        monto: Double,
        metodoPago: String,
        notas: String
    ) {
        viewModelScope.launch {
            val result = recordCustomerPaymentUseCase(
                customerId = customerId,
                monto = monto,
                metodoPago = metodoPago,
                notas = notas
            )
            result.onSuccess {
                dismissRecordPaymentDialog()
                if (_internalState.value.selectedCustomerForStatement?.id == customerId) {
                    loadAccountStatement(customerId)
                }
            }.onFailure { error ->
                _internalState.update {
                    it.copy(errorMessage = error.message ?: "Error al registrar abono")
                }
            }
        }
    }

    fun deletePayment(paymentId: String, customerId: String) {
        viewModelScope.launch {
            recordCustomerPaymentUseCase.deletePayment(paymentId)
            if (_internalState.value.selectedCustomerForStatement?.id == customerId) {
                loadAccountStatement(customerId)
            }
        }
    }

    fun openDeleteConfirm(customer: Customer) {
        _internalState.update {
            it.copy(showDeleteConfirmFor = customer)
        }
    }

    fun dismissDeleteConfirm() {
        _internalState.update {
            it.copy(showDeleteConfirmFor = null)
        }
    }

    fun deleteCustomer(id: String) {
        viewModelScope.launch {
            saveCustomerUseCase.setCustomerActiveStatus(id, activo = false)
            dismissDeleteConfirm()
            if (_internalState.value.selectedCustomerForStatement?.id == id) {
                dismissAccountStatement()
            }
        }
    }

    fun clearErrorMessage() {
        _internalState.update { it.copy(errorMessage = null) }
    }

    fun clearUserMessage() {
        _internalState.update { it.copy(userMessage = null) }
    }
}
