package com.tzh.sme.ui.pos

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

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
                title = { Text("Checkout") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            Text("Order Summary", style = MaterialTheme.typography.titleLarge)
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
