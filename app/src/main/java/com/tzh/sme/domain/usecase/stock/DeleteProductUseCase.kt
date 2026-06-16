package com.tzh.sme.domain.usecase.stock

import com.tzh.sme.data.model.ProductModel
import com.tzh.sme.domain.repository.ProductRepository
import javax.inject.Inject

class DeleteProductUseCase @Inject constructor(
    private val repository: ProductRepository
) {
    suspend operator fun invoke(product: ProductModel): Result<Unit> {
        return try {
            repository.deleteProduct(product)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
