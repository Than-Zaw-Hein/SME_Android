package com.tzh.sme.ui.pos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tzh.sme.data.local.entities.ProductEntity
import com.tzh.sme.domain.model.CartItem
import com.tzh.sme.domain.repository.StockRepository
import com.tzh.sme.domain.usecase.ProcessSaleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PosViewModel @Inject constructor(
    private val repository: StockRepository,
    private val processSaleUseCase: ProcessSaleUseCase
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _cart = MutableStateFlow<List<CartItem>>(emptyList())
    private val _isCheckoutInProgress = MutableStateFlow(false)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<PosUiState> = combine(
        _searchQuery.flatMapLatest { query ->
            if (query.isEmpty()) repository.getAllProducts()
            else repository.searchProducts(query)
        },
        _cart,
        _searchQuery,
        _isCheckoutInProgress
    ) { products, cart, query, inProgress ->
        PosUiState.Success(
            products = products,
            cart = cart,
            searchQuery = query,
            isCheckoutInProgress = inProgress
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PosUiState.Loading
    )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
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
                currentCart[index] = currentCart[index].copy(quantity = currentCart[index].quantity - 1)
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
