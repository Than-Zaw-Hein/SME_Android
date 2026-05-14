package com.tzh.sme.ui.pos.check_out

import com.tzh.sme.data.model.CategoryModel
import com.tzh.sme.data.model.ProductModel
import com.tzh.sme.data.model.TransactionItemModel
import com.tzh.sme.data.model.TransactionModel

data class CheckOutUiState(
    val discount: Double = 0.0,
    val isCheckOutInProgress: Boolean = false,
    val isPrinting: Boolean = false,
    val printMessage: String = "",
    val printError: String = "",
    val errorMessage: String = "",
    val completedTransaction: Pair<TransactionModel, List<TransactionItemModel>>? = null
)

sealed class CheckOutEvent {
    data class OnAddToCart(val product: ProductModel) : CheckOutEvent()
    data class OnRemoveFromCart(val product: ProductModel) : CheckOutEvent()
    data class OnDiscountChange(val discount: Double) : CheckOutEvent()

    data object OnCheckOut : CheckOutEvent()
    data class OnPrint(val transaction: TransactionModel, val items: List<TransactionItemModel>) :
        CheckOutEvent()
}
