package com.dnavarro.poskmp.domain.usecase

import com.dnavarro.poskmp.data.CustomerRepository
import com.dnavarro.poskmp.domain.model.Customer
import com.dnavarro.poskmp.domain.model.CustomerDebtSummary
import kotlinx.coroutines.flow.Flow

class GetCustomersUseCase(
    private val customerRepository: CustomerRepository
) {
    operator fun invoke(): Flow<List<Customer>> =
        customerRepository.getActiveCustomers()

    fun getAll(): Flow<List<Customer>> =
        customerRepository.getAllCustomers()

    suspend fun search(query: String): List<Customer> =
        customerRepository.searchActiveCustomers(query)

    suspend fun getById(id: String): Customer? =
        customerRepository.getCustomerById(id)

    suspend fun getDebtSummary(): CustomerDebtSummary =
        customerRepository.getCustomerDebtSummary()
}
