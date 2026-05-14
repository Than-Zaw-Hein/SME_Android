package com.tzh.sme.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.tzh.sme.data.model.SupplierModel
import com.tzh.sme.domain.repository.SupplierRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class SupplierRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : SupplierRepository {

    private fun getAuthStateFlow(): Flow<String?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser?.uid)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    private fun getSupplierCollection(userId: String) = firestore
        .collection("users")
        .document(userId)
        .collection("suppliers")

    override fun getAllSuppliers(): Flow<List<SupplierModel>> = getAuthStateFlow().flatMapLatest { uid ->
        if (uid == null) {
            flowOf(emptyList())
        } else {
            callbackFlow {
                val subscription = getSupplierCollection(uid).addSnapshotListener { snapshot, error ->
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
        val uid = auth.currentUser?.uid ?: return null
        return try {
            getSupplierCollection(uid).document(id).get().await().toObject(SupplierModel::class.java)
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun addSupplier(supplier: SupplierModel): String {
        val uid = auth.currentUser?.uid ?: return ""
        return try {
            val docRef = getSupplierCollection(uid).document()
            val finalSupplier = supplier.copy(id = docRef.id)
            docRef.set(finalSupplier).await()
            docRef.id
        } catch (e: Exception) {
            Log.e("SupplierRepository", "Failed to add supplier: ${e.message}")
            ""
        }
    }

    override suspend fun updateSupplier(supplier: SupplierModel) {
        val uid = auth.currentUser?.uid ?: return
        try {
            getSupplierCollection(uid).document(supplier.id).set(supplier).await()
        } catch (e: Exception) {
            Log.e("SupplierRepository", "Failed to update supplier: ${e.message}")
        }
    }

    override suspend fun deleteSupplier(id: String) {
        val uid = auth.currentUser?.uid ?: return
        try {
            getSupplierCollection(uid).document(id).delete().await()
        } catch (e: Exception) {
            Log.e("SupplierRepository", "Failed to delete supplier: ${e.message}")
        }
    }
}
