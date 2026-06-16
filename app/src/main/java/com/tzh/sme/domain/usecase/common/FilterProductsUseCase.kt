package com.tzh.sme.domain.usecase.common

import com.tzh.sme.data.model.CategoryModel
import com.tzh.sme.data.model.ProductModel
import javax.inject.Inject

class FilterProductsUseCase @Inject constructor() {
    operator fun invoke(
        products: List<ProductModel>,
        selectedCategory: CategoryModel,
        searchQuery: String,
        onlyAvailable: Boolean = false
    ): List<ProductModel> {
        return products.filter { product ->
            val matchesCategory = selectedCategory.name == "All" || product.categoryId == selectedCategory.id
            val matchesSearch = searchQuery.isEmpty() || 
                    product.name.contains(searchQuery, ignoreCase = true) || 
                    product.barcode.contains(searchQuery)
            val matchesAvailability = !onlyAvailable || product.quantity > 0
            
            matchesCategory && matchesSearch && matchesAvailability
        }
    }
}
