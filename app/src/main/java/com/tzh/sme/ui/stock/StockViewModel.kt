package com.tzh.sme.ui.stock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tzh.sme.domain.repository.StockRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StockViewModel @Inject constructor(
    private val repository: StockRepository,
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _showScanner = MutableStateFlow(false)

    val uiState: StateFlow<StockUiState> = combine(
        repository.getAllProducts(),
        _searchQuery,
        _showScanner
    ) { products, query, showScanner ->
        val filteredProducts = if (query.isEmpty()) {
            products
        } else {
            products.filter { 
                it.name.contains(query, ignoreCase = true) || 
                it.barcode.contains(query) 
            }
        }
        StockUiState(
            products = filteredProducts,
            searchQuery = query,
            showScanner = showScanner,
            isLoading = false
        )
    }.catch { e ->
        emit(StockUiState(error = e.message ?: "An unknown error occurred", isLoading = false))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = StockUiState(isLoading = true)
    )

    init {
        syncData()
    }

    fun onStockEvent(event: StockEvent) {
        when (event) {
            is StockEvent.OnSearchQueryChange -> {
                _searchQuery.value = event.query
            }
            is StockEvent.OnToggleScanner -> {
                _showScanner.value = event.show
            }
            is StockEvent.OnBarcodeScanned -> {
                _searchQuery.value = event.barcode
                _showScanner.value = false
            }
            StockEvent.OnSyncData -> {
                syncData()
            }
        }
    }

    private fun syncData() {
        viewModelScope.launch {
            repository.syncFromFirestore()
        }
    }
}
