package com.tzh.sme.domain.model

import com.tzh.sme.data.model.ProductModel

data class CartItem(
    val product: ProductModel,
    val quantity: Int
) {
    val totalPrice: Double get() = product.price * quantity
}
