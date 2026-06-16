package com.tzh.sme.ui.stock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tzh.sme.data.model.CategoryModel
import com.tzh.sme.data.model.ProductModel
import com.tzh.sme.domain.usecase.stock.StockUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class StockViewModel @Inject constructor(
    private val stockUseCases: StockUseCases
) : ViewModel() {

    private val _uiState = MutableStateFlow(StockUiState())
    val uiState: StateFlow<StockUiState> = combine(
        _uiState,
        stockUseCases.getCategories(),
        stockUseCases.getProducts(),
        stockUseCases.getAuthState.currentUser,
        stockUseCases.getAuthState.currentUser.flatMapLatest { user ->
            user?.id?.let { stockUseCases.getSyncStatus(it) } ?: flowOf(false)
        }
    ) { array ->
        val state = array[0] as StockUiState
        val categories = array[1] as List<CategoryModel>
        val products = array[2] as List<ProductModel>
        val user = array[3] as com.tzh.sme.domain.repository.User?
        val isSyncing = array[4] as Boolean

        val filteredProducts =  products.filter { product ->
            val matchesCategory = state.selectedCategory.name == "All" || product.categoryId == state.selectedCategory.id
            val matchesSearch = state.searchQuery.isEmpty() ||
                    product.name.contains(state.searchQuery, ignoreCase = true) ||
                    product.barcode.contains(state.searchQuery)

            matchesCategory && matchesSearch
        }

        state.copy(
            products = filteredProducts,
            categories = categories,
            currentUser = user,
            isSyncing = isSyncing,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StockUiState(isLoading = true))

    private val _sideEffect = Channel<StockUiSideEffect>(Channel.BUFFERED)
    val sideEffect = _sideEffect.receiveAsFlow()

    init {
        sendIntent(StockUiIntent.SyncData)
    }

    fun sendIntent(intent: StockUiIntent) {
        when (intent) {
            is StockUiIntent.UpdateSearchQuery -> _uiState.update { it.copy(searchQuery = intent.query) }
            is StockUiIntent.ToggleScanner -> _uiState.update { it.copy(showScanner = intent.show) }
            is StockUiIntent.BarcodeScanned -> _uiState.update { it.copy(searchQuery = intent.barcode, showScanner = false) }
            is StockUiIntent.SelectCategory -> _uiState.update { it.copy(selectedCategory = intent.category) }
            StockUiIntent.SyncData -> syncData()
        }
    }

    private fun syncData() {
        viewModelScope.launch {
            stockUseCases.syncStock()
        }
    }
}
