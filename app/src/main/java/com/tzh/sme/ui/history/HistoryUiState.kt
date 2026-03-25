package com.tzh.sme.ui.history

import com.tzh.sme.data.local.entities.TransactionEntity

sealed interface HistoryUiState {
    object Loading : HistoryUiState
    data class Success(val transactions: List<TransactionEntity>) : HistoryUiState
    data class Error(val message: String) : HistoryUiState
}
