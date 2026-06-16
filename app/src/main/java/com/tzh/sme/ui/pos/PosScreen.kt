package com.tzh.sme.ui.pos

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.tzh.sme.R
import com.tzh.sme.data.DefaultData.defaultCategory
import com.tzh.sme.data.model.CategoryModel
import com.tzh.sme.data.model.ProductModel
import com.tzh.sme.data.model.ShopModel
import com.tzh.sme.domain.model.CartItem
import com.tzh.sme.ui.inventory.BarcodeScannerView
import com.tzh.sme.ui.theme.SMETheme
import java.util.Locale

@Composable
fun PosScreen(
    viewModel: PosViewModel,
    onNavigateToCheckout: () -> Unit,
    onOpenDrawer: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val filterProductList by viewModel.filterProduct.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel.sideEffect) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                is PosUiSideEffect.ShowError -> snackbarHostState.showSnackbar(effect.message)
                is PosUiSideEffect.ShowToast -> snackbarHostState.showSnackbar(effect.message)
                PosUiSideEffect.NavigateToCheckout -> onNavigateToCheckout()
                PosUiSideEffect.NavigateBack -> {}
            }
        }
    }

    PosContent(
        uiState = uiState,
        filterProductList =filterProductList,
        onIntent = viewModel::sendIntent,
        onNavigateToCheckout = onNavigateToCheckout,
        onOpenDrawer = onOpenDrawer,
        snackbarHostState = snackbarHostState
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosContent(
    uiState: PosUiState,
    filterProductList: List<ProductModel>,
    onIntent: (PosUiIntent) -> Unit,
    onNavigateToCheckout: () -> Unit,
    onOpenDrawer: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    var showScanner by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) showScanner = true
    }

    val searchBar = @Composable { modifier: Modifier ->
        DockedSearchBar(
            inputField = {
                SearchBarDefaults.InputField(
                    query = uiState.searchQuery,
                    onQueryChange = { onIntent(PosUiIntent.UpdateSearchQuery(it)) },
                    onSearch = {},
                    expanded = false,
                    onExpandedChange = {},
                    placeholder = { Text("Search by name or barcode") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                )
            },
            expanded = false,
            onExpandedChange = {},
            modifier = modifier,
        ) {}
    }

    val scanButton = @Composable {
        IconButton(onClick = {
            val permissionCheckResult =
                ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
            if (permissionCheckResult == PackageManager.PERMISSION_GRANTED) {
                showScanner = true
            } else {
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }) { Icon(Icons.Default.QrCodeScanner, "Scan Barcode") }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                uiState.currentShop?.name ?: "POS",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.LocationOn,
                                    null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(Modifier.width(2.dp))
                                Text(
                                    text = uiState.currentShop?.address ?: "Unknown",
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                        if (isLandscape) {
                            searchBar(
                                Modifier
                                    .weight(1f)
                                    .padding(end = 4.dp, start = 8.dp)
                            )
                            scanButton()
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
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (!isLandscape) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        searchBar(
                            Modifier
                                .weight(1f)
                                .padding(end = 4.dp)
                        )
                        scanButton()
                    }
                }

                CategoryList(
                    categories = uiState.categories,
                    selectedCategory = uiState.selectedCategory,
                    onCategorySelect = { onIntent(PosUiIntent.SelectCategory(it)) })

                ProductGrid(
                    products = filterProductList,
                    cart = uiState.cartItems,
                    getCategory = { id ->
                        uiState.categories.find { it.id == id } ?: CategoryModel()
                    },
                    onAddToCart = { onIntent(PosUiIntent.AddToCart(it)) },
                    onRemoveFromCart = { onIntent(PosUiIntent.RemoveFromCart(it)) },
                )
            }

            if (uiState.cartItems.isNotEmpty()) {
                Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                    CartPopup(
                        itemCount = uiState.cartItems.sumOf { it.quantity },
                        totalPrice = uiState.cartItems.sumOf { it.totalPrice },
                        onCartClick = onNavigateToCheckout
                    )
                }
            }

            if (showScanner) {
                BarcodeScannerView(onBarcodeScanned = { barcode ->
                    onIntent(PosUiIntent.BarcodeScanned(barcode))
                    showScanner = false
                }, onClose = { showScanner = false })
            }
        }
    }
}

@Composable
fun CategoryList(
    categories: List<CategoryModel>,
    selectedCategory: CategoryModel,
    onCategorySelect: (CategoryModel) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        lazyItems(categories) { category ->
            val isSelected = category.name == selectedCategory.name
            FilterChip(
                selected = isSelected,
                onClick = { onCategorySelect(category) },
                label = { Text(category.name, style = MaterialTheme.typography.labelLarge) },
                leadingIcon = { Text(category.icon, fontSize = 16.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    selectedBorderColor = MaterialTheme.colorScheme.secondary
                ),
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}

@Composable
fun ProductGrid(
    products: List<ProductModel>,
    cart: List<CartItem>,
    getCategory: (id: String) -> CategoryModel,
    onAddToCart: (ProductModel) -> Unit,
    onRemoveFromCart: (ProductModel) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 150.dp),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 80.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        gridItems(products, key = { it.id }) { product ->
            val cartItem = cart.find { it.product.id == product.id }
            ProductCard(
                product = product,
                quantityInCart = cartItem?.quantity ?: 0,
                getCategory = getCategory,
                onAddToCart = { onAddToCart(product) },
                onRemoveFromCart = { onRemoveFromCart(product) })
        }
    }
}

@Composable
fun ProductCard(
    product: ProductModel,
    quantityInCart: Int,
    getCategory: (id: String) -> CategoryModel,
    onAddToCart: () -> Unit,
    onRemoveFromCart: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .aspectRatio(1.1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                product.imagePaths.firstOrNull()?.let {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current).data(it).crossfade(true)
                            .build(),
                        placeholder = painterResource(R.drawable.placeholder),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } ?: Image(
                    painter = painterResource(R.drawable.placeholder),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                if (quantityInCart > 0) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(26.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = quantityInCart.toString(),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp,
                    modifier = Modifier.heightIn(min = 36.dp)
                )
                Text(
                    text = getCategory(product.categoryId).name,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$${String.format(Locale.US, "%.2f", product.sellPrice)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Visible,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (quantityInCart > 0) {
                            Surface(
                                onClick = onRemoveFromCart,
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(30.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Remove,
                                        null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                        }
                        Surface(
                            onClick = onAddToCart,
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Add,
                                    null,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CartPopup(itemCount: Int, totalPrice: Double, onCartClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .widthIn(max = 600.dp)
            .fillMaxWidth()
            .height(68.dp),
        color = MaterialTheme.colorScheme.primary,
        shape = RoundedCornerShape(34.dp),
        shadowElevation = 12.dp,
        onClick = onCartClick
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f),
                        shape = CircleShape
                    ), contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.ShoppingBasket,
                    null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                Text(
                    text = stringResource(R.string.items_selected, itemCount),
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    text = "$${String.format(Locale.US, "%.2f", totalPrice)}",
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.height(36.dp)
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "VIEW CART",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PosScreenPreview() {
    SMETheme {
        PosContent(
            uiState = PosUiState(
                currentShop = ShopModel(name = "My Shop", address = "Yangon"),
                categories = listOf(defaultCategory)
            ),
            filterProductList = listOf(

            ),
            onIntent = {},
            onNavigateToCheckout = {},
            onOpenDrawer = {},
            snackbarHostState = SnackbarHostState()
        )
    }
}
