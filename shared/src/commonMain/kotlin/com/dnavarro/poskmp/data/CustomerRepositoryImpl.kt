package com.dnavarro.poskmp.data

import com.dnavarro.poskmp.data.source.local.CustomerLocalDataSource
import com.dnavarro.poskmp.domain.model.AccountStatementItem
import com.dnavarro.poskmp.domain.model.AccountStatementType
import com.dnavarro.poskmp.domain.model.Customer
import com.dnavarro.poskmp.domain.model.CustomerDebtSummary
import com.dnavarro.poskmp.domain.model.CustomerPayment
import com.dnavarro.poskmp.util.currentTimeMillis
import kotlinx.coroutines.flow.Flow

class CustomerRepositoryImpl(
    private val localDataSource: CustomerLocalDataSource
) : CustomerRepository {

    override fun getActiveCustomers(): Flow<List<Customer>> =
        localDataSource.getActiveCustomers()

    override fun getAllCustomers(): Flow<List<Customer>> =
        localDataSource.getAllCustomers()

    override suspend fun getCustomerById(id: String): Customer? =
        localDataSource.getCustomerById(id)

    override suspend fun searchActiveCustomers(query: String): List<Customer> =
        localDataSource.searchActiveCustomers(query)

    override suspend fun saveCustomer(customer: Customer) {
        val existing = localDataSource.getCustomerById(customer.id)
        if (existing == null) {
            localDataSource.insertCustomer(customer)
        } else {
            localDataSource.updateCustomer(customer)
        }
    }

    override suspend fun setCustomerActiveStatus(id: String, activo: Boolean) {
        localDataSource.setCustomerActiveStatus(id, activo, currentTimeMillis())
    }

    override suspend fun deleteCustomer(id: String) {
        localDataSource.deleteCustomer(id)
    }

    override suspend fun recordCustomerPayment(payment: CustomerPayment) {
        localDataSource.insertCustomerPayment(payment)
    }

    override suspend fun deleteCustomerPayment(paymentId: String) {
        localDataSource.deleteCustomerPayment(paymentId)
    }

    override suspend fun getCustomerDebtSummary(): CustomerDebtSummary =
        localDataSource.getCustomerDebtSummary()

    override suspend fun getCustomerAccountStatement(customerId: String): List<AccountStatementItem> {
        val creditSales = localDataSource.getCreditSalesByCustomerId(customerId)
        val payments = localDataSource.getPaymentsByCustomerId(customerId)

        val movements = mutableListOf<AccountStatementItem>()

        creditSales.forEach { sale ->
            val isMixto = sale.metodo_pago == "MIXTO"
            val cargoMonto = if (isMixto) (sale.total - sale.pago_con).coerceAtLeast(0.0) else sale.total
            val descripcion = if (isMixto) {
                "Ticket #${sale.folio} (Pago Mixto - Resto a Crédito)"
            } else {
                "Ticket #${sale.folio} (Compra a Crédito)"
            }
            movements.add(
                AccountStatementItem(
                    id = sale.id,
                    fecha = sale.created_at,
                    tipo = AccountStatementType.CARGO_CREDITO,
                    folio = sale.folio,
                    monto = cargoMonto,
                    metodoPago = sale.metodo_pago,
                    descripcion = descripcion,
                    notas = if (isMixto) "Total: $${sale.total} • Pagado: $${sale.pago_con}" else ""
                )
            )
        }

        payments.forEach { payment ->
            movements.add(
                AccountStatementItem(
                    id = payment.id,
                    fecha = payment.createdAt,
                    tipo = AccountStatementType.ABONO,
                    folio = null,
                    monto = payment.monto,
                    metodoPago = payment.metodoPago,
                    descripcion = "Abono a Cuenta",
                    notas = payment.notas
                )
            )
        }

        // Sort chronologically ascending to calculate running balance
        val sortedAsc = movements.sortedBy { it.fecha }
        var runningBalance = 0.0
        val withRunningBalance = sortedAsc.map { item ->
            when (item.tipo) {
                AccountStatementType.CARGO_CREDITO -> runningBalance += item.monto
                AccountStatementType.ABONO -> runningBalance -= item.monto
            }
            item.copy(saldoResultante = runningBalance)
        }

        // Return sorted chronologically descending for UI display
        return withRunningBalance.sortedByDescending { it.fecha }
    }
}
