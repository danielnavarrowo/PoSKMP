package com.dnavarro.poskmp.data.sync

import com.dnavarro.poskmp.data.source.remote.SupabaseRemoteDataSource
import com.dnavarro.poskmp.data.source.remote.dto.CashMovementDto
import com.dnavarro.poskmp.data.source.remote.dto.CashierDto
import com.dnavarro.poskmp.data.source.remote.dto.CustomerDto
import com.dnavarro.poskmp.data.source.remote.dto.CustomerPaymentDto
import com.dnavarro.poskmp.data.source.remote.dto.DeletedRecordDto
import com.dnavarro.poskmp.data.source.remote.dto.ProductDto
import com.dnavarro.poskmp.data.source.remote.dto.RemoteAuditLogDto
import com.dnavarro.poskmp.data.source.remote.dto.SaleDto
import com.dnavarro.poskmp.data.source.remote.dto.SaleItemDto
import com.dnavarro.poskmp.data.source.remote.dto.ShiftDto
import com.dnavarro.poskmp.data.source.remote.dto.StoreSettingsDto

class FakeSupabaseRemoteDataSource : SupabaseRemoteDataSource {

    // In-memory remote database tables
    val remoteProducts = LinkedHashMap<String, ProductDto>()
    val remoteCustomers = LinkedHashMap<String, CustomerDto>()
    val remoteCustomerPayments = LinkedHashMap<String, CustomerPaymentDto>()
    val remoteCashiers = LinkedHashMap<String, CashierDto>()
    val remoteShifts = LinkedHashMap<String, ShiftDto>()
    val remoteCashMovements = LinkedHashMap<String, CashMovementDto>()
    val remoteSales = LinkedHashMap<String, SaleDto>()
    val remoteSaleItems = LinkedHashMap<String, SaleItemDto>()
    var remoteStoreSettings: StoreSettingsDto? = null
    val remoteDeletedRecords = mutableListOf<DeletedRecordDto>()
    val remoteAuditLogs = mutableListOf<RemoteAuditLogDto>()

    // Audit logs of pushed records
    val pushedProducts = mutableListOf<ProductDto>()
    val pushedCustomers = mutableListOf<CustomerDto>()
    val pushedCustomerPayments = mutableListOf<CustomerPaymentDto>()
    val pushedCashiers = mutableListOf<CashierDto>()
    val pushedShifts = mutableListOf<ShiftDto>()
    val pushedCashMovements = mutableListOf<CashMovementDto>()
    val pushedSales = mutableListOf<SaleDto>()
    val pushedSaleItems = mutableListOf<SaleItemDto>()
    val pushedStoreSettings = mutableListOf<StoreSettingsDto>()
    val pushedDeletedRecords = mutableListOf<DeletedRecordDto>()

    // Configurable error triggers
    var connectionError: Throwable? = null
    var pushProductsError: Throwable? = null
    var pullProductsError: Throwable? = null
    var pushCustomersError: Throwable? = null
    var pullCustomersError: Throwable? = null
    var pushCustomerPaymentsError: Throwable? = null
    var pullCustomerPaymentsError: Throwable? = null
    var pushCashiersError: Throwable? = null
    var pullCashiersError: Throwable? = null
    var pushShiftsError: Throwable? = null
    var pullShiftsError: Throwable? = null
    var pushCashMovementsError: Throwable? = null
    var pullCashMovementsError: Throwable? = null
    var pushSalesError: Throwable? = null
    var pullSalesError: Throwable? = null
    var pushSaleItemsError: Throwable? = null
    var pullSaleItemsError: Throwable? = null
    var pushStoreSettingsError: Throwable? = null
    var pullStoreSettingsError: Throwable? = null
    var deleteRemoteProductError: Throwable? = null
    var deleteRemoteCustomerError: Throwable? = null
    var deleteRemoteCustomerPaymentError: Throwable? = null
    var pushDeletedRecordsError: Throwable? = null
    var pullDeletedRecordsError: Throwable? = null

    // Call execution counters
    var testConnectionCallCount = 0
    var pullProductsCallCount = 0
    var pushProductsCallCount = 0
    var pullCustomersCallCount = 0
    var pushCustomersCallCount = 0
    var pullCustomerPaymentsCallCount = 0
    var pushCustomerPaymentsCallCount = 0
    var pullCashiersCallCount = 0
    var pushCashiersCallCount = 0
    var pullShiftsCallCount = 0
    var pushShiftsCallCount = 0
    var pullCashMovementsCallCount = 0
    var pushCashMovementsCallCount = 0
    var pullSalesCallCount = 0
    var pushSalesCallCount = 0
    var pullSaleItemsCallCount = 0
    var pushSaleItemsCallCount = 0
    var pullStoreSettingsCallCount = 0
    var pushStoreSettingsCallCount = 0
    var pullDeletedRecordsCallCount = 0
    var pushDeletedRecordsCallCount = 0

    fun resetErrors() {
        connectionError = null
        pushProductsError = null
        pullProductsError = null
        pushCustomersError = null
        pullCustomersError = null
        pushCustomerPaymentsError = null
        pullCustomerPaymentsError = null
        pushCashiersError = null
        pullCashiersError = null
        pushShiftsError = null
        pullShiftsError = null
        pushCashMovementsError = null
        pullCashMovementsError = null
        pushSalesError = null
        pullSalesError = null
        pushSaleItemsError = null
        pullSaleItemsError = null
        pushStoreSettingsError = null
        pullStoreSettingsError = null
        deleteRemoteProductError = null
        deleteRemoteCustomerError = null
        deleteRemoteCustomerPaymentError = null
        pushDeletedRecordsError = null
        pullDeletedRecordsError = null
    }

    fun clearAll() {
        remoteProducts.clear()
        remoteCustomers.clear()
        remoteCustomerPayments.clear()
        remoteCashiers.clear()
        remoteShifts.clear()
        remoteCashMovements.clear()
        remoteSales.clear()
        remoteSaleItems.clear()
        remoteStoreSettings = null
        remoteDeletedRecords.clear()
        remoteAuditLogs.clear()

        pushedProducts.clear()
        pushedCustomers.clear()
        pushedCustomerPayments.clear()
        pushedCashiers.clear()
        pushedShifts.clear()
        pushedCashMovements.clear()
        pushedSales.clear()
        pushedSaleItems.clear()
        pushedStoreSettings.clear()
        pushedDeletedRecords.clear()

        resetErrors()
        testConnectionCallCount = 0
        pullProductsCallCount = 0
        pushProductsCallCount = 0
        pullCustomersCallCount = 0
        pushCustomersCallCount = 0
        pullCustomerPaymentsCallCount = 0
        pushCustomerPaymentsCallCount = 0
        pullCashiersCallCount = 0
        pushCashiersCallCount = 0
        pullShiftsCallCount = 0
        pushShiftsCallCount = 0
        pullCashMovementsCallCount = 0
        pushCashMovementsCallCount = 0
        pullSalesCallCount = 0
        pushSalesCallCount = 0
        pullSaleItemsCallCount = 0
        pushSaleItemsCallCount = 0
        pullStoreSettingsCallCount = 0
        pushStoreSettingsCallCount = 0
        pullDeletedRecordsCallCount = 0
        pushDeletedRecordsCallCount = 0
    }

    override suspend fun testConnection(url: String, key: String): Result<Boolean> {
        testConnectionCallCount++
        connectionError?.let { return Result.failure(it) }
        return Result.success(true)
    }

    override suspend fun pushProducts(url: String, key: String, products: List<ProductDto>): Result<Unit> {
        pushProductsCallCount++
        pushProductsError?.let { return Result.failure(it) }
        pushedProducts.addAll(products)
        for (p in products) {
            remoteProducts[p.id] = p
        }
        return Result.success(Unit)
    }

    override suspend fun pullProducts(url: String, key: String, sinceTimestamp: Long): Result<List<ProductDto>> {
        pullProductsCallCount++
        pullProductsError?.let { return Result.failure(it) }
        val result = if (sinceTimestamp <= 0L) {
            remoteProducts.values.toList()
        } else {
            remoteProducts.values.filter { it.updatedAt > sinceTimestamp }
        }
        return Result.success(result)
    }

    override suspend fun pushCustomers(url: String, key: String, customers: List<CustomerDto>): Result<Unit> {
        pushCustomersCallCount++
        pushCustomersError?.let { return Result.failure(it) }
        pushedCustomers.addAll(customers)
        for (c in customers) {
            remoteCustomers[c.id] = c
        }
        return Result.success(Unit)
    }

    override suspend fun pullCustomers(url: String, key: String, sinceTimestamp: Long): Result<List<CustomerDto>> {
        pullCustomersCallCount++
        pullCustomersError?.let { return Result.failure(it) }
        val result = if (sinceTimestamp <= 0L) {
            remoteCustomers.values.toList()
        } else {
            remoteCustomers.values.filter { it.updatedAt > sinceTimestamp }
        }
        return Result.success(result)
    }

    override suspend fun pushCustomerPayments(
        url: String,
        key: String,
        payments: List<CustomerPaymentDto>
    ): Result<Unit> {
        pushCustomerPaymentsCallCount++
        pushCustomerPaymentsError?.let { return Result.failure(it) }
        pushedCustomerPayments.addAll(payments)
        for (p in payments) {
            remoteCustomerPayments[p.id] = p
        }
        return Result.success(Unit)
    }

    override suspend fun pullCustomerPayments(
        url: String,
        key: String,
        sinceTimestamp: Long
    ): Result<List<CustomerPaymentDto>> {
        pullCustomerPaymentsCallCount++
        pullCustomerPaymentsError?.let { return Result.failure(it) }
        val result = if (sinceTimestamp <= 0L) {
            remoteCustomerPayments.values.toList()
        } else {
            remoteCustomerPayments.values.filter { it.createdAt > sinceTimestamp }
        }
        return Result.success(result)
    }

    override suspend fun pushCashiers(url: String, key: String, cashiers: List<CashierDto>): Result<Unit> {
        pushCashiersCallCount++
        pushCashiersError?.let { return Result.failure(it) }
        pushedCashiers.addAll(cashiers)
        for (c in cashiers) {
            remoteCashiers[c.id] = c
        }
        return Result.success(Unit)
    }

    override suspend fun pullCashiers(url: String, key: String, sinceTimestamp: Long): Result<List<CashierDto>> {
        pullCashiersCallCount++
        pullCashiersError?.let { return Result.failure(it) }
        val result = if (sinceTimestamp <= 0L) {
            remoteCashiers.values.toList()
        } else {
            remoteCashiers.values.filter { it.updatedAt > sinceTimestamp }
        }
        return Result.success(result)
    }

    override suspend fun pushShifts(url: String, key: String, shifts: List<ShiftDto>): Result<Unit> {
        pushShiftsCallCount++
        pushShiftsError?.let { return Result.failure(it) }
        pushedShifts.addAll(shifts)
        for (s in shifts) {
            remoteShifts[s.id] = s
        }
        return Result.success(Unit)
    }

    override suspend fun pullShifts(url: String, key: String, sinceTimestamp: Long): Result<List<ShiftDto>> {
        pullShiftsCallCount++
        pullShiftsError?.let { return Result.failure(it) }
        val result = if (sinceTimestamp <= 0L) {
            remoteShifts.values.toList()
        } else {
            remoteShifts.values.filter {
                val effectiveUpdated = if (it.updatedAt > 0L) it.updatedAt else (it.endTime ?: it.startTime)
                effectiveUpdated > sinceTimestamp
            }
        }
        return Result.success(result)
    }

    override suspend fun pushCashMovements(
        url: String,
        key: String,
        movements: List<CashMovementDto>
    ): Result<Unit> {
        pushCashMovementsCallCount++
        pushCashMovementsError?.let { return Result.failure(it) }
        pushedCashMovements.addAll(movements)
        for (m in movements) {
            remoteCashMovements[m.id] = m
        }
        return Result.success(Unit)
    }

    override suspend fun pullCashMovements(
        url: String,
        key: String,
        sinceTimestamp: Long
    ): Result<List<CashMovementDto>> {
        pullCashMovementsCallCount++
        pullCashMovementsError?.let { return Result.failure(it) }
        val result = if (sinceTimestamp <= 0L) {
            remoteCashMovements.values.toList()
        } else {
            remoteCashMovements.values.filter { it.createdAt > sinceTimestamp }
        }
        return Result.success(result)
    }

    override suspend fun pushSales(url: String, key: String, sales: List<SaleDto>): Result<Unit> {
        pushSalesCallCount++
        pushSalesError?.let { return Result.failure(it) }
        pushedSales.addAll(sales)
        for (s in sales) {
            remoteSales[s.id] = s
        }
        return Result.success(Unit)
    }

    override suspend fun pullSales(url: String, key: String, sinceTimestamp: Long): Result<List<SaleDto>> {
        pullSalesCallCount++
        pullSalesError?.let { return Result.failure(it) }
        val result = if (sinceTimestamp <= 0L) {
            remoteSales.values.toList()
        } else {
            remoteSales.values.filter {
                val effectiveUpdated = if (it.updatedAt > 0L) it.updatedAt else it.createdAt
                effectiveUpdated > sinceTimestamp
            }
        }
        return Result.success(result)
    }

    override suspend fun pushSaleItems(url: String, key: String, items: List<SaleItemDto>): Result<Unit> {
        pushSaleItemsCallCount++
        pushSaleItemsError?.let { return Result.failure(it) }
        pushedSaleItems.addAll(items)
        for (item in items) {
            remoteSaleItems[item.id] = item
        }
        return Result.success(Unit)
    }

    override suspend fun pullSaleItems(url: String, key: String, sinceTimestamp: Long): Result<List<SaleItemDto>> {
        pullSaleItemsCallCount++
        pullSaleItemsError?.let { return Result.failure(it) }
        val result = if (sinceTimestamp <= 0L) {
            remoteSaleItems.values.toList()
        } else {
            remoteSaleItems.values.filter { it.createdAt > sinceTimestamp }
        }
        return Result.success(result)
    }

    override suspend fun pushStoreSettings(url: String, key: String, settings: StoreSettingsDto): Result<Unit> {
        pushStoreSettingsCallCount++
        pushStoreSettingsError?.let { return Result.failure(it) }
        pushedStoreSettings.add(settings)
        remoteStoreSettings = settings
        return Result.success(Unit)
    }

    override suspend fun pullStoreSettings(url: String, key: String): Result<StoreSettingsDto?> {
        pullStoreSettingsCallCount++
        pullStoreSettingsError?.let { return Result.failure(it) }
        return Result.success(remoteStoreSettings)
    }

    override suspend fun deleteRemoteProduct(url: String, key: String, id: String): Result<Unit> {
        deleteRemoteProductError?.let { return Result.failure(it) }
        remoteProducts.remove(id)
        return Result.success(Unit)
    }

    override suspend fun deleteRemoteCustomer(url: String, key: String, id: String): Result<Unit> {
        deleteRemoteCustomerError?.let { return Result.failure(it) }
        remoteCustomers.remove(id)
        return Result.success(Unit)
    }

    override suspend fun deleteRemoteCustomerPayment(url: String, key: String, id: String): Result<Unit> {
        deleteRemoteCustomerPaymentError?.let { return Result.failure(it) }
        remoteCustomerPayments.remove(id)
        return Result.success(Unit)
    }

    override suspend fun pushDeletedRecords(
        url: String,
        key: String,
        records: List<DeletedRecordDto>
    ): Result<Unit> {
        pushDeletedRecordsCallCount++
        pushDeletedRecordsError?.let { return Result.failure(it) }
        pushedDeletedRecords.addAll(records)
        remoteDeletedRecords.addAll(records)
        return Result.success(Unit)
    }

    override suspend fun pullDeletedRecords(
        url: String,
        key: String,
        sinceTimestamp: Long
    ): Result<List<DeletedRecordDto>> {
        pullDeletedRecordsCallCount++
        pullDeletedRecordsError?.let { return Result.failure(it) }
        val result = if (sinceTimestamp <= 0L) {
            remoteDeletedRecords.toList()
        } else {
            remoteDeletedRecords.filter { it.deletedAt > sinceTimestamp }
        }
        return Result.success(result)
    }

    override suspend fun fetchRemoteAuditLogs(
        url: String,
        key: String,
        limit: Int
    ): Result<List<RemoteAuditLogDto>> {
        return Result.success(remoteAuditLogs.take(limit))
    }
}
