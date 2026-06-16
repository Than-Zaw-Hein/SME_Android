package com.tzh.sme.domain.usecase.history

import com.tzh.sme.data.model.TransactionItemModel
import com.tzh.sme.data.model.TransactionModel
import com.tzh.sme.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetTransactionByIdUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    operator fun invoke(id: String): Flow<Pair<TransactionModel, List<TransactionItemModel>>?> {
        return repository.getTransactionById(id)
    }
}
