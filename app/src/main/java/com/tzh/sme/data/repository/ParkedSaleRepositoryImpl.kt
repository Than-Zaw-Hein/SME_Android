package com.tzh.sme.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.tzh.sme.data.model.ParkedSaleModel
import com.tzh.sme.domain.repository.AuthRepository
import com.tzh.sme.domain.repository.ParkedSaleRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.ExperimentalCoroutinesApi

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class ParkedSaleRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val authRepository: AuthRepository
) : ParkedSaleRepository {

    private fun getParkedSalesCollection(shopId: String) = firestore
        .collection("shops")
        .document(shopId)
        .collection("parked_sales")

    override fun getParkedSales(): Flow<List<ParkedSaleModel>> =
        authRepository.currentUser.flatMapLatest { user ->
            val shopId = user?.shopId ?: user?.id
            if (shopId == null) {
                flowOf(emptyList())
            } else {
                callbackFlow {
                    val subscription = getParkedSalesCollection(shopId)
                        .orderBy("timestamp")
                        .addSnapshotListener { snapshot, error ->
                            if (error != null) {
                                close(error)
                                return@addSnapshotListener
                            }
                            if (snapshot != null) {
                                val sales = snapshot.toObjects(ParkedSaleModel::class.java)
                                trySend(sales)
                            }
                        }
                    awaitClose { subscription.remove() }
                }
            }
        }

    override suspend fun parkSale(sale: ParkedSaleModel): Result<Unit> {
        val currentUser = authRepository.currentUser.value
        val shopId = currentUser?.shopId ?: currentUser?.id ?: return Result.failure(Exception("Shop not found"))
        
        return try {
            val collection = getParkedSalesCollection(shopId)
            val docRef = if (sale.id.isEmpty()) collection.document() else collection.document(sale.id)
            val finalSale = if (sale.id.isEmpty()) sale.copy(id = docRef.id) else sale
            docRef.set(finalSale).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun resumeSale(saleId: String): Result<ParkedSaleModel?> {
        val currentUser = authRepository.currentUser.value
        val shopId = currentUser?.shopId ?: currentUser?.id ?: return Result.failure(Exception("Shop not found"))

        return try {
            val snapshot = getParkedSalesCollection(shopId).document(saleId).get().await()
            val sale = snapshot.toObject(ParkedSaleModel::class.java)
            Result.success(sale)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteParkedSale(saleId: String): Result<Unit> {
        val currentUser = authRepository.currentUser.value
        val shopId = currentUser?.shopId ?: currentUser?.id ?: return Result.failure(Exception("Shop not found"))

        return try {
            getParkedSalesCollection(shopId).document(saleId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
