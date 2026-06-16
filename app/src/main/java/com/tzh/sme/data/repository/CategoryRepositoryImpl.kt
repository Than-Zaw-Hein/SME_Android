package com.tzh.sme.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.tzh.sme.data.model.CategoryModel
import com.tzh.sme.domain.repository.AuthRepository
import com.tzh.sme.domain.repository.CategoryRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class CategoryRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val authRepository: AuthRepository
) : CategoryRepository {

    private fun getCategoryCollection(shopId: String) = firestore
        .collection("shops")
        .document(shopId)
        .collection("categories")

    override fun getAllCategories(): Flow<List<CategoryModel>> =
        authRepository.currentUser.flatMapLatest { user ->
            val shopId = user?.shopId ?: user?.id
            if (shopId == null) {
                flowOf(emptyList())
            } else {
                callbackFlow {
                    val subscription =
                        getCategoryCollection(shopId).addSnapshotListener { snapshot, error ->
                            if (error != null) {
                                Log.w("CategoryRepository", "Listen failed: $error")
                                return@addSnapshotListener
                            }
                            if (snapshot != null) {
                                val categories = snapshot.toObjects(CategoryModel::class.java)
                                trySend(categories)
                            }
                        }
                    awaitClose { subscription.remove() }
                }
            }
        }

    override suspend fun addCategory(category: CategoryModel): CategoryModel? {
        return try {
            val currentUser = authRepository.currentUser.value
            val shopId = currentUser?.shopId ?: currentUser?.id ?: return null
            val docRef = if (category.id.isEmpty()) {
                getCategoryCollection(shopId).document()
            } else {
                getCategoryCollection(shopId).document(category.id)
            }
            val finalCategory = if (category.id.isEmpty()) {
                category.copy(id = docRef.id)
            } else {
                category
            }
            docRef.set(finalCategory).await()
            finalCategory
        } catch (e: Exception) {
            Log.e("CategoryRepository", "Failed to add/update category: ${e.message}")
            null
        }
    }

    override suspend fun updateCategory(category: CategoryModel) {
        addCategory(category)
    }

    override suspend fun deleteCategory(category: CategoryModel) {
        try {
            val currentUser = authRepository.currentUser.value
            val shopId = currentUser?.shopId ?: currentUser?.id ?: return
            if (category.id.isNotEmpty()) {
                getCategoryCollection(shopId).document(category.id).delete().await()
            }
        } catch (e: Exception) {
            Log.e("CategoryRepository", "Failed to delete category: ${e.message}")
        }
    }
}
