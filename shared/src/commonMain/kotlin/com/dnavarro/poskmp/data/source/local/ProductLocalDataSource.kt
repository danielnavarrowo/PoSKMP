package com.dnavarro.poskmp.data.source.local

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.dnavarro.poskmp.db.AppDatabase
import com.dnavarro.poskmp.db.Products
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

import com.dnavarro.poskmp.util.matchesBarcode
import com.dnavarro.poskmp.util.normalizeBarcode
import com.dnavarro.poskmp.util.parseBarcodes

interface ProductLocalDataSource {
    fun getAllProducts(): Flow<List<Products>>
    fun getActiveProducts(): Flow<List<Products>>
    fun searchProducts(query: String, activeOnly: Boolean = false): Flow<List<Products>>
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
                for (product in products) {
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
            queries.deleteProductHard(id)
        }
    }

    override suspend fun deleteAllProducts() {
        withContext(Dispatchers.IO) {
            queries.deleteAllProducts()
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
