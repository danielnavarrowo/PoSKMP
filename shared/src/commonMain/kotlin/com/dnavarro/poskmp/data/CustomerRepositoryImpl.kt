package com.dnavarro.poskmp.data

import com.dnavarro.poskmp.data.source.local.CustomerLocalDataSource
import com.dnavarro.poskmp.domain.model.AccountStatementItem
import com.dnavarro.poskmp.domain.model.AccountStatementType
import com.dnavarro.poskmp.domain.model.Customer
import com.dnavarro.poskmp.domain.model.CustomerDebtSummary
import com.dnavarro.poskmp.domain.model.CustomerPayment
import com.dnavarro.poskmp.util.currentTimeMillis
import com.dnavarro.poskmp.util.formatPrice
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
        val sales = localDataSource.getSalesByCustomerId(customerId)
        val payments = localDataSource.getPaymentsByCustomerId(customerId)

        val movements = mutableListOf<AccountStatementItem>()

        sales.forEach { sale ->
            val isMixto = sale.metodo_pago == "MIXTO"
            val isCredito = sale.metodo_pago == "CREDITO"
            val creditAmount = if (isMixto) {
                if (sale.monto_credito > 0.0) sale.monto_credito else (sale.total - sale.pago_con).coerceAtLeast(0.0)
            } else if (isCredito) {
                sale.total
            } else {
                0.0
            }

            if (creditAmount > 0.0) {
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
                        monto = creditAmount,
                        metodoPago = sale.metodo_pago,
                        descripcion = descripcion,
                        notas = if (isMixto) "Total: $${sale.total.toString().formatPrice()} • Pagado: $${(sale.total - creditAmount).toString().formatPrice()}" else ""
                    )
                )
            } else {
                val paymentLabel = when (sale.metodo_pago.uppercase()) {
                    "EFECTIVO" -> "Efectivo"
                    "TARJETA" -> "Tarjeta"
                    "TRANSFERENCIA" -> "Transferencia"
                    "MIXTO" -> "Mixto"
                    else -> sale.metodo_pago
                }
                val descripcion = "Ticket #${sale.folio} (Venta al Contado - $paymentLabel)"
                movements.add(
                    AccountStatementItem(
                        id = sale.id,
                        fecha = sale.created_at,
                        tipo = AccountStatementType.VENTA_CONTADO,
                        folio = sale.folio,
                        monto = sale.total,
                        metodoPago = sale.metodo_pago,
                        descripcion = descripcion,
                        notas = ""
                    )
                )
            }
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
                AccountStatementType.VENTA_CONTADO -> { /* Contado sales do not change debt running balance */ }
            }
            item.copy(saldoResultante = runningBalance)
        }

        // Return sorted chronologically descending for UI display
        return withRunningBalance.sortedByDescending { it.fecha }
    }
}
