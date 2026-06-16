package com.tzh.sme.domain.usecase.stock

import com.tzh.sme.domain.repository.FileRepository
import javax.inject.Inject

class DeleteProductImagesFolderUseCase @Inject constructor(
    private val repository: FileRepository
) {
    suspend operator fun invoke(productId: String, productName: String, shopName: String): Result<Unit> {
        return repository.deleteProductImagesFolder(productId, productName, shopName)
    }
}
