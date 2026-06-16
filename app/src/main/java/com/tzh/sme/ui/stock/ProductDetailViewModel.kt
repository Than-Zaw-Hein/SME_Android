package com.tzh.sme.ui.stock

import android.net.Uri
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.tzh.sme.data.DefaultData.defaultCategory
import com.tzh.sme.data.model.*
import com.tzh.sme.domain.usecase.stock.StockUseCases
import com.tzh.sme.ui.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Collections
import javax.inject.Inject

@HiltViewModel
class ProductDetailViewModel @Inject constructor(
    private val stockUseCases: StockUseCases,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val productId: String? = savedStateHandle["productId"]
    private val _uiState = MutableStateFlow(ProductDetailUiState())
    val uiState = _uiState.asStateFlow()

    private val _sideEffect = Channel<ProductDetailUiSideEffect>(Channel.BUFFERED)
    val sideEffect = _sideEffect.receiveAsFlow()

    private val removeImageList = Collections.synchronizedList(mutableListOf<String>())

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            launch {
                combine(
                    stockUseCases.getCategories(),
                    stockUseCases.getSuppliers()
                ) { categories, suppliers ->
                    _uiState.update { it.copy(categories = categories, suppliers = suppliers) }
                }.collect()
            }
            launch {
                loadProduct()
            }
        }
    }

    private fun loadProduct() {
        val id = productId ?: "-1"
        if (id == "-1") {
            _uiState.update { it.copy(isEditMode = false) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, isEditMode = true) }
            val product = stockUseCases.getProductById(id)
            if (product != null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        id = product.id,
                        name = product.name,
                        barcode = product.barcode,
                        sellPrice = product.sellPrice.toString(),
                        costPrice = product.costPrice.toString(),
                        editableQty = "0",
                        currentQty = product.quantity,
                        minStockLevel = product.minStockLevel.toString(),
                        category = it.categories.find { c -> c.id == product.categoryId }
                            ?: CategoryModel(),
                        selectedSupplier = it.suppliers.find { s -> s.id == product.supplierId },
                        description = product.description,
                        imagePaths = product.imagePaths
                    )
                }
                loadProductTransactions(product.id)
            } else {
                _uiState.update { it.copy(isLoading = false, error = "Product not found") }
            }
        }
    }

    private fun loadProductTransactions(id: String) {
        viewModelScope.launch {
            stockUseCases.getProductTransactions(id).collect { history ->
                _uiState.update { it.copy(priceHistory = history) }
            }
        }
    }

    fun sendIntent(intent: ProductDetailUiIntent) {
        when (intent) {
            is ProductDetailUiIntent.NameChanged -> _uiState.update { it.copy(name = intent.name) }
            is ProductDetailUiIntent.BarcodeChanged -> _uiState.update { it.copy(barcode = intent.barcode) }
            is ProductDetailUiIntent.PriceChanged -> _uiState.update { it.copy(sellPrice = intent.price) }
            is ProductDetailUiIntent.CostPriceChanged -> _uiState.update { it.copy(costPrice = intent.costPrice) }
            is ProductDetailUiIntent.QuantityChanged -> _uiState.update { it.copy(editableQty = intent.quantity) }
            is ProductDetailUiIntent.MinStockChanged -> _uiState.update { it.copy(minStockLevel = intent.minStock) }
            is ProductDetailUiIntent.TransactionTypeChanged -> _uiState.update {
                it.copy(
                    transactionType = intent.type,
                    editableQty = "0"
                )
            }

            is ProductDetailUiIntent.CategoryChanged -> _uiState.update { it.copy(category = intent.category) }
            is ProductDetailUiIntent.DescriptionChanged -> _uiState.update { it.copy(description = intent.description) }
            is ProductDetailUiIntent.SupplierChanged -> _uiState.update { it.copy(selectedSupplier = intent.supplier) }
            is ProductDetailUiIntent.AddImages -> _uiState.update { it.copy(imagePaths = it.imagePaths + intent.uris) }
            is ProductDetailUiIntent.RemoveImage -> removeImage(intent.path)
            is ProductDetailUiIntent.ToggleScanner -> _uiState.update { it.copy(showScanner = intent.show) }
            is ProductDetailUiIntent.SaveProduct -> saveProduct()
            is ProductDetailUiIntent.DeleteProduct -> deleteProduct()
            is ProductDetailUiIntent.AddCategory -> addCategory(intent.category)
            is ProductDetailUiIntent.UpdateCategory -> updateCategory(intent.category)
            is ProductDetailUiIntent.DeleteCategory -> deleteCategory(intent.category)
            is ProductDetailUiIntent.AddSupplier -> addSupplier(intent.supplier)
            is ProductDetailUiIntent.ClearError -> _uiState.update { it.copy(error = null) }
        }
    }

    private fun removeImage(path: String) {
        if (!path.startsWith("content://")) {
            removeImageList.add(path)
        }
        _uiState.update { it.copy(imagePaths = it.imagePaths.filter { p -> p != path }) }
    }

    private fun addCategory(category: CategoryModel) {
        viewModelScope.launch {
            if (_uiState.value.categories.any {
                    it.name.equals(
                        category.name,
                        ignoreCase = true
                    )
                }) {
                _uiState.update { it.copy(error = "Category '${category.name}' already exists") }
            } else {
                stockUseCases.addCategory(category).onSuccess { category ->
                    _uiState.update {
                        it.copy(
                            category = category ?: defaultCategory,
                            error = null
                        )
                    }
                }.onFailure {
                    _uiState.update { it.copy(category = defaultCategory, error = null) }
                }

            }
        }
    }

    private fun updateCategory(category: CategoryModel) {
        viewModelScope.launch {
            if (_uiState.value.categories.any {
                    it.id != category.id && it.name.equals(
                        category.name,
                        ignoreCase = true
                    )
                }) {
                _uiState.update { it.copy(error = "Category '${category.name}' already exists") }
            } else {
                stockUseCases.updateCategory(category)
                if (_uiState.value.category.id == category.id) {
                    _uiState.update { it.copy(category = category, error = null) }
                }
            }
        }
    }

    private fun deleteCategory(category: CategoryModel) {
        viewModelScope.launch {
            stockUseCases.deleteCategory(category)
            if (_uiState.value.category.id == category.id) {
                _uiState.update { it.copy(category = CategoryModel()) }
            }
        }
    }

    private fun addSupplier(supplier: SupplierModel) {
        viewModelScope.launch {
            if (_uiState.value.suppliers.any { it.name.equals(supplier.name, ignoreCase = true) }) {
                _uiState.update { it.copy(error = "Supplier '${supplier.name}' already exists") }
            } else {
                val result = stockUseCases.addSupplier(supplier)
                result.onSuccess { id ->
                    _uiState.update {
                        it.copy(
                            selectedSupplier = supplier.copy(id = id),
                            error = null
                        )
                    }
                }
            }
        }
    }

    private fun deleteProduct() {
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true) }
            val state = uiState.value
            val product = stockUseCases.getProductById(state.id)
            if (product != null) {
                val user = stockUseCases.getAuthState.currentUser.value
                val shopId = user?.shopId ?: user?.id
                if (shopId != null) {
                    val shop = stockUseCases.getShopById(shopId).first()
                    stockUseCases.deleteProductImagesFolder(
                        state.id,
                        state.name,
                        shop?.name ?: "UnknownShop"
                    )
                }
                stockUseCases.deleteProduct(product)
                _uiState.update { it.copy(isDeleting = false) }
                _sideEffect.send(ProductDetailUiSideEffect.NavigateBack)
            } else {
                _uiState.update { it.copy(isDeleting = false) }
            }
        }
    }

    private fun saveProduct() {
        val state = uiState.value
        if (state.name.isEmpty() || state.sellPrice.isEmpty() || state.costPrice.isEmpty()) {
            _uiState.update { it.copy(error = "Please fill in all required fields") }
            return
        }

        if (state.costPrice > state.sellPrice) {
            _uiState.update { it.copy(error = "Cost price must not be greater than selling price.") }
            return
        }


        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            val pId =
                if (state.id.isEmpty() || state.id == "-1") stockUseCases.generateProductId() else state.id

            val existingBarcode = stockUseCases.getProductByBarcode(state.barcode)
            if (existingBarcode != null && existingBarcode.id != pId) {
                _uiState.update { it.copy(isSaving = false, error = "Barcode already exists") }
                return@launch
            }

            val oldProduct =
                if (state.id.isNotEmpty() && state.id != "-1") stockUseCases.getProductById(state.id) else null
            val finalImagePaths = mutableListOf<String>()
            val shop = stockUseCases.getAuthState.currentUser.value?.shopId?.let {
                stockUseCases.getShopById(it).first()
            }
            val shopName = shop?.name ?: "UnknownShop"

            for ((index, path) in state.imagePaths.withIndex()) {
                if (path.startsWith("content://")) {
                    val result = stockUseCases.uploadProductImage(
                        Uri.parse(path),
                        pId,
                        state.name,
                        shopName,
                        "${pId}_${System.currentTimeMillis()}_$index.webp"
                    )
                    result.onSuccess { finalImagePaths.add(it) }.onFailure {
                        _uiState.update { s ->
                            s.copy(
                                isSaving = false,
                                error = "Upload failed: ${it.message}"
                            ); s
                        }; return@launch
                    }
                } else {
                    finalImagePaths.add(path)
                }
            }

            val productToSave = ProductModel(
                id = pId,
                name = state.name,
                barcode = state.barcode,
                sellPrice = state.sellPrice.toDoubleOrNull() ?: 0.0,
                costPrice = state.costPrice.toDoubleOrNull() ?: 0.0,
                quantity = oldProduct?.quantity ?: 0,
                minStockLevel = state.minStockLevel.toIntOrNull() ?: 0,
                categoryId = state.category.id,
                supplierId = state.selectedSupplier?.id ?: "",
                description = state.description,
                imagePaths = finalImagePaths
            )

            stockUseCases.addOrUpdateProduct(productToSave).onSuccess {
                val enteredQty = state.editableQty.toIntOrNull() ?: 0
                if (enteredQty != 0 || ((state.costPrice.toDoubleOrNull()
                        ?: 0.0) != (oldProduct?.costPrice ?: 0.0))
                ) {
                    val currentUser = stockUseCases.getAuthState.currentUser.value
                    val transaction = TransactionModel(
                        type = state.transactionType,
                        totalAmount = enteredQty * productToSave.costPrice,
                        netAmount = enteredQty * productToSave.costPrice,
                        userId = currentUser?.id,
                        userName = currentUser?.displayName ?: "Unknown Staff",
                        supplierId = state.selectedSupplier?.id,
                        supplierName = state.selectedSupplier?.name
                    )

                    val item = TransactionItemModel(
                        productId = productToSave.id,
                        productName = productToSave.name,
                        quantity = enteredQty,
                        priceAtTime = productToSave.costPrice
                    )

                    stockUseCases.executeTransaction(transaction, listOf(item))
                }

                val toDelete = removeImageList.toList()
                removeImageList.clear()
                if (toDelete.isNotEmpty()) {
                    toDelete.map { async { stockUseCases.deleteProductImage(it) } }.awaitAll()
                }

                _uiState.update { it.copy(isSaving = false) }
                _sideEffect.send(ProductDetailUiSideEffect.NavigateBack)
            }.onFailure { _uiState.update { s -> s.copy(isSaving = false, error = it.message); s } }
        }
    }
}
