package com.dnavarro.poskmp.domain.usecase

import com.dnavarro.poskmp.data.CustomerRepository
import com.dnavarro.poskmp.domain.model.CustomerPayment
import com.dnavarro.poskmp.util.currentTimeMillis
import com.dnavarro.poskmp.util.generateUUID

class RecordCustomerPaymentUseCase(
    private val customerRepository: CustomerRepository
) {
    suspend operator fun invoke(
        customerId: String,
        monto: Double,
        metodoPago: String = "EFECTIVO",
        notas: String = ""
    ): Result<CustomerPayment> {
        if (customerId.isBlank()) {
            return Result.failure(IllegalArgumentException("El ID del cliente no es válido"))
        }
        if (monto <= 0.0) {
            return Result.failure(IllegalArgumentException("El monto del abono debe ser mayor a 0"))
        }

        val payment = CustomerPayment(
            id = generateUUID(),
            customerId = customerId,
            monto = monto,
            metodoPago = metodoPago,
            notas = notas.trim(),
            createdAt = currentTimeMillis(),
            syncState = "PENDING_INSERT"
        )

        customerRepository.recordCustomerPayment(payment)
        return Result.success(payment)
    }

    suspend fun deletePayment(paymentId: String) {
        customerRepository.deleteCustomerPayment(paymentId)
    }
}
