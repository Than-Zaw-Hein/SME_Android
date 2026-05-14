package com.tzh.sme.domain.usecase

import com.tzh.sme.data.model.CategoryModel
import com.tzh.sme.data.model.ProductModel
import com.tzh.sme.domain.repository.StockRepository
import javax.inject.Inject

class AddProductToStockUseCase @Inject constructor(
    private val repository: StockRepository
) {
    suspend operator fun invoke(barcode: String, quantityToAdd: Int = 1): Result<Unit> {
        return try {
            val existingProduct = repository.getProductByBarcode(barcode)
            if (existingProduct != null) {
                val updatedProduct = existingProduct.copy(
                    quantity = existingProduct.quantity + quantityToAdd,
                    lastUpdated = System.currentTimeMillis()
                )
                repository.addOrUpdateProduct(updatedProduct)
            } else {
                val newProduct = ProductModel(
                    barcode = barcode,
                    name = "New Product ($barcode)",
                    description = "",
                    price = 0.0,
                    quantity = quantityToAdd,
//                    category = CategoryModel("Uncategorized", "📦")
                    categoryId = ""
                )
                repository.addOrUpdateProduct(newProduct)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
