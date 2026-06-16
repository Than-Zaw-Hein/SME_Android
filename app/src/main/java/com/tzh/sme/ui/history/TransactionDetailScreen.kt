package com.tzh.sme.ui.history

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tzh.sme.data.model.TransactionItemModel
import com.tzh.sme.data.model.TransactionModel
import com.tzh.sme.data.model.TransactionType
import com.tzh.sme.ui.theme.SMETheme
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailScreen(
    onNavigateUp: () -> Unit,
    viewModel: TransactionDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    TransactionDetailScreenContent(
        uiState = uiState,
        onNavigateUp = onNavigateUp
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionDetailScreenContent(
    uiState: TransactionDetailUiState,
    onNavigateUp: () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Transaction Detail", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = { IconButton(onClick = onNavigateUp) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Column {
                AnimatedVisibility(uiState.isLoading, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                }
                AnimatedVisibility(uiState.error != null) {
                    Text(text = uiState.error ?: "Unknown Error", color = MaterialTheme.colorScheme.error, modifier = Modifier.fillMaxWidth().padding(16.dp))
                }
                uiState.transaction?.let { TransactionDetailContent(transaction = it, items = uiState.items) }
            }
        }
    }
}

@Composable
fun TransactionDetailContent(transaction: TransactionModel, items: List<TransactionItemModel>) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }
    val (typeIcon, typeColor, typeLabel) = when (transaction.type) {
        TransactionType.SALE -> Triple(Icons.Default.PointOfSale, MaterialTheme.colorScheme.primary, "Sale Transaction")
        TransactionType.STOCK_IN -> Triple(Icons.Default.AddShoppingCart, Color(0xFF1B5E20), "Stock In / Restock")
        TransactionType.STOCK_OUT -> Triple(Icons.Default.Inventory2, Color(0xFFB71C1C), "Stock Out / Removal")
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp)
    ) {
        // Header Status Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = typeColor.copy(alpha = 0.08f)),
                shape = MaterialTheme.shapes.extraLarge,
                border = androidx.compose.foundation.BorderStroke(1.dp, typeColor.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        modifier = Modifier.size(64.dp),
                        shape = MaterialTheme.shapes.large,
                        color = typeColor.copy(alpha = 0.15f)
                    ) {
                        Icon(
                            imageVector = typeIcon,
                            contentDescription = null,
                            tint = typeColor,
                            modifier = Modifier.padding(16.dp).size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = typeLabel,
                        style = MaterialTheme.typography.titleMedium,
                        color = typeColor,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "$${String.format(Locale.US, "%,.2f", transaction.netAmount)}",
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-1).sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Transaction Summary / Financials
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Payment Summary",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    SummaryRow(label = "Subtotal", value = transaction.totalAmount)
                    
                    if (transaction.discount > 0) {
                        SummaryRow(
                            label = "Discount",
                            value = transaction.discount,
                            valueColor = MaterialTheme.colorScheme.error,
                            prefix = "-$"
                        )
                    }
                    
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Grand Total",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$${String.format(Locale.US, "%,.2f", transaction.netAmount)}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Details Information
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    InfoRow(label = "Transaction ID", value = transaction.transactionId, icon = Icons.Default.Tag)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    InfoRow(label = "Timestamp", value = dateFormat.format(Date(transaction.timestamp)), icon = Icons.Default.Event)
                    
                    transaction.userName?.let {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        InfoRow(label = "Processed By", value = it, icon = Icons.Default.Person)
                    }
                    
                    transaction.supplierName?.let {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        InfoRow(label = "Supplier", value = it, icon = Icons.Default.Business)
                    }
                }
            }
        }

        // Items Section Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.AutoMirrored.Filled.ListAlt, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Items List",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.extraSmall
                ) {
                    Text(
                        text = "${items.size} ${if (items.size == 1) "item" else "items"}",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        items(items) { item -> TransactionItemRow(item) }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: Double,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    prefix: String = "$"
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = "$prefix${String.format(Locale.US, "%,.2f", value)}",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = valueColor
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun TransactionItemRow(item: TransactionItemModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.productName.ifEmpty { "Product ID: ${item.productId}" },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.extraSmall
                ) {
                    Text(
                        text = "${item.quantity} × $${String.format(Locale.US, "%.2f", item.priceAtTime)}",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "$${String.format(Locale.US, "%.2f", item.priceAtTime * item.quantity)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Preview(showBackground = true, name = "Light Mode")
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Dark Mode")
@Composable
fun TransactionDetailScreenPreview() {
    SMETheme(dynamicColor = false) {
        Surface {
            TransactionDetailScreenContent(
                uiState = TransactionDetailUiState(
                    isLoading = false,
                    transaction = TransactionModel(
                        transactionId = "TXN-2026-0505-001",
                        type = TransactionType.SALE,
                        totalAmount = 125.50,
                        discount = 15.0,
                        netAmount = 110.50,
                        timestamp = System.currentTimeMillis(),
                        userName = "Admin User",
                        supplierName = "Global Tech Supplies"
                    ),
                    items = listOf(
                        TransactionItemModel(
                            productId = "PROD-001",
                            productName = "Premium Wireless Headphones MK2",
                            quantity = 2,
                            priceAtTime = 50.0
                        ),
                        TransactionItemModel(
                            productId = "PROD-002",
                            productName = "Fast USB-C Charging Cable",
                            quantity = 1,
                            priceAtTime = 25.50
                        )
                    )
                ),
                onNavigateUp = {}
            )
        }
    }
}
