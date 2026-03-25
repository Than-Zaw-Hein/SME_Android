package com.tzh.sme.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TransactionType {
    STOCK_IN, STOCK_OUT, SALE
}

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val transactionId: Long = 0,
    val type: TransactionType,
    val timestamp: Long = System.currentTimeMillis(),
    val totalAmount: Double = 0.0,
    val userId: String? = null // For future Auth integration
)

@Entity(tableName = "transaction_items")
data class TransactionItemEntity(
    @PrimaryKey(autoGenerate = true) val itemId: Long = 0,
    val transactionId: Long,
    val productId: Long,
    val quantity: Int,
    val priceAtTime: Double
)
