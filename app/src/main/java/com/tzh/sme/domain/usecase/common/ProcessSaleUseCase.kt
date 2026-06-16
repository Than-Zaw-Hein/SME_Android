package com.tzh.sme.domain.usecase.common

import com.tzh.sme.data.model.TransactionModel
import com.tzh.sme.data.model.TransactionItemModel
import com.tzh.sme.data.model.TransactionType
import com.tzh.sme.domain.model.CartItem
import com.tzh.sme.domain.repository.TransactionRepository
import javax.inject.Inject

class ProcessSaleUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(
        cartItems: List<CartItem>,
        discount: Double = 0.0,
        sellerId: String? = null,
        sellerName: String? = null
    ): Result<Pair<TransactionModel, List<TransactionItemModel>>> {
        if (cartItems.isEmpty()) return Result.failure(Exception("Cart is empty"))

        return try {
            val totalAmount = cartItems.sumOf { it.totalPrice }
            val netAmount = totalAmount - discount
            val transaction = TransactionModel(
                type = TransactionType.SALE,
                totalAmount = totalAmount,
                discount = discount,
                netAmount = netAmount,
                userId = sellerId,
                userName = sellerName
            )

            val items = cartItems.map { cartItem ->
                TransactionItemModel(
                    transactionId = "", // Will be set in repository
                    productId = cartItem.product.id,
                    productName = cartItem.product.name,
                    quantity = cartItem.quantity,
                    priceAtTime = cartItem.product.sellPrice
                )
            }

            transactionRepository.executeTransaction(transaction, items)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
