package com.dnavarro.poskmp.domain.usecase

import com.dnavarro.poskmp.data.CustomerRepository
import com.dnavarro.poskmp.domain.model.AccountStatementItem

class GetCustomerAccountStatementUseCase(
    private val customerRepository: CustomerRepository
) {
    suspend operator fun invoke(customerId: String): List<AccountStatementItem> {
        if (customerId.isBlank()) return emptyList()
        return customerRepository.getCustomerAccountStatement(customerId)
    }
}
