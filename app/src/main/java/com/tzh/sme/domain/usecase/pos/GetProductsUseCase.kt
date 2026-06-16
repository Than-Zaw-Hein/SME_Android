package com.tzh.sme.domain.usecase.pos

import com.tzh.sme.data.model.ProductModel
import com.tzh.sme.domain.repository.StockRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetProductsUseCase @Inject constructor(
    private val repository: StockRepository
) {
    operator fun invoke(): Flow<List<ProductModel>> {
        return repository.getAllProducts()
    }
}
