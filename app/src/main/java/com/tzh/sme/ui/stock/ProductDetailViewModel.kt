package com.tzh.sme.ui.stock

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tzh.sme.data.local.entities.CategoryEntity
import com.tzh.sme.data.local.entities.ProductEntity
import com.tzh.sme.domain.repository.StockRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProductDetailViewModel @Inject constructor(
    private val repository: StockRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductDetailUiState())
    val uiState: StateFlow<ProductDetailUiState> = _uiState.asStateFlow()

    init {
        observeCategories()
        // Handle Edit Mode if productId is present
        val productId: Long? = savedStateHandle["productId"]
        if (productId != null && productId != -1L) {
            loadProduct(productId)
        }
    }

    private fun observeCategories() {
        viewModelScope.launch {
            repository.getAllCategories().collectLatest { categories ->
                _uiState.update { state ->
                    val categoryNames = categories.map { it.name }
                    state.copy(
                        categories = if (categoryNames.isEmpty()) listOf("General") else categoryNames
                    )
                }
            }
        }
    }

    private fun loadProduct(id: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, isEditMode = true) }
            val product = repository.getProductById(id)
            if (product != null) {
                _uiState.update { 
                    it.copy(
                        id = product.id,
                        barcode = product.barcode,
                        name = product.name,
                        price = product.price.toString(),
                        quantity = product.quantity.toString(),
                        category = product.category,
                        imagePaths = product.imagePaths,
                        isLoading = false
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false, error = "Product not found") }
            }
        }
    }

    fun onEvent(event: ProductDetailEvent) {
        when (event) {
            is ProductDetailEvent.BarcodeChanged -> _uiState.update { it.copy(barcode = event.barcode) }
            is ProductDetailEvent.NameChanged -> _uiState.update { it.copy(name = event.name) }
            is ProductDetailEvent.PriceChanged -> _uiState.update { it.copy(price = event.price) }
            is ProductDetailEvent.QuantityChanged -> {
                if (event.quantity.all { it.isDigit() }) {
                    _uiState.update { it.copy(quantity = event.quantity) }
                }
            }
            is ProductDetailEvent.CategoryChanged -> {
                _uiState.update { it.copy(category = event.category) }
                // Save the category if it's new
                viewModelScope.launch {
                    repository.addCategory(CategoryEntity(event.category))
                }
            }
            is ProductDetailEvent.ToggleScanner -> _uiState.update { it.copy(showScanner = event.show) }
            is ProductDetailEvent.AddImages -> {
                val current = _uiState.value.imagePaths
                val remaining = 3 - current.size
                if (remaining > 0) {
                    _uiState.update { it.copy(imagePaths = (current + event.paths).take(3)) }
                }
            }
            is ProductDetailEvent.RemoveImage -> {
                _uiState.update { it.copy(imagePaths = it.imagePaths - event.path) }
            }
            ProductDetailEvent.SaveProduct -> saveProduct()
            ProductDetailEvent.DeleteProduct -> deleteProduct()
            ProductDetailEvent.NavigatedBack -> _uiState.update { it.copy(navigateBack = false) }
        }
    }

    private fun saveProduct() {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val product = ProductEntity(
                id = state.id,
                barcode = state.barcode,
                name = state.name,
                description = "",
                price = state.price.toDoubleOrNull() ?: 0.0,
                quantity = state.quantity.toIntOrNull() ?: 0,
                category = state.category,
                imagePaths = state.imagePaths
            )
            repository.addOrUpdateProduct(product)
            _uiState.update { it.copy(isSaving = false, navigateBack = true) }
        }
    }

    private fun deleteProduct() {
        val state = _uiState.value
        viewModelScope.launch {
            val product = repository.getProductById(state.id)
            if (product != null) {
                repository.deleteProduct(product)
                _uiState.update { it.copy(navigateBack = true) }
            }
        }
    }
}
