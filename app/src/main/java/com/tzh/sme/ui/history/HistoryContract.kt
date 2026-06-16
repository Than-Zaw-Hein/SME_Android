package com.tzh.sme.ui.history

import com.tzh.sme.data.model.TransactionModel
import com.tzh.sme.data.model.TransactionType

data class HistoryUiState(
    val transactions: List<TransactionModel> = emptyList(),
    val filteredTransactions: List<TransactionModel> = emptyList(),
    val selectedTab: HistoryTab = HistoryTab.SALES,
    val selectedStockFilter: TransactionType? = null,
    val searchQuery: String = "",
    val startDate: Long? = null,
    val endDate: Long? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

enum class HistoryTab {
    SALES, STOCK
}

sealed interface HistoryUiIntent {
    data class SelectTab(val tab: HistoryTab) : HistoryUiIntent
    data class SelectStockFilter(val type: TransactionType?) : HistoryUiIntent
    data class UpdateSearchQuery(val query: String) : HistoryUiIntent
    data class SelectDateRange(val start: Long?, val end: Long?) : HistoryUiIntent
}

sealed interface HistoryUiSideEffect {
    data class ShowError(val message: String) : HistoryUiSideEffect
}
