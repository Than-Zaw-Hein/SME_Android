package com.tzh.sme.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.tzh.sme.data.model.CategoryModel
import com.tzh.sme.data.model.ProductModel
import com.tzh.sme.data.model.TransactionModel
import com.tzh.sme.data.model.TransactionItemModel
import com.tzh.sme.domain.repository.CategoryRepository
import com.tzh.sme.domain.repository.ProductRepository
import com.tzh.sme.domain.repository.StockRepository
import com.tzh.sme.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StockRepositoryImpl @Inject constructor(
    private val productRepository: ProductRepository,
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository,
    private val firestore: FirebaseFirestore,

    ) : StockRepository {

    override fun getAllProducts(): Flow<List<ProductModel>> = productRepository.getAllProducts()

    override fun searchProducts(query: String): Flow<List<ProductModel>> =
        productRepository.searchProducts(query)

    override suspend fun getProductByBarcode(barcode: String): ProductModel? =
        productRepository.getProductByBarcode(barcode)

    override suspend fun getProductById(id: String): ProductModel? =
        productRepository.getProductById(id)

    override suspend fun addOrUpdateProduct(product: ProductModel) =
        productRepository.addOrUpdateProduct(product)

    override suspend fun deleteProduct(product: ProductModel) =
        productRepository.deleteProduct(product)

    override suspend fun executeTransaction(
        transaction: TransactionModel,
        items: List<TransactionItemModel>
    ): Result<Pair<TransactionModel, List<TransactionItemModel>>> {
        return transactionRepository.executeTransaction(transaction, items)
    }

    override fun getAllCategories(): Flow<List<CategoryModel>> =
        categoryRepository.getAllCategories()

    override suspend fun addCategory(category: CategoryModel) : CategoryModel? =
        categoryRepository.addCategory(category)

    override suspend fun deleteCategory(category: CategoryModel) =
        categoryRepository.deleteCategory(category)

    override fun generateNextId(): String = productRepository.generateNextId()

    override suspend fun syncFromFirestore(): Result<Unit> = Result.success(Unit)
}
