package com.tzh.sme.domain.usecase.stock

import com.tzh.sme.data.model.SupplierModel
import com.tzh.sme.domain.repository.SupplierRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetSuppliersUseCase @Inject constructor(
    private val repository: SupplierRepository
) {
    operator fun invoke(): Flow<List<SupplierModel>> {
        return repository.getAllSuppliers()
    }
}
