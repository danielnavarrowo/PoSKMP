package com.dnavarro.poskmp.data

import com.dnavarro.poskmp.data.source.local.ProductLocalDataSource
import com.dnavarro.poskmp.db.Products
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface defining data operations for Products.
 * Adheres to Google's Android Data Layer architecture guidelines.
 */
interface ProductRepository {
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

/**
 * Concrete implementation of [ProductRepository] coordinating local and future remote data sources.
 */
class ProductRepositoryImpl(
    private val localDataSource: ProductLocalDataSource
) : ProductRepository {

    override fun getAllProducts(): Flow<List<Products>> = localDataSource.getAllProducts()

    override fun getActiveProducts(): Flow<List<Products>> = localDataSource.getActiveProducts()

    override fun searchProducts(query: String, activeOnly: Boolean): Flow<List<Products>> =
        localDataSource.searchProducts(query, activeOnly)

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
}
