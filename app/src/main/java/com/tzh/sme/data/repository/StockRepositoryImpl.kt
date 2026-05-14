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

    override suspend fun addCategory(category: CategoryModel) =
        categoryRepository.addCategory(category)

    override suspend fun deleteCategory(category: CategoryModel) =
        categoryRepository.deleteCategory(category)

    override fun generateNextId(): String = productRepository.generateNextId()

    override suspend fun syncFromFirestore(): Result<Unit> = Result.success(Unit)


//    suspend fun updateDocumentId(oldId: String, newId: String): Result<Unit> {
//        return try {
//
//            // 2. Define the subcollections that need to move
//            val subcollections = listOf("products", "categories", "transactions")
//
//            // 4. Migrate each subcollection's documents
//            for (subName in subcollections) {
//                migrateSubcollection(oldId, newId, subName)
//            }
//
//            Log.d("Migration", "Successfully migrated user $oldId and all subcollections to $newId")
//            Result.success(Unit)
//        } catch (e: Exception) {
//            Log.e("Migration", "Error during migration: ${e.message}")
//            Result.failure(e)
//        }
//    }
//
//    private suspend fun migrateSubcollection(
//        oldUserId: String,
//        newUserId: String,
//        collectionName: String
//    ) {
//        val oldCollection =
//            firestore.collection("users").document(oldUserId).collection(collectionName)
//        val newCollection =
//            firestore.collection("users").document(newUserId).collection(collectionName)
//
//        val snapshot = oldCollection.get().await()
//        if (snapshot.isEmpty) return
//        Log.e("Migration", "Migrating $collectionName")
//        Log.e("ASDASDASDASD", snapshot.documents.toString())
//        // Firestore batches have a limit of 500 operations
//        val batches = snapshot.documents.chunked(500)
//        for (chunk in batches) {
//            firestore.runBatch { batch ->
//                for (doc in chunk) {
//                    val data = doc.data ?: continue
//                    // Create document with the same ID at the new path
//                    batch.set(newCollection.document(doc.id), data)
//                    // Delete the document from the old path
//                    batch.delete(doc.reference)
//                }
//            }.await()
//        }
//    }
}
