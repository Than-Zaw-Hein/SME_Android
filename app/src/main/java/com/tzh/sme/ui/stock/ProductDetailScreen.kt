package com.tzh.sme.ui.stock

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.tzh.sme.ui.inventory.BarcodeScannerView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    viewModel: ProductDetailViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(uiState.navigateBack) {
        if (uiState.navigateBack) {
            onNavigateBack()
            viewModel.onEvent(ProductDetailEvent.NavigatedBack)
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents(),
        onResult = { uris ->
            viewModel.onEvent(ProductDetailEvent.AddImages(uris.map { it.toString() }))
        }
    )

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) viewModel.onEvent(ProductDetailEvent.ToggleScanner(true))
    }

    if (uiState.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isEditMode) "Edit Product" else "Add New Product") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState.isEditMode) {
                        IconButton(onClick = { viewModel.onEvent(ProductDetailEvent.DeleteProduct) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Product")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.showScanner) {
            Box(modifier = Modifier.fillMaxSize()) {
                BarcodeScannerView(onBarcodeDetected = { text ->
                    viewModel.onEvent(ProductDetailEvent.BarcodeChanged(text))
                    viewModel.onEvent(ProductDetailEvent.ToggleScanner(false))
                })
                Button(
                    onClick = { viewModel.onEvent(ProductDetailEvent.ToggleScanner(false)) },
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp)
                ) {
                    Text("Close Scanner")
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Product Images (Max 3)", style = MaterialTheme.typography.titleSmall)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().height(100.dp)
                ) {
                    items(uiState.imagePaths) { path ->
                        Box(modifier = Modifier.size(100.dp)) {
                            AsyncImage(
                                model = path,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize().clip(MaterialTheme.shapes.medium),
                                contentScale = ContentScale.Crop
                            )
                            IconButton(
                                onClick = { viewModel.onEvent(ProductDetailEvent.RemoveImage(path)) },
                                modifier = Modifier.align(Alignment.TopEnd).size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Remove",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                    if (uiState.imagePaths.size < 3) {
                        item {
                            OutlinedCard(
                                onClick = { photoPickerLauncher.launch("image/*") },
                                modifier = Modifier.size(100.dp)
                            ) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.AddAPhoto, contentDescription = "Add Photo")
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = uiState.barcode,
                    onValueChange = { viewModel.onEvent(ProductDetailEvent.BarcodeChanged(it)) },
                    label = { Text("Barcode") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = {
                            val permissionCheckResult = ContextCompat.checkSelfPermission(
                                context, Manifest.permission.CAMERA
                            )
                            if (permissionCheckResult == PackageManager.PERMISSION_GRANTED) {
                                viewModel.onEvent(ProductDetailEvent.ToggleScanner(true))
                            } else {
                                permissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        }) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan Barcode")
                        }
                    }
                )

                OutlinedTextField(
                    value = uiState.name,
                    onValueChange = { viewModel.onEvent(ProductDetailEvent.NameChanged(it)) },
                    label = { Text("Product Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                CategoryDropdown(
                    selectedCategory = uiState.category,
                    categories = uiState.categories,
                    onCategorySelected = { viewModel.onEvent(ProductDetailEvent.CategoryChanged(it)) }
                )

                OutlinedTextField(
                    value = uiState.price,
                    onValueChange = { viewModel.onEvent(ProductDetailEvent.PriceChanged(it)) },
                    label = { Text("Price") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.quantity,
                        onValueChange = { viewModel.onEvent(ProductDetailEvent.QuantityChanged(it)) },
                        label = { Text(if (uiState.isEditMode) "Quantity" else "Initial Quantity") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    
                    IconButton(onClick = {
                        val current = uiState.quantity.toIntOrNull() ?: 0
                        if (current > 0) viewModel.onEvent(ProductDetailEvent.QuantityChanged((current - 1).toString()))
                    }) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrement")
                    }
                    
                    IconButton(onClick = {
                        val current = uiState.quantity.toIntOrNull() ?: 0
                        viewModel.onEvent(ProductDetailEvent.QuantityChanged((current + 1).toString()))
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Increment")
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = { viewModel.onEvent(ProductDetailEvent.SaveProduct) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = uiState.barcode.isNotBlank() && uiState.name.isNotBlank() && !uiState.isSaving
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text(if (uiState.isEditMode) "Update Product" else "Add Product")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDropdown(
    selectedCategory: String,
    categories: List<String>,
    onCategorySelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("New Category") },
            text = {
                OutlinedTextField(
                    value = newCategoryName,
                    onValueChange = { newCategoryName = it },
                    label = { Text("Category Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newCategoryName.isNotBlank()) {
                            onCategorySelected(newCategoryName)
                            showDialog = false
                            newCategoryName = ""
                        }
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedCategory,
            onValueChange = {},
            readOnly = true,
            label = { Text("Category") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            categories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category) },
                    onClick = {
                        onCategorySelected(category)
                        expanded = false
                    }
                )
            }
            HorizontalDivider()
            DropdownMenuItem(
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Add New Category")
                    }
                },
                onClick = {
                    showDialog = true
                    expanded = false
                }
            )
        }
    }
}
