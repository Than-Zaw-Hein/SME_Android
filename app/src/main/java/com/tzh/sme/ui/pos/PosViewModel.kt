package com.tzh.sme.ui.pos

import android.annotation.SuppressLint
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tzh.sme.data.DefaultData.defaultCategory
import com.tzh.sme.data.model.CategoryModel
import com.tzh.sme.data.model.ProductModel
import com.tzh.sme.data.model.TransactionItemModel
import com.tzh.sme.data.model.TransactionModel
import com.tzh.sme.domain.model.CartItem
import com.tzh.sme.domain.repository.AuthRepository
import com.tzh.sme.domain.repository.StockRepository
import com.tzh.sme.domain.usecase.ProcessSaleUseCase
import com.dantsu.escposprinter.EscPosPrinter
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections
import com.tzh.sme.ui.pos.check_out.CheckOutEvent
import com.tzh.sme.ui.pos.check_out.CheckOutUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@HiltViewModel
class PosViewModel @Inject constructor(
    private val repository: StockRepository,
    private val processSaleUseCase: ProcessSaleUseCase,
    private val authRepository: AuthRepository
) : ViewModel() {

    val user = authRepository.currentUser.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )


    private val _posUiState = MutableStateFlow(PosUiState())
    val posUiState: StateFlow<PosUiState> = _posUiState.asStateFlow()

    private val _checkOutUiState = MutableStateFlow(CheckOutUiState())
    val checkOutUiState: StateFlow<CheckOutUiState> = _checkOutUiState.asStateFlow()
    val cartItem = mutableStateListOf<CartItem>()

     val categories = repository.getAllCategories().map { list ->
        listOf(defaultCategory) + list
    }.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), listOf(defaultCategory)
    )
    val productList = combine(posUiState, repository.getAllProducts()) { currentUiState, products ->
        val category = currentUiState.selectedCategory
        val query = currentUiState.searchQuery
        products.filter { product ->
            product.quantity > 0 && (category.name == "All" || product.categoryId == category.id) && (query.isEmpty() || product.name.contains(
                query, ignoreCase = true
            ) || product.barcode.contains(query))
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

//    @OptIn(ExperimentalCoroutinesApi::class)
//    val uiState: StateFlow<PosUiState> = combine(
//        combine(
//            _searchQuery,
//            _selectedCategory
//        ) { query, category ->
//            query to category
//        }.flatMapLatest { (query, category) ->
//            repository.getAllProducts().map { products ->
//                products.filter { product ->
//                    product.quantity > 0 && (category.name == "All" || product.categoryId == category.id) && (query.isEmpty() || product.name.contains(
//                        query, ignoreCase = true
//                    ) || product.barcode.contains(query))
//                }
//            }
//        },
//        _categories,
//        _selectedCategory,
//        _searchQuery,
//        authRepository.currentUser,
//        _completedTransaction
//    ) {
//        PosUiState(
//            products = it[0] as List<ProductModel>,
//            categories = it[1] as List<CategoryModel>,
//            selectedCategory = it[2] as CategoryModel,
//            searchQuery = it[4] as String,
//            user = it[6] as? User,
//            completedTransaction = it[7] as? Pair<TransactionModel, List<TransactionItemModel>>
//        )
//    }.stateIn(
//        scope = viewModelScope,
//        started = SharingStarted.WhileSubscribed(5000),
//        initialValue = PosUiState()
//    )


    fun onPosEvent(event: PosEvent) {
        when (event) {
            is PosEvent.OnAddToCart -> {
                addToCart(event.product)
            }

            is PosEvent.OnBarcodeScanned -> {
                onBarcodeScanned(event.barcode)
            }

            is PosEvent.OnRemoveFromCart -> {
                removeFromCart(event.product)
            }

            is PosEvent.OnSearchQueryChange -> {
                onSearchQueryChange(event.query)
            }

            is PosEvent.OnSelectedCategory -> {
                onCategorySelect(event.category)
            }
        }
    }

    fun onCheckOutEvent(event: CheckOutEvent) {
        when (event) {
            is CheckOutEvent.OnAddToCart -> {
                addToCart(event.product)
            }

            is CheckOutEvent.OnRemoveFromCart -> {
                removeFromCart(event.product)
            }

            is CheckOutEvent.OnDiscountChange -> {
                onDiscountChange(event.discount)
            }

            CheckOutEvent.OnCheckOut -> {
                checkout()
            }

            is CheckOutEvent.OnPrint -> {
                printReceipt(event.transaction, event.items)
            }
        }
    }

    private fun onSearchQueryChange(query: String) {
        _posUiState.update {
            it.copy(
                searchQuery = query
            )
        }
    }

    private fun onCategorySelect(category: CategoryModel) {
        _posUiState.update {
            it.copy(
                selectedCategory = category
            )
        }
    }


    private fun onBarcodeScanned(barcode: String) {
        viewModelScope.launch {
            val product = productList.value.find { it.barcode == barcode }
            if (product != null) {
                addToCart(product)
            }
        }
    }

    private fun addToCart(product: ProductModel) {
        if (product.quantity <= 0) return

        val index = cartItem.indexOfFirst { it.product.id == product.id }
        if (index != -1) {
            val maxQuantity = product.quantity
            if (cartItem[index].quantity >= maxQuantity) return
            cartItem[index] = cartItem[index].copy(quantity = cartItem[index].quantity + 1)
        } else {
            cartItem.add(CartItem(product, 1))
        }
    }

    private fun removeFromCart(product: ProductModel) {
        val index = cartItem.indexOfFirst { it.product.id == product.id }
        if (index != -1) {
            if (cartItem[index].quantity > 1) {
                cartItem[index] =
                    cartItem[index].copy(quantity = cartItem[index].quantity - 1)
            } else {
                cartItem.removeAt(index)
            }
        }
    }

    private fun onDiscountChange(discount: Double) {
        _checkOutUiState.update {
            it.copy(
                discount = discount
            )
        }
    }

    private fun checkout() {
        viewModelScope.launch {
            _checkOutUiState.update {
                it.copy(isCheckOutInProgress = true)
            }
            val result = processSaleUseCase(cartItem, _checkOutUiState.value.discount)
            result.onSuccess { (transaction, items) ->
                cartItem.clear()
                _checkOutUiState.update {
                    it.copy(
                        isCheckOutInProgress = false,
                        printMessage = "Receipt printed successfully",
                        completedTransaction = transaction to items
                    )
                }
//                printReceipt(transaction, items)
            }.onFailure { e ->
                _checkOutUiState.update {
                    it.copy(
                        errorMessage = e.message ?: "An error occurred during checkout"
                    )
                }
            }
            _checkOutUiState.update {
                it.copy(isCheckOutInProgress = false)
            }
        }
    }


    @SuppressLint("MissingPermission")
    private fun printReceipt(transaction: TransactionModel, items: List<TransactionItemModel>) {
        viewModelScope.launch {
            _checkOutUiState.update {
                it.copy(
                    isPrinting = true,
                    printError = "",
                    printMessage = ""
                )
            }
            try {
                // Correct way to get the paired printers connection
                val printers = BluetoothPrintersConnections.selectFirstPaired()
                if (printers != null) {
                    val printer = EscPosPrinter(printers, 203, 48f, 32)
                    val date = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(
                        Date(transaction.timestamp)
                    )

                    var receiptText =
                        "[C]<b>SME POS</b>\n" + "[C]--------------------------------\n" + "[L]Date: $date\n" + "[L]ID: ${
                            transaction.transactionId.takeLast(8)
                        }\n" + "[C]--------------------------------\n"

                    items.forEach { item ->
                        val itemTotal = item.quantity * item.priceAtTime
                        receiptText += "[L]${item.productName}\n" + "[L]  ${item.quantity} x $${
                            String.format(
                                Locale.US, "%.2f", item.priceAtTime
                            )
                        } [R]$${String.format(Locale.US, "%.2f", itemTotal)}\n"
                    }

                    receiptText += "[C]--------------------------------\n" + "[L]TOTAL [R]$${
                        String.format(
                            Locale.US, "%.2f", transaction.totalAmount
                        )
                    }\n" + "[L]DISCOUNT [R]$${
                        String.format(
                            Locale.US, "%.2f", transaction.discount
                        )
                    }\n" + "[L]<b>NET TOTAL</b> [R]<b>$${
                        String.format(
                            Locale.US, "%.2f", transaction.netAmount
                        )
                    }</b>\n" + "[C]--------------------------------\n" + "[C]Thank you for your visit!\n"

                    printer.printFormattedText(receiptText)
                    _checkOutUiState.update {
                        it.copy(isPrinting = false, printMessage = "Receipt printed successfully")
                    }
                } else {
                    _checkOutUiState.update {
                        it.copy(
                            isPrinting = false,
                            printError = "No paired bluetooth printer found"
                        )
                    }
                }
            } catch (e: Exception) {
                _checkOutUiState.update {
                    it.copy(isPrinting = false, printError = "Printing failed: ${e.message}")
                }
            }
        }
    }
    fun defaultCheckOutUiState(){
        _checkOutUiState.update { CheckOutUiState() }

    }
    fun dismissSuccessDialog() {
        _checkOutUiState.update {
            it.copy(
                completedTransaction = null,
                printError = "",
                printMessage = ""
            )
        }
    }

    fun clearPrintMessage() {
        _checkOutUiState.update {
            it.copy(
                printMessage = ""
            )
        }
    }

    fun clearErrorMessage() {
        _checkOutUiState.update {
            it.copy(
                errorMessage = ""
            )
        }
    }
}
