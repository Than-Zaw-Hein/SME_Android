package com.tzh.sme.domain.usecase.history

import com.tzh.sme.data.model.TransactionModel
import com.tzh.sme.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetTransactionsUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    operator fun invoke(): Flow<List<TransactionModel>> {
        return repository.getAllTransactions()
    }
}
