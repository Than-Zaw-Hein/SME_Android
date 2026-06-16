package com.tzh.sme.domain.usecase.stock

import com.tzh.sme.data.model.TransactionItemModel
import com.tzh.sme.data.model.TransactionModel
import com.tzh.sme.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetProductTransactionsUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    operator fun invoke(productId: String): Flow<List<Pair<TransactionModel, TransactionItemModel>>> {
        return repository.getTransactionsByProduct(productId)
    }
}
