package com.dnavarro.poskmp.data

import com.dnavarro.poskmp.domain.model.AccountStatementItem
import com.dnavarro.poskmp.domain.model.Customer
import com.dnavarro.poskmp.domain.model.CustomerDebtSummary
import com.dnavarro.poskmp.domain.model.CustomerPayment
import kotlinx.coroutines.flow.Flow

interface CustomerRepository {
    fun getActiveCustomers(): Flow<List<Customer>>
    fun getAllCustomers(): Flow<List<Customer>>
    suspend fun getCustomerById(id: String): Customer?
    suspend fun searchActiveCustomers(query: String): List<Customer>
    suspend fun saveCustomer(customer: Customer)
    suspend fun setCustomerActiveStatus(id: String, activo: Boolean)
    suspend fun deleteCustomer(id: String)
    suspend fun recordCustomerPayment(payment: CustomerPayment)
    suspend fun deleteCustomerPayment(paymentId: String)
    suspend fun getCustomerDebtSummary(): CustomerDebtSummary
    suspend fun getCustomerAccountStatement(customerId: String): List<AccountStatementItem>
}
