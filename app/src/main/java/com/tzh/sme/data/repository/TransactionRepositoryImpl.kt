package com.tzh.sme.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.tzh.sme.data.model.TransactionModel
import com.tzh.sme.data.model.TransactionItemModel
import com.tzh.sme.domain.repository.TransactionRepository
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
class TransactionRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : TransactionRepository {

    private fun getAuthStateFlow(): Flow<String?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser?.uid)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    private fun getTransactionsCollection(userId: String) = firestore
        .collection("users")
        .document(userId)
        .collection("transactions")

    private fun getProductsCollection(userId: String) = firestore
        .collection("users")
        .document(userId)
        .collection("products")

    override fun getAllTransactions(): Flow<List<TransactionModel>> = getAuthStateFlow().flatMapLatest { uid ->
        if (uid == null) {
            flowOf(emptyList())
        } else {
            callbackFlow {
                val subscription = getTransactionsCollection(uid)
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

    override fun getTransactionById(transactionId: String): Flow<Pair<TransactionModel, List<TransactionItemModel>>?> = getAuthStateFlow().flatMapLatest { uid ->
        if (uid == null) {
            flowOf(null)
        } else {
            callbackFlow {
                val subscription = getTransactionsCollection(uid).document(transactionId).addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w("TransactionRepo", "Listen failed: $error")
                        return@addSnapshotListener
                    }
                    if (snapshot != null && snapshot.exists()) {
                        val transaction = snapshot.get("transaction", TransactionModel::class.java)
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

    override fun getTransactionsByProduct(productId: String): Flow<List<Pair<TransactionModel, TransactionItemModel>>> = getAuthStateFlow().flatMapLatest { uid ->
        if (uid == null) {
            flowOf(emptyList())
        } else {
            callbackFlow {
                // Optimized: Only check the last 50 transactions to find product history
                // Note: For a production app with high volume, consider adding a 'productIds' array 
                // to the transaction document to query directly using whereArrayContains.
                val subscription = getTransactionsCollection(uid)
                    .orderBy("transaction.timestamp", Query.Direction.DESCENDING)
                    .limit(3)
                    .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val results = mutableListOf<Pair<TransactionModel, TransactionItemModel>>()
                        snapshot.documents.forEach { doc ->
                            val transaction = doc.get("transaction", TransactionModel::class.java)
                            val items = doc.get("items") as? List<Map<String, Any>>
                            val matchingItemMap = items?.find { it["productId"] == productId }
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
        val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))
        return try {
            val result = firestore.runTransaction { firestoreTransaction ->
                val productUpdates = items.map { item ->
                    val productRef = getProductsCollection(uid).document(item.productId)
                    val productDoc = firestoreTransaction.get(productRef)
                    val currentQuantity = productDoc.getLong("quantity") ?: 0L
                    val newQuantity = if (transaction.type == com.tzh.sme.data.model.TransactionType.STOCK_IN) {
                        currentQuantity + item.quantity
                    } else {
                        currentQuantity - item.quantity
                    }
                    productRef to newQuantity
                }

                val transRef = getTransactionsCollection(uid).document()
                val finalTransaction = transaction.copy(transactionId = transRef.id)
                val itemsWithId = items.map { it.copy(transactionId = transRef.id, itemId = transRef.collection("items").document().id) }

                val transactionMap = hashMapOf(
                    "transaction" to finalTransaction,
                    "items" to itemsWithId
                )

                firestoreTransaction.set(transRef, transactionMap)
                productUpdates.forEach { (productRef, newQuantity) ->
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
