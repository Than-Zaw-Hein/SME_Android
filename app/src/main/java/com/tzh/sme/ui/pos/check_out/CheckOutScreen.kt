package com.tzh.sme.ui.pos.check_out

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.tzh.sme.R
import com.tzh.sme.data.model.ProductModel
import com.tzh.sme.domain.model.CartItem
import com.tzh.sme.ui.pos.PosUiIntent
import com.tzh.sme.ui.pos.PosUiSideEffect
import com.tzh.sme.ui.pos.PosViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckOutScreen(
    viewModel: PosViewModel,
    windowWidthSizeClass: WindowWidthSizeClass,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isSideBySide = windowWidthSizeClass == WindowWidthSizeClass.Expanded || isLandscape
    val isCompactHeight = configuration.screenHeightDp < 480

    LaunchedEffect(viewModel.sideEffect) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                is PosUiSideEffect.ShowError -> snackbarHostState.showSnackbar(effect.message)
                is PosUiSideEffect.ShowToast -> snackbarHostState.showSnackbar(effect.message)
                PosUiSideEffect.NavigateToCheckout -> {}
                PosUiSideEffect.NavigateBack -> onNavigateBack()
            }
        }
    }

    uiState.completedTransaction?.let { (transaction, items) ->
        CheckOutCompleteDialog(
            transaction = transaction,
            items = items,
            isPrinting = uiState.isPrinting,
            printError = uiState.printError,
            onPrint = { viewModel.sendIntent(PosUiIntent.PrintReceipt(transaction, items)) },
            onClose = { viewModel.sendIntent(PosUiIntent.DismissSuccessDialog) },
            onGoHome = {
                viewModel.sendIntent(PosUiIntent.DismissSuccessDialog)
                onNavigateBack()
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.checkout)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            AnimatedVisibility (uiState.cartItems.isNotEmpty() && !isSideBySide,
                enter = slideInVertically(),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                CheckoutSummaryCard(
                    modifier = Modifier.navigationBarsPadding(),
                    cart = uiState.cartItems,
                    discount = uiState.discount,
                    isCheckoutInProgress = uiState.isCheckOutInProgress,
                    onDiscountChange = { viewModel.sendIntent(PosUiIntent.UpdateDiscount(it)) },
                    onCheckout = { viewModel.sendIntent(PosUiIntent.Checkout) }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
            if (uiState.cartItems.isEmpty()) {
                EmptyCartState(modifier = Modifier.align(Alignment.Center))
            } else {
                if (isSideBySide) {
                    Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CartList(
                            cart = uiState.cartItems,
                            onAdd = { viewModel.sendIntent(PosUiIntent.AddToCart(it)) },
                            onRemove = { viewModel.sendIntent(PosUiIntent.RemoveFromCart(it)) },
                            modifier = Modifier.weight(if (windowWidthSizeClass == WindowWidthSizeClass.Expanded) 1f else 0.55f)
                        )
                        Box(modifier = Modifier
                            .then(
                                if (windowWidthSizeClass == WindowWidthSizeClass.Expanded) Modifier.width(
                                    400.dp
                                ) else Modifier.weight(0.45f)
                            )
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                            .padding(8.dp)) {
                            CheckoutSummaryCard(
                                cart = uiState.cartItems,
                                discount = uiState.discount,
                                isCheckoutInProgress = uiState.isCheckOutInProgress,
                                isExpanded = true,
                                isCompact = isCompactHeight,
                                onDiscountChange = { viewModel.sendIntent(PosUiIntent.UpdateDiscount(it)) },
                                onCheckout = { viewModel.sendIntent(PosUiIntent.Checkout) }
                            )
                        }
                    }
                } else {
                    CartList(
                        cart = uiState.cartItems,
                        onAdd = { viewModel.sendIntent(PosUiIntent.AddToCart(it)) },
                        onRemove = { viewModel.sendIntent(PosUiIntent.RemoveFromCart(it)) },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            AnimatedVisibility(visible = uiState.isCheckOutInProgress, enter = fadeIn(), exit = fadeOut()) {
                Surface(color = Color.Black.copy(alpha = 0.5f), modifier = Modifier.fillMaxSize()) {
                    Box(contentAlignment = Alignment.Center) { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }
                }
            }
        }
    }
}

@Composable
fun CartList(cart: List<CartItem>, onAdd: (ProductModel) -> Unit, onRemove: (ProductModel) -> Unit, modifier: Modifier = Modifier, footer: @Composable (() -> Unit)? = null) {
    LazyColumn(modifier = modifier, contentPadding = PaddingValues(8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text(text = stringResource(R.string.order_summary), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 8.dp)) }
        items(cart, key = { it.product.id }) { item ->
            CartItemRow(item = item, onAdd = { onAdd(item.product) }, onRemove = { onRemove(item.product) })
        }
        if (footer != null) item { footer() }
    }
}

@Composable
fun CartItemRow(item: CartItem, onAdd: () -> Unit, onRemove: () -> Unit) {
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Row(modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(modifier = Modifier.size(64.dp), shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                if (item.product.imagePaths.isNotEmpty()) {
                    AsyncImage(model = item.product.imagePaths.first(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else {
                    Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.ShoppingCart, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = item.product.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(text = "$${String.format(Locale.US, "%.2f", item.product.sellPrice)} / unit", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = "$${String.format(Locale.US, "%.2f", item.totalPrice)}", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier
                .padding(horizontal = 4.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    CircleShape
                )
                .padding(vertical = 2.dp)) {
                IconButton(onClick = onRemove, modifier = Modifier.size(32.dp), colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)) {
                    Icon(Icons.Default.Remove, null, modifier = Modifier.size(18.dp))
                }
                Text(text = "${item.quantity}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp))
                IconButton(onClick = onAdd, modifier = Modifier.size(32.dp), colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun CheckoutSummaryCard(modifier: Modifier = Modifier, cart: List<CartItem>, discount: Double, isCheckoutInProgress: Boolean, isExpanded: Boolean = false, isCompact: Boolean = false, onDiscountChange: (Double) -> Unit, onCheckout: () -> Unit) {
    val total = cart.sumOf { it.totalPrice }
    val netTotal = (total - discount).coerceAtLeast(0.0)
    val shape = if (isExpanded) RoundedCornerShape(24.dp) else RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    val padding = if (isCompact) 12.dp else 24.dp
    val spacing = if (isCompact) 8.dp else 20.dp

    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = shape, colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp)) {
        Column(modifier = modifier.padding(padding)) {
            OutlinedTextField(
                value = if (discount == 0.0) "" else String.format(Locale.US, "%.2f", discount),
                onValueChange = { onDiscountChange(it.toDoubleOrNull() ?: 0.0) },
                label = { Text("Discount Amount", style = if (isCompact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyLarge) },
                modifier = Modifier.fillMaxWidth(),
                prefix = { Text("$") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                textStyle = if (isCompact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge
            )
            Spacer(Modifier.height(spacing))
            SummaryRow("Subtotal", "$${String.format(Locale.US, "%.2f", total)}", isCompact = isCompact)
            SummaryRow("Discount", "-$${String.format(Locale.US, "%.2f", discount)}", color = MaterialTheme.colorScheme.error, isCompact = isCompact)
            Spacer(Modifier.height(if (isCompact) 4.dp else 12.dp))
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Spacer(Modifier.height(if (isCompact) 4.dp else 12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Total Amount", style = if (isCompact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("$${String.format(Locale.US, "%.2f", netTotal)}", style = if (isCompact) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)
            }
            Spacer(Modifier.height(if (isCompact) 12.dp else 24.dp))
            Button(onClick = onCheckout, modifier = Modifier
                .fillMaxWidth()
                .height(if (isCompact) 44.dp else 56.dp), enabled = cart.isNotEmpty() && !isCheckoutInProgress, shape = RoundedCornerShape(12.dp)) {
                if (isCheckoutInProgress) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                } else {
                    Text("COMPLETE CHECKOUT", style = if (isCompact) MaterialTheme.typography.labelLarge else MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun SummaryRow(label: String, value: String, color: Color = MaterialTheme.colorScheme.onSurface, isCompact: Boolean = false) {
    Row(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = if (isCompact) 2.dp else 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, style = if (isCompact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = if (isCompact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = color)
    }
}

@Composable
fun EmptyCartState(modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Surface(modifier = Modifier.size(120.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
            Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.ShoppingCart, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary) }
        }
        Spacer(Modifier.height(24.dp))
        Text("Your cart is empty", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Add some products to start a new transaction.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}
