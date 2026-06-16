package com.tzh.sme.domain.usecase.supplier

import com.tzh.sme.domain.usecase.common.GetProductsUseCase
import javax.inject.Inject

data class SupplierUseCases @Inject constructor(
    val getSuppliers: GetSuppliersUseCase,
    val getSupplierById: GetSupplierByIdUseCase,
    val addSupplier: AddSupplierUseCase,
    val updateSupplier: UpdateSupplierUseCase,
    val deleteSupplier: DeleteSupplierUseCase,
    val filterSuppliers: FilterSuppliersUseCase,
    val getProducts: GetProductsUseCase
)
