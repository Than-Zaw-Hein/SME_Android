package com.tzh.sme.domain.usecase.supplier

import com.tzh.sme.data.model.SupplierModel
import com.tzh.sme.domain.repository.SupplierRepository
import javax.inject.Inject

class UpdateSupplierUseCase @Inject constructor(
    private val repository: SupplierRepository
) {
    suspend operator fun invoke(supplier: SupplierModel): Result<Unit> {
        return try {
            repository.updateSupplier(supplier)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
