package com.tzh.sme.data.repository

import com.tzh.sme.data.local.dao.CategoryDao
import com.tzh.sme.data.local.dao.ProductDao
import com.tzh.sme.data.local.dao.TransactionDao
import com.tzh.sme.data.local.entities.CategoryEntity
import com.tzh.sme.data.local.entities.ProductEntity
import com.tzh.sme.data.local.entities.TransactionEntity
import com.tzh.sme.data.local.entities.TransactionItemEntity
import com.tzh.sme.domain.repository.StockRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StockRepositoryImpl @Inject constructor(
    private val productDao: ProductDao,
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao
) : StockRepository {

    override fun getAllProducts(): Flow<List<ProductEntity>> = productDao.getAllProducts()

    override fun searchProducts(query: String): Flow<List<ProductEntity>> = productDao.searchProducts(query)

    override suspend fun getProductByBarcode(barcode: String): ProductEntity? = productDao.getProductByBarcode(barcode)

    override suspend fun getProductById(id: Long): ProductEntity? = productDao.getProductById(id)

    override suspend fun addOrUpdateProduct(product: ProductEntity) {
        productDao.insertProduct(product)
        // Also ensure the category exists in the categories table
        categoryDao.insertCategory(CategoryEntity(product.category))
    }

    override suspend fun deleteProduct(product: ProductEntity) {
        productDao.deleteProduct(product)
    }

    override suspend fun executeTransaction(transaction: TransactionEntity, items: List<TransactionItemEntity>) {
        val id = transactionDao.insertTransaction(transaction)
        val itemsWithId = items.map { it.copy(transactionId = id) }
        transactionDao.insertTransactionItems(itemsWithId)
        items.forEach { item ->
            productDao.decrementQuantity(item.productId, item.quantity)
        }
    }

    override fun getAllCategories(): Flow<List<CategoryEntity>> = categoryDao.getAllCategories()

    override suspend fun addCategory(category: CategoryEntity) {
        categoryDao.insertCategory(category)
    }
}
