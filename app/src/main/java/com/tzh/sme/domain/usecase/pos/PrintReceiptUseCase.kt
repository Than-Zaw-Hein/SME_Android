package com.tzh.sme.domain.usecase.pos

import com.tzh.sme.data.model.ShopModel
import com.tzh.sme.data.model.TransactionItemModel
import com.tzh.sme.data.model.TransactionModel
import com.tzh.sme.domain.repository.PrinterRepository
import com.tzh.sme.domain.repository.User
import javax.inject.Inject

class PrintReceiptUseCase @Inject constructor(
    private val printerRepository: PrinterRepository
) {
    suspend operator fun invoke(
        transaction: TransactionModel,
        items: List<TransactionItemModel>,
        shop: ShopModel?,
        user: User?
    ): Result<Unit> {
        return printerRepository.printReceipt(transaction, items, shop, user)
    }
}
