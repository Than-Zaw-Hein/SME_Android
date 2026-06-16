package com.tzh.sme.domain.repository

import com.tzh.sme.data.model.ParkedSaleModel
import kotlinx.coroutines.flow.Flow

interface ParkedSaleRepository {
    fun getParkedSales(): Flow<List<ParkedSaleModel>>
    suspend fun parkSale(sale: ParkedSaleModel): Result<Unit>
    suspend fun resumeSale(saleId: String): Result<ParkedSaleModel?>
    suspend fun deleteParkedSale(saleId: String): Result<Unit>
}
