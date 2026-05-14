package com.tzh.sme.ui.pos.check_out

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tzh.sme.R
import com.tzh.sme.data.model.ProductModel
import com.tzh.sme.domain.model.CartItem
import com.tzh.sme.ui.pos.PosUiState
import com.tzh.sme.ui.pos.PosViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckOutScreen(
    viewModel: PosViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.checkOutUiState.collectAsState()

    val errorMessage = uiState.errorMessage
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.printMessage) {
        if (uiState.printMessage.isNotEmpty()) {
            snackbarHostState.showSnackbar(uiState.printMessage)
            viewModel.clearPrintMessage()
        }
    }
    LaunchedEffect(errorMessage) {
        if (errorMessage.isNotEmpty()) {
            snackbarHostState.showSnackbar(
                errorMessage,
            )
        }
    }
    uiState.completedTransaction?.let { (transaction, items) ->
        CheckOutCompleteDialog(
            transaction = transaction,
            items = items,
            isPrinting = uiState.isPrinting,
            printError = uiState.printError,
            onPrint = {
                viewModel.onCheckOutEvent(event = CheckOutEvent.OnPrint(transaction, items))
            },
            onClose = { viewModel.dismissSuccessDialog() },
            onGoHome = {
                viewModel.dismissSuccessDialog()
                onNavigateBack() // Navigates back to POS Home
            }
        )
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) {
                Snackbar(
                    snackbarData = it,
                    modifier = Modifier.padding(16.dp)
                )
            }
        },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.checkout)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    stringResource(R.string.order_summary),
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(Modifier.height(16.dp))

                CartList(
                    cart = viewModel.cartItem,
                    onAdd = {
                        viewModel.onCheckOutEvent(event = CheckOutEvent.OnAddToCart(it))
                    },
                    onRemove = {
                        viewModel.onCheckOutEvent(event = CheckOutEvent.OnRemoveFromCart(it))
                    },
                    modifier = Modifier.weight(1f)
                )

                Spacer(Modifier.height(16.dp))

                CheckoutSection(
                    cart = viewModel.cartItem,
                    discount = uiState.discount,
                    isCheckoutInProgress = uiState.isCheckOutInProgress,
                    onDiscountChange = {
                        viewModel.onCheckOutEvent(event = CheckOutEvent.OnDiscountChange(it))
                    },
                    onCheckout = {
                        viewModel.onCheckOutEvent(event = CheckOutEvent.OnCheckOut)
                    }
                )
            }

            if (uiState.isCheckOutInProgress) {
                Surface(
                    color = Color.Black.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}


@Composable
fun CartList(
    cart: List<CartItem>,
    onAdd: (ProductModel) -> Unit,
    onRemove: (ProductModel) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(cart) { item ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.product.name, maxLines = 1)
                    Text("$${item.totalPrice}", style = MaterialTheme.typography.bodySmall)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onRemove(item.product) }) {
                        Icon(Icons.Default.Remove, contentDescription = null)
                    }
                    Text("${item.quantity}")
                    IconButton(
                        onClick = { onAdd(item.product) },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor =
                                MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                    }
                }
            }
            HorizontalDivider()
        }
    }
}

@Composable
fun CheckoutSection(
    cart: List<CartItem>,
    discount: Double,
    isCheckoutInProgress: Boolean,
    onDiscountChange: (Double) -> Unit,
    onCheckout: () -> Unit
) {
    val total = cart.sumOf { it.totalPrice }
    val netTotal = total - discount

    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = if (discount == 0.0) "" else discount.toString(),
            onValueChange = {
                val newDiscount = it.toDoubleOrNull() ?: 0.0
                onDiscountChange(newDiscount)
            },
            label = { Text("Discount") },
            modifier = Modifier.fillMaxWidth(),
            prefix = { Text("$") },
            singleLine = true
        )

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Total", style = MaterialTheme.typography.bodyLarge)
            Text("$${String.format("%.2f", total)}", style = MaterialTheme.typography.bodyLarge)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Discount", style = MaterialTheme.typography.bodyLarge)
            Text("-$${String.format("%.2f", discount)}", style = MaterialTheme.typography.bodyLarge)
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Net Total", style = MaterialTheme.typography.headlineSmall)
            Text(
                "$${String.format("%.2f", netTotal)}",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onCheckout,
            modifier = Modifier.fillMaxWidth(),
            enabled = cart.isNotEmpty() && !isCheckoutInProgress
        ) {
            if (isCheckoutInProgress) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("CHECKOUT")
            }
        }
    }
}
