package com.tzh.sme.domain.usecase

import com.tzh.sme.data.local.entities.ProductEntity
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
                // If product doesn't exist, we might need more details (name, price, etc.)
                // In a real app, this might trigger a UI to enter details or fetch from a global DB.
                // For this requirement, we'll assume a placeholder or that we only increment if exists.
                // However, the prompt says "otherwise, create a new entry". 
                // We'll create a skeleton entry if it's new.
                val newProduct = ProductEntity(
                    barcode = barcode,
                    name = "New Product ($barcode)",
                    description = "",
                    price = 0.0,
                    quantity = quantityToAdd,
                    category = "Uncategorized"
                )
                repository.addOrUpdateProduct(newProduct)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
