package com.tzh.sme.ui.supplier

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tzh.sme.data.model.ProductModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplierDetailScreen(
    viewModel: SupplierViewModel,
    supplierId: String?,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var name by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(supplierId) { viewModel.sendIntent(SupplierUiIntent.SelectSupplier(supplierId)) }

    LaunchedEffect(uiState.selectedSupplier) {
        uiState.selectedSupplier?.let { name = it.name; contact = it.contact; address = it.address; category = it.category }
    }

    LaunchedEffect(viewModel.sideEffect) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                SupplierUiSideEffect.NavigateBack -> onNavigateBack()
                is SupplierUiSideEffect.ShowError -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { snackbarHostState.showSnackbar(it); viewModel.sendIntent(SupplierUiIntent.ClearError) }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Supplier") },
            text = { Text("Are you sure you want to delete this supplier? This action cannot be undone.") },
            confirmButton = {
                Button(onClick = { showDeleteDialog = false; supplierId?.let { viewModel.sendIntent(SupplierUiIntent.DeleteSupplier(it)) } }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(if (supplierId == null) "Add Supplier" else "Edit Supplier") },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = {
                    if (supplierId != null) {
                        IconButton(onClick = { showDeleteDialog = true }) { Icon(Icons.Default.Delete, null, tint = Color.Red) }
                    }
                    TextButton(enabled = name.isNotBlank() && !uiState.isSaving, onClick = { viewModel.sendIntent(SupplierUiIntent.SaveSupplier(name, contact, address, category)) }) {
                        if (uiState.isSaving) CircularProgressIndicator(modifier = Modifier.size(18.dp)) else Text("Save")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            item {
                Text("Supplier Information", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
                ElevatedCard(shape = RoundedCornerShape(24.dp)) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        SupplierField(name, { name = it }, "Supplier Name", Icons.Default.Business)
                        SupplierField(contact, { contact = it }, "Phone / Email", Icons.Default.Phone)
                        SupplierField(category, { category = it }, "Category", Icons.Default.Category)
                        OutlinedTextField(value = address, onValueChange = { address = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Address") }, leadingIcon = { Icon(Icons.Default.LocationOn, null) }, minLines = 3, shape = RoundedCornerShape(16.dp))
                    }
                }
            }

            if (supplierId != null) {
                item {
                    val total = uiState.supplierProducts.sumOf { it.sellPrice * it.quantity }
                    Text("Inventory Summary", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    ElevatedCard(shape = RoundedCornerShape(24.dp), colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                        Column(Modifier.padding(24.dp)) {
                            Text("$${"%.2f".format(Locale.US, total)}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Text("Total Inventory Value")
                            Spacer(Modifier.height(12.dp))
                            AssistChip(onClick = {}, enabled = false, label = { Text("${uiState.supplierProducts.size} Products") })
                        }
                    }
                }
                item { Text("Supplied Products", style = MaterialTheme.typography.titleLarge) }
                if (uiState.supplierProducts.isEmpty()) {
                    item {
                        ElevatedCard(shape = RoundedCornerShape(24.dp)) {
                            Column(modifier = Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Inventory2, null, Modifier.size(48.dp))
                                Spacer(Modifier.height(16.dp))
                                Text("No supplied products")
                            }
                        }
                    }
                } else {
                    items(uiState.supplierProducts) { ProductSummaryItem(it) }
                }
            }
        }
    }
}

@Composable
fun SupplierField(value: String, onChange: (String) -> Unit, label: String, icon: ImageVector) {
    OutlinedTextField(value = value, onValueChange = onChange, modifier = Modifier.fillMaxWidth(), label = { Text(label) }, leadingIcon = { Icon(icon, null) }, shape = RoundedCornerShape(16.dp), singleLine = true)
}

@Composable
fun ProductSummaryItem(product: ProductModel) {
    ElevatedCard(shape = RoundedCornerShape(20.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(18.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(product.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Text("Stock • ${product.quantity}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                Text("$${"%.2f".format(Locale.US, product.sellPrice * product.quantity)}", modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp), fontWeight = FontWeight.Bold)
            }
        }
    }
}
