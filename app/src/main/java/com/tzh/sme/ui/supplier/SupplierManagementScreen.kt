package com.tzh.sme.ui.supplier

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplierManagementScreen(
    viewModel: SupplierViewModel,
    onNavigateToAddSupplier: () -> Unit,
    onNavigateToSupplierDetail: (String) -> Unit,
    onOpenDrawer: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Supplier Management") },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) { Icon(Icons.Default.Business, "Menu") }
                }
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(modifier = Modifier.fillMaxSize()) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.sendIntent(SupplierUiIntent.UpdateSearchQuery(it)) },
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    placeholder = { Text("Search suppliers...") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    singleLine = true
                )

                if (uiState.isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                } else if (uiState.filteredSuppliers.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No suppliers found", style = MaterialTheme.typography.bodyLarge)
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(uiState.filteredSuppliers) { supplier ->
                            SupplierItem(supplier = supplier, onClick = { onNavigateToSupplierDetail(supplier.id) })
                        }
                    }
                }
            }
            FloatingActionButton(onClick = onNavigateToAddSupplier, modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)) {
                Icon(Icons.Default.Add, "Add Supplier")
            }
        }
    }
}

@Composable
fun SupplierItem(supplier: com.tzh.sme.data.model.SupplierModel, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            Text(text = supplier.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = supplier.category.ifEmpty { "General Supplier" }, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = supplier.contact, style = MaterialTheme.typography.bodySmall)
        }
    }
}
