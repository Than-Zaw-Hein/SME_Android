package com.tzh.sme.domain.usecase.stock

import com.tzh.sme.domain.repository.FileRepository
import javax.inject.Inject

class DeleteProductImageUseCase @Inject constructor(
    private val repository: FileRepository
) {
    suspend operator fun invoke(path: String): Result<Unit> {
        return repository.deleteProductImage(path)
    }
}
