package com.tzh.sme.ui.pos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tzh.sme.data.local.entities.ProductEntity
import com.tzh.sme.domain.model.CartItem
import com.tzh.sme.domain.repository.AuthRepository
import com.tzh.sme.domain.repository.StockRepository
import com.tzh.sme.domain.repository.User
import com.tzh.sme.domain.usecase.ProcessSaleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PosViewModel @Inject constructor(
    private val repository: StockRepository,
    private val processSaleUseCase: ProcessSaleUseCase,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategory = MutableStateFlow("All")
    private val _cart = MutableStateFlow<List<CartItem>>(emptyList())
    private val _isCheckoutInProgress = MutableStateFlow(false)

    private val _categories = repository.getAllCategories()
        .map { list -> listOf("All") + list.map { it.name } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("All"))

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<PosUiState> = combine(
        combine(_searchQuery, _selectedCategory) { query, category ->
            query to category
        }.flatMapLatest { (query, category) ->
            repository.getAllProducts().map { products ->
                products.filter { product ->
                    (category == "All" || product.category == category) &&
                            (query.isEmpty() || product.name.contains(
                                query,
                                ignoreCase = true
                            ) || product.barcode.contains(query))
                }
            }
        },
        _categories,
        _selectedCategory,
        _cart,
        _searchQuery,
        _isCheckoutInProgress,
        authRepository.currentUser
    ) {
        PosUiState.Success(
            products = it[0] as List<ProductEntity>,
            categories = it[1] as List<String>,
            selectedCategory = it[2] as String,
            cart = it[3] as List<CartItem>,
            searchQuery = it[4] as String,
            isCheckoutInProgress = it[5] as Boolean,
            user = it[6] as? User
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PosUiState.Loading
    )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onCategorySelect(category: String) {
        _selectedCategory.value = category
    }

    fun onBarcodeScanned(barcode: String) {
        viewModelScope.launch {
            val product = repository.getProductByBarcode(barcode)
            if (product != null) {
                addToCart(product)
            }
        }
    }

    fun addToCart(product: ProductEntity) {
        val currentCart = _cart.value.toMutableList()
        val index = currentCart.indexOfFirst { it.product.id == product.id }
        if (index != -1) {
            currentCart[index] = currentCart[index].copy(quantity = currentCart[index].quantity + 1)
        } else {
            currentCart.add(CartItem(product, 1))
        }
        _cart.value = currentCart
    }

    fun removeFromCart(product: ProductEntity) {
        val currentCart = _cart.value.toMutableList()
        val index = currentCart.indexOfFirst { it.product.id == product.id }
        if (index != -1) {
            if (currentCart[index].quantity > 1) {
                currentCart[index] =
                    currentCart[index].copy(quantity = currentCart[index].quantity - 1)
            } else {
                currentCart.removeAt(index)
            }
        }
        _cart.value = currentCart
    }

    fun checkout() {
        viewModelScope.launch {
            _isCheckoutInProgress.value = true
            val result = processSaleUseCase(_cart.value)
            if (result.isSuccess) {
                _cart.value = emptyList()
            }
            _isCheckoutInProgress.value = false
        }
    }
}
