package com.tzh.sme.domain.usecase.supplier

import com.tzh.sme.domain.repository.SupplierRepository
import javax.inject.Inject

class DeleteSupplierUseCase @Inject constructor(
    private val repository: SupplierRepository
) {
    suspend operator fun invoke(id: String): Result<Unit> {
        return try {
            repository.deleteSupplier(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
