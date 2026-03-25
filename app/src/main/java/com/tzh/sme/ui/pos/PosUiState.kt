package com.tzh.sme.ui.pos

import com.tzh.sme.data.local.entities.ProductEntity
import com.tzh.sme.domain.model.CartItem

sealed interface PosUiState {
    object Loading : PosUiState
    data class Success(
        val products: List<ProductEntity> = emptyList(),
        val cart: List<CartItem> = emptyList(),
        val searchQuery: String = "",
        val isCheckoutInProgress: Boolean = false
    ) : PosUiState
    data class Error(val message: String) : PosUiState
}
