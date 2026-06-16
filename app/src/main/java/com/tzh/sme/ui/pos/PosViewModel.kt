package com.tzh.sme.ui.pos

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tzh.sme.data.model.CategoryModel
import com.tzh.sme.data.model.ProductModel
import com.tzh.sme.data.model.ShopModel
import com.tzh.sme.data.model.TransactionItemModel
import com.tzh.sme.data.model.TransactionModel
import com.tzh.sme.domain.model.CartItem
import com.tzh.sme.domain.repository.User
import com.tzh.sme.domain.usecase.pos.PosUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PosViewModel @Inject constructor(
    private val posUseCases: PosUseCases
) : ViewModel() {

    private val _uiState = MutableStateFlow(PosUiState())
    val uiState: StateFlow<PosUiState> = _uiState.asStateFlow()

    private val _sideEffect = Channel<PosUiSideEffect>(Channel.BUFFERED)
    val sideEffect = _sideEffect.receiveAsFlow()

    init {
        viewModelScope.launch {
            combine(
                posUseCases.getCategories(),
                posUseCases.getProducts(),
                posUseCases.getAuthState.currentUser,
                posUseCases.getAuthState.currentUser.flatMapLatest { user ->
                    user?.shopId?.let { posUseCases.getShopById(it) } ?: flowOf(null)
                },
                posUseCases.getAuthState.currentUser.flatMapLatest { user ->
                    user?.id?.let { posUseCases.getSyncStatus(it) } ?: flowOf(false)
                }
            ) { categories, products, currentUser, shop, isSyncing ->
                val updatedCartItems = uiState.value.cartItems.mapNotNull { cartItem ->
                    val latestProduct = products.find { it.id == cartItem.product.id }
                    when {
                        latestProduct == null -> null
                        latestProduct.quantity < 1 -> null
                        cartItem.quantity > latestProduct.quantity -> cartItem.copy(
                            product = latestProduct, quantity = latestProduct.quantity
                        )

                        else -> cartItem.copy(product = latestProduct)
                    }
                }
                PosUiState(
                    categories = categories,
                    products = products,
                    cartItems = updatedCartItems,
                    currentUser = currentUser,
                    currentShop = shop,
                    isSyncing = isSyncing
                )
            }.collectLatest { state ->
                _uiState.update {
                    it.copy(
                        categories = state.categories,
                        products = state.products,
                        cartItems = state.cartItems,
                        currentUser = state.currentUser,
                        currentShop = state.currentShop,
                        isSyncing = state.isSyncing
                    )
                }
            }
        }
    }

    fun sendIntent(intent: PosUiIntent) {
        when (intent) {
            is PosUiIntent.SelectCategory -> onCategorySelect(intent.category)
            is PosUiIntent.UpdateSearchQuery -> onSearchQueryChange(intent.query)
            is PosUiIntent.AddToCart -> addToCart(intent.product)
            is PosUiIntent.RemoveFromCart -> removeFromCart(intent.product)
            is PosUiIntent.BarcodeScanned -> onBarcodeScanned(intent.barcode)
            is PosUiIntent.UpdateDiscount -> onDiscountChange(intent.discount)
            PosUiIntent.Checkout -> checkout()
            is PosUiIntent.PrintReceipt -> printReceipt(intent.transaction, intent.items)
            PosUiIntent.DismissSuccessDialog -> dismissSuccessDialog()
            PosUiIntent.ClearPrintMessage -> clearPrintMessage()
            PosUiIntent.ClearErrorMessage -> clearErrorMessage()
            PosUiIntent.ResetCheckoutState -> resetCheckoutState()
        }
    }

    private fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    private fun onCategorySelect(category: CategoryModel) {
        _uiState.update {
            it.copy(
                selectedCategory = category,
            )
        }
    }

    val filterProduct = uiState.map {
        posUseCases.filterProducts(
            products = it.products,
            selectedCategory = it.selectedCategory,
            searchQuery = it.searchQuery,
            onlyAvailable = true
        )
    }.distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())


    private fun onBarcodeScanned(barcode: String) {
        viewModelScope.launch {
            val product = uiState.value.products.find { it.barcode == barcode }
            if (product != null) {
                addToCart(product)
            }
        }
    }

    private fun addToCart(product: ProductModel) {
        if (product.quantity < 1) return
        val currentCart = _uiState.value.cartItems.toMutableList()
        val index = currentCart.indexOfFirst { it.product.id == product.id }
        if (index != -1) {
            val latestProduct = uiState.value.products.find { it.id == product.id } ?: product
            if (currentCart[index].quantity < latestProduct.quantity) {
                currentCart[index] = currentCart[index].copy(
                    product = latestProduct, quantity = currentCart[index].quantity + 1
                )
            }
        } else {
            currentCart.add(CartItem(product, 1))
        }
        _uiState.update { it.copy(cartItems = currentCart) }
    }

    private fun removeFromCart(product: ProductModel) {
        val currentCart = _uiState.value.cartItems.toMutableList()
        val index = currentCart.indexOfFirst { it.product.id == product.id }
        if (index != -1) {
            if (currentCart[index].quantity > 1) {
                currentCart[index] =
                    currentCart[index].copy(quantity = currentCart[index].quantity - 1)
            } else {
                currentCart.removeAt(index)
            }
        }
        _uiState.update { it.copy(cartItems = currentCart) }
    }

    private fun onDiscountChange(discount: Double) {
        _uiState.update { it.copy(discount = discount) }
    }

    private fun checkout() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCheckOutInProgress = true) }
            val currentUser = uiState.value.currentUser
            val result = posUseCases.processSale(
                cartItems = uiState.value.cartItems,
                discount = uiState.value.discount,
                sellerId = currentUser?.id,
                sellerName = currentUser?.displayName ?: "Unknown Staff"
            )
            result.onSuccess { (transaction, items) ->
                _uiState.update {
                    it.copy(
                        cartItems = emptyList(),
                        isCheckOutInProgress = false,
                        printMessage = "Sale completed successfully.",
                        completedTransaction = transaction to items
                    )
                }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(
                        isCheckOutInProgress = false,
                        errorMessage = e.message ?: "An error occurred during checkout"
                    )
                }
            }
        }
    }

    private fun printReceipt(
        transaction: TransactionModel,
        items: List<TransactionItemModel>
    ) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isPrinting = true,
                    printError = "",
                    printMessage = ""
                )
            }
            val result = posUseCases.printReceipt(
                transaction, items, uiState.value.currentShop, uiState.value.currentUser
            )
            result.onSuccess {
                _uiState.update {
                    it.copy(
                        isPrinting = false, printMessage = "Receipt printed successfully"
                    )
                }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(
                        isPrinting = false, printError = "Printing failed: ${e.message}"
                    )
                }
            }
        }
    }

    private fun resetCheckoutState() {
        _uiState.update {
            it.copy(
                discount = 0.0,
                isCheckOutInProgress = false,
                isPrinting = false,
                printMessage = "",
                printError = "",
                errorMessage = "",
                completedTransaction = null
            )
        }
    }

    private fun dismissSuccessDialog() {
        _uiState.update {
            it.copy(
                completedTransaction = null,
                printError = "",
                printMessage = ""
            )
        }
    }

    private fun clearPrintMessage() {
        _uiState.update { it.copy(printMessage = "") }
    }

    private fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = "") }
    }
}
