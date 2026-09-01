package com.dnavarro.poskmp.data.source.remote

import com.dnavarro.poskmp.data.source.remote.dto.CashierDto
import com.dnavarro.poskmp.data.source.remote.dto.CustomerDto
import com.dnavarro.poskmp.data.source.remote.dto.CustomerPaymentDto
import com.dnavarro.poskmp.data.source.remote.dto.DeletedRecordDto
import com.dnavarro.poskmp.data.source.remote.dto.ProductDto
import com.dnavarro.poskmp.data.source.remote.dto.SaleDto
import com.dnavarro.poskmp.data.source.remote.dto.SaleItemDto
import com.dnavarro.poskmp.data.source.remote.dto.StoreSettingsDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.delete
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import io.ktor.client.request.get

interface SupabaseRemoteDataSource {
    suspend fun testConnection(url: String, key: String): Result<Boolean>
    suspend fun pushProducts(url: String, key: String, products: List<ProductDto>): Result<Unit>
    suspend fun pullProducts(url: String, key: String, sinceTimestamp: Long): Result<List<ProductDto>>
    suspend fun pushCustomers(url: String, key: String, customers: List<CustomerDto>): Result<Unit>
    suspend fun pullCustomers(url: String, key: String, sinceTimestamp: Long): Result<List<CustomerDto>>
    suspend fun pushCustomerPayments(url: String, key: String, payments: List<CustomerPaymentDto>): Result<Unit>
    suspend fun pullCustomerPayments(url: String, key: String, sinceTimestamp: Long): Result<List<CustomerPaymentDto>>
    suspend fun pushCashiers(url: String, key: String, cashiers: List<CashierDto>): Result<Unit>
    suspend fun pullCashiers(url: String, key: String, sinceTimestamp: Long): Result<List<CashierDto>>
    suspend fun pushSales(url: String, key: String, sales: List<SaleDto>): Result<Unit>
    suspend fun pullSales(url: String, key: String, sinceTimestamp: Long): Result<List<SaleDto>>
    suspend fun pushSaleItems(url: String, key: String, items: List<SaleItemDto>): Result<Unit>
    suspend fun pullSaleItems(url: String, key: String, sinceTimestamp: Long): Result<List<SaleItemDto>>
    suspend fun pushStoreSettings(url: String, key: String, settings: StoreSettingsDto): Result<Unit>
    suspend fun pullStoreSettings(url: String, key: String): Result<StoreSettingsDto?>
    suspend fun deleteRemoteProduct(url: String, key: String, id: String): Result<Unit>
    suspend fun deleteRemoteCustomer(url: String, key: String, id: String): Result<Unit>
    suspend fun deleteRemoteCustomerPayment(url: String, key: String, id: String): Result<Unit>
    suspend fun pushDeletedRecords(url: String, key: String, records: List<DeletedRecordDto>): Result<Unit>
    suspend fun pullDeletedRecords(url: String, key: String, sinceTimestamp: Long): Result<List<DeletedRecordDto>>
}

class SupabaseRemoteDataSourceImpl(
    private val httpClient: HttpClient = createDefaultHttpClient()
) : SupabaseRemoteDataSource {

    companion object {
        fun createDefaultHttpClient(): HttpClient {
            return HttpClient(CIO) {
                install(ContentNegotiation) {
                    json(Json {
                        prettyPrint = false
                        isLenient = true
                        ignoreUnknownKeys = true
                        encodeDefaults = true
                    })
                }
            }
        }
    }

    private fun normalizeUrl(url: String): String {
        return url.trim().trimEnd('/')
    }

    private suspend inline fun <reified T> fetchAllPaged(
        cleanUrl: String,
        key: String,
        table: String,
        queryString: String,
        pageSize: Int = 1000
    ): List<T> {
        val allResults = mutableListOf<T>()
        var offset = 0

        while (true) {
            val endpoint = if (queryString.isNotBlank()) {
                "$cleanUrl/rest/v1/$table?$queryString&limit=$pageSize&offset=$offset"
            } else {
                "$cleanUrl/rest/v1/$table?limit=$pageSize&offset=$offset"
            }
            val response: HttpResponse = httpClient.get(endpoint) {
                header("apikey", key.trim())
                header("Authorization", "Bearer ${key.trim()}")
                header("Accept", "application/json")
            }
            if (response.status.value !in 200..299) {
                throw Exception("Error HTTP ${response.status.value} al descargar $table: ${response.status.description}")
            }
            val page: List<T> = response.body()
            allResults.addAll(page)
            if (page.size < pageSize) {
                break
            }
            offset += page.size
        }
        return allResults
    }

    private suspend inline fun <reified T> pushInChunks(
        cleanUrl: String,
        key: String,
        table: String,
        items: List<T>,
        chunkSize: Int = 500
    ) {
        for (chunk in items.chunked(chunkSize)) {
            val response = httpClient.post("$cleanUrl/rest/v1/$table") {
                header("apikey", key.trim())
                header("Authorization", "Bearer ${key.trim()}")
                header("Prefer", "resolution=merge-duplicates")
                contentType(ContentType.Application.Json)
                setBody(chunk)
            }
            if (response.status.value !in 200..299) {
                val errorDetail = try { response.bodyAsText() } catch (_: Exception) { "" }
                throw Exception("Error HTTP ${response.status.value} al subir datos a $table: ${response.status.description} ($errorDetail)")
            }
        }
    }

    override suspend fun testConnection(url: String, key: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val cleanUrl = normalizeUrl(url)
            if (cleanUrl.isBlank() || key.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("URL o API Key vacías"))
            }
            val response: HttpResponse = httpClient.get("$cleanUrl/rest/v1/products?limit=1") {
                header("apikey", key.trim())
                header("Authorization", "Bearer ${key.trim()}")
                header("Accept", "application/json")
            }
            if (response.status == HttpStatusCode.OK || response.status == HttpStatusCode.PartialContent) {
                Result.success(true)
            } else {
                Result.failure(Exception("Error de conexión: HTTP ${response.status.value}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun pushProducts(
        url: String,
        key: String,
        products: List<ProductDto>
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (products.isEmpty()) return@withContext Result.success(Unit)
        try {
            val cleanUrl = normalizeUrl(url)
            pushInChunks(cleanUrl, key, "products", products)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun pullProducts(
        url: String,
        key: String,
        sinceTimestamp: Long
    ): Result<List<ProductDto>> = withContext(Dispatchers.IO) {
        try {
            val cleanUrl = normalizeUrl(url)
            val query = if (sinceTimestamp > 0) {
                "updated_at=gt.$sinceTimestamp&order=updated_at.asc"
            } else {
                "order=updated_at.asc"
            }
            val list: List<ProductDto> = fetchAllPaged(cleanUrl, key, "products", query)
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun pushCustomers(
        url: String,
        key: String,
        customers: List<CustomerDto>
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (customers.isEmpty()) return@withContext Result.success(Unit)
        try {
            val cleanUrl = normalizeUrl(url)
            pushInChunks(cleanUrl, key, "customers", customers)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun pullCustomers(
        url: String,
        key: String,
        sinceTimestamp: Long
    ): Result<List<CustomerDto>> = withContext(Dispatchers.IO) {
        try {
            val cleanUrl = normalizeUrl(url)
            val query = if (sinceTimestamp > 0) {
                "updated_at=gt.$sinceTimestamp&order=updated_at.asc"
            } else {
                "order=updated_at.asc"
            }
            val list: List<CustomerDto> = fetchAllPaged(cleanUrl, key, "customers", query)
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun pushCustomerPayments(
        url: String,
        key: String,
        payments: List<CustomerPaymentDto>
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (payments.isEmpty()) return@withContext Result.success(Unit)
        try {
            val cleanUrl = normalizeUrl(url)
            pushInChunks(cleanUrl, key, "customer_payments", payments)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun pullCustomerPayments(
        url: String,
        key: String,
        sinceTimestamp: Long
    ): Result<List<CustomerPaymentDto>> = withContext(Dispatchers.IO) {
        try {
            val cleanUrl = normalizeUrl(url)
            val query = if (sinceTimestamp > 0) {
                "created_at=gt.$sinceTimestamp&order=created_at.asc"
            } else {
                "order=created_at.asc"
            }
            val list: List<CustomerPaymentDto> = fetchAllPaged(cleanUrl, key, "customer_payments", query)
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun pushCashiers(
        url: String,
        key: String,
        cashiers: List<CashierDto>
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (cashiers.isEmpty()) return@withContext Result.success(Unit)
        try {
            val cleanUrl = normalizeUrl(url)
            pushInChunks(cleanUrl, key, "cashiers", cashiers)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun pullCashiers(
        url: String,
        key: String,
        sinceTimestamp: Long
    ): Result<List<CashierDto>> = withContext(Dispatchers.IO) {
        try {
            val cleanUrl = normalizeUrl(url)
            val query = if (sinceTimestamp > 0) {
                "updated_at=gt.$sinceTimestamp&order=updated_at.asc"
            } else {
                "order=updated_at.asc"
            }
            val list: List<CashierDto> = fetchAllPaged(cleanUrl, key, "cashiers", query)
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun pushSales(
        url: String,
        key: String,
        sales: List<SaleDto>
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (sales.isEmpty()) return@withContext Result.success(Unit)
        try {
            val cleanUrl = normalizeUrl(url)
            pushInChunks(cleanUrl, key, "sales", sales)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun pullSales(
        url: String,
        key: String,
        sinceTimestamp: Long
    ): Result<List<SaleDto>> = withContext(Dispatchers.IO) {
        try {
            val cleanUrl = normalizeUrl(url)
            val query = if (sinceTimestamp > 0) {
                "created_at=gt.$sinceTimestamp&order=created_at.asc"
            } else {
                "order=created_at.asc"
            }
            val list: List<SaleDto> = fetchAllPaged(cleanUrl, key, "sales", query)
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun pushSaleItems(
        url: String,
        key: String,
        items: List<SaleItemDto>
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (items.isEmpty()) return@withContext Result.success(Unit)
        try {
            val cleanUrl = normalizeUrl(url)
            pushInChunks(cleanUrl, key, "sale_items", items)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun pullSaleItems(
        url: String,
        key: String,
        sinceTimestamp: Long
    ): Result<List<SaleItemDto>> = withContext(Dispatchers.IO) {
        try {
            val cleanUrl = normalizeUrl(url)
            val query = if (sinceTimestamp > 0) {
                "created_at=gt.$sinceTimestamp&order=created_at.asc"
            } else {
                "order=created_at.asc"
            }
            val list: List<SaleItemDto> = fetchAllPaged(cleanUrl, key, "sale_items", query)
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun pushStoreSettings(
        url: String,
        key: String,
        settings: StoreSettingsDto
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val cleanUrl = normalizeUrl(url)
            val response = httpClient.post("$cleanUrl/rest/v1/store_settings") {
                header("apikey", key.trim())
                header("Authorization", "Bearer ${key.trim()}")
                header("Prefer", "resolution=merge-duplicates")
                contentType(ContentType.Application.Json)
                setBody(settings)
            }
            if (response.status.value in 200..299) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Fallo al sincronizar ajustes de negocio: HTTP ${response.status.value}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun pullStoreSettings(
        url: String,
        key: String
    ): Result<StoreSettingsDto?> = withContext(Dispatchers.IO) {
        try {
            val cleanUrl = normalizeUrl(url)
            val response = httpClient.get("$cleanUrl/rest/v1/store_settings?id=eq.default&limit=1") {
                header("apikey", key.trim())
                header("Authorization", "Bearer ${key.trim()}")
                header("Accept", "application/json")
            }
            if (response.status.value in 200..299) {
                val list: List<StoreSettingsDto> = response.body()
                Result.success(list.firstOrNull())
            } else {
                Result.failure(Exception("Fallo al descargar ajustes de negocio: HTTP ${response.status.value}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteRemoteProduct(
        url: String,
        key: String,
        id: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val cleanUrl = normalizeUrl(url)
            val response = httpClient.delete("$cleanUrl/rest/v1/products?id=eq.$id") {
                header("apikey", key.trim())
                header("Authorization", "Bearer ${key.trim()}")
            }
            if (response.status.value in 200..299) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Error al borrar producto remoto: HTTP ${response.status.value}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteRemoteCustomer(
        url: String,
        key: String,
        id: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val cleanUrl = normalizeUrl(url)
            val response = httpClient.delete("$cleanUrl/rest/v1/customers?id=eq.$id") {
                header("apikey", key.trim())
                header("Authorization", "Bearer ${key.trim()}")
            }
            if (response.status.value in 200..299) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Error al borrar cliente remoto: HTTP ${response.status.value}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteRemoteCustomerPayment(
        url: String,
        key: String,
        id: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val cleanUrl = normalizeUrl(url)
            val response = httpClient.delete("$cleanUrl/rest/v1/customer_payments?id=eq.$id") {
                header("apikey", key.trim())
                header("Authorization", "Bearer ${key.trim()}")
            }
            if (response.status.value in 200..299) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Error al borrar abono remoto: HTTP ${response.status.value}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun pushDeletedRecords(
        url: String,
        key: String,
        records: List<DeletedRecordDto>
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (records.isEmpty()) return@withContext Result.success(Unit)
        try {
            val cleanUrl = normalizeUrl(url)
            pushInChunks(cleanUrl, key, "deleted_records", records)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun pullDeletedRecords(
        url: String,
        key: String,
        sinceTimestamp: Long
    ): Result<List<DeletedRecordDto>> = withContext(Dispatchers.IO) {
        try {
            val cleanUrl = normalizeUrl(url)
            val query = if (sinceTimestamp > 0) {
                "deleted_at=gt.$sinceTimestamp&order=deleted_at.asc"
            } else {
                "order=deleted_at.asc"
            }
            val list: List<DeletedRecordDto> = fetchAllPaged(cleanUrl, key, "deleted_records", query)
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
