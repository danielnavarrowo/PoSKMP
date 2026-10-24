package com.dnavarro.poskmp.ui.clientes

import com.dnavarro.poskmp.domain.model.AccountStatementItem
import com.dnavarro.poskmp.domain.model.Customer
import com.dnavarro.poskmp.domain.model.CustomerDebtSummary
import com.dnavarro.poskmp.domain.model.Sale
import com.dnavarro.poskmp.domain.model.SaleItem

data class ClientesUiState(
    val clientes: List<Customer> = emptyList(),
    val filteredClientes: List<Customer> = emptyList(),
    val searchQuery: String = "",
    val debtSummary: CustomerDebtSummary = CustomerDebtSummary(),
    val isLoading: Boolean = false,
    val selectedCustomerForStatement: Customer? = null,
    val statementItems: List<AccountStatementItem> = emptyList(),
    val isLoadingStatement: Boolean = false,
    val selectedSaleDetails: Pair<Sale, List<SaleItem>>? = null,
    val saleToCancel: Sale? = null,
    val isCancellingSale: Boolean = false,
    val showCustomerForm: Boolean = false,
    val customerToEdit: Customer? = null,
    val showPaymentDialogFor: Customer? = null,
    val showDeleteConfirmFor: Customer? = null,
    val errorMessage: String? = null,
    val userMessage: String? = null,
    val isSyncing: Boolean = false,
    val receiptSettings: com.dnavarro.poskmp.domain.model.ReceiptSettings = com.dnavarro.poskmp.domain.model.ReceiptSettings()
)
