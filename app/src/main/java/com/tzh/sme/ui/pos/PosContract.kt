package com.tzh.sme.ui.pos

import com.tzh.sme.data.DefaultData.defaultCategory
import com.tzh.sme.data.model.CategoryModel
import com.tzh.sme.data.model.ProductModel
import com.tzh.sme.data.model.ShopModel
import com.tzh.sme.data.model.TransactionItemModel
import com.tzh.sme.data.model.TransactionModel
import com.tzh.sme.domain.model.CartItem
import com.tzh.sme.domain.repository.User

data class PosUiState(
    val selectedCategory: CategoryModel = defaultCategory,
    val searchQuery: String = "",
    val categories: List<CategoryModel> = listOf(defaultCategory),
    val products: List<ProductModel> = emptyList(),
    val filterProduct: List<ProductModel> = emptyList(),
    val cartItems: List<CartItem> = emptyList(),
    val discount: Double = 0.0,
    val isLoading: Boolean = false,
    val isCheckOutInProgress: Boolean = false,
    val isPrinting: Boolean = false,
    val printMessage: String = "",
    val printError: String = "",
    val errorMessage: String = "",
    val completedTransaction: Pair<TransactionModel, List<TransactionItemModel>>? = null,
    val currentUser: User? = null,
    val currentShop: ShopModel? = null,
    val isSyncing: Boolean = false
)

sealed interface PosUiIntent {
    data class SelectCategory(val category: CategoryModel) : PosUiIntent
    data class UpdateSearchQuery(val query: String) : PosUiIntent
    data class AddToCart(val product: ProductModel) : PosUiIntent
    data class RemoveFromCart(val product: ProductModel) : PosUiIntent
    data class BarcodeScanned(val barcode: String) : PosUiIntent
    data class UpdateDiscount(val discount: Double) : PosUiIntent
    object Checkout : PosUiIntent
    data class PrintReceipt(
        val transaction: TransactionModel,
        val items: List<TransactionItemModel>
    ) : PosUiIntent

    object DismissSuccessDialog : PosUiIntent
    object ClearPrintMessage : PosUiIntent
    object ClearErrorMessage : PosUiIntent
    object ResetCheckoutState : PosUiIntent
}

sealed interface PosUiSideEffect {
    data class ShowToast(val message: String) : PosUiSideEffect
    data class ShowError(val message: String) : PosUiSideEffect
    object NavigateToCheckout : PosUiSideEffect
    object NavigateBack : PosUiSideEffect
}
