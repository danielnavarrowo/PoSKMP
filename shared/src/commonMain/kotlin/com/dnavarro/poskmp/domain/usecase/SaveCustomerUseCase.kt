package com.dnavarro.poskmp.domain.usecase

import com.dnavarro.poskmp.data.CustomerRepository
import com.dnavarro.poskmp.domain.model.Customer
import com.dnavarro.poskmp.util.currentTimeMillis
import com.dnavarro.poskmp.util.generateUUID

class SaveCustomerUseCase(
    private val customerRepository: CustomerRepository
) {
    suspend operator fun invoke(
        id: String?,
        nombre: String,
        telefono: String = "",
        direccion: String = "",
        notas: String = "",
        limiteCredito: Double = 0.0,
        activo: Boolean = true
    ): Result<Customer> {
        val trimmedNombre = nombre.trim()
        if (trimmedNombre.isEmpty()) {
            return Result.failure(IllegalArgumentException("El nombre del cliente no puede estar vacío"))
        }

        val now = currentTimeMillis()
        val customerId = if (id.isNullOrBlank()) generateUUID() else id
        val isNew = id.isNullOrBlank()

        val existing = if (!isNew) customerRepository.getCustomerById(customerId) else null

        val customer = Customer(
            id = customerId,
            nombre = trimmedNombre,
            telefono = telefono.trim(),
            direccion = direccion.trim(),
            notas = notas.trim(),
            limiteCredito = limiteCredito.coerceAtLeast(0.0),
            activo = activo,
            saldoDeudor = existing?.saldoDeudor ?: 0.0,
            totalCompras = existing?.totalCompras ?: 0L,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
            syncState = if (isNew) "PENDING_INSERT" else "PENDING_UPDATE"
        )

        customerRepository.saveCustomer(customer)
        return Result.success(customer)
    }

    suspend fun setCustomerActiveStatus(id: String, activo: Boolean) {
        customerRepository.setCustomerActiveStatus(id, activo)
    }
}
