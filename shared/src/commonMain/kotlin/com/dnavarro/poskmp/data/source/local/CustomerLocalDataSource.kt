package com.dnavarro.poskmp.data.source.local

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.dnavarro.poskmp.db.AppDatabase
import com.dnavarro.poskmp.db.Sales
import com.dnavarro.poskmp.domain.model.Customer
import com.dnavarro.poskmp.domain.model.CustomerDebtSummary
import com.dnavarro.poskmp.domain.model.CustomerPayment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import com.dnavarro.poskmp.util.currentTimeMillis

interface CustomerLocalDataSource {
    fun getActiveCustomers(): Flow<List<Customer>>
    fun getAllCustomers(): Flow<List<Customer>>
    suspend fun getCustomerById(id: String): Customer?
    suspend fun searchActiveCustomers(query: String): List<Customer>
    suspend fun insertCustomer(customer: Customer)
    suspend fun updateCustomer(customer: Customer)
    suspend fun setCustomerActiveStatus(id: String, activo: Boolean, updatedAt: Long)
    suspend fun deleteCustomer(id: String)
    suspend fun insertCustomerPayment(payment: CustomerPayment)
    suspend fun getPaymentsByCustomerId(customerId: String): List<CustomerPayment>
    suspend fun deleteCustomerPayment(paymentId: String)
    suspend fun getCustomerDebtSummary(): CustomerDebtSummary
    suspend fun getCreditSalesByCustomerId(customerId: String): List<Sales>
}

class SqlDelightCustomerDataSource(
    database: AppDatabase
) : CustomerLocalDataSource {
    private val queries = database.appDatabaseQueries

    override fun getActiveCustomers(): Flow<List<Customer>> {
        return queries.selectAllActiveCustomers()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { list ->
                list.map { row ->
                    Customer(
                        id = row.id,
                        nombre = row.nombre,
                        telefono = row.telefono,
                        direccion = row.direccion,
                        notas = row.notas,
                        limiteCredito = row.limite_credito,
                        activo = row.activo == 1L,
                        saldoDeudor = row.saldo_deudor,
                        totalCompras = row.total_compras,
                        createdAt = row.created_at,
                        updatedAt = row.updated_at,
                        syncState = row.sync_state
                    )
                }
            }
    }

    override fun getAllCustomers(): Flow<List<Customer>> {
        return queries.selectAllCustomers()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { list ->
                list.map { row ->
                    Customer(
                        id = row.id,
                        nombre = row.nombre,
                        telefono = row.telefono,
                        direccion = row.direccion,
                        notas = row.notas,
                        limiteCredito = row.limite_credito,
                        activo = row.activo == 1L,
                        saldoDeudor = row.saldo_deudor,
                        totalCompras = row.total_compras,
                        createdAt = row.created_at,
                        updatedAt = row.updated_at,
                        syncState = row.sync_state
                    )
                }
            }
    }

    override suspend fun getCustomerById(id: String): Customer? = withContext(Dispatchers.IO) {
        queries.selectCustomerById(id).executeAsOneOrNull()?.let { row ->
            Customer(
                id = row.id,
                nombre = row.nombre,
                telefono = row.telefono,
                direccion = row.direccion,
                notas = row.notas,
                limiteCredito = row.limite_credito,
                activo = row.activo == 1L,
                saldoDeudor = row.saldo_deudor,
                totalCompras = row.total_compras,
                createdAt = row.created_at,
                updatedAt = row.updated_at,
                syncState = row.sync_state
            )
        }
    }

    override suspend fun searchActiveCustomers(query: String): List<Customer> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        queries.searchActiveCustomers(trimmed).executeAsList().map { row ->
            Customer(
                id = row.id,
                nombre = row.nombre,
                telefono = row.telefono,
                direccion = row.direccion,
                notas = row.notas,
                limiteCredito = row.limite_credito,
                activo = row.activo == 1L,
                saldoDeudor = row.saldo_deudor,
                totalCompras = row.total_compras,
                createdAt = row.created_at,
                updatedAt = row.updated_at,
                syncState = row.sync_state
            )
        }
    }

    override suspend fun insertCustomer(customer: Customer) {
        withContext(Dispatchers.IO) {
            queries.insertCustomer(
                id = customer.id,
                nombre = customer.nombre,
                telefono = customer.telefono,
                direccion = customer.direccion,
                notas = customer.notas,
                limite_credito = customer.limiteCredito,
                activo = if (customer.activo) 1L else 0L,
                created_at = customer.createdAt,
                updated_at = customer.updatedAt,
                sync_state = customer.syncState
            )
        }
    }

    override suspend fun updateCustomer(customer: Customer) {
        withContext(Dispatchers.IO) {
            queries.updateCustomer(
                nombre = customer.nombre,
                telefono = customer.telefono,
                direccion = customer.direccion,
                notas = customer.notas,
                limite_credito = customer.limiteCredito,
                updated_at = customer.updatedAt,
                sync_state = customer.syncState,
                id = customer.id
            )
        }
    }

    override suspend fun setCustomerActiveStatus(
        id: String,
        activo: Boolean,
        updatedAt: Long
    ) {
        withContext(Dispatchers.IO) {
            queries.setCustomerActiveStatus(
                activo = if (activo) 1L else 0L,
                updated_at = updatedAt,
                sync_state = "PENDING_UPDATE",
                id = id
            )
        }
    }

    override suspend fun deleteCustomer(id: String) {
        withContext(Dispatchers.IO) {
            queries.transaction {
                queries.deleteCustomerHard(id)
                queries.insertDeletedSyncRecord(
                    id = id,
                    entity_type = "CUSTOMER",
                    deleted_at = currentTimeMillis()
                )
            }
        }
    }

    override suspend fun insertCustomerPayment(payment: CustomerPayment) {
        withContext(Dispatchers.IO) {
            queries.insertCustomerPayment(
                id = payment.id,
                customer_id = payment.customerId,
                monto = payment.monto,
                metodo_pago = payment.metodoPago,
                notas = payment.notas,
                created_at = payment.createdAt,
                sync_state = payment.syncState
            )
        }
    }

    override suspend fun getPaymentsByCustomerId(customerId: String): List<CustomerPayment> = withContext(Dispatchers.IO) {
        queries.selectPaymentsByCustomerId(customerId).executeAsList().map { row ->
            CustomerPayment(
                id = row.id,
                customerId = row.customer_id,
                monto = row.monto,
                metodoPago = row.metodo_pago,
                notas = row.notas,
                createdAt = row.created_at,
                syncState = row.sync_state
            )
        }
    }

    override suspend fun deleteCustomerPayment(paymentId: String) {
        withContext(Dispatchers.IO) {
            queries.transaction {
                queries.deleteCustomerPayment(paymentId)
                queries.insertDeletedSyncRecord(
                    id = paymentId,
                    entity_type = "PAYMENT",
                    deleted_at = currentTimeMillis()
                )
            }
        }
    }

    override suspend fun getCustomerDebtSummary(): CustomerDebtSummary = withContext(Dispatchers.IO) {
        val row = queries.selectCustomerDebtSummary().executeAsOne()
        CustomerDebtSummary(
            totalClientes = row.total_clientes,
            totalDeudaAcumulada = row.total_deuda_acumulada,
            clientesConDeuda = row.clientes_con_deuda
        )
    }

    override suspend fun getCreditSalesByCustomerId(customerId: String): List<Sales> = withContext(Dispatchers.IO) {
        queries.selectCreditSalesByCustomerId(customerId).executeAsList()
    }
}
