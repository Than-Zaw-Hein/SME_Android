package com.tzh.sme.domain.usecase

import com.tzh.sme.data.local.entities.TransactionEntity
import com.tzh.sme.data.local.entities.TransactionItemEntity
import com.tzh.sme.data.local.entities.TransactionType
import com.tzh.sme.domain.model.CartItem
import com.tzh.sme.domain.repository.StockRepository
import javax.inject.Inject

class ProcessSaleUseCase @Inject constructor(
    private val repository: StockRepository
) {
    suspend operator fun invoke(cartItems: List<CartItem>): Result<Unit> {
        if (cartItems.isEmpty()) return Result.failure(Exception("Cart is empty"))

        return try {
            val totalAmount = cartItems.sumOf { it.totalPrice }
            val transaction = TransactionEntity(
                type = TransactionType.SALE,
                totalAmount = totalAmount
            )

            val items = cartItems.map { cartItem ->
                TransactionItemEntity(
                    transactionId = 0, // Will be set in repository
                    productId = cartItem.product.id,
                    quantity = cartItem.quantity,
                    priceAtTime = cartItem.product.price
                )
            }

            repository.executeTransaction(transaction, items)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
