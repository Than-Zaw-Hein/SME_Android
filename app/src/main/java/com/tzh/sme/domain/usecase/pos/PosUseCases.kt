package com.tzh.sme.domain.usecase.pos

import com.tzh.sme.domain.usecase.common.*
import javax.inject.Inject

data class PosUseCases @Inject constructor(
    val getProducts: GetProductsUseCase,
    val getCategories: GetCategoriesUseCase,
    val processSale: ProcessSaleUseCase,
    val printReceipt: PrintReceiptUseCase,
    val getAuthState: GetAuthStateUseCase,
    val getShopById: GetShopByIdUseCase,
    val getSyncStatus: GetSyncStatusUseCase,
    val filterProducts: FilterProductsUseCase
)
