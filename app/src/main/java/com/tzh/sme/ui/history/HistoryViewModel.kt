package com.tzh.sme.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tzh.sme.data.model.TransactionType
import com.tzh.sme.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(HistoryTab.SALES)
    private val _selectedStockFilter = MutableStateFlow<TransactionType?>(null)
    private val _searchQuery = MutableStateFlow("")
    private val _dateRange = MutableStateFlow<Pair<Long?, Long?>>(null to null)

    val uiState: StateFlow<HistoryUiState> = combine(
        transactionRepository.getAllTransactions(),
        _selectedTab,
        _selectedStockFilter,
        _searchQuery,
        _dateRange
    ) { transactions, tab, stockFilter, query, range ->
        // Adjust end date to the end of the day (23:59:59.999)
        val adjustedRange = if (range.first != null && range.second != null) {
            val endCal = Calendar.getInstance().apply {
                timeInMillis = range.second!!
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }
            range.first!! to endCal.timeInMillis
        } else range

        val filtered = transactions.filter { tx ->
            val isDateInRange = if (adjustedRange.first != null && adjustedRange.second != null) {
                tx.timestamp >= adjustedRange.first!! && tx.timestamp <= adjustedRange.second!!
            } else true

            when (tab) {
                HistoryTab.SALES -> {
                    tx.type == TransactionType.SALE &&
                            (query.isEmpty() || tx.transactionId.contains(query, ignoreCase = true)) &&
                            isDateInRange
                }
                HistoryTab.STOCK -> {
                    val isStockType = tx.type == TransactionType.STOCK_IN || tx.type == TransactionType.STOCK_OUT
                    val matchesStockFilter = stockFilter == null || tx.type == stockFilter
                    isStockType && matchesStockFilter && isDateInRange
                }
            }
        }

        HistoryUiState(
            transactions = transactions,
            filteredTransactions = filtered,
            selectedTab = tab,
            selectedStockFilter = stockFilter,
            searchQuery = query,
            startDate = range.first,
            endDate = range.second,
            isLoading = false
        )
    }.catch { e ->
        emit(HistoryUiState(error = e.message ?: "An unknown error occurred", isLoading = false))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HistoryUiState(isLoading = true)
    )

    fun onHistoryEvent(event: HistoryEvent) {
        when (event) {
            is HistoryEvent.OnTabSelected -> _selectedTab.value = event.tab
            is HistoryEvent.OnStockFilterSelected -> _selectedStockFilter.value = event.type
            is HistoryEvent.OnSearchQueryChanged -> _searchQuery.value = event.query
            is HistoryEvent.OnDateRangeSelected -> _dateRange.value = event.start to event.end
        }
    }
}
