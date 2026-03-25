package com.tzh.sme.ui.stock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tzh.sme.data.local.entities.ProductEntity
import com.tzh.sme.domain.repository.StockRepository
import com.tzh.sme.domain.usecase.AddProductToStockUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StockViewModel @Inject constructor(
    private val repository: StockRepository,
) : ViewModel() {

    val uiState: StateFlow<StockUiState> = repository.getAllProducts()
        .map { StockUiState.Success(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = StockUiState.Loading
        )
}
