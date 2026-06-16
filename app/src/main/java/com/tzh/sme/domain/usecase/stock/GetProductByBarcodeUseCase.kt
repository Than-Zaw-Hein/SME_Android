package com.tzh.sme.domain.usecase.stock

import com.tzh.sme.data.model.ProductModel
import com.tzh.sme.domain.repository.ProductRepository
import javax.inject.Inject

class GetProductByBarcodeUseCase @Inject constructor(
    private val repository: ProductRepository
) {
    suspend operator fun invoke(barcode: String): ProductModel? {
        return repository.getProductByBarcode(barcode)
    }
}
