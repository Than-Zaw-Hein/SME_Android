package com.tzh.sme.ui.stock

import com.tzh.sme.data.model.ProductModel

data class StockUiState(
    val products: List<ProductModel> = emptyList(),
    val searchQuery: String = "",
    val showScanner: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed class StockEvent {
    data class OnSearchQueryChange(val query: String) : StockEvent()
    data class OnToggleScanner(val show: Boolean) : StockEvent()
    data class OnBarcodeScanned(val barcode: String) : StockEvent()
    data object OnSyncData : StockEvent()
}
