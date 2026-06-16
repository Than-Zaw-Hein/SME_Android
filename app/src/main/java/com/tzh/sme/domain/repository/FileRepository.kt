package com.tzh.sme.domain.repository

import android.net.Uri

interface FileRepository {
    suspend fun uploadProductImage(uri: Uri, productId: String, productName: String, shopName: String, fileName: String): Result<String>
    suspend fun deleteProductImage(path: String): Result<Unit>
    suspend fun deleteProductImagesFolder(productId: String, productName: String, shopName: String): Result<Unit>
    suspend fun registerImage(url: String, path: String, relatedEntityId: String, shopId: String): Result<Unit>
}
