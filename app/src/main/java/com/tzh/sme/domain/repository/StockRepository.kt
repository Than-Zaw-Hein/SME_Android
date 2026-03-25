package com.tzh.sme.domain.repository

import com.tzh.sme.data.local.entities.CategoryEntity
import com.tzh.sme.data.local.entities.ProductEntity
import com.tzh.sme.data.local.entities.TransactionEntity
import com.tzh.sme.data.local.entities.TransactionItemEntity
import kotlinx.coroutines.flow.Flow

interface StockRepository {
    fun getAllProducts(): Flow<List<ProductEntity>>
    fun searchProducts(query: String): Flow<List<ProductEntity>>
    suspend fun getProductByBarcode(barcode: String): ProductEntity?
    suspend fun getProductById(id: Long): ProductEntity?
    suspend fun addOrUpdateProduct(product: ProductEntity)
    suspend fun deleteProduct(product: ProductEntity)
    suspend fun executeTransaction(transaction: TransactionEntity, items: List<TransactionItemEntity>)
    
    fun getAllCategories(): Flow<List<CategoryEntity>>
    suspend fun addCategory(category: CategoryEntity)
}
