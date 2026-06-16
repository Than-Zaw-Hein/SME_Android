package com.tzh.sme.domain.repository

import com.tzh.sme.data.model.ShopModel
import com.tzh.sme.data.model.TransactionItemModel
import com.tzh.sme.data.model.TransactionModel

interface PrinterRepository {
    suspend fun printReceipt(
        transaction: TransactionModel,
        items: List<TransactionItemModel>,
        shop: ShopModel?,
        user: User?
    ): Result<Unit>
}
