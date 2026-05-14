package com.tzh.sme.domain.repository

import android.net.Uri

interface FileRepository {
    suspend fun uploadProductImage(uri: Uri, productId: String, fileName: String): Result<String>
    suspend fun deleteProductImage(path: String): Result<Unit>
    suspend fun deleteProductImagesFolder(productId: String): Result<Unit>
}
