package com.tzh.sme.domain.repository

import com.tzh.sme.data.model.ProductModel
import kotlinx.coroutines.flow.Flow

interface ProductRepository {
    fun getAllProducts(): Flow<List<ProductModel>>
    fun searchProducts(query: String): Flow<List<ProductModel>>
    suspend fun getProductByBarcode(barcode: String): ProductModel?
    suspend fun getProductById(id: String): ProductModel?
    suspend fun addOrUpdateProduct(product: ProductModel)
    suspend fun deleteProduct(product: ProductModel)
    fun generateNextId(): String
}
