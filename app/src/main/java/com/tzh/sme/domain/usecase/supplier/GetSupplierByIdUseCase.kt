package com.tzh.sme.domain.usecase.supplier

import com.tzh.sme.data.model.SupplierModel
import com.tzh.sme.domain.repository.SupplierRepository
import javax.inject.Inject

class GetSupplierByIdUseCase @Inject constructor(
    private val repository: SupplierRepository
) {
    suspend operator fun invoke(id: String): SupplierModel? {
        return repository.getSupplierById(id)
    }
}
