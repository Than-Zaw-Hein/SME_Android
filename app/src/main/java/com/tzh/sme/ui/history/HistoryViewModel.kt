package com.tzh.sme.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tzh.sme.data.model.TransactionType
import com.tzh.sme.domain.usecase.history.HistoryUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val historyUseCases: HistoryUseCases
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(HistoryTab.SALES)
    private val _selectedStockFilter = MutableStateFlow<TransactionType?>(null)
    private val _searchQuery = MutableStateFlow("")
    private val _dateRange = MutableStateFlow<Pair<Long?, Long?>>(null to null)

    val uiState: StateFlow<HistoryUiState> = combine(
        historyUseCases.getTransactions(),
        _selectedTab,
        _selectedStockFilter,
        _searchQuery,
        _dateRange
    ) { transactions, tab, stockFilter, query, range ->
        val filtered = historyUseCases.filterTransactions(
            transactions = transactions,
            tab = tab,
            stockFilter = stockFilter,
            query = query,
            startDate = range.first,
            endDate = range.second
        )

        HistoryUiState(
            transactions = transactions, filteredTransactions = filtered,
            selectedTab = tab, selectedStockFilter = stockFilter,
            searchQuery = query, startDate = range.first, endDate = range.second,
            isLoading = false
        )
    }.catch { e ->
        emit(HistoryUiState(error = e.message ?: "An unknown error occurred", isLoading = false))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HistoryUiState(isLoading = true))

    fun sendIntent(intent: HistoryUiIntent) {
        when (intent) {
            is HistoryUiIntent.SelectTab -> _selectedTab.value = intent.tab
            is HistoryUiIntent.SelectStockFilter -> _selectedStockFilter.value = intent.type
            is HistoryUiIntent.UpdateSearchQuery -> _searchQuery.value = intent.query
            is HistoryUiIntent.SelectDateRange -> _dateRange.value = intent.start to intent.end
        }
    }
}
