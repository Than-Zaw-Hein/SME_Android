package com.tzh.sme.domain.usecase.stock

import com.tzh.sme.domain.repository.StockRepository
import javax.inject.Inject

class SyncStockUseCase @Inject constructor(
    private val repository: StockRepository
) {
    suspend operator fun invoke() {
        repository.syncFromFirestore()
    }
}
