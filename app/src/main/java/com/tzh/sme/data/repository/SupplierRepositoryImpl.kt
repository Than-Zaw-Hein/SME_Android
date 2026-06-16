package com.tzh.sme.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.tzh.sme.data.model.SupplierModel
import com.tzh.sme.domain.repository.AuthRepository
import com.tzh.sme.domain.repository.SupplierRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class SupplierRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val authRepository: AuthRepository
) : SupplierRepository {

    private fun getSupplierCollection(shopId: String) = firestore
        .collection("shops")
        .document(shopId)
        .collection("suppliers")

    override fun getAllSuppliers(): Flow<List<SupplierModel>> =
        authRepository.currentUser.flatMapLatest { user ->
            val shopId = user?.shopId ?: user?.id
            if (shopId == null) {
                flowOf(emptyList())
            } else {
                callbackFlow {
                    val subscription =
                        getSupplierCollection(shopId).addSnapshotListener { snapshot, error ->
                            if (error != null) {
                                Log.w("SupplierRepository", "Listen failed: $error")
                                return@addSnapshotListener
                            }
                            if (snapshot != null) {
                                val suppliers = snapshot.toObjects(SupplierModel::class.java)
                                trySend(suppliers)
                            }
                        }
                    awaitClose { subscription.remove() }
                }
            }
        }

    override suspend fun getSupplierById(id: String): SupplierModel? {
        val currentUser = authRepository.currentUser.value
        val shopId = currentUser?.shopId ?: currentUser?.id ?: return null
        return try {
            getSupplierCollection(shopId).document(id).get().await()
                .toObject(SupplierModel::class.java)
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun addSupplier(supplier: SupplierModel): String {
        val currentUser = authRepository.currentUser.value
        val shopId = currentUser?.shopId ?: currentUser?.id ?: return ""
        return try {
            val docRef = getSupplierCollection(shopId).document()
            val finalSupplier = supplier.copy(id = docRef.id)
            docRef.set(finalSupplier).await()
            docRef.id
        } catch (e: Exception) {
            Log.e("SupplierRepository", "Failed to add supplier: ${e.message}")
            ""
        }
    }

    override suspend fun updateSupplier(supplier: SupplierModel) {
        val currentUser = authRepository.currentUser.value
        val shopId = currentUser?.shopId ?: currentUser?.id ?: return
        try {
            getSupplierCollection(shopId).document(supplier.id).set(supplier).await()
        } catch (e: Exception) {
            Log.e("SupplierRepository", "Failed to update supplier: ${e.message}")
        }
    }

    override suspend fun deleteSupplier(id: String) {
        val currentUser = authRepository.currentUser.value
        val shopId = currentUser?.shopId ?: currentUser?.id ?: return
        try {
            getSupplierCollection(shopId).document(id).delete().await()
        } catch (e: Exception) {
            Log.e("SupplierRepository", "Failed to delete supplier: ${e.message}")
        }
    }
}
