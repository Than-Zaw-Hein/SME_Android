package com.tzh.sme.domain.repository

import com.tzh.sme.data.model.CategoryModel
import com.tzh.sme.data.model.ProductModel
import com.tzh.sme.data.model.TransactionModel
import com.tzh.sme.data.model.TransactionItemModel
import kotlinx.coroutines.flow.Flow

interface StockRepository {
    fun getAllProducts(): Flow<List<ProductModel>>
    fun searchProducts(query: String): Flow<List<ProductModel>>
    suspend fun getProductByBarcode(barcode: String): ProductModel?
    suspend fun getProductById(id: String): ProductModel?
    suspend fun addOrUpdateProduct(product: ProductModel)
    suspend fun deleteProduct(product: ProductModel)
    suspend fun executeTransaction(transaction: TransactionModel, items: List<TransactionItemModel>): Result<Pair<TransactionModel, List<TransactionItemModel>>>
    
    fun getAllCategories(): Flow<List<CategoryModel>>
    suspend fun addCategory(category: CategoryModel)
    suspend fun deleteCategory(category: CategoryModel)
    
    fun generateNextId(): String
    suspend fun syncFromFirestore(): Result<Unit>
}
