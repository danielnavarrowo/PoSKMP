package com.dnavarro.poskmp.data

import com.dnavarro.poskmp.data.source.local.ProductLocalDataSource
import com.dnavarro.poskmp.db.Products
import com.dnavarro.poskmp.util.currentTimeMillis
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface defining data operations for Products.
 * Adheres to Google's Android Data Layer architecture guidelines.
 */
interface ProductRepository {
    fun getAllProducts(): Flow<List<Products>>
    fun getActiveProducts(): Flow<List<Products>>
    fun searchProducts(query: String): Flow<List<Products>>
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
    suspend fun insertDummyDataIfEmpty()
    suspend fun findProductByBarcode(barcode: String): Products?
    suspend fun findConflictingProductForBarcodes(barcodes: List<String>, excludeProductId: String? = null): Pair<String, Products>?
}

/**
 * Concrete implementation of [ProductRepository] coordinating local and future remote data sources.
 */
class ProductRepositoryImpl(
    private val localDataSource: ProductLocalDataSource
) : ProductRepository {

    override fun getAllProducts(): Flow<List<Products>> = localDataSource.getAllProducts()

    override fun getActiveProducts(): Flow<List<Products>> = localDataSource.getActiveProducts()

    override fun searchProducts(query: String): Flow<List<Products>> = localDataSource.searchProducts(query)

    override suspend fun getProductById(id: String): Products? = localDataSource.getProductById(id)

    override suspend fun insertProduct(product: Products) = localDataSource.insertProduct(product)

    override suspend fun insertProducts(products: List<Products>) = localDataSource.insertProducts(products)

    override suspend fun updateProduct(product: Products) = localDataSource.updateProduct(product)

    override suspend fun deleteProductSoft(id: String, updatedAt: Long) =
        localDataSource.deleteProductSoft(id, updatedAt)

    override suspend fun deleteProductHard(id: String) = localDataSource.deleteProductHard(id)

    override suspend fun deleteAllProducts() = localDataSource.deleteAllProducts()

    override suspend fun getAllProductsList(): List<Products> = localDataSource.getAllProductsList()

    override suspend fun getUnsyncedProducts(): List<Products> = localDataSource.getUnsyncedProducts()

    override suspend fun updateSyncStatus(id: String, syncState: String, updatedAt: Long) =
        localDataSource.updateSyncStatus(id, syncState, updatedAt)

    override suspend fun findProductByBarcode(barcode: String): Products? =
        localDataSource.findProductByBarcode(barcode)

    override suspend fun findConflictingProductForBarcodes(
        barcodes: List<String>,
        excludeProductId: String?
    ): Pair<String, Products>? =
        localDataSource.findConflictingProductForBarcodes(barcodes, excludeProductId)

    override suspend fun insertDummyDataIfEmpty() {
        val existing = localDataSource.getAllProductsList()
        if (existing.isEmpty()) {
            val now = currentTimeMillis()
            val dummyList = listOf(
                Products(id = "1", codigos = "[\"75010001\"]", nombre = "Coca Cola 600ml", precio = 18.0, costo = 12.5, categoria = "Bebidas", activo = 1, por_peso = 0, precio_mayoreo = 16.0, es_favorito = 1, piezas = 1.0, updated_at = now, sync_state = "PENDING_INSERT"),
                Products(id = "2", codigos = "[\"75010002\"]", nombre = "Sabritas Sal 45g", precio = 17.0, costo = 11.0, categoria = "Botanas", activo = 1, por_peso = 0, precio_mayoreo = 15.0, es_favorito = 1, piezas = 1.0, updated_at = now, sync_state = "PENDING_INSERT"),
                Products(id = "3", codigos = "[\"75010003\"]", nombre = "Jitomate Saladet", precio = 35.0, costo = 20.0, categoria = "Frutas y Verduras", activo = 1, por_peso = 1, precio_mayoreo = 30.0, es_favorito = 0, piezas = 1.0, updated_at = now, sync_state = "PENDING_INSERT"),
                Products(id = "4", codigos = "[\"75010004\"]", nombre = "Huevo Blanco Kg", precio = 42.0, costo = 34.0, categoria = "Abarrotes", activo = 1, por_peso = 1, precio_mayoreo = 38.0, es_favorito = 0, piezas = 1.0, updated_at = now, sync_state = "PENDING_INSERT"),
                Products(id = "5", codigos = "[\"75010005\"]", nombre = "Gansito Marinela 50g", precio = 15.5, costo = 10.0, categoria = "Panadería", activo = 1, por_peso = 0, precio_mayoreo = 14.0, es_favorito = 1, piezas = 1.0, updated_at = now, sync_state = "PENDING_INSERT")
            )
            localDataSource.insertProducts(dummyList)
        }
    }
}
