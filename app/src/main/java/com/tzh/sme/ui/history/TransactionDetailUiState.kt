package com.tzh.sme.ui.history

import com.tzh.sme.data.model.TransactionItemModel
import com.tzh.sme.data.model.TransactionModel

data class TransactionDetailUiState(
    val transaction: TransactionModel = TransactionModel(),
    val items: List<TransactionItemModel> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed class TransactionDetailEvent {
    data object OnNavigateBack : TransactionDetailEvent()
}
