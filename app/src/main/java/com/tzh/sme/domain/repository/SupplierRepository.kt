package com.tzh.sme.domain.repository

import com.tzh.sme.data.model.SupplierModel
import kotlinx.coroutines.flow.Flow

interface SupplierRepository {
    fun getAllSuppliers(): Flow<List<SupplierModel>>
    suspend fun getSupplierById(id: String): SupplierModel?
    suspend fun addSupplier(supplier: SupplierModel): String
    suspend fun updateSupplier(supplier: SupplierModel)
    suspend fun deleteSupplier(id: String)
}
