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

sealed class HistoryEvent {
    data class OnTabSelected(val tab: HistoryTab) : HistoryEvent()
    data class OnStockFilterSelected(val type: TransactionType?) : HistoryEvent()
    data class OnSearchQueryChanged(val query: String) : HistoryEvent()
    data class OnDateRangeSelected(val start: Long?, val end: Long?) : HistoryEvent()
}
