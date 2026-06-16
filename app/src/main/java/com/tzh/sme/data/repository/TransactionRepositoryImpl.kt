package com.tzh.sme.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.tzh.sme.data.model.TransactionModel
import com.tzh.sme.data.model.TransactionItemModel
import com.tzh.sme.data.model.TransactionType
import com.tzh.sme.domain.repository.AuthRepository
import com.tzh.sme.domain.repository.TransactionRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.ExperimentalCoroutinesApi

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class TransactionRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val authRepository: AuthRepository
) : TransactionRepository {

    private fun getTransactionsCollection(shopId: String) = firestore
        .collection("shops")
        .document(shopId)
        .collection("transactions")

    private fun getProductsCollection(shopId: String) = firestore
        .collection("shops")
        .document(shopId)
        .collection("products")

    override fun getAllTransactions(): Flow<List<TransactionModel>> =
        authRepository.currentUser.flatMapLatest { user ->
            val shopId = user?.shopId ?: user?.id
            if (shopId == null) {
                flowOf(emptyList())
            } else {
                callbackFlow {
                    val subscription = getTransactionsCollection(shopId)
                        .orderBy("transaction.timestamp", Query.Direction.DESCENDING)
                        .addSnapshotListener { snapshot, error ->
                            if (error != null) {
                                Log.w("TransactionRepo", "Listen failed: $error")
                                return@addSnapshotListener
                            }
                            if (snapshot != null) {
                                val transactions = snapshot.documents.mapNotNull { doc ->
                                    doc.get("transaction", TransactionModel::class.java)
                                }
                                trySend(transactions)
                            }
                        }
                    awaitClose { subscription.remove() }
                }
            }
        }

    override fun getTransactionById(transactionId: String): Flow<Pair<TransactionModel, List<TransactionItemModel>>?> =
        authRepository.currentUser.flatMapLatest { user ->
            val shopId = user?.shopId ?: user?.id
            if (shopId == null) {
                flowOf(null)
            } else {
                callbackFlow {
                    val subscription = getTransactionsCollection(shopId).document(transactionId)
                        .addSnapshotListener { snapshot, error ->
                            if (error != null) {
                                Log.w("TransactionRepo", "Listen failed: $error")
                                return@addSnapshotListener
                            }
                            if (snapshot != null && snapshot.exists()) {
                                val transaction =
                                    snapshot.get("transaction", TransactionModel::class.java)
                                val items = snapshot.get("items") as? List<Map<String, Any>>
                                val itemModels = items?.map { itemMap ->
                                    TransactionItemModel(
                                        itemId = itemMap["itemId"] as? String ?: "",
                                        transactionId = itemMap["transactionId"] as? String ?: "",
                                        productId = itemMap["productId"] as? String ?: "",
                                        productName = itemMap["productName"] as? String ?: "",
                                        quantity = (itemMap["quantity"] as? Long ?: 0L).toInt(),
                                        priceAtTime = (itemMap["priceAtTime"] as? Double ?: 0.0)
                                    )
                                } ?: emptyList()

                                if (transaction != null) {
                                    trySend(Pair(transaction, itemModels))
                                } else {
                                    trySend(null)
                                }
                            } else {
                                trySend(null)
                            }
                        }
                    awaitClose { subscription.remove() }
                }
            }
        }

    override fun getTransactionsByProduct(productId: String): Flow<List<Pair<TransactionModel, TransactionItemModel>>> =
        authRepository.currentUser.flatMapLatest { user ->
            val shopId = user?.shopId ?: user?.id
            if (shopId == null) {
                flowOf(emptyList())
            } else {
                callbackFlow {
                    // Optimized: Only check the last 50 transactions to find product history
                    val subscription = getTransactionsCollection(shopId)
                        .orderBy("transaction.timestamp", Query.Direction.DESCENDING)
                        .limit(50)
                        .addSnapshotListener { snapshot, error ->
                            if (error != null) {
                                trySend(emptyList())
                                return@addSnapshotListener
                            }
                            if (snapshot != null) {
                                val results =
                                    mutableListOf<Pair<TransactionModel, TransactionItemModel>>()
                                snapshot.documents.forEach { doc ->
                                    val transaction =
                                        doc.get("transaction", TransactionModel::class.java)
                                    val items = doc.get("items") as? List<Map<String, Any>>
                                    val matchingItemMap =
                                        items?.find { it["productId"] == productId }
                                    if (transaction != null && matchingItemMap != null) {
                                        val item = TransactionItemModel(
                                            itemId = matchingItemMap["itemId"] as? String ?: "",
                                            transactionId = matchingItemMap["transactionId"] as? String ?: "",
                                            productId = matchingItemMap["productId"] as? String ?: "",
                                            productName = matchingItemMap["productName"] as? String ?: "",
                                            quantity = (matchingItemMap["quantity"] as? Long ?: 0L).toInt(),
                                            priceAtTime = (matchingItemMap["priceAtTime"] as? Double ?: 0.0)
                                        )
                                        results.add(transaction to item)
                                    }
                                }
                                trySend(results)
                            }
                        }
                    awaitClose { subscription.remove() }
                }
            }
        }

    override suspend fun executeTransaction(
        transaction: TransactionModel,
        items: List<TransactionItemModel>
    ): Result<Pair<TransactionModel, List<TransactionItemModel>>> {
        val currentUser = authRepository.currentUser.value
        val shopId = currentUser?.shopId ?: currentUser?.id
            ?: return Result.failure(Exception("Shop not found"))
        return try {
            val result = firestore.runTransaction { firestoreTransaction ->
                    val productUpdates = items.map { item ->
                        val productRef = getProductsCollection(shopId).document(item.productId)
                        val productDoc = firestoreTransaction.get(productRef)
                        
                        if (!productDoc.exists()) {
                            throw Exception("Product '${item.productName}' no longer exists.")
                        }

                        val currentQuantity = productDoc.getLong("quantity") ?: 0L
                        val costAtTime = productDoc.getDouble("costPrice") ?: 0.0
                        
                        if (transaction.type == TransactionType.SALE && currentQuantity < item.quantity) {
                            throw Exception("Insufficient stock for '${item.productName}'. Available: $currentQuantity")
                        }

                        val newQuantity =
                            if (transaction.type == TransactionType.STOCK_IN) {
                                currentQuantity + item.quantity
                            } else {
                                currentQuantity - item.quantity
                            }
                        Triple(productRef, newQuantity, costAtTime)
                    }

                    val transRef = getTransactionsCollection(shopId).document()
                    val totalCost = productUpdates.sumOf { it.third * items.find { item -> item.productId == it.first.id }?.quantity!! }
                    
                    val finalTransaction = transaction.copy(
                        transactionId = transRef.id,
                        totalCost = totalCost
                    )
                    val itemsWithId = items.map { item ->
                        val costAtTime = productUpdates.find { it.first.id == item.productId }?.third ?: 0.0
                        item.copy(
                            transactionId = transRef.id,
                            itemId = transRef.collection("items").document().id,
                            costAtTime = costAtTime
                        )
                    }

                val transactionMap = hashMapOf(
                    "transaction" to finalTransaction,
                    "items" to itemsWithId
                )

                firestoreTransaction.set(transRef, transactionMap)
                productUpdates.forEach { (productRef, newQuantity, _) ->
                    firestoreTransaction.update(productRef, "quantity", newQuantity)
                }

                finalTransaction to itemsWithId
            }.await()
            Result.success(result)
        } catch (e: Exception) {
            Log.e("TransactionRepo", "Error executing transaction", e)
            Result.failure(e)
        }
    }
}
