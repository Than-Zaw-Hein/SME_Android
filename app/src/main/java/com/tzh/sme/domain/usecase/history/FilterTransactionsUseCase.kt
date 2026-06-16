package com.tzh.sme.domain.usecase.history

import com.tzh.sme.data.model.TransactionModel
import com.tzh.sme.data.model.TransactionType
import com.tzh.sme.ui.history.HistoryTab
import java.util.Calendar
import javax.inject.Inject

class FilterTransactionsUseCase @Inject constructor() {
    operator fun invoke(
        transactions: List<TransactionModel>,
        tab: HistoryTab,
        stockFilter: TransactionType?,
        query: String,
        startDate: Long?,
        endDate: Long?
    ): List<TransactionModel> {
        val adjustedRange = if (startDate != null && endDate != null) {
            val endCal = Calendar.getInstance().apply {
                timeInMillis = endDate
                set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999)
            }
            startDate to endCal.timeInMillis
        } else startDate to endDate

        return transactions.filter { tx ->
            val isDateInRange = if (adjustedRange.first != null && adjustedRange.second != null) {
                tx.timestamp >= adjustedRange.first!! && tx.timestamp <= adjustedRange.second!!
            } else true

            when (tab) {
                HistoryTab.SALES -> tx.type == TransactionType.SALE && (query.isEmpty() || tx.transactionId.contains(query, ignoreCase = true)) && isDateInRange
                HistoryTab.STOCK -> {
                    val isStockType = tx.type == TransactionType.STOCK_IN || tx.type == TransactionType.STOCK_OUT
                    val matchesStockFilter = stockFilter == null || tx.type == stockFilter
                    isStockType && matchesStockFilter && isDateInRange
                }
            }
        }
    }
}
