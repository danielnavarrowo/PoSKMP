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

    override suspend fun insertDummyDataIfEmpty() {
        val existing = localDataSource.getAllProductsList()
        if (existing.isEmpty()) {
            val now = currentTimeMillis()
            val dummyList = listOf(
                Products("1", "[\"75010001\"]", "Coca Cola 600ml", 18.0, 12.5, "Bebidas", 1, 0, 16.0, 1, now, "PENDING_INSERT"),
                Products("2", "[\"75010002\"]", "Sabritas Sal 45g", 17.0, 11.0, "Botanas", 1, 0, 15.0, 1, now, "PENDING_INSERT"),
                Products("3", "[\"75010003\"]", "Jitomate Saladet", 35.0, 20.0, "Frutas y Verduras", 1, 1, 30.0, 0, now, "PENDING_INSERT"),
                Products("4", "[\"75010004\"]", "Huevo Blanco Kg", 42.0, 34.0, "Abarrotes", 1, 1, 38.0, 0, now, "PENDING_INSERT"),
                Products("5", "[\"75010005\"]", "Gansito Marinela 50g", 15.5, 10.0, "Panadería", 1, 0, 14.0, 1, now, "PENDING_INSERT")
            )
            localDataSource.insertProducts(dummyList)
        }
    }
}
