package com.tzh.sme.domain.usecase.stock

import com.tzh.sme.data.model.TransactionModel
import com.tzh.sme.data.model.TransactionItemModel
import com.tzh.sme.domain.repository.StockRepository
import javax.inject.Inject

class ExecuteTransactionUseCase @Inject constructor(
    private val repository: StockRepository
) {
    suspend operator fun invoke(
        transaction: TransactionModel,
        items: List<TransactionItemModel>
    ): Result<Pair<TransactionModel, List<TransactionItemModel>>> {
        return repository.executeTransaction(transaction, items)
    }
}
