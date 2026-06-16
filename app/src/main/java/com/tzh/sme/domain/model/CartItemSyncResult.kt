package com.tzh.sme.domain.model

sealed class CartItemSyncResult {
    data class ProductRemoved(val productName: String) : CartItemSyncResult()
    data class QuantityCapped(val productName: String, val newQuantity: Int) : CartItemSyncResult()
}
