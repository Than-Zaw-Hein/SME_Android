package com.tzh.sme.ui.stock

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.tzh.sme.R
import com.tzh.sme.data.remote.BASE_URL
import com.tzh.sme.ui.inventory.BarcodeScannerView
import com.tzh.sme.ui.pos.PosUiState
import androidx.compose.material.icons.automirrored.filled.Notes
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockManagementScreen(
    viewModel: StockViewModel,
    onNavigateToAddProduct: () -> Unit,
    onNavigateToEditProduct: (String) -> Unit,
    onOpenDrawer: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) viewModel.onStockEvent(StockEvent.OnToggleScanner(true))
    }

    if (uiState.showScanner) {
        BarcodeScannerView(
            onBarcodeScanned = { barcode ->
                viewModel.onStockEvent(StockEvent.OnBarcodeScanned(barcode))
            },
            onClose = { viewModel.onStockEvent(StockEvent.OnToggleScanner(false)) }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(R.string.stock_management))
                },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(
                            Icons.AutoMirrored.Filled.Notes,
                            contentDescription = "Menu"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToAddProduct) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_product))
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 12.dp)
                .fillMaxSize()
        ) {
            if (uiState.isLoading) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.error != null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(uiState.error!!)
                }
            } else {
                if (uiState.products.isEmpty() && uiState.searchQuery.isEmpty()) {
                    EmptyStockState(onAddClick = onNavigateToAddProduct)
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                DockedSearchBar(
                                    inputField = {
                                        SearchBarDefaults.InputField(
                                            query = uiState.searchQuery,
                                            onQueryChange = { viewModel.onStockEvent(StockEvent.OnSearchQueryChange(it)) },
                                            onSearch = {},
                                            expanded = false,
                                            onExpandedChange = {},
                                            placeholder = { Text("Search by name or barcode") },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Default.Search,
                                                    contentDescription = null
                                                )
                                            },
                                        )
                                    },
                                    expanded = false,
                                    onExpandedChange = {},
                                    modifier = Modifier.weight(1f),
                                ) {}

                                Spacer(Modifier.width(12.dp))
                                IconButton(onClick = {
                                    val permissionCheckResult =
                                        ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.CAMERA
                                        )
                                    if (permissionCheckResult == PackageManager.PERMISSION_GRANTED) {
                                        viewModel.onStockEvent(StockEvent.OnToggleScanner(true))

                                    } else {
                                        permissionLauncher.launch(Manifest.permission.CAMERA)
                                    }
                                }) {
                                    Icon(
                                        Icons.Default.QrCodeScanner,
                                        contentDescription = "Scan Barcode"
                                    )
                                }
                            }
                        }

                        items(uiState.products) { product ->
                            val firstImage = product.imagePaths.firstOrNull()?.let { path ->
                                if (path.startsWith("http")) path else BASE_URL + path
                            }
                            StockItemCard(
                                name = product.name,
                                qty = product.quantity,
                                price = product.price,
                                barcode = product.barcode,
                                imagePath = firstImage,
                                onClick = { onNavigateToEditProduct(product.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyStockState(onAddClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Inventory2,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Your stock list is empty",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Start adding products to manage your inventory effectively.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onAddClick) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add your first item")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockItemCard(
    name: String,
    qty: Int,
    price: Double,
    barcode: String,
    imagePath: String?,
    onClick: () -> Unit
) {
    val isLowStock = qty < 5
    val quantityColor = if (isLowStock) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }
    val onQuantityColor = if (isLowStock) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding( vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Product Image
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (imagePath != null) {
                    AsyncImage(
                        model = imagePath,
                        contentDescription = name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Inventory2,
                        contentDescription = null,
                        modifier = Modifier
                            .size(32.dp)
                            .align(Alignment.Center),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "$${String.format(Locale.US, "%,.2f", price)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = " $barcode",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = quantityColor
                    ) {
                        Text(
                            text = "Qty: $qty",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = onQuantityColor,
                            fontWeight = if (isLowStock) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}
