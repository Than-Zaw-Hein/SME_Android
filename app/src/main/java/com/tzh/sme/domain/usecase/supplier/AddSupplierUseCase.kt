package com.tzh.sme.domain.usecase.supplier

import com.tzh.sme.data.model.SupplierModel
import com.tzh.sme.domain.repository.SupplierRepository
import javax.inject.Inject

class AddSupplierUseCase @Inject constructor(
    private val repository: SupplierRepository
) {
    suspend operator fun invoke(supplier: SupplierModel): Result<String> {
        return try {
            val id = repository.addSupplier(supplier)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
