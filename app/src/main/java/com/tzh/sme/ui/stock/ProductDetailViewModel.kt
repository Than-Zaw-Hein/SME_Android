package com.tzh.sme.ui.stock

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tzh.sme.data.model.CategoryModel
import com.tzh.sme.data.model.ProductModel
import com.tzh.sme.data.model.TransactionItemModel
import com.tzh.sme.data.model.TransactionModel
import com.tzh.sme.data.model.TransactionType
import com.tzh.sme.data.model.SupplierModel
import com.tzh.sme.data.remote.BASE_URL
import com.tzh.sme.domain.repository.CategoryRepository
import com.tzh.sme.domain.repository.FileRepository
import com.tzh.sme.domain.repository.ProductRepository
import com.tzh.sme.domain.repository.SupplierRepository
import com.tzh.sme.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Collections
import javax.inject.Inject

sealed class ProductDetailEvent {
    data class NameChanged(val name: String) : ProductDetailEvent()
    data class BarcodeChanged(val barcode: String) : ProductDetailEvent()
    data class PriceChanged(val price: String) : ProductDetailEvent()
    data class QuantityChanged(val quantity: String) : ProductDetailEvent()
    data class TransactionTypeChanged(val type: TransactionType) : ProductDetailEvent()
    data class CategoryChanged(val category: CategoryModel) : ProductDetailEvent()
    data class DescriptionChanged(val description: String) : ProductDetailEvent()
    data class SupplierChanged(val supplier: SupplierModel?) : ProductDetailEvent()
    data class AddImages(val uris: List<String>) : ProductDetailEvent()
    data class RemoveImage(val path: String) : ProductDetailEvent()
    data class ToggleScanner(val show: Boolean) : ProductDetailEvent()
    object SaveProduct : ProductDetailEvent()
    object DeleteProduct : ProductDetailEvent()
    data class AddCategory(val category: CategoryModel) : ProductDetailEvent()
    data class UpdateCategory(val category: CategoryModel) : ProductDetailEvent()
    data class DeleteCategory(val category: CategoryModel) : ProductDetailEvent()
    data class AddSupplier(val supplier: SupplierModel) : ProductDetailEvent()
    object ErrorShown : ProductDetailEvent()
}

sealed interface ProductDetailEffect {
    object NavigateBack : ProductDetailEffect
}

@HiltViewModel
class ProductDetailViewModel @Inject constructor(
    private val repository: ProductRepository,
    private val fileRepository: FileRepository,
    private val categoryRepository: CategoryRepository,
    private val supplierRepository: SupplierRepository,
    private val transactionRepository: TransactionRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val productId: String? = savedStateHandle["productId"]
    private val _uiState = MutableStateFlow(ProductDetailUiState())
    val uiState = _uiState.asStateFlow()

    private val _effect = Channel<ProductDetailEffect>()
    val effect = _effect.receiveAsFlow()

    // Use a synchronized list to avoid potential issues during rapid interactions
    private val removeImageList = Collections.synchronizedList(mutableListOf<String>())

    init {
        loadProduct()
        loadCategories()
        loadSuppliers()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            categoryRepository.getAllCategories().collect { categories ->
                _uiState.update { it.copy(categories = categories) }
            }
        }
    }

    private fun loadSuppliers() {
        viewModelScope.launch {
            supplierRepository.getAllSuppliers().collect { suppliers ->
                _uiState.update { it.copy(suppliers = suppliers) }
            }
        }
    }

    private fun loadProduct() {
        val id = productId ?: return
        if (id == "-1") {
            _uiState.update { it.copy(isEditMode = false) }
            return
        }

        viewModelScope.launch {
            val categories = categoryRepository.getAllCategories().first()
            _uiState.update {
                it.copy(
                    isLoading = true,
                    isEditMode = true,
                    categories = categories
                )
            }
            val product = repository.getProductById(id)

            if (product != null) {
                val selectedCategory = categories.find { it.id == product.categoryId }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        id = product.id,
                        name = product.name,
                        barcode = product.barcode,
                        price = product.price.toString(),
                        editableQty = "0", // Start enter qty from zero
                        currentQty = product.quantity,
                        category = selectedCategory ?: CategoryModel(),
                        description = product.description,
                        imagePaths = product.imagePaths.map { path ->
                            if (path.startsWith("http")) path else BASE_URL + path
                        }
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
            transactionRepository.getTransactionsByProduct(id).collect { history ->
                _uiState.update { it.copy(priceHistory = history) }
            }
        }
    }

    fun onEvent(event: ProductDetailEvent) {
        when (event) {
            is ProductDetailEvent.NameChanged -> _uiState.update { it.copy(name = event.name) }
            is ProductDetailEvent.BarcodeChanged -> _uiState.update { it.copy(barcode = event.barcode) }
            is ProductDetailEvent.PriceChanged -> _uiState.update { it.copy(price = event.price) }
            is ProductDetailEvent.QuantityChanged -> _uiState.update { it.copy(editableQty = event.quantity) }
            is ProductDetailEvent.TransactionTypeChanged -> _uiState.update {
                it.copy(
                    transactionType = event.type
                )
            }

            is ProductDetailEvent.CategoryChanged -> _uiState.update { it.copy(category = event.category) }
            is ProductDetailEvent.DescriptionChanged -> _uiState.update { it.copy(description = event.description) }
            is ProductDetailEvent.SupplierChanged -> _uiState.update { it.copy(selectedSupplier = event.supplier) }
            is ProductDetailEvent.AddImages -> {
                _uiState.update { it.copy(imagePaths = it.imagePaths + event.uris) }
            }

            is ProductDetailEvent.ErrorShown -> {
                _uiState.update { it.copy(error = null) }
            }

            is ProductDetailEvent.RemoveImage -> removeImage(event.path)
            is ProductDetailEvent.ToggleScanner -> _uiState.update { it.copy(showScanner = event.show) }
            is ProductDetailEvent.SaveProduct -> saveProduct()
            is ProductDetailEvent.DeleteProduct -> deleteProduct()
            is ProductDetailEvent.AddCategory -> {
                viewModelScope.launch {
                    val currentCategories = _uiState.value.categories
                    val isDuplicate = currentCategories.any {
                        it.name.equals(event.category.name, ignoreCase = true)
                    }

                    if (isDuplicate) {
                        _uiState.update { it.copy(error = "Category '${event.category.name}' already exists") }
                    } else {
                        categoryRepository.addCategory(event.category)
                        _uiState.update { it.copy(category = event.category, error = null) }
                    }
                }
            }

            is ProductDetailEvent.UpdateCategory -> {
                viewModelScope.launch {
                    val currentCategories = _uiState.value.categories
                    val isDuplicate = currentCategories.any {
                        it.id != event.category.id && it.name.equals(
                            event.category.name,
                            ignoreCase = true
                        )
                    }

                    if (isDuplicate) {
                        _uiState.update { it.copy(error = "Category '${event.category.name}' already exists") }
                    } else {
                        categoryRepository.updateCategory(event.category)
                        // If the updated category was selected, update the selection too
                        if (_uiState.value.category.id == event.category.id) {
                            _uiState.update { it.copy(category = event.category, error = null) }
                        }
                    }
                }
            }

            is ProductDetailEvent.DeleteCategory -> {
                viewModelScope.launch {
                    categoryRepository.deleteCategory(event.category)
                    if (_uiState.value.category.id == event.category.id) {
                        _uiState.update { it.copy(category = CategoryModel()) }
                    }
                }
            }

            is ProductDetailEvent.AddSupplier -> {
                viewModelScope.launch {
                    val currentSuppliers = _uiState.value.suppliers
                    val isDuplicate = currentSuppliers.any {
                        it.name.equals(event.supplier.name, ignoreCase = true)
                    }

                    if (isDuplicate) {
                        _uiState.update { it.copy(error = "Supplier '${event.supplier.name}' already exists") }
                    } else {
                        val supplierId = supplierRepository.addSupplier(event.supplier)
                        _uiState.update {
                            it.copy(
                                selectedSupplier = event.supplier.copy(id = supplierId),
                                error = null
                            )
                        }
                    }
                }
            }
        }
    }

    private fun removeImage(path: String) {
        // Track the image for deletion upon saving if it's a remote URL
        if (!path.startsWith("content://")) {
            removeImageList.add(path)
        }
        _uiState.update {
            it.copy(imagePaths = it.imagePaths.filter { p -> p != path })
        }
    }

    private fun deleteProduct() {
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true) }
            val state = uiState.value
            val product = repository.getProductById(state.id)
            if (product != null) {
                fileRepository.deleteProductImagesFolder(state.id)
                repository.deleteProduct(product)
                _uiState.update { it.copy(isDeleting = false) }
                _effect.send(ProductDetailEffect.NavigateBack)
            } else {
                _uiState.update { it.copy(isDeleting = false) }
            }
        }
    }

    private fun saveProduct() {
        val state = uiState.value
        if (state.name.isEmpty() || state.price.isEmpty() || state.editableQty.isEmpty()) {
            _uiState.update { it.copy(error = "Please fill in all required fields") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }

            val pId = if (state.id.isEmpty() || state.id == "-1") {
                repository.generateNextId()
            } else {
                state.id
            }

            val existingProductByBarcode = repository.getProductByBarcode(state.barcode)
            if (existingProductByBarcode != null && existingProductByBarcode.id != pId) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        error = "A product with this barcode already exists: ${existingProductByBarcode.name}"
                    )
                }
                return@launch
            }

            val oldProduct =
                if (state.id.isNotEmpty() && state.id != "-1") repository.getProductById(state.id) else null
            val oldQuantity = oldProduct?.quantity ?: 0
            val enteredQuantity = state.editableQty.toIntOrNull() ?: 0
            val transactionType = state.transactionType

            // Calculate new quantity based on transaction type
            // The TransactionRepository.executeTransaction will handle the actual stock update in Firestore
            // We just need to pass the delta (enteredQuantity) and the type.

            // Using timestamps in filenames prevents name collisions that lead to accidental deletions
            val finalImagePaths = state.imagePaths.mapIndexed { index, path ->
                if (path.startsWith("content://")) {
                    async {
                        val timestamp = System.currentTimeMillis()
                        val fileName = "${pId}_${timestamp}_$index.webp"
                        val result =
                            fileRepository.uploadProductImage(Uri.parse(path), pId, fileName)
                        result.getOrNull()?.removePrefix(BASE_URL)
                    }
                } else {
                    async { path.removePrefix(BASE_URL) }
                }
            }.awaitAll().filterNotNull()

            if (finalImagePaths.size < state.imagePaths.size) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        error = "Some images failed to upload to the server."
                    )
                }
                return@launch
            }

            val newPrice = state.price.toDoubleOrNull() ?: 0.0

            val productToSave = ProductModel(
                id = pId,
                name = state.name,
                barcode = state.barcode,
                price = newPrice,
                quantity = oldQuantity, // Keep old quantity; TransactionRepository.executeTransaction will update it atomically
                categoryId = state.category.id,
                description = state.description,
                imagePaths = finalImagePaths
            )

            try {
                repository.addOrUpdateProduct(productToSave)

                // Transaction Logic
                val priceChanged = oldProduct == null || oldProduct.price != newPrice

                if (enteredQuantity != 0 || priceChanged) {
                    val totalAmount = newPrice * enteredQuantity
                    val transaction = TransactionModel(
                        type = transactionType,
                        totalAmount = totalAmount,
                        netAmount = totalAmount,
                        supplierId = state.selectedSupplier?.id,
                        supplierName = state.selectedSupplier?.name
                    )
                    val item = TransactionItemModel(
                        productId = pId,
                        productName = state.name,
                        quantity = enteredQuantity,
                        priceAtTime = newPrice
                    )
                    transactionRepository.executeTransaction(transaction, listOf(item))
                }

                // Clear the list immediately to prevent multiple deletion attempts
                val toDelete = removeImageList.toList()
                removeImageList.clear()

                if (toDelete.isNotEmpty()) {
                    toDelete.map { path ->
                        async {
                            fileRepository.deleteProductImage(path.removePrefix(BASE_URL))
                        }
                    }.awaitAll()
                }

                _uiState.update { it.copy(isSaving = false) }
                _effect.send(ProductDetailEffect.NavigateBack)
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, error = e.message) }
            }
        }
    }
}

data class ProductDetailUiState(
    val id: String = "",
    val name: String = "",
    val barcode: String = "",
    val price: String = "",
    val editableQty: String = "0",
    val currentQty: Int = 0,
    val transactionType: TransactionType = TransactionType.STOCK_IN,
    val category: CategoryModel = CategoryModel(),
    val description: String = "",
    val suppliers: List<SupplierModel> = emptyList(),
    val selectedSupplier: SupplierModel? = null,
    val imagePaths: List<String> = emptyList(),
    val categories: List<CategoryModel> = emptyList(),
    val priceHistory: List<Pair<TransactionModel, TransactionItemModel>> = emptyList(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isDeleting: Boolean = false,
    val isEditMode: Boolean = false,
    val showScanner: Boolean = false,
    val error: String? = null
)
