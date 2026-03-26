package com.tzh.sme.ui.pos

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tzh.sme.R
import com.tzh.sme.data.local.entities.ProductEntity
import com.tzh.sme.domain.model.CartItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckOutScreen(
    viewModel: PosViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
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
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(stringResource(R.string.order_summary), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))
            
            if (uiState is PosUiState.Success) {
                val state = uiState as PosUiState.Success
                CartList(
                    cart = state.cart,
                    onAdd = viewModel::addToCart,
                    onRemove = viewModel::removeFromCart,
                    modifier = Modifier.weight(1f)
                )

                Spacer(Modifier.height(16.dp))

                CheckoutSection(
                    cart = state.cart,
                    isCheckoutInProgress = state.isCheckoutInProgress,
                    onCheckout = {
                        viewModel.checkout()
                        // Optionally navigate back or show success
                    }
                )
            }
        }
    }
}



@Composable
fun CartList(
    cart: List<CartItem>,
    onAdd: (ProductEntity) -> Unit,
    onRemove: (ProductEntity) -> Unit,
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
                    IconButton(onClick = { onAdd(item.product) }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                    }
                }
            }
        }
    }
}

@Composable
fun CheckoutSection(
    cart: List<CartItem>,
    isCheckoutInProgress: Boolean,
    onCheckout: () -> Unit
) {
    val total = cart.sumOf { it.totalPrice }
    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Total", style = MaterialTheme.typography.headlineSmall)
            Text("$${String.format("%.2f", total)}", style = MaterialTheme.typography.headlineSmall)
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onCheckout,
            modifier = Modifier.fillMaxWidth(),
            enabled = cart.isNotEmpty() && !isCheckoutInProgress
        ) {
            if (isCheckoutInProgress) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text("CHECKOUT")
            }
        }
    }
}
