package com.tzh.sme.data.model

enum class TransactionType {
    STOCK_IN, STOCK_OUT, SALE
}

data class TransactionModel(
    val transactionId: String = "",
    val type: TransactionType = TransactionType.SALE,
    val timestamp: Long = System.currentTimeMillis(),
    val totalAmount: Double = 0.0,
    val totalCost: Double = 0.0,
    val discount: Double = 0.0,
    val netAmount: Double = 0.0,
    val supplierId: String? = null,
    val supplierName: String? = null,
    val userId: String? = null,
    val userName: String? = null
)

data class TransactionItemModel(
    val itemId: String = "",
    val transactionId: String = "",
    val productId: String = "",
    val productName: String = "",
    val quantity: Int = 0,
    val priceAtTime: Double = 0.0,
    val costAtTime: Double = 0.0
)
