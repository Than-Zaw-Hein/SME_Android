package com.tzh.sme.ui.stock

import com.tzh.sme.data.DefaultData.defaultCategory
import com.tzh.sme.data.model.CategoryModel
import com.tzh.sme.data.model.ProductModel
import com.tzh.sme.domain.repository.User

data class StockUiState(
    val products: List<ProductModel> = emptyList(),
    val categories: List<CategoryModel> = emptyList(),
    val selectedCategory: CategoryModel = defaultCategory,
    val searchQuery: String = "",
    val showScanner: Boolean = false,
    val isLoading: Boolean = false,
    val isSyncing: Boolean = false,
    val currentUser: User? = null,
    val error: String? = null
)

sealed interface StockUiIntent {
    data class UpdateSearchQuery(val query: String) : StockUiIntent
    data class ToggleScanner(val show: Boolean) : StockUiIntent
    data class BarcodeScanned(val barcode: String) : StockUiIntent
    data class SelectCategory(val category: CategoryModel) : StockUiIntent
    object SyncData : StockUiIntent
}

sealed interface StockUiSideEffect {
    data class ShowError(val message: String) : StockUiSideEffect
}
