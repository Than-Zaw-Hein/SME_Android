package com.tzh.sme.domain.usecase.stock

import com.tzh.sme.domain.usecase.common.*
import com.tzh.sme.domain.usecase.supplier.AddSupplierUseCase
import com.tzh.sme.domain.usecase.supplier.GetSuppliersUseCase
import javax.inject.Inject

data class StockUseCases @Inject constructor(
    val getProducts: GetProductsUseCase,
    val getCategories: GetCategoriesUseCase,
    val syncStock: SyncStockUseCase,
    val getSyncStatus: GetSyncStatusUseCase,
    val getAuthState: GetAuthStateUseCase,
    val getShopById: GetShopByIdUseCase,
    val getProductById: GetProductByIdUseCase,
    val getProductByBarcode: GetProductByBarcodeUseCase,
    val addOrUpdateProduct: AddOrUpdateProductUseCase,
    val deleteProduct: DeleteProductUseCase,
    val generateProductId: GenerateProductIdUseCase,
    val addCategory: AddCategoryUseCase,
    val updateCategory: UpdateCategoryUseCase,
    val deleteCategory: DeleteCategoryUseCase,
    val getSuppliers: GetSuppliersUseCase,
    val addSupplier: AddSupplierUseCase,
    val getProductTransactions: GetProductTransactionsUseCase,
    val uploadProductImage: UploadProductImageUseCase,
    val deleteProductImage: DeleteProductImageUseCase,
    val deleteProductImagesFolder: DeleteProductImagesFolderUseCase,
    val filterProducts: FilterProductsUseCase,
    val processSale: ProcessSaleUseCase,
    val executeTransaction: ExecuteTransactionUseCase
)
