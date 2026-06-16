package com.tzh.sme.domain.usecase.history

import javax.inject.Inject

data class HistoryUseCases @Inject constructor(
    val getTransactions: GetTransactionsUseCase,
    val getTransactionById: GetTransactionByIdUseCase,
    val filterTransactions: FilterTransactionsUseCase
)
