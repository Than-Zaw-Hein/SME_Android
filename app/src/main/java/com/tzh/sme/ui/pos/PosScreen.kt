package com.tzh.sme.ui.pos

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.launch
import com.tzh.sme.BuildConfig
import com.tzh.sme.R
import com.tzh.sme.data.model.CategoryModel
import com.tzh.sme.data.model.ProductModel
import com.tzh.sme.domain.model.CartItem
import com.tzh.sme.domain.repository.User
import com.tzh.sme.ui.inventory.BarcodeScannerView
import com.tzh.sme.ui.theme.SMETheme
import java.util.Locale

enum class SettingsType(
    val label: String,
    val icon: ImageVector,
    val hasBadge: Boolean = false,
    val hasChevron: Boolean = true
) {
    LANGUAGES("Languages", Icons.Default.Public, hasBadge = true),
    CONTACT_US("Contact Us", Icons.Default.Email, hasBadge = true),
    FAQ("FAQ", Icons.AutoMirrored.Filled.HelpOutline, hasBadge = true),
    PRIVACY_POLICY("Privacy Policy", Icons.Default.Folder, hasChevron = true),
    TERMS_OF_SERVICE("Terms of Service", Icons.Default.Folder, hasChevron = true),
    VERSIONS("Versions", Icons.Default.Info, hasChevron = false)
}

@Composable
fun PosScreen(
    viewModel: PosViewModel,
    onNavigateToCheckout: () -> Unit,
    onOpenDrawer: () -> Unit,
) {
    val uiState by viewModel.posUiState.collectAsState()
    val user by viewModel.user.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val productList by viewModel.productList.collectAsState()
    val cartItems = viewModel.cartItem
    PosContent(
        uiState = uiState,
        user = user,
        categories = categories,
        productList = productList,
        cartItems = cartItems,
        onPosEvent = viewModel::onPosEvent,
        onNavigateToCheckout = onNavigateToCheckout,
        onOpenDrawer = onOpenDrawer
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosContent(
    uiState: PosUiState,
    user: User?,
    categories: List<CategoryModel>,
    productList: List<ProductModel>,
    cartItems: List<CartItem>,
    onPosEvent: (PosEvent) -> Unit,
    onNavigateToCheckout: () -> Unit,
    onOpenDrawer: () -> Unit,
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
                    onQueryChange = {
                        onPosEvent(PosEvent.OnSearchQueryChange(it))
                    },
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
            modifier = modifier,
        ) {}
    }

    val scanButton = @Composable {
        IconButton(onClick = {
            val permissionCheckResult =
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CAMERA
                )
            if (permissionCheckResult == PackageManager.PERMISSION_GRANTED) {
                showScanner = true
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isLandscape) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("POS", style = MaterialTheme.typography.titleMedium)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(Modifier.width(2.dp))
                                    Text(
                                        text = user?.address ?: "Unknown",
                                        color = MaterialTheme.colorScheme.secondary,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                            Spacer(Modifier.width(16.dp))
                            searchBar(Modifier.weight(1f))
                        }
                    } else {
                        Column {
                            Text("POS", style = MaterialTheme.typography.titleMedium)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(Modifier.width(2.dp))
                                Text(
                                    text = user?.address ?: "Unknown",
                                    color = MaterialTheme.colorScheme.secondary,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(
                            Icons.AutoMirrored.Filled.Notes,
                            contentDescription = "Menu"
                        )
                    }
                },
                actions = {
                    if (isLandscape) {
                        scanButton()
                    }
                }
            )
        }) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 8.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (!isLandscape) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        searchBar(
                            Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        )
                        scanButton()
                    }
                }

                CategoryList(
                    categories = categories,
                    selectedCategory = uiState.selectedCategory,
                    onCategorySelect = {
                        onPosEvent(PosEvent.OnSelectedCategory(it))
                    }
                )

                ProductGrid(
                    products = productList,
                    cart = cartItems,
                    getCategory = { id ->
                        categories.find { it.id == id } ?: CategoryModel()
                    },
                    onAddToCart = {
                        onPosEvent(PosEvent.OnAddToCart(it))
                    },
                    onRemoveFromCart = {
                        onPosEvent(PosEvent.OnRemoveFromCart(it))
                    },
                )
            }

            if (cartItems.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp)
                ) {
                    CartPopup(
                        itemCount = cartItems.sumOf { it.quantity },
                        totalPrice = cartItems.sumOf { it.totalPrice },
                        onCartClick = onNavigateToCheckout
                    )
                }
            }

            if (showScanner) {
                BarcodeScannerView(
                    onBarcodeScanned = { barcode ->
                        onPosEvent(PosEvent.OnBarcodeScanned(barcode))
                        showScanner = false
                    },
                    onClose = {
                        showScanner = false
                    }
                )
            }
        }
    }
}


@Composable
fun AppDrawerContent(
    onItemClick: (SettingsType) -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                )
            }
            Text(
                text = "SME",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Spacer(Modifier.height(16.dp))


        // Settings Items
        Text(
            text = "Settings & Support",
            style = MaterialTheme.typography.labelMedium,
            color = Color.Gray,
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
        )
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                SettingsType.entries.forEachIndexed { index, type ->
                    DrawerItem(
                        icon = type.icon,
                        label = type.label,
                        value = when (type) {
                            SettingsType.LANGUAGES -> "English (en)"
                            SettingsType.VERSIONS -> "V${BuildConfig.VERSION_NAME}"
                            else -> null
                        },
                        showBadge = type.hasBadge,
                        showChevron = type.hasChevron,
                        onClick = { onItemClick(type) }
                    )
                    if (index < SettingsType.entries.size - 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = Color.Gray.copy(alpha = 0.2f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DrawerItem(
    icon: ImageVector,
    label: String,
    value: String? = null,
    showBadge: Boolean = false,
    showChevron: Boolean = true,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        if (value != null) {
            Text(
                text = value,
                color = Color.Gray,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(end = 8.dp)
            )
        }
        if (showBadge || showChevron) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(20.dp)
            )
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
        items(categories) { category ->
            val isSelected = category.name == selectedCategory.name
            FilterChip(
                selected = isSelected,
                onClick = { onCategorySelect(category) },
                label = {
                    Text(
                        category.name,
                        style = MaterialTheme.typography.labelLarge
                    )
                },
                leadingIcon = {
                    Text(category.icon, fontSize = 16.sp)
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
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
        columns = GridCells.Adaptive(minSize = 160.dp),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(products) { product ->
            val cartItem = cart.find { it.product.id == product.id }
            ProductCard(
                product = product,
                quantityInCart = cartItem?.quantity ?: 0,
                getCategory = getCategory,
                onAddToCart = { onAddToCart(product) },
                onRemoveFromCart = { onRemoveFromCart(product) },
            )
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
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .aspectRatio(1.2f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                product.imagePaths.firstOrNull()?.let {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(it)
                            .crossfade(true)
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
                    contentScale = ContentScale.Crop,
                )

                if (quantityInCart > 0) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = quantityInCart.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Increased minimum height and removed tight line height to support Myanmar script vertical metrics
            Column(modifier = Modifier.heightIn(min = 54.dp)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 22.sp // Increased line height for Burmese character support
                )

                Text(
                    text = getCategory(product.categoryId).name,
                    fontSize = 12.sp,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        lineHeight = 18.sp
                    ),
                )
                Text(
                    text = "$${String.format(Locale.US, "%.2f", product.price)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {

                if (quantityInCart > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = onRemoveFromCart,
                            modifier = Modifier.size(32.dp),
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(
                                Icons.Default.Remove,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        IconButton(
                            onClick = onAddToCart,
                            modifier = Modifier.size(32.dp),
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = MaterialTheme.colorScheme.onSecondary
                            )
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                } else {
                    IconButton(
                        onClick = onAddToCart,
                        modifier = Modifier.size(32.dp),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CartPopup(
    itemCount: Int,
    totalPrice: Double,
    onCartClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .widthIn(max = 500.dp)
            .fillMaxWidth()
            .height(64.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f),
        shape = RoundedCornerShape(32.dp),
        shadowElevation = 8.dp,
        onClick = onCartClick
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.items_selected, itemCount),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "$${String.format(Locale.US, "%.2f", totalPrice)}",
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
            Spacer(Modifier.width(16.dp))
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.ShoppingBasket,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, heightDp = 2000, widthDp = 800)
@Preview(
    showBackground = true,
    heightDp = 2000,
    widthDp = 800,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun PosScreenPreview() {
    val sampleProducts = listOf(
        ProductModel(
            "1",
            "1230000",
            "Noodles Ramen",
            "Spicy",
            53000000.0,
            10,
            categoryId = "1"
        ),
        ProductModel(
            "2",
            "456",
            "Dumplings",
            "Beef",
            3.27,
            20,
            categoryId = "3"
        ),
        ProductModel(
            "3",
            "789",
            "Beef Burger",
            "Cheese",
            6.50,
            15,
            categoryId = "2"
        ),
        ProductModel(
            "4",
            "012",
            "Pizza Sicilia",
            "Large",
            9.66,
            5,
            categoryId = "4"
        )
    )
    SMETheme() {
        PosContent(
            uiState = PosUiState(
                selectedCategory = CategoryModel("All", "🛠️"),
                searchQuery = "",
            ),
            user = User(
                displayName = "Maung Maung",
                email = "william.henry.harrison@example-pet-store.com",
                id = "",
                phone = "08123456789",
                address = "Yangon"
            ),
            categories = listOf(
                CategoryModel("1", "All", "📦"),
                CategoryModel("2", "Snack", "📦"),
                CategoryModel("3", "Food", "📦"),
                CategoryModel("4", "Drink", "🛠️"),
                CategoryModel("5", "Fruits", "🛠️")
            ),
            productList = sampleProducts,
            cartItems = emptyList(),
            onPosEvent = {},
            onNavigateToCheckout = {},
            onOpenDrawer = {}
        )
    }
}
