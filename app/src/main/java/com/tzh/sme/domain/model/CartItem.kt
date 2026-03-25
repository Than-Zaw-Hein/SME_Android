package com.tzh.sme.domain.model

import com.tzh.sme.data.local.entities.ProductEntity

data class CartItem(
    val product: ProductEntity,
    val quantity: Int
) {
    val totalPrice: Double get() = product.price * quantity
}
