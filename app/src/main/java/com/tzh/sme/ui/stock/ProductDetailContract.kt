package com.tzh.sme.ui.stock

import com.tzh.sme.data.DefaultData.defaultCategory
import com.tzh.sme.data.model.*

data class ProductDetailUiState(
    val id: String = "",
    val name: String = "",
    val barcode: String = "",
    val sellPrice: String = "",
    val costPrice: String = "",
    val editableQty: String = "0",
    val currentQty: Int = 0,
    val minStockLevel: String = "0",
    val transactionType: TransactionType = TransactionType.STOCK_IN,
    val category: CategoryModel = defaultCategory,
    val description: String = "",
    val suppliers: List<SupplierModel> = emptyList(),
    val selectedSupplier: SupplierModel? = null,
    val imagePaths: List<String> = emptyList(),
    val categories: List<CategoryModel> = emptyList(),
    val priceHistory: List<Pair<TransactionModel, TransactionItemModel>> = emptyList(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isDeleting: Boolean = false,
    val isEditMode: Boolean = false,
    val showScanner: Boolean = false,
    val error: String? = null
)

sealed interface ProductDetailUiIntent {
    data class NameChanged(val name: String) : ProductDetailUiIntent
    data class BarcodeChanged(val barcode: String) : ProductDetailUiIntent
    data class PriceChanged(val price: String) : ProductDetailUiIntent
    data class CostPriceChanged(val costPrice: String) : ProductDetailUiIntent
    data class QuantityChanged(val quantity: String) : ProductDetailUiIntent
    data class MinStockChanged(val minStock: String) : ProductDetailUiIntent
    data class TransactionTypeChanged(val type: TransactionType) : ProductDetailUiIntent
    data class CategoryChanged(val category: CategoryModel) : ProductDetailUiIntent
    data class DescriptionChanged(val description: String) : ProductDetailUiIntent
    data class SupplierChanged(val supplier: SupplierModel?) : ProductDetailUiIntent
    data class AddImages(val uris: List<String>) : ProductDetailUiIntent
    data class RemoveImage(val path: String) : ProductDetailUiIntent
    data class ToggleScanner(val show: Boolean) : ProductDetailUiIntent
    object SaveProduct : ProductDetailUiIntent
    object DeleteProduct : ProductDetailUiIntent
    data class AddCategory(val category: CategoryModel) : ProductDetailUiIntent
    data class UpdateCategory(val category: CategoryModel) : ProductDetailUiIntent
    data class DeleteCategory(val category: CategoryModel) : ProductDetailUiIntent
    data class AddSupplier(val supplier: SupplierModel) : ProductDetailUiIntent
    object ClearError : ProductDetailUiIntent
}

sealed interface ProductDetailUiSideEffect {
    object NavigateBack : ProductDetailUiSideEffect
    data class ShowError(val message: String) : ProductDetailUiSideEffect
    data class ShowToast(val message: String) : ProductDetailUiSideEffect
}
