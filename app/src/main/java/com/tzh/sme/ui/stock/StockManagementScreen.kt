package com.tzh.sme.ui.stock

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.tzh.sme.BuildConfig
import com.tzh.sme.R
import com.tzh.sme.ui.inventory.BarcodeScannerView
import com.tzh.sme.ui.pos.CategoryList
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockManagementScreen(
    viewModel: StockViewModel,
    onNavigateToAddProduct: () -> Unit,
    onNavigateToEditProduct: (String) -> Unit,
    onOpenDrawer: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted -> if (isGranted) viewModel.sendIntent(StockUiIntent.ToggleScanner(true)) }



    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.windowInsetsPadding(
                    WindowInsets.safeDrawing.only(
                        WindowInsetsSides.Horizontal
                    )
                ),
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isLandscape) {
                            Column {
                                stringResource(R.string.stock_management).split(" ").forEach {
                                    Text(
                                        it,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            lineHeight = 16.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }
                            DockedSearchBar(
                                inputField = {
                                    SearchBarDefaults.InputField(
                                        query = uiState.searchQuery,
                                        onQueryChange = {
                                            viewModel.sendIntent(
                                                StockUiIntent.UpdateSearchQuery(
                                                    it
                                                )
                                            )
                                        },
                                        onSearch = {}, expanded = false, onExpandedChange = {},
                                        placeholder = { Text("Search by name or barcode") },
                                        leadingIcon = { Icon(Icons.Default.Search, null) },
                                    )
                                },
                                expanded = false, onExpandedChange = {},
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 8.dp),
                            ) {}
                            Spacer(Modifier.width(8.dp))
                            IconButton(onClick = {
                                if (ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.CAMERA
                                    ) == PackageManager.PERMISSION_GRANTED
                                ) {
                                    viewModel.sendIntent(StockUiIntent.ToggleScanner(true))
                                } else permissionLauncher.launch(Manifest.permission.CAMERA)
                            }) { Icon(Icons.Default.QrCodeScanner, "Scan Barcode") }
                        } else {
                            Text(stringResource(R.string.stock_management))
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(
                            Icons.AutoMirrored.Filled.Notes,
                            "Menu"
                        )
                    }
                }
            )
        },
    ) { padding ->

        if (uiState.showScanner) {
            BarcodeScannerView(
                onBarcodeScanned = { barcode ->
                    viewModel.sendIntent(
                        StockUiIntent.BarcodeScanned(
                            barcode
                        )
                    )
                },
                modifier = Modifier
                    .padding(padding),
                onClose = { viewModel.sendIntent(StockUiIntent.ToggleScanner(false)) }
            )

        } else {

            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(horizontal = 8.dp)
            ) {
                if (uiState.isLoading) {
                    Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator() }
                } else if (uiState.error != null) {
                    Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) { Text(uiState.error!!) }
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        if (!isLandscape) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                DockedSearchBar(
                                    inputField = {
                                        SearchBarDefaults.InputField(
                                            query = uiState.searchQuery,
                                            onQueryChange = {
                                                viewModel.sendIntent(
                                                    StockUiIntent.UpdateSearchQuery(
                                                        it
                                                    )
                                                )
                                            },
                                            onSearch = {},
                                            expanded = false,
                                            onExpandedChange = {},
                                            placeholder = { Text("Search by name or barcode") },
                                            leadingIcon = { Icon(Icons.Default.Search, null) },
                                        )
                                    },
                                    expanded = false, onExpandedChange = {},
                                    modifier = Modifier.weight(1f),
                                ) {}
                                Spacer(Modifier.width(12.dp))
                                IconButton(onClick = {
                                    if (ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.CAMERA
                                        ) == PackageManager.PERMISSION_GRANTED
                                    ) {
                                        viewModel.sendIntent(StockUiIntent.ToggleScanner(true))
                                    } else permissionLauncher.launch(Manifest.permission.CAMERA)
                                }) { Icon(Icons.Default.QrCodeScanner, "Scan Barcode") }
                            }
                        }
                        CategoryList(
                            categories = uiState.categories,
                            selectedCategory = uiState.selectedCategory,
                            onCategorySelect = {
                                viewModel.sendIntent(
                                    StockUiIntent.SelectCategory(it)
                                )
                            },
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        if (uiState.products.isEmpty() && uiState.searchQuery.isEmpty()) {
                            EmptyStockState(onAddClick = onNavigateToAddProduct)
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 80.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(uiState.products) { product ->
                                    val firstImage = product.imagePaths.firstOrNull()
                                        ?.let { if (it.startsWith("http")) it else BuildConfig.FILE_SERVER_URL + it }
                                    StockItemCard(
                                        name = product.name,
                                        qty = product.quantity,
                                        price = product.sellPrice,
                                        barcode = product.barcode,
                                        imagePath = firstImage,
                                        minStock = product.minStockLevel,
                                        onClick = { onNavigateToEditProduct(product.id) })
                                }
                            }
                        }
                    }
                }
                FloatingActionButton(
                    onClick = onNavigateToAddProduct,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = stringResource(R.string.add_product)
                    )
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
            Icon(Icons.Default.Add, null)
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
    minStock: Int,
    onClick: () -> Unit
) {
    val isLowStock = qty <= minStock && minStock > 0
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
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
                        color = if (isLowStock) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = "Qty: $qty",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isLowStock) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = if (isLowStock) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}
