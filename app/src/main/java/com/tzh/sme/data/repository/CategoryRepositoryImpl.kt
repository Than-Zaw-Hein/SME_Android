package com.tzh.sme.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.tzh.sme.data.model.CategoryModel
import com.tzh.sme.domain.repository.CategoryRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.jvm.Throws

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class CategoryRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : CategoryRepository {

    private fun getAuthStateFlow(): Flow<String?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser?.uid)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    private fun getCategoryCollection(userId:String) = firestore
        .collection("users")
        .document(userId)
        .collection("categories")

    override fun getAllCategories(): Flow<List<CategoryModel>> = getAuthStateFlow().flatMapLatest { uid ->
        if (uid == null) {
            flowOf(emptyList())
        } else {
            callbackFlow {
                val subscription = getCategoryCollection(uid).addSnapshotListener { snapshot, error ->
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

    override suspend fun addCategory(category: CategoryModel) {
        try {
            val uid = auth.currentUser?.uid ?: return
            val docRef = if (category.id.isEmpty()) {
                getCategoryCollection(uid).document()
            } else {
                getCategoryCollection(uid).document(category.id)
            }
            val finalCategory = if (category.id.isEmpty()) {
                category.copy(id = docRef.id)
            } else {
                category
            }
            docRef.set(finalCategory).await()
        } catch (e: Exception) {
            Log.e("CategoryRepository", "Failed to add/update category: ${e.message}")
        }
    }

    override suspend fun updateCategory(category: CategoryModel) {
        addCategory(category)
    }

    override suspend fun deleteCategory(category: CategoryModel) {
        try {
            val uid = auth.currentUser?.uid ?: return
            if (category.id.isNotEmpty()) {
                getCategoryCollection(uid).document(category.id).delete().await()
            }
        } catch (e: Exception) {
            Log.e("CategoryRepository", "Failed to delete category: ${e.message}")
        }
    }
}
