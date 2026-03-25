package com.tzh.sme.ui.stock

data class ProductDetailUiState(
    val id: Long = 0,
    val barcode: String = "",
    val name: String = "",
    val price: String = "",
    val quantity: String = "0",
    val category: String = "General",
    val imagePaths: List<String> = emptyList(),
    val showScanner: Boolean = false,
    val isEditMode: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val navigateBack: Boolean = false,
    val categories: List<String> = listOf("General", "Food", "Drink", "Snack")
)

sealed interface ProductDetailEvent {
    data class BarcodeChanged(val barcode: String) : ProductDetailEvent
    data class NameChanged(val name: String) : ProductDetailEvent
    data class PriceChanged(val price: String) : ProductDetailEvent
    data class QuantityChanged(val quantity: String) : ProductDetailEvent
    data class CategoryChanged(val category: String) : ProductDetailEvent
    data class ToggleScanner(val show: Boolean) : ProductDetailEvent
    data class AddImages(val paths: List<String>) : ProductDetailEvent
    data class RemoveImage(val path: String) : ProductDetailEvent
    object SaveProduct : ProductDetailEvent
    object DeleteProduct : ProductDetailEvent
    object NavigatedBack : ProductDetailEvent
}
