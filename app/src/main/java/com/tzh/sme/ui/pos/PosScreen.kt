package com.tzh.sme.ui.pos

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.tzh.sme.data.local.entities.ProductEntity
import com.tzh.sme.domain.model.CartItem
import com.tzh.sme.ui.inventory.BarcodeScannerView
import com.tzh.sme.ui.theme.SMETheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreen(
    viewModel: PosViewModel,
    windowWidthSizeClass: WindowWidthSizeClass,
    onNavigateToCheckout: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
    PosContent(
        uiState = uiState,
        windowWidthSizeClass = windowWidthSizeClass,
        onSearchQueryChange = viewModel::onSearchQueryChange,
        onBarcodeScanned = viewModel::onBarcodeScanned,
        onAddToCart = viewModel::addToCart,
        onRemoveFromCart = viewModel::removeFromCart,
        onCheckout = viewModel::checkout,
        onNavigateToCheckout = onNavigateToCheckout
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosContent(
    uiState: PosUiState,
    windowWidthSizeClass: WindowWidthSizeClass,
    onSearchQueryChange: (String) -> Unit,
    onBarcodeScanned: (String) -> Unit,
    onAddToCart: (ProductEntity) -> Unit,
    onRemoveFromCart: (ProductEntity) -> Unit,
    onCheckout: () -> Unit,
    onNavigateToCheckout: () -> Unit
) {
    var showScanner by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) showScanner = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SME POS") },
                actions = {
                    IconButton(onClick = {
                        val permissionCheckResult = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                        if (permissionCheckResult == PackageManager.PERMISSION_GRANTED) {
                            showScanner = true
                        } else {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    }) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan Barcode")
                    }
                    IconButton(onClick = onNavigateToCheckout) {
                        BadgedBox(
                            badge = {
                                if (uiState is PosUiState.Success && uiState.cart.isNotEmpty()) {
                                    Badge { Text("${uiState.cart.size}") }
                                }
                            }
                        ) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = "Cart")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            when (uiState) {
                is PosUiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is PosUiState.Success -> {
                    if (windowWidthSizeClass == WindowWidthSizeClass.Expanded) {
                        TabletLayout(
                            state = uiState,
                            onSearchQueryChange = onSearchQueryChange,
                            onAddToCart = onAddToCart,
                            onRemoveFromCart = onRemoveFromCart,
                            onCheckout = onCheckout,
                            padding = innerPadding
                        )
                    } else {
                        MobileLayout(
                            state = uiState,
                            onSearchQueryChange = onSearchQueryChange,
                            onAddToCart = onAddToCart,
                            padding = innerPadding
                        )
                    }
                }
                is PosUiState.Error -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(uiState.message, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
            
            if (showScanner) {
                Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                    BarcodeScannerView(onBarcodeDetected = { barcode ->
                        onBarcodeScanned(barcode)
                        showScanner = false
                    })
                    
                    Button(
                        onClick = { showScanner = false },
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp)
                    ) {
                        Text("Close Scanner")
                    }
                }
            }
        }
    }
}

@Composable
fun TabletLayout(
    state: PosUiState.Success,
    onSearchQueryChange: (String) -> Unit,
    onAddToCart: (ProductEntity) -> Unit,
    onRemoveFromCart: (ProductEntity) -> Unit,
    onCheckout: () -> Unit,
    padding: PaddingValues
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(16.dp)
        ) {
            SearchBar(state.searchQuery, onSearchQueryChange)
            Spacer(Modifier.height(16.dp))
            ProductGrid(state.products, onAddToCart)
        }

        VerticalDivider()

        Column(
            modifier = Modifier
                .width(350.dp)
                .padding(16.dp)
        ) {
            Text("Current Order", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))
            CartList(
                cart = state.cart,
                onAdd = onAddToCart,
                onRemove = onRemoveFromCart,
                modifier = Modifier.weight(1f)
            )
            CheckoutSection(state.cart, state.isCheckoutInProgress, onCheckout)
        }
    }
}

@Composable
fun MobileLayout(
    state: PosUiState.Success,
    onSearchQueryChange: (String) -> Unit,
    onAddToCart: (ProductEntity) -> Unit,
    padding: PaddingValues
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp)
    ) {
        SearchBar(state.searchQuery, onSearchQueryChange)
        Spacer(Modifier.height(16.dp))
        ProductGrid(state.products, onAddToCart)
    }
}

@Composable
fun SearchBar(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("Search product or scan barcode...") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        shape = MaterialTheme.shapes.medium
    )
}

@Composable
fun ProductGrid(products: List<ProductEntity>, onProductClick: (ProductEntity) -> Unit) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(products) { product ->
            ElevatedCard(
                onClick = { onProductClick(product) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(product.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        Text(product.barcode, style = MaterialTheme.typography.bodySmall)
                    }
                    Text("$${product.price}", style = MaterialTheme.typography.bodyLarge)
                }
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

@Preview(showBackground = true, widthDp = 400, heightDp = 800)
@Composable
fun PosScreenMobilePreview() {
    val sampleProducts = listOf(
        ProductEntity(1, "123", "Product 1", "Description 1", 10.0, 100, "Category 1"),
        ProductEntity(2, "456", "Product 2", "Description 2", 20.0, 50, "Category 2")
    )
    val sampleCart = listOf(
        CartItem(sampleProducts[0], 2)
    )
    SMETheme {
        PosContent(
            uiState = PosUiState.Success(
                products = sampleProducts,
                cart = sampleCart
            ),
            windowWidthSizeClass = WindowWidthSizeClass.Compact,
            onSearchQueryChange = {},
            onBarcodeScanned = {},
            onAddToCart = {},
            onRemoveFromCart = {},
            onCheckout = {},
            onNavigateToCheckout = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 1280, heightDp = 800)
@Composable
fun PosScreenTabletPreview() {
    val sampleProducts = listOf(
        ProductEntity(1, "123", "Product 1", "Description 1", 10.0, 100, "Category 1"),
        ProductEntity(2, "456", "Product 2", "Description 2", 20.0, 50, "Category 2")
    )
    val sampleCart = listOf(
        CartItem(sampleProducts[0], 2)
    )
    SMETheme {
        PosContent(
            uiState = PosUiState.Success(
                products = sampleProducts,
                cart = sampleCart
            ),
            windowWidthSizeClass = WindowWidthSizeClass.Expanded,
            onSearchQueryChange = {},
            onBarcodeScanned = {},
            onAddToCart = {},
            onRemoveFromCart = {},
            onCheckout = {},
            onNavigateToCheckout = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PosScreenLoadingPreview() {
    SMETheme {
        PosContent(
            uiState = PosUiState.Loading,
            windowWidthSizeClass = WindowWidthSizeClass.Compact,
            onSearchQueryChange = {},
            onBarcodeScanned = {},
            onAddToCart = {},
            onRemoveFromCart = {},
            onCheckout = {},
            onNavigateToCheckout = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PosScreenErrorPreview() {
    SMETheme {
        PosContent(
            uiState = PosUiState.Error("Failed to load products. Please try again."),
            windowWidthSizeClass = WindowWidthSizeClass.Compact,
            onSearchQueryChange = {},
            onBarcodeScanned = {},
            onAddToCart = {},
            onRemoveFromCart = {},
            onCheckout = {},
            onNavigateToCheckout = {}
        )
    }
}
