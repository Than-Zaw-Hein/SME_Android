package com.tzh.sme.domain.model

import com.tzh.sme.data.model.ProductModel

data class CartItem(
    val product: ProductModel = ProductModel(),
    val quantity: Int = 0
) {
    val totalPrice: Double get() = product.sellPrice * quantity
}
