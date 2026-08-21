package com.dnavarro.poskmp.domain.usecase

import com.dnavarro.poskmp.data.CustomerRepository
import com.dnavarro.poskmp.domain.model.Customer
import kotlinx.coroutines.flow.Flow

class GetCustomersUseCase(
    private val customerRepository: CustomerRepository
) {
    operator fun invoke(): Flow<List<Customer>> =
        customerRepository.getActiveCustomers()

    suspend fun getById(id: String): Customer? =
        customerRepository.getCustomerById(id)
}

