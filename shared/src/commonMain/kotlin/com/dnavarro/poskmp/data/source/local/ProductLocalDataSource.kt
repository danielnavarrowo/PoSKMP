package com.dnavarro.poskmp.data.source.local

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.dnavarro.poskmp.db.AppDatabase
import com.dnavarro.poskmp.db.Products
import com.dnavarro.poskmp.domain.model.ProductSalesStats
import com.dnavarro.poskmp.util.currentTimeMillis
import com.dnavarro.poskmp.util.matchesBarcode
import com.dnavarro.poskmp.util.matchesSearch
import com.dnavarro.poskmp.util.normalizeBarcode
import com.dnavarro.poskmp.util.parseBarcodes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

interface ProductLocalDataSource {
    fun getAllProducts(): Flow<List<Products>>
    fun getActiveProducts(): Flow<List<Products>>
    fun searchProducts(query: String, activeOnly: Boolean = false): Flow<List<Products>>
    fun getProductSalesStats(): Flow<Map<String, ProductSalesStats>>
    suspend fun getProductById(id: String): Products?
    suspend fun insertProduct(product: Products)
    suspend fun insertProducts(products: List<Products>)
    suspend fun updateProduct(product: Products)
    suspend fun updateProducts(products: List<Products>)
    suspend fun deleteProductSoft(id: String, updatedAt: Long)
    suspend fun deleteProductHard(id: String)
    suspend fun deleteProductsHard(ids: List<String>)
    suspend fun deleteAllProducts()
    suspend fun getAllProductsList(): List<Products>
    suspend fun getUnsyncedProducts(): List<Products>
    suspend fun updateSyncStatus(id: String, syncState: String, updatedAt: Long)
    suspend fun findProductByBarcode(barcode: String): Products?
    suspend fun findConflictingProductForBarcodes(barcodes: List<String>, excludeProductId: String? = null): Pair<String, Products>?
    suspend fun getProductByName(name: String): Products?
}

class SqlDelightProductDataSource(
    database: AppDatabase
) : ProductLocalDataSource {
    private val queries = database.appDatabaseQueries

    @Volatile
    private var isBarcodesTableAvailable: Boolean = false

    private fun checkBarcodesTableExists(): Boolean {
        return isBarcodesTableAvailable || try {
            queries.selectBarcodesByProductId("").executeAsList()
            isBarcodesTableAvailable = true
            true
        } catch (_: Exception) {
            isBarcodesTableAvailable = false
            false
        }
    }

    init {
        // Asegurar que la tabla product_barcodes esté poblada en bases de datos existentes
        try {
            if (checkBarcodesTableExists()) {
                val allProds = queries.selectAllProducts().executeAsList()
                if (allProds.isNotEmpty()) {
                    val firstProductWithCodes = allProds.firstOrNull { parseBarcodes(it.codigos).isNotEmpty() }
                    if (firstProductWithCodes != null) {
                        val firstCode = parseBarcodes(firstProductWithCodes.codigos).first()
                        val existing = queries.selectProductByBarcode(firstCode, normalizeBarcode(firstCode)).executeAsOneOrNull()
                        if (existing == null) {
                            queries.transaction {
                                for ((id, codigos) in allProds) {
                                    syncBarcodesForProduct(id, codigos)
                                }
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {
            isBarcodesTableAvailable = false
        }
    }

    private fun syncBarcodesForProduct(productId: String, codigosJson: String) {
        if (!checkBarcodesTableExists()) return
        try {
            queries.deleteBarcodesByProductId(productId)
            val codes = parseBarcodes(codigosJson)
            for (code in codes) {
                val trimmed = code.trim()
                if (trimmed.isNotEmpty()) {
                    queries.insertProductBarcode(trimmed, productId)
                    val norm = normalizeBarcode(trimmed)
                    if (norm.isNotEmpty() && norm != trimmed) {
                        queries.insertProductBarcode(norm, productId)
                    }
                }
            }
        } catch (_: Exception) {
            isBarcodesTableAvailable = false
        }
    }

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
        val sourceFlow = if (activeOnly) queries.selectActiveProducts() else queries.selectAllProducts()
        if (trimmed.isEmpty()) {
            return sourceFlow.asFlow().mapToList(Dispatchers.IO)
        }
        val normalized = normalizeBarcode(trimmed)
        return sourceFlow.asFlow().mapToList(Dispatchers.IO).map { products ->
            products.filter { product ->
                product.matchesSearch(trimmed, normalized)
            }
        }
    }

    override suspend fun getProductById(id: String): Products? = withContext(Dispatchers.IO) {
        queries.selectProductById(id).executeAsOneOrNull()
    }

    override suspend fun insertProduct(product: Products) {
        withContext(Dispatchers.IO) {
            queries.transaction {
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
                    precio_delivery = product.precio_delivery,
                    created_at = product.created_at,
                    updated_at = product.updated_at,
                    sync_state = product.sync_state
                )
                syncBarcodesForProduct(product.id, product.codigos)
            }
        }
    }

    override suspend fun insertProducts(products: List<Products>) {
        withContext(Dispatchers.IO) {
            queries.transaction {
                for ((id, codigos, nombre, precio, costo, categoria, activo, por_peso, precio_mayoreo, es_favorito, piezas, precio_delivery, created_at, updated_at, sync_state) in products) {
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
                        precio_delivery = precio_delivery,
                        created_at = created_at,
                        updated_at = updated_at,
                        sync_state = sync_state
                    )
                    syncBarcodesForProduct(id, codigos)
                }
            }
        }
    }

    override suspend fun updateProduct(product: Products) {
        withContext(Dispatchers.IO) {
            queries.transaction {
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
                    precio_delivery = product.precio_delivery,
                    created_at = product.created_at,
                    updated_at = product.updated_at,
                    sync_state = product.sync_state
                )
                syncBarcodesForProduct(product.id, product.codigos)
            }
        }
    }

    override suspend fun updateProducts(products: List<Products>) {
        withContext(Dispatchers.IO) {
            queries.transaction {
                for ((id, codigos, nombre, precio, costo, categoria, activo, por_peso, precio_mayoreo, es_favorito, piezas, precio_delivery, created_at, updated_at, sync_state) in products) {
                    queries.updateProduct(
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
                        precio_delivery = precio_delivery,
                        created_at = created_at,
                        updated_at = updated_at,
                        sync_state = sync_state
                    )
                    syncBarcodesForProduct(id, codigos)
                }
            }
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
                if (checkBarcodesTableExists()) {
                    try {
                        queries.deleteBarcodesByProductId(id)
                    } catch (_: Exception) {
                        isBarcodesTableAvailable = false
                    }
                }
                queries.deleteProductHard(id)
                queries.insertDeletedSyncRecord(
                    id = id,
                    entity_type = "PRODUCT",
                    deleted_at = currentTimeMillis()
                )
            }
        }
    }

    override suspend fun deleteProductsHard(ids: List<String>) {
        withContext(Dispatchers.IO) {
            queries.transaction {
                val now = currentTimeMillis()
                if (checkBarcodesTableExists()) {
                    try {
                        for (chunk in ids.chunked(500)) {
                            queries.deleteBarcodesByProductIds(chunk)
                        }
                    } catch (_: Exception) {
                        isBarcodesTableAvailable = false
                    }
                }
                for (id in ids) {
                    queries.deleteProductHard(id)
                    queries.insertDeletedSyncRecord(
                        id = id,
                        entity_type = "PRODUCT",
                        deleted_at = now
                    )
                }
            }
        }
    }

    override suspend fun deleteAllProducts() {
        withContext(Dispatchers.IO) {
            queries.transaction {
                val allIds = queries.selectAllProducts().executeAsList().map { it.id }
                val now = currentTimeMillis()
                if (checkBarcodesTableExists()) {
                    try {
                        queries.deleteAllProductBarcodes()
                    } catch (_: Exception) {
                        isBarcodesTableAvailable = false
                    }
                }
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
        val norm = normalizeBarcode(trimmed)

        if (checkBarcodesTableExists()) {
            try {
                val found = queries.selectProductByBarcode(barcode = trimmed, normalizedBarcode = norm).executeAsOneOrNull()
                if (found != null) return@withContext found
            } catch (_: Exception) {
                isBarcodesTableAvailable = false
            }
        }

        // Fallback a escaneo en memoria si la tabla no existe o falló la consulta
        val list = queries.selectActiveProducts().executeAsList()
        list.firstOrNull { product ->
            parseBarcodes(product.codigos).matchesBarcode(trimmed)
        }
    }

    override suspend fun findConflictingProductForBarcodes(
        barcodes: List<String>,
        excludeProductId: String?
    ): Pair<String, Products>? = withContext(Dispatchers.IO) {
        val cleanBarcodes = barcodes.map { it.trim().replace("\"", "") }.filter { it.isNotEmpty() }
        if (cleanBarcodes.isEmpty()) return@withContext null

        val allSearchCodes = cleanBarcodes.flatMap { code ->
            val norm = normalizeBarcode(code)
            if (norm.isNotEmpty() && norm != code) listOf(code, norm) else listOf(code)
        }.distinct()

        if (checkBarcodesTableExists()) {
            try {
                val conflictRow = queries.selectConflictingBarcode(
                    barcode = allSearchCodes,
                    excludeProductId = excludeProductId
                ).executeAsOneOrNull()

                if (conflictRow != null) {
                    val product = Products(
                        id = conflictRow.id,
                        codigos = conflictRow.codigos,
                        nombre = conflictRow.nombre,
                        precio = conflictRow.precio,
                        costo = conflictRow.costo,
                        categoria = conflictRow.categoria,
                        activo = conflictRow.activo,
                        por_peso = conflictRow.por_peso,
                        precio_mayoreo = conflictRow.precio_mayoreo,
                        es_favorito = conflictRow.es_favorito,
                        piezas = conflictRow.piezas,
                        precio_delivery = conflictRow.precio_delivery,
                        created_at = conflictRow.created_at,
                        updated_at = conflictRow.updated_at,
                        sync_state = conflictRow.sync_state
                    )
                    return@withContext Pair(conflictRow.barcode, product)
                }
                return@withContext null
            } catch (_: Exception) {
                isBarcodesTableAvailable = false
            }
        }

        // Fallback a escaneo en memoria si la tabla no existe o falló la consulta
        val allProducts = queries.selectAllProducts().executeAsList()
        for (product in allProducts) {
            if (excludeProductId != null && product.id == excludeProductId) continue

            val productBarcodes = parseBarcodes(product.codigos)
            val matchingBarcode = cleanBarcodes.firstOrNull { code ->
                productBarcodes.matchesBarcode(code)
            }
            if (matchingBarcode != null) {
                return@withContext Pair(matchingBarcode, product)
            }
        }
        null
    }

    override suspend fun getProductByName(name: String): Products? = withContext(Dispatchers.IO) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return@withContext null
        queries.selectProductByName(trimmed).executeAsOneOrNull()
    }
}
