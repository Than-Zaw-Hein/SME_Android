package com.tzh.sme.ui.pos

import com.tzh.sme.data.local.entities.ProductEntity
import com.tzh.sme.domain.model.CartItem
import com.tzh.sme.domain.repository.User

sealed interface PosUiState {
    object Loading : PosUiState
    data class Success(
        val products: List<ProductEntity> = emptyList(),
        val categories: List<String> = emptyList(),
        val selectedCategory: String = "All",
        val cart: List<CartItem> = emptyList(),
        val searchQuery: String = "",
        val isCheckoutInProgress: Boolean = false,
        val user: User?,
    ) : PosUiState
    data class Error(val message: String) : PosUiState
}
