package com.tzh.sme.ui.stock

import com.tzh.sme.data.local.entities.ProductEntity

sealed interface StockUiState {
    object Loading : StockUiState
    data class Success(val products: List<ProductEntity>) : StockUiState
    data class Error(val message: String) : StockUiState
}
