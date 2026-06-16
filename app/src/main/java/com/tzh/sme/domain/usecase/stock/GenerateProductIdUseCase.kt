package com.tzh.sme.domain.usecase.stock

import com.tzh.sme.domain.repository.ProductRepository
import javax.inject.Inject

class GenerateProductIdUseCase @Inject constructor(
    private val repository: ProductRepository
) {
    suspend operator fun invoke(): String {
        return repository.generateNextId()
    }
}
