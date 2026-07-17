package com.dnavarro.poskmp.data

import com.dnavarro.poskmp.db.AppDatabase
import com.dnavarro.poskmp.db.Products
import com.dnavarro.poskmp.util.currentTimeMillis
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ProductRepository(database: AppDatabase) {
    private val queries = database.appDatabaseQueries

    fun getAllProducts(): Flow<List<Products>> {
        return queries.selectAllProducts().asFlow().mapToList(Dispatchers.IO)
    }

    fun getActiveProducts(): Flow<List<Products>> {
        return queries.selectActiveProducts().asFlow().mapToList(Dispatchers.IO)
    }

    fun getProductById(id: String): Products? {
        return queries.selectProductById(id).executeAsOneOrNull()
    }

    fun searchProducts(query: String): Flow<List<Products>> {
        return queries.searchProducts(query, query, query).asFlow().mapToList(Dispatchers.IO)
    }

    fun insertProduct(product: Products) {
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
            updated_at = product.updated_at,
            sync_state = product.sync_state
        )
    }

    fun updateProduct(product: Products) {
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
            updated_at = product.updated_at,
            sync_state = product.sync_state
        )
    }

    fun deleteProductSoft(id: String, updatedAt: Long) {
        queries.deleteProductSoft(updated_at = updatedAt, id = id)
    }

    fun deleteProductHard(id: String) {
        queries.deleteProductHard(id)
    }

    fun deleteAllProducts() {
        queries.deleteAllProducts()
    }

    fun getAllProductsList(): List<Products> {
        return queries.selectAllProducts().executeAsList()
    }

    fun getUnsyncedProducts(): List<Products> {
        return queries.selectUnsyncedProducts().executeAsList()
    }

    fun updateSyncStatus(id: String, syncState: String, updatedAt: Long) {
        queries.updateSyncStatus(sync_state = syncState, updated_at = updatedAt, id = id)
    }

    fun insertDummyDataIfEmpty() {
        val existing = queries.selectAllProducts().executeAsList()
        if (existing.isEmpty()) {
            val now = currentTimeMillis()
            val dummyList = listOf(
                ProductDummy("1", "[\"75010001\"]", "Coca Cola 600ml", 18.0, 12.5, "Bebidas", 1, 0, 16.0, 1),
                ProductDummy("2", "[\"75010002\"]", "Sabritas Sal 45g", 17.0, 11.0, "Botanas", 1, 0, 15.0, 1),
                ProductDummy("3", "[\"75010003\"]", "Jitomate Saladet", 35.0, 20.0, "Frutas y Verduras", 1, 1, 30.0, 0),
                ProductDummy("4", "[\"75010004\"]", "Huevo Blanco Kg", 42.0, 34.0, "Abarrotes", 1, 1, 38.0, 0),
                ProductDummy("5", "[\"75010005\"]", "Gansito Marinela 50g", 15.5, 10.0, "Panadería", 1, 0, 14.0, 1)
            )
            for ((id, codigos, nombre, precio, costo, categoria, activo, porPeso, precioMayoreo, esFavorito) in dummyList) {
                queries.insertProduct(
                    id = id,
                    codigos = codigos,
                    nombre = nombre,
                    precio = precio,
                    costo = costo,
                    categoria = categoria,
                    activo = activo,
                    por_peso = porPeso,
                    precio_mayoreo = precioMayoreo,
                    es_favorito = esFavorito,
                    updated_at = now,
                    sync_state = "PENDING_INSERT"
                )
            }
        }
    }

    suspend fun findProductByBarcode(barcode: String): Products? {
        return withContext(Dispatchers.IO) {
            val list = queries.selectActiveProducts().executeAsList()
            list.firstOrNull { product ->
                product.id == barcode || product.codigos.contains(barcode)
            }
        }
    }
}

private data class ProductDummy(
    val id: String,
    val codigos: String,
    val nombre: String,
    val precio: Double,
    val costo: Double,
    val categoria: String,
    val activo: Long,
    val porPeso: Long,
    val precioMayoreo: Double,
    val esFavorito: Long
)
