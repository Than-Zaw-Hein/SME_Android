package com.tzh.sme.domain.repository

import com.tzh.sme.data.model.TransactionModel
import com.tzh.sme.data.model.TransactionItemModel
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun getAllTransactions(): Flow<List<TransactionModel>>
    fun getTransactionById(transactionId: String): Flow<Pair<TransactionModel, List<TransactionItemModel>>?>
    fun getTransactionsByProduct(productId: String): Flow<List<Pair<TransactionModel, TransactionItemModel>>>
    suspend fun executeTransaction(transaction: TransactionModel, items: List<TransactionItemModel>): Result<Pair<TransactionModel, List<TransactionItemModel>>>
}
