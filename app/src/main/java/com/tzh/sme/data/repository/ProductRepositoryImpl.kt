package com.tzh.sme.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.tzh.sme.data.model.ProductModel
import com.tzh.sme.domain.repository.AuthRepository
import com.tzh.sme.domain.repository.CategoryRepository
import com.tzh.sme.domain.repository.ProductRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class ProductRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val authRepository: AuthRepository,
    private val categoryRepository: CategoryRepository
) : ProductRepository {

    private fun getProductsCollection(shopId: String) = firestore
        .collection("shops")
        .document(shopId)
        .collection("products")

    override fun getAllProducts(): Flow<List<ProductModel>> =
        authRepository.currentUser.flatMapLatest { user ->
            val shopId = user?.shopId ?: user?.id
            if (shopId == null) {
                flowOf(emptyList())
            } else {
                callbackFlow {
                    val subscription =
                        getProductsCollection(shopId).addSnapshotListener { snapshot, error ->
                            if (error != null) {
                                Log.w("ProductRepository", "Listen failed: $error")
                                return@addSnapshotListener
                            }
                            if (snapshot != null) {
                                val products = snapshot.toObjects(ProductModel::class.java)
                                trySend(products.sortedBy { it.name })
                            }
                        }
                    awaitClose { subscription.remove() }
                }
            }
        }

    override fun searchProducts(query: String): Flow<List<ProductModel>> =
        authRepository.currentUser.flatMapLatest { user ->
            val shopId = user?.shopId ?: user?.id
            if (shopId == null) {
                flowOf(emptyList())
            } else {
                callbackFlow {
                    val subscription =
                        getProductsCollection(shopId).addSnapshotListener { snapshot, error ->
                            if (error != null) {
                                Log.w("ProductRepository", "Listen failed: $error")
                                return@addSnapshotListener
                            }
                            if (snapshot != null) {
                                val products = snapshot.toObjects(ProductModel::class.java)
                                    .filter {
                                        it.name.contains(
                                            query,
                                            ignoreCase = true
                                        ) || it.barcode.contains(query)
                                    }
                                trySend(products)
                            }
                        }
                    awaitClose { subscription.remove() }
                }
            }
        }

    override suspend fun getProductByBarcode(barcode: String): ProductModel? {
        val currentUser = authRepository.currentUser.value
        val shopId = currentUser?.shopId ?: currentUser?.id ?: return null
        return try {
            val snapshot = getProductsCollection(shopId)
                .whereEqualTo("barcode", barcode)
                .get()
                .await()
            snapshot.toObjects(ProductModel::class.java).firstOrNull()
        } catch (e: Exception) {
            Log.e("ProductRepository", "Error getting product by barcode", e)
            null
        }
    }

    override suspend fun getProductById(id: String): ProductModel? {
        val currentUser = authRepository.currentUser.value
        val shopId = currentUser?.shopId ?: currentUser?.id ?: return null
        return try {
            val snapshot = getProductsCollection(shopId).document(id).get().await()
            snapshot.toObject(ProductModel::class.java)
        } catch (e: Exception) {
            Log.e("ProductRepository", "Error getting product by id", e)
            null
        }
    }

    override suspend fun addOrUpdateProduct(product: ProductModel) {
        val currentUser = authRepository.currentUser.value
        val shopId = currentUser?.shopId ?: currentUser?.id ?: return
        try {
            val collection = getProductsCollection(shopId)
            val docRef = if (product.id.isEmpty()) {
                collection.document()
            } else {
                collection.document(product.id)
            }

            val finalProduct = if (product.id.isEmpty()) product.copy(id = docRef.id) else product
            docRef.set(finalProduct).await()
        } catch (e: Exception) {
            Log.e("ProductRepository", "Error adding/updating product", e)
        }
    }

    override suspend fun deleteProduct(product: ProductModel) {
        val currentUser = authRepository.currentUser.value
        val shopId = currentUser?.shopId ?: currentUser?.id ?: return
        try {
            getProductsCollection(shopId).document(product.id).delete().await()
        } catch (e: Exception) {
            Log.e("ProductRepository", "Error deleting product", e)
        }
    }

    override fun generateNextId(): String {
        val currentUser = authRepository.currentUser.value
        val shopId = currentUser?.shopId ?: currentUser?.id ?: return ""
        return getProductsCollection(shopId).document().id
    }
}
