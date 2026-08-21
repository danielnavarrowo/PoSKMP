package com.dnavarro.poskmp.data.source.remote

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
            val response = httpClient.post("$cleanUrl/rest/v1/products") {
                header("apikey", key.trim())
                header("Authorization", "Bearer ${key.trim()}")
                header("Prefer", "resolution=merge-duplicates")
                contentType(ContentType.Application.Json)
                setBody(products)
            }
            if (response.status.value in 200..299) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Fallo al sincronizar productos: HTTP ${response.status.value}"))
            }
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
            val endpoint = if (sinceTimestamp > 0) {
                "$cleanUrl/rest/v1/products?updated_at=gt.$sinceTimestamp&order=updated_at.asc"
            } else {
                "$cleanUrl/rest/v1/products?order=updated_at.asc"
            }
            val response = httpClient.get(endpoint) {
                header("apikey", key.trim())
                header("Authorization", "Bearer ${key.trim()}")
                header("Accept", "application/json")
            }
            if (response.status.value in 200..299) {
                val list: List<ProductDto> = response.body()
                Result.success(list)
            } else {
                Result.failure(Exception("Fallo al descargar productos: HTTP ${response.status.value}"))
            }
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
            val response = httpClient.post("$cleanUrl/rest/v1/customers") {
                header("apikey", key.trim())
                header("Authorization", "Bearer ${key.trim()}")
                header("Prefer", "resolution=merge-duplicates")
                contentType(ContentType.Application.Json)
                setBody(customers)
            }
            if (response.status.value in 200..299) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Fallo al sincronizar clientes: HTTP ${response.status.value}"))
            }
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
            val endpoint = if (sinceTimestamp > 0) {
                "$cleanUrl/rest/v1/customers?updated_at=gt.$sinceTimestamp&order=updated_at.asc"
            } else {
                "$cleanUrl/rest/v1/customers?order=updated_at.asc"
            }
            val response = httpClient.get(endpoint) {
                header("apikey", key.trim())
                header("Authorization", "Bearer ${key.trim()}")
                header("Accept", "application/json")
            }
            if (response.status.value in 200..299) {
                val list: List<CustomerDto> = response.body()
                Result.success(list)
            } else {
                Result.failure(Exception("Fallo al descargar clientes: HTTP ${response.status.value}"))
            }
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
            val response = httpClient.post("$cleanUrl/rest/v1/customer_payments") {
                header("apikey", key.trim())
                header("Authorization", "Bearer ${key.trim()}")
                header("Prefer", "resolution=merge-duplicates")
                contentType(ContentType.Application.Json)
                setBody(payments)
            }
            if (response.status.value in 200..299) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Fallo al sincronizar abonos: HTTP ${response.status.value}"))
            }
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
            val endpoint = if (sinceTimestamp > 0) {
                "$cleanUrl/rest/v1/customer_payments?created_at=gt.$sinceTimestamp&order=created_at.asc"
            } else {
                "$cleanUrl/rest/v1/customer_payments?order=created_at.asc"
            }
            val response = httpClient.get(endpoint) {
                header("apikey", key.trim())
                header("Authorization", "Bearer ${key.trim()}")
                header("Accept", "application/json")
            }
            if (response.status.value in 200..299) {
                val list: List<CustomerPaymentDto> = response.body()
                Result.success(list)
            } else {
                Result.failure(Exception("Fallo al descargar abonos: HTTP ${response.status.value}"))
            }
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
            val response = httpClient.post("$cleanUrl/rest/v1/sales") {
                header("apikey", key.trim())
                header("Authorization", "Bearer ${key.trim()}")
                header("Prefer", "resolution=merge-duplicates")
                contentType(ContentType.Application.Json)
                setBody(sales)
            }
            if (response.status.value in 200..299) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Fallo al sincronizar ventas: HTTP ${response.status.value}"))
            }
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
            val endpoint = if (sinceTimestamp > 0) {
                "$cleanUrl/rest/v1/sales?created_at=gt.$sinceTimestamp&order=created_at.asc"
            } else {
                "$cleanUrl/rest/v1/sales?order=created_at.asc"
            }
            val response = httpClient.get(endpoint) {
                header("apikey", key.trim())
                header("Authorization", "Bearer ${key.trim()}")
                header("Accept", "application/json")
            }
            if (response.status.value in 200..299) {
                val list: List<SaleDto> = response.body()
                Result.success(list)
            } else {
                Result.failure(Exception("Fallo al descargar ventas: HTTP ${response.status.value}"))
            }
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
            val response = httpClient.post("$cleanUrl/rest/v1/sale_items") {
                header("apikey", key.trim())
                header("Authorization", "Bearer ${key.trim()}")
                header("Prefer", "resolution=merge-duplicates")
                contentType(ContentType.Application.Json)
                setBody(items)
            }
            if (response.status.value in 200..299) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Fallo al sincronizar partidas de venta: HTTP ${response.status.value}"))
            }
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
            val endpoint = if (sinceTimestamp > 0) {
                "$cleanUrl/rest/v1/sale_items?created_at=gt.$sinceTimestamp&order=created_at.asc"
            } else {
                "$cleanUrl/rest/v1/sale_items?order=created_at.asc"
            }
            val response = httpClient.get(endpoint) {
                header("apikey", key.trim())
                header("Authorization", "Bearer ${key.trim()}")
                header("Accept", "application/json")
            }
            if (response.status.value in 200..299) {
                val list: List<SaleItemDto> = response.body()
                Result.success(list)
            } else {
                Result.failure(Exception("Fallo al descargar partidas de venta: HTTP ${response.status.value}"))
            }
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
            val response = httpClient.post("$cleanUrl/rest/v1/deleted_records") {
                header("apikey", key.trim())
                header("Authorization", "Bearer ${key.trim()}")
                header("Prefer", "resolution=merge-duplicates")
                contentType(ContentType.Application.Json)
                setBody(records)
            }
            if (response.status.value in 200..299) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Error al registrar eliminaciones remotas: HTTP ${response.status.value}"))
            }
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
            val endpoint = if (sinceTimestamp > 0) {
                "$cleanUrl/rest/v1/deleted_records?deleted_at=gt.$sinceTimestamp&order=deleted_at.asc"
            } else {
                "$cleanUrl/rest/v1/deleted_records?order=deleted_at.asc"
            }
            val response = httpClient.get(endpoint) {
                header("apikey", key.trim())
                header("Authorization", "Bearer ${key.trim()}")
                header("Accept", "application/json")
            }
            if (response.status.value in 200..299) {
                val list: List<DeletedRecordDto> = response.body()
                Result.success(list)
            } else {
                Result.failure(Exception("Error al descargar eliminaciones remotas: HTTP ${response.status.value}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
