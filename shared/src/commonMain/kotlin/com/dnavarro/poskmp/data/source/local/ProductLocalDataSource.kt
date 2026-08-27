package com.dnavarro.poskmp.data.source.local

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.dnavarro.poskmp.db.AppDatabase
import com.dnavarro.poskmp.db.Products
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import com.dnavarro.poskmp.domain.model.ProductSalesStats

import com.dnavarro.poskmp.util.currentTimeMillis
import com.dnavarro.poskmp.util.matchesBarcode
import com.dnavarro.poskmp.util.normalizeBarcode
import com.dnavarro.poskmp.util.parseBarcodes

interface ProductLocalDataSource {
    fun getAllProducts(): Flow<List<Products>>
    fun getActiveProducts(): Flow<List<Products>>
    fun searchProducts(query: String, activeOnly: Boolean = false): Flow<List<Products>>
    fun getProductSalesStats(): Flow<Map<String, ProductSalesStats>>
    suspend fun getProductById(id: String): Products?
    suspend fun insertProduct(product: Products)
    suspend fun insertProducts(products: List<Products>)
    suspend fun updateProduct(product: Products)
    suspend fun deleteProductSoft(id: String, updatedAt: Long)
    suspend fun deleteProductHard(id: String)
    suspend fun deleteAllProducts()
    suspend fun getAllProductsList(): List<Products>
    suspend fun getUnsyncedProducts(): List<Products>
    suspend fun updateSyncStatus(id: String, syncState: String, updatedAt: Long)
    suspend fun findProductByBarcode(barcode: String): Products?
    suspend fun findConflictingProductForBarcodes(barcodes: List<String>, excludeProductId: String? = null): Pair<String, Products>?
}

class SqlDelightProductDataSource(
    database: AppDatabase
) : ProductLocalDataSource {
    private val queries = database.appDatabaseQueries

    override fun getAllProducts(): Flow<List<Products>> {
        return queries.selectAllProducts().asFlow().mapToList(Dispatchers.IO)
    }

    override fun getActiveProducts(): Flow<List<Products>> {
        return queries.selectActiveProducts().asFlow().mapToList(Dispatchers.IO)
    }

    override fun getProductSalesStats(): Flow<Map<String, ProductSalesStats>> {
        return queries.selectAllProductSalesStats().asFlow().mapToList(Dispatchers.IO).map { list ->
            list.associate { row ->
                row.product_id to ProductSalesStats(
                    productId = row.product_id,
                    totalVentas = row.total_unidades_vendidas,
                    ultimaVenta = row.ultima_venta
                )
            }
        }
    }

    override fun searchProducts(query: String, activeOnly: Boolean): Flow<List<Products>> {
        val trimmed = query.trim()
        val normalized = normalizeBarcode(trimmed)
        val q = if (activeOnly) {
            queries.searchActiveProducts(query = trimmed, normalizedQuery = normalized)
        } else {
            queries.searchProducts(query = trimmed, normalizedQuery = normalized)
        }
        return q.asFlow().mapToList(Dispatchers.IO)
    }

    override suspend fun getProductById(id: String): Products? = withContext(Dispatchers.IO) {
        queries.selectProductById(id).executeAsOneOrNull()
    }

    override suspend fun insertProduct(product: Products) {
        withContext(Dispatchers.IO) {
            queries.insertProduct(
                id = product.id,
                codigos = product.codigos,
                nombre = product.nombre,
                precio = product.precio,
                costo = product.costo,
                categoria = product.categoria,
                activo = product.activo,
                por_peso = product.por_peso,
                precio_mayoreo = product.precio_mayoreo,
                es_favorito = product.es_favorito,
                piezas = product.piezas,
                updated_at = product.updated_at,
                sync_state = product.sync_state
            )
        }
    }

    override suspend fun insertProducts(products: List<Products>) {
        withContext(Dispatchers.IO) {
            queries.transaction {
                for ((id, codigos, nombre, precio, costo, categoria, activo, por_peso, precio_mayoreo, es_favorito, piezas, updated_at, sync_state) in products) {
                    queries.insertProduct(
                        id = id,
                        codigos = codigos,
                        nombre = nombre,
                        precio = precio,
                        costo = costo,
                        categoria = categoria,
                        activo = activo,
                        por_peso = por_peso,
                        precio_mayoreo = precio_mayoreo,
                        es_favorito = es_favorito,
                        piezas = piezas,
                        updated_at = updated_at,
                        sync_state = sync_state
                    )
                }
            }
        }
    }

    override suspend fun updateProduct(product: Products) {
        withContext(Dispatchers.IO) {
            queries.updateProduct(
                id = product.id,
                codigos = product.codigos,
                nombre = product.nombre,
                precio = product.precio,
                costo = product.costo,
                categoria = product.categoria,
                activo = product.activo,
                por_peso = product.por_peso,
                precio_mayoreo = product.precio_mayoreo,
                es_favorito = product.es_favorito,
                piezas = product.piezas,
                updated_at = product.updated_at,
                sync_state = product.sync_state
            )
        }
    }

    override suspend fun deleteProductSoft(id: String, updatedAt: Long) {
        withContext(Dispatchers.IO) {
            queries.deleteProductSoft(updated_at = updatedAt, id = id)
        }
    }

    override suspend fun deleteProductHard(id: String) {
        withContext(Dispatchers.IO) {
            queries.transaction {
                queries.deleteProductHard(id)
                queries.insertDeletedSyncRecord(
                    id = id,
                    entity_type = "PRODUCT",
                    deleted_at = currentTimeMillis()
                )
            }
        }
    }

    override suspend fun deleteAllProducts() {
        withContext(Dispatchers.IO) {
            queries.transaction {
                val allIds = queries.selectAllProducts().executeAsList().map { it.id }
                val now = currentTimeMillis()
                queries.deleteAllProducts()
                for (id in allIds) {
                    queries.insertDeletedSyncRecord(
                        id = id,
                        entity_type = "PRODUCT",
                        deleted_at = now
                    )
                }
            }
        }
    }

    override suspend fun getAllProductsList(): List<Products> = withContext(Dispatchers.IO) {
        queries.selectAllProducts().executeAsList()
    }

    override suspend fun getUnsyncedProducts(): List<Products> = withContext(Dispatchers.IO) {
        queries.selectUnsyncedProducts().executeAsList()
    }

    override suspend fun updateSyncStatus(id: String, syncState: String, updatedAt: Long) {
        withContext(Dispatchers.IO) {
            queries.updateSyncStatus(sync_state = syncState, updated_at = updatedAt, id = id)
        }
    }

    override suspend fun findProductByBarcode(barcode: String): Products? = withContext(Dispatchers.IO) {
        val trimmed = barcode.trim()
        if (trimmed.isEmpty()) return@withContext null

        val list = queries.selectActiveProducts().executeAsList()
        list.firstOrNull { product ->
            product.parseBarcodes().matchesBarcode(trimmed)
        }
    }

    override suspend fun findConflictingProductForBarcodes(
        barcodes: List<String>,
        excludeProductId: String?
    ): Pair<String, Products>? = withContext(Dispatchers.IO) {
        val cleanBarcodes = barcodes.map { it.trim().replace("\"", "") }.filter { it.isNotEmpty() }
        if (cleanBarcodes.isEmpty()) return@withContext null

        val allProducts = queries.selectAllProducts().executeAsList()
        for (product in allProducts) {
            if (excludeProductId != null && product.id == excludeProductId) continue

            val productBarcodes = product.parseBarcodes()
            val matchingBarcode = cleanBarcodes.firstOrNull { code ->
                productBarcodes.matchesBarcode(code)
            }
            if (matchingBarcode != null) {
                return@withContext Pair(matchingBarcode, product)
            }
        }
        null
    }
}
