package com.tzh.sme.ui.pos

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.tzh.sme.R
import com.tzh.sme.data.local.entities.ProductEntity
import com.tzh.sme.domain.model.CartItem
import com.tzh.sme.domain.repository.User
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
        onCategorySelect = viewModel::onCategorySelect,
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
    onCategorySelect: (String) -> Unit,
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
            Column(
                modifier = Modifier
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { /* Menu */ }) {
                        Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = null)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = Color(0xFFE96145),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            if (uiState is PosUiState.Success) {
                                uiState.user?.address.toString()
                            } else {
                                "Unknown"
                            },
                            color = Color(0xFFE96145),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(onClick = { /* Settings */ }) {
                        Icon(Icons.Default.Person, contentDescription = null)
                    }
                }

                Spacer(Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = (uiState as? PosUiState.Success)?.searchQuery ?: "",
                        onValueChange = onSearchQueryChange,
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Search") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search, contentDescription = null
                            )
                        },
                        shape = RoundedCornerShape(24.dp),

                        )
                    Spacer(Modifier.width(12.dp))
                    IconButton(onClick = {
                        val permissionCheckResult =
                            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                        if (permissionCheckResult == PackageManager.PERMISSION_GRANTED) {
                            showScanner = true
                        } else {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    }) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan Barcode")
                    }
                }
            }
        }) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (uiState) {
                is PosUiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                is PosUiState.Success -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        CategoryList(
                            categories = uiState.categories,
                            selectedCategory = uiState.selectedCategory,
                            onCategorySelect = onCategorySelect
                        )

                        ProductGrid(
                            products = uiState.products, onAddToCart = onAddToCart
                        )
                    }

                    if (uiState.cart.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 16.dp)
                        ) {
                            CartPopup(
                                itemCount = uiState.cart.sumOf { it.quantity },
                                totalPrice = uiState.cart.sumOf { it.totalPrice },
                                onCartClick = onNavigateToCheckout
                            )
                        }
                    }
                }

                is PosUiState.Error -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(uiState.message, color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            if (showScanner) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                ) {
                    BarcodeScannerView(onBarcodeDetected = { barcode ->
                        onBarcodeScanned(barcode)
                        showScanner = false
                    })

                    Button(
                        onClick = { showScanner = false },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 32.dp)
                    ) {
                        Text(stringResource(R.string.close_scanner))
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryList(
    categories: List<String>, selectedCategory: String, onCategorySelect: (String) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        items(categories) { category ->
            val isSelected = category == selectedCategory
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onCategorySelect(category) }) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isSelected) Color(0xFFE96145) else Color.Transparent
                    ),
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        // Using icons based on name (mock)
                        val icon = when (category.lowercase()) {
                            "snack" -> Icons.Default.Fastfood
                            "food" -> Icons.Default.Restaurant
                            "drink" -> Icons.Default.LocalDrink
                            "fruits" -> Icons.Default.BakeryDining
                            else -> Icons.Default.AllInclusive
                        }
                        Icon(
                            icon,
                            contentDescription = null,
                            tint = if (isSelected) Color(0xFFE96145) else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = category,
                    fontSize = 12.sp,
                    color = if (isSelected) Color(0xFFE96145) else MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun ProductGrid(
    products: List<ProductEntity>, onAddToCart: (ProductEntity) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(products) { product ->
            ProductCard(product = product, onAddToCart = { onAddToCart(product) })
        }
    }
}

@Composable
fun ProductCard(product: ProductEntity, onAddToCart: () -> Unit) {
    Card(
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onAddToCart() }) {
        Column(
            modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFFF9F9F9)), contentAlignment = Alignment.Center
            ) {

                product.imagePaths.firstOrNull()?.let {

                    AsyncImage(
                        placeholder = painterResource(R.drawable.placeholder),
                        model = it,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } ?: Image(
                    painter = painterResource(R.drawable.placeholder),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
                // Discount Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFDE8E4),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 10.dp)
                ) {
                    Text(
                        text = "-10%",
                        fontSize = 10.sp,
                        color = Color(0xFFE96145),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = product.name,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
            )

            Text(
                text = product.category, // Mock subtext
                fontSize = 11.sp,
                style = MaterialTheme.typography.titleSmall.copy(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                ),

                )

            Spacer(Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "$",
                    fontSize = 12.sp,
                    color = Color(0xFFE96145),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = String.format("%.2f", product.price).replace(".", ","),
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    ),
                )
            }
        }
    }
}

@Composable
fun CartPopup(
    itemCount: Int, totalPrice: Double, onCartClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(32.dp))
            .clickable { onCartClick() }) {
        // Blur background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .blur(20.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.2f), Color.White.copy(alpha = 0.1f)
                        )
                    )
                )
        )

        // Glass surface with border and content
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.DarkGray.copy(alpha = 0.7f), // Semi-transparent dark background
            shape = RoundedCornerShape(32.dp),
            shadowElevation = 0.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(
                        width = 0.5.dp, brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.5f), Color.Transparent
                            )
                        ), shape = RoundedCornerShape(32.dp)
                    )
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .fillMaxHeight(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.items_selected, itemCount),
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "$${String.format("%.2f", totalPrice)}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                    Spacer(Modifier.width(16.dp))
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFE96145),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.ShoppingBasket,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PosScreenPreview() {
    val sampleProducts = listOf(
        ProductEntity(1, "123", "Noodles Ramen", "Spicy", 5.35, 10, "Food"),
        ProductEntity(2, "456", "Dumplings", "Beef", 3.27, 20, "Food"),
        ProductEntity(3, "789", "Beef Burger", "Cheese", 6.50, 15, "Food"),
        ProductEntity(4, "012", "Pizza Sicilia", "Large", 9.66, 5, "Food")
    )
    SMETheme {
        PosContent(
            uiState = PosUiState.Success(
                products = sampleProducts,
                categories = listOf("All", "Snack", "Food", "Drink", "Fruits"),
                selectedCategory = "All",
                cart = listOf(CartItem(sampleProducts[0], 2)),
                searchQuery = "",
                user = User(
                    displayName = "Maung Maung",
                    email = "william.henry.harrison@example-pet-store.com",
                    id = "",
                    phone = "08123456789",
                    address = "Yangon"

                )
            ),
            windowWidthSizeClass = WindowWidthSizeClass.Compact,
            onSearchQueryChange = {},
            onCategorySelect = {},
            onBarcodeScanned = {},
            onAddToCart = {},
            onRemoveFromCart = {},
            onCheckout = {},
            onNavigateToCheckout = {})
    }
}
