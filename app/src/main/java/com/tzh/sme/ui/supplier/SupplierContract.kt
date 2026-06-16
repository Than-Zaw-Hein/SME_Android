package com.tzh.sme.ui.supplier

import com.tzh.sme.data.model.ProductModel
import com.tzh.sme.data.model.SupplierModel

data class SupplierUiState(
    val suppliers: List<SupplierModel> = emptyList(),
    val filteredSuppliers: List<SupplierModel> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val selectedSupplier: SupplierModel? = null,
    val supplierProducts: List<ProductModel> = emptyList(),
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false
)

sealed interface SupplierUiIntent {
    data class UpdateSearchQuery(val query: String) : SupplierUiIntent
    data class SelectSupplier(val id: String?) : SupplierUiIntent
    data class SaveSupplier(val name: String, val contact: String, val address: String, val category: String) : SupplierUiIntent
    data class DeleteSupplier(val id: String) : SupplierUiIntent
    object ClearError : SupplierUiIntent
    object ResetSaveStatus : SupplierUiIntent
}

sealed interface SupplierUiSideEffect {
    object NavigateBack : SupplierUiSideEffect
    data class ShowError(val message: String) : SupplierUiSideEffect
}
