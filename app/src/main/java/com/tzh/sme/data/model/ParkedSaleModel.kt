package com.tzh.sme.data.model

import com.tzh.sme.domain.model.CartItem

data class ParkedSaleModel(
    val id: String = "",
    val customerName: String? = null,
    val items: List<CartItem> = emptyList(),
    val totalAmount: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis()
)
