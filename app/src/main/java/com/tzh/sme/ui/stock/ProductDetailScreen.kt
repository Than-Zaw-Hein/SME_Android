package com.tzh.sme.ui.stock

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.tzh.sme.R
import com.tzh.sme.data.model.*
import com.tzh.sme.ui.inventory.BarcodeScannerView
import com.tzh.sme.ui.theme.SMETheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ProductDetailScreen(
    viewModel: ProductDetailViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(viewModel.effect) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ProductDetailEffect.NavigateBack -> onNavigateBack()
            }
        }
    }

    ProductDetailContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onNavigateBack = onNavigateBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailContent(
    uiState: ProductDetailUiState,
    onEvent: (ProductDetailEvent) -> Unit,
    onNavigateBack: () -> Unit
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showHistorySheet by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(
                message = error,
                actionLabel = "Dismiss",
                duration = SnackbarDuration.Short
            )
            onEvent(ProductDetailEvent.ErrorShown)
        }
    }

    val stockInHistory = remember(uiState.priceHistory) {
        uiState.priceHistory
            .filter { it.first.type == TransactionType.STOCK_IN }
            .sortedByDescending { it.first.timestamp }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) onEvent(ProductDetailEvent.ToggleScanner(true))
    }

    if (uiState.showScanner) {
        BarcodeScannerView(
            onBarcodeScanned = { barcode ->
                onEvent(ProductDetailEvent.BarcodeChanged(barcode))
                onEvent(ProductDetailEvent.ToggleScanner(false))
            },
            onClose = { onEvent(ProductDetailEvent.ToggleScanner(false)) }
        )
        return
    }

//    if (uiState.isSaving) {
//        Dialog(onDismissRequest = { }) {
//            Surface(
//                shape = MaterialTheme.shapes.medium,
//                color = MaterialTheme.colorScheme.surface,
//                tonalElevation = 8.dp,
//                modifier = Modifier
//                    .fillMaxSize()
//                    .padding(16.dp)
//            ) {
//                Column(
//                    modifier = Modifier.padding(24.dp),
//                    horizontalAlignment = Alignment.CenterHorizontally,
//                    verticalArrangement = Arrangement.Center
//                ) {
//                    CircularProgressIndicator()
//                    Spacer(Modifier.height(16.dp))
//                    Text(
//                        text = if (uiState.isEditMode) "Updating product..." else "Saving product...",
//                        style = MaterialTheme.typography.bodyLarge
//                    )
//                }
//            }
//        }
//    }
    if (uiState.isDeleting) {
        Dialog(onDismissRequest = { }) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "Deleting product...",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_confirmation_title)) },
            text = { Text(stringResource(R.string.delete_confirmation_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        onEvent(ProductDetailEvent.DeleteProduct)
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        if (uiState.isEditMode) stringResource(R.string.edit_product)
                        else stringResource(R.string.add_new_product),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
                        Text(stringResource(R.string.cancel))
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            if (!uiState.isEditMode && (uiState.editableQty.toIntOrNull()
                                    ?: 0) == 0
                            ) {
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = "Product must have a quantity greater than 0",
                                        actionLabel = "Dismiss",
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            } else {
                                onEvent(ProductDetailEvent.SaveProduct)
                            }

                        },
                        enabled = !uiState.isSaving,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .defaultMinSize(minWidth = 80.dp)
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(stringResource(R.string.save))
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .imePadding() // Handles the keyboard height
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Image Gallery Component
                ImageGalleryComponent(
                    imagePaths = uiState.imagePaths,
                    onAddImages = { onEvent(ProductDetailEvent.AddImages(it)) },
                    onRemoveImage = { onEvent(ProductDetailEvent.RemoveImage(it)) }
                )

                // Basic Info Section
                Column(modifier = Modifier.fillMaxWidth()) {
                    SectionHeader(stringResource(R.string.product_name))
                    OutlinedTextField(
                        value = uiState.name,
                        onValueChange = { onEvent(ProductDetailEvent.NameChanged(it)) },
                        placeholder = {
                            Text(
                                text = "e.g. Organic Coffee Beans",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 1.0f)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 488.dp),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 488.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        SectionHeader(stringResource(R.string.barcode))
                        OutlinedTextField(
                            value = uiState.barcode,
                            onValueChange = { onEvent(ProductDetailEvent.BarcodeChanged(it)) },
                            trailingIcon = {
                                IconButton(
                                    onClick = {
                                        val permissionCheckResult =
                                            ContextCompat.checkSelfPermission(
                                                context, Manifest.permission.CAMERA
                                            )
                                        if (permissionCheckResult == PackageManager.PERMISSION_GRANTED) {
                                            onEvent(ProductDetailEvent.ToggleScanner(true))
                                        } else {
                                            permissionLauncher.launch(Manifest.permission.CAMERA)
                                        }
                                    },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan")
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        SectionHeader(stringResource(R.string.price))
                        OutlinedTextField(
                            value = uiState.price,
                            onValueChange = { onEvent(ProductDetailEvent.PriceChanged(it)) },
                            prefix = {
                                Text(
                                    text = "$ ",
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                // Category Section
                CategorySection(
                    selectedCategory = uiState.category,
                    categories = uiState.categories,
                    onCategorySelected = { onEvent(ProductDetailEvent.CategoryChanged(it)) },
                    onAddCategory = { onEvent(ProductDetailEvent.AddCategory(it)) },
                    onUpdateCategory = { onEvent(ProductDetailEvent.UpdateCategory(it)) },
                    onDeleteCategory = { onEvent(ProductDetailEvent.DeleteCategory(it)) }
                )

                // Stock Management Section
                StockManagementSection(
                    currentQty = uiState.currentQty,
                    deltaQty = uiState.editableQty,
                    transactionType = uiState.transactionType,
                    suppliers = uiState.suppliers,
                    selectedSupplier = uiState.selectedSupplier,
                    onTypeChanged = { onEvent(ProductDetailEvent.TransactionTypeChanged(it)) },
                    onQtyChanged = { onEvent(ProductDetailEvent.QuantityChanged(it)) },
                    onSupplierChanged = { onEvent(ProductDetailEvent.SupplierChanged(it)) },
                    onAddSupplier = { onEvent(ProductDetailEvent.AddSupplier(it)) }
                )


                if (uiState.transactionType == TransactionType.STOCK_IN &&
                    uiState.isEditMode && stockInHistory.isNotEmpty()
                ) {

                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.stock_in_history),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier
                        )

                        TextButton(onClick = { showHistorySheet = true }) {
                            Text(stringResource(R.string.view_all))
                        }
                    }
//                    PriceHistoryTable(
//                        displayHistory = stockInHistory.take(1),
//                        fullHistory = stockInHistory
//                    )
                    val latestHistory = stockInHistory.first()
                    StockHistoryCard(
                        latestHistory.first,
                        latestHistory.second
                    )


                }
                if (uiState.isEditMode) {

                    Spacer(modifier = Modifier.height(32.dp))

                    TextButton(
                        onClick = { showDeleteDialog = true },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = stringResource(R.string.delete_product)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.delete_product))
                    }
                }
            }

            if (showHistorySheet) {
                ModalBottomSheet(
                    onDismissRequest = { showHistorySheet = false },
                    sheetState = rememberModalBottomSheetState()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .navigationBarsPadding()
                    ) {
                        Text(
                            text = stringResource(R.string.stock_in_history),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(stockInHistory) { (transaction, item) ->
                                StockHistoryCard(transaction, item)
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }

            if (uiState.isSaving) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = if (uiState.isEditMode) "Updating product..." else "Saving product...",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

@Composable
fun ImageGalleryComponent(
    imagePaths: List<String>,
    onAddImages: (List<String>) -> Unit,
    onRemoveImage: (String) -> Unit
) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        onAddImages(uris.map { it.toString() })
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Primary Image
        Box(
            modifier = Modifier
                .weight(2f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable { launcher.launch("image/*") }
        ) {
            if (imagePaths.isNotEmpty()) {
                AsyncImage(
                    model = imagePaths[0],
                    contentDescription = "Primary Image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                IconButton(
                    onClick = { onRemoveImage(imagePaths[0]) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        .size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Remove",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.AddPhotoAlternate,
                        contentDescription = stringResource(R.string.add_photo),
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        stringResource(R.string.add_photo),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }

        // Thumbnails
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(2) { index ->
                val imageIndex = index + 1
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { launcher.launch("image/*") }
                ) {
                    if (imageIndex < imagePaths.size) {
                        AsyncImage(
                            model = imagePaths[imageIndex],
                            contentDescription = "Thumbnail",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        IconButton(
                            onClick = { onRemoveImage(imagePaths[imageIndex]) },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(2.dp)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                .size(20.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Remove",
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    } else {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = stringResource(R.string.add_photo),
                            modifier = Modifier.align(Alignment.Center),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StockManagementSection(
    currentQty: Int,
    deltaQty: String,
    transactionType: TransactionType,
    suppliers: List<SupplierModel>,
    selectedSupplier: SupplierModel?,
    onTypeChanged: (TransactionType) -> Unit,
    onQtyChanged: (String) -> Unit,
    onSupplierChanged: (SupplierModel?) -> Unit,
    onAddSupplier: (SupplierModel) -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),

        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    SectionHeader(stringResource(R.string.inventory_status))
                    Text(
                        stringResource(R.string.in_stock, currentQty),
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }

                // Segmented Button replacement (Simplified with AssistChip-like logic)
                Row(
                    modifier = Modifier
                        .wrapContentSize()
                        .padding(4.dp)
                ) {
                    StockTypeButton(
                        text = stringResource(R.string.`in`),
                        isSelected = transactionType == TransactionType.STOCK_IN,
                        onClick = { onTypeChanged(TransactionType.STOCK_IN) }
                    )
                    if (currentQty > 0) {
                        StockTypeButton(
                            text = stringResource(R.string.out),
                            isSelected = transactionType == TransactionType.STOCK_OUT,
                            onClick = { onTypeChanged(TransactionType.STOCK_OUT) }
                        )
                    }
                }
            }

            // Quantity Stepper
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                IconButton(
                    onClick = {
                        val current = deltaQty.toIntOrNull() ?: 0
                        onQtyChanged((current - 1).coerceAtLeast(0).toString())
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Minus")
                }

                TextField(
                    value = deltaQty,
                    onValueChange = { if (it.all { char -> char.isDigit() }) onQtyChanged(it) },
                    textStyle = MaterialTheme.typography.headlineMedium.copy(
                        textAlign = TextAlign.Center
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .widthIn(min = 80.dp, max = 120.dp)
                        .padding(horizontal = 8.dp)
                )

                IconButton(
                    onClick = {
                        val current = deltaQty.toIntOrNull() ?: 0
                        if (transactionType == TransactionType.STOCK_IN) {
                            onQtyChanged((current + 1).toString())
                        } else {

                            if ((current + 1) <= currentQty) {
                                onQtyChanged((current + 1).toString())
                            }
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Plus")
                }
            }

            if (transactionType == TransactionType.STOCK_IN) {
                SupplierDropdown(
                    selectedSupplier = selectedSupplier,
                    suppliers = suppliers,
                    onSupplierAdded = { name, contact ->
                        onAddSupplier(
                            SupplierModel(
                                name = name,
                                contact = contact
                            )
                        )
                    },
                    onSupplierSelected = onSupplierChanged
                )
            }
        }
    }
}

@Composable
fun StockTypeButton(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 24.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
        )
    }
}

@Composable
fun StockHistoryCard(transaction: TransactionModel, item: TransactionItemModel) {
    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    val dateStr = sdf.format(Date(transaction.timestamp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when (transaction.type) {
                        TransactionType.STOCK_IN -> "Stock In / Restock / Update"
                        TransactionType.STOCK_OUT -> "Stock Out / Sale"
                        else -> "Adjustment"
                    },
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    dateStr,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                val prefix = if (transaction.type == TransactionType.STOCK_IN) "+" else "-"
                val color =
                    if (transaction.type == TransactionType.STOCK_IN) Color(0xFF2E7D32) else Color(
                        0xFFC62828
                    )

                Text(
                    text = "$prefix${item.quantity}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                )
                Text(
                    text = "$ ${item.priceAtTime}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategorySection(
    selectedCategory: CategoryModel,
    categories: List<CategoryModel>,
    onCategorySelected: (CategoryModel) -> Unit,
    onAddCategory: (CategoryModel) -> Unit,
    onUpdateCategory: (CategoryModel) -> Unit,
    onDeleteCategory: (CategoryModel) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<CategoryModel?>(null) }
    var categoryName by remember { mutableStateOf("") }
    var selectedEmoji by remember { mutableStateOf("📦") }

    LaunchedEffect(editingCategory) {
        if (editingCategory != null) {
            categoryName = editingCategory!!.name
            selectedEmoji = editingCategory!!.icon
        } else {
            categoryName = ""
            selectedEmoji = "📦"
        }
    }

    val emojis = listOf(
        "📦", "🍎", "🍔", "🥤",
        "🍦", "👕", "📱", "🏠",
        "🛠️", "📚", "🎮", "💊",
        "🧼", "💄", "👟", "⌚",
        "⚡"
    )

    if (showDialog) {
        AlertDialog(
            onDismissRequest = {
                showDialog = false
                editingCategory = null
            },
            title = {
                Text(
                    if (editingCategory != null) stringResource(R.string.edit_category)
                    else stringResource(R.string.new_category)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = categoryName,
                        onValueChange = { categoryName = it },
                        label = { Text(stringResource(R.string.category_name)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Select Icon:", style = MaterialTheme.typography.bodyMedium)

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        modifier = Modifier.height(160.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(emojis) { emoji ->
                            Surface(
                                onClick = { selectedEmoji = emoji },
                                shape = MaterialTheme.shapes.small,
                                color = if (selectedEmoji == emoji) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                border = if (selectedEmoji == emoji) null else AssistChipDefaults.assistChipBorder(
                                    true
                                )
                            ) {
                                Text(
                                    text = emoji,
                                    fontSize = 24.sp,
                                    modifier = Modifier.padding(8.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (categoryName.isNotBlank()) {
                        if (editingCategory != null) {
                            onUpdateCategory(
                                editingCategory!!.copy(
                                    name = categoryName,
                                    icon = selectedEmoji
                                )
                            )
                        } else {
                            onAddCategory(CategoryModel(name = categoryName, icon = selectedEmoji))
                        }
                        showDialog = false
                        editingCategory = null
                    }
                }) {
                    Text(
                        if (editingCategory != null) stringResource(R.string.save)
                        else stringResource(R.string.add)
                    )
                }
            }
        )
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(stringResource(R.string.category))
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 488.dp)
        ) {
            OutlinedTextField(
                value = selectedCategory.name.ifEmpty { "Select Category" },
                onValueChange = {},
                readOnly = true,
                leadingIcon = if (selectedCategory.name.isNotEmpty()) {
                    { Text(selectedCategory.icon) }
                } else null,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                categories.forEach { category ->
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(category.icon)
                                Text(category.name)
                            }
                        },
                        onClick = {
                            onCategorySelected(category)
                            expanded = false
                        },
                        trailingIcon = {
                            Row {
                                IconButton(onClick = {
                                    editingCategory = category
                                    showDialog = true
                                    expanded = false
                                }) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Edit Category",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                IconButton(onClick = { onDeleteCategory(category) }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete Category",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    )
                }
                HorizontalDivider()
                DropdownMenuItem(
                    text = {
                        Text(
                            "+ Add New Category",
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    onClick = {
                        showDialog = true
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplierDropdown(
    selectedSupplier: SupplierModel?,
    suppliers: List<SupplierModel>,
    onSupplierAdded: (String, String) -> Unit,
    onSupplierSelected: (SupplierModel?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }
    var newSupplierName by remember { mutableStateOf("") }
    var newSupplierContact by remember { mutableStateOf("") }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("New Supplier") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newSupplierName,
                        onValueChange = { newSupplierName = it },
                        label = { Text("Name") })
                    OutlinedTextField(
                        value = newSupplierContact,
                        onValueChange = { newSupplierContact = it },
                        label = { Text("Contact") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (newSupplierName.isNotBlank()) {
                        onSupplierAdded(newSupplierName, newSupplierContact)
                        showDialog = false
                    }
                }) { Text("Add") }
            }
        )
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 488.dp)
    ) {
        OutlinedTextField(
            value = selectedSupplier?.name ?: "No Supplier",
            onValueChange = {},
            readOnly = true,
            label = { Text("Supplier") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            leadingIcon = { Icon(Icons.Default.Business, contentDescription = null) }
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("None") },
                onClick = { onSupplierSelected(null); expanded = false })
            suppliers.forEach { supplier ->
                DropdownMenuItem(
                    text = { Text(supplier.name) },
                    onClick = { onSupplierSelected(supplier); expanded = false })
            }
            HorizontalDivider()
            DropdownMenuItem(text = {
                Text(
                    "+ Add New Supplier",
                    color = MaterialTheme.colorScheme.primary
                )
            }, onClick = { showDialog = true; expanded = false })
        }
    }
}

@Composable
fun PriceHistoryTable(
    displayHistory: List<Pair<TransactionModel, TransactionItemModel>>,
    fullHistory: List<Pair<TransactionModel, TransactionItemModel>>
) {


    Column(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 488.dp)
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant,
                RoundedCornerShape(12.dp)
            )
            .clip(RoundedCornerShape(12.dp))
    ) {
        // Table Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(12.dp)
        ) {
            Text(
                text = "Date",
                modifier = Modifier.weight(1.5f),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "Qty",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.End
            )
            Text(
                text = "Cost",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.End
            )
        }

        displayHistory.forEach { (transaction, item) ->
            val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            val dateStr = sdf.format(Date(transaction.timestamp))

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateStr,
                    modifier = Modifier.weight(1.5f),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = item.quantity.toString(),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.End
                )
                Text(
                    text = "$${item.priceAtTime}",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    ),
                    textAlign = TextAlign.End
                )
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
fun ProductDetailPreview() {
    SMETheme() {
        ProductDetailContent(
            uiState = ProductDetailUiState(
                name = "Test Product",
                barcode = "123456789",
                price = "99.99",
                currentQty = 50,
                editableQty = "5",
                isEditMode = true,
                categories = listOf(
                    CategoryModel(name = "Test Category", icon = "📦"),
                ),
                priceHistory = listOf(
                    Pair(
                        TransactionModel(
                            type = TransactionType.STOCK_IN,
                            timestamp = System.currentTimeMillis()
                        ),
                        TransactionItemModel(
                            quantity = 10,
                            priceAtTime = 100.0
                        )
                    )
                )
            ),
            onEvent = {},
            onNavigateBack = {}
        )
    }
}
