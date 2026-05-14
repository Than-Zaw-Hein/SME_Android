package com.tzh.sme.ui.pos

import com.tzh.sme.data.DefaultData.defaultCategory
import com.tzh.sme.data.model.CategoryModel
import com.tzh.sme.data.model.ProductModel
import com.tzh.sme.data.model.TransactionItemModel
import com.tzh.sme.data.model.TransactionModel
import com.tzh.sme.domain.model.CartItem
import com.tzh.sme.domain.repository.User

data class PosUiState(
    val selectedCategory: CategoryModel = defaultCategory ,
    val searchQuery: String = "",
)

sealed class PosEvent{

    data class OnSelectedCategory(val category: CategoryModel): PosEvent()
    data class OnSearchQueryChange(val query: String): PosEvent()
    data class OnAddToCart(val product: ProductModel): PosEvent()
    data class OnRemoveFromCart(val product: ProductModel): PosEvent()
    data class OnBarcodeScanned(val barcode: String): PosEvent()

}
