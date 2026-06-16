package com.tzh.sme.domain.usecase.supplier

import com.tzh.sme.data.model.SupplierModel
import javax.inject.Inject

class FilterSuppliersUseCase @Inject constructor() {
    operator fun invoke(
        suppliers: List<SupplierModel>,
        searchQuery: String
    ): List<SupplierModel> {
        return if (searchQuery.isEmpty()) {
            suppliers
        } else {
            suppliers.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                        it.category.contains(searchQuery, ignoreCase = true)
            }
        }
    }
}
