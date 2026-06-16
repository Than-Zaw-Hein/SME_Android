package com.tzh.sme.domain.usecase.stock

import android.net.Uri
import com.tzh.sme.domain.repository.FileRepository
import javax.inject.Inject

class UploadProductImageUseCase @Inject constructor(
    private val repository: FileRepository
) {
    suspend operator fun invoke(
        uri: Uri,
        productId: String,
        productName: String,
        shopName: String,
        fileName: String
    ): Result<String> {
        return repository.uploadProductImage(uri, productId, productName, shopName, fileName)
    }
}
