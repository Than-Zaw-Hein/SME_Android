package com.tzh.sme.ui.history

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tzh.sme.R
import com.tzh.sme.data.model.TransactionModel
import com.tzh.sme.data.model.TransactionType
import com.tzh.sme.ui.theme.SMETheme
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onNavigateToDetail: (String) -> Unit,
    onOpenDrawer: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    HistoryContent(
        uiState = uiState,
        onNavigateToDetail = onNavigateToDetail,
        onOpenDrawer = onOpenDrawer,
        onEvent = viewModel::onHistoryEvent
    )
}

@Immutable
data class HistorySummary(
    val title: String,
    val label1: String,
    val value1: Double,
    val label2: String,
    val value2: Double
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HistoryContent(
    uiState: HistoryUiState,
    onNavigateToDetail: (String) -> Unit,
    onOpenDrawer: () -> Unit,
    onEvent: (HistoryEvent) -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }
    val sectionFormatter = remember { SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault()) }

    // Optimization: Use derivedStateOf to recalculate summary only when necessary
    val summaryData by remember(uiState.filteredTransactions, uiState.selectedTab, uiState.searchQuery, uiState.startDate) {
        derivedStateOf {
            val transactions = uiState.filteredTransactions
            val tab = uiState.selectedTab
            
            val isTodayRange = if (uiState.startDate != null && uiState.endDate != null) {
                val today = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                uiState.startDate == today
            } else false

            if (tab == HistoryTab.SALES) {
                val title = when {
                    isTodayRange -> "Today's Sales Summary"
                    uiState.startDate != null -> "Sales Period Summary"
                    uiState.searchQuery.isNotEmpty() -> "Sales Search Summary"
                    else -> "All Sales Summary"
                }
                val total = transactions.sumOf { it.netAmount }
                HistorySummary(
                    title = title,
                    label1 = "Total Sales",
                    value1 = total,
                    label2 = "Avg. Sale",
                    value2 = if (transactions.isNotEmpty()) total / transactions.size else 0.0
                )
            } else {
                val title = when {
                    isTodayRange -> "Today's Stock Summary"
                    uiState.startDate != null -> "Stock Period Summary"
                    else -> "All Stock Summary"
                }
                HistorySummary(
                    title = title,
                    label1 = "Stock In",
                    value1 = transactions.filter { it.type == TransactionType.STOCK_IN }.sumOf { it.netAmount },
                    label2 = "Stock Out",
                    value2 = transactions.filter { it.type == TransactionType.STOCK_OUT }.sumOf { it.netAmount }
                )
            }
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(stringResource(R.string.history_title)) },
                    navigationIcon = {
                        IconButton(onClick = onOpenDrawer) {
                            Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = "Menu")
                        }
                    }
                )
                PrimaryTabRow(
                    selectedTabIndex = uiState.selectedTab.ordinal,
                    containerColor = MaterialTheme.colorScheme.background,
                    divider = {}
                ) {
                    HistoryTab.entries.forEach { tab ->
                        Tab(
                            selected = uiState.selectedTab == tab,
                            onClick = { onEvent(HistoryEvent.OnTabSelected(tab)) },
                            text = { 
                                Text(
                                    text = if (tab == HistoryTab.SALES) "Sales" else "Stock History",
                                    style = MaterialTheme.typography.titleSmall
                                ) 
                            }
                        )
                    }
                }
                
                FilterContent(uiState, onEvent)
            }
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.error != null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(uiState.error, color = MaterialTheme.colorScheme.error)
            }
        } else {
            if (uiState.filteredTransactions.isEmpty()) {
                EmptyHistoryState(modifier = Modifier.padding(padding))
            } else {
                val groupedTransactions = remember(uiState.filteredTransactions) {
                    uiState.filteredTransactions.groupBy { tx ->
                        val cal = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
                        cal.set(Calendar.HOUR_OF_DAY, 0)
                        cal.set(Calendar.MINUTE, 0)
                        cal.set(Calendar.SECOND, 0)
                        cal.set(Calendar.MILLISECOND, 0)
                        cal.timeInMillis
                    }
                }

                LazyColumn(
                    modifier = Modifier.padding(padding).fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        HistorySummaryHeader(
                            summary = summaryData,
                            tab = uiState.selectedTab
                        )
                    }

                    groupedTransactions.forEach { (dateInMillis, transactions) ->
                        stickyHeader {
                            DateHeader(
                                date = sectionFormatter.format(Date(dateInMillis)),
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }

                        items(transactions, key = { it.transactionId }) { tx ->
                            TransactionCard(
                                transaction = tx,
                                dateFormatter = dateFormatter,
                                onClick = { onNavigateToDetail(tx.transactionId) },
                                modifier = Modifier.animateItem()
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterContent(
    uiState: HistoryUiState,
    onEvent: (HistoryEvent) -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (uiState.selectedTab == HistoryTab.SALES) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { onEvent(HistoryEvent.OnSearchQueryChanged(it)) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search by Transaction ID") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = uiState.selectedStockFilter == null,
                    onClick = { onEvent(HistoryEvent.OnStockFilterSelected(null)) },
                    label = { Text("All") }
                )
                FilterChip(
                    selected = uiState.selectedStockFilter == TransactionType.STOCK_IN,
                    onClick = { onEvent(HistoryEvent.OnStockFilterSelected(TransactionType.STOCK_IN)) },
                    label = { Text("Stock In") }
                )
                FilterChip(
                    selected = uiState.selectedStockFilter == TransactionType.STOCK_OUT,
                    onClick = { onEvent(HistoryEvent.OnStockFilterSelected(TransactionType.STOCK_OUT)) },
                    label = { Text("Stock Out") }
                )
            }
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val dateRangeText = if (uiState.startDate != null && uiState.endDate != null) {
                val df = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                if (uiState.startDate == uiState.endDate) {
                    df.format(Date(uiState.startDate))
                } else {
                    "${df.format(Date(uiState.startDate))} - ${df.format(Date(uiState.endDate))}"
                }
            } else "Filter by Date Range"
            
            Button(
                onClick = { showDatePicker = true },
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (uiState.startDate != null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.primary,
                    contentColor = if (uiState.startDate != null) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(Icons.Default.DateRange, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(dateRangeText)
            }

            if (uiState.startDate != null) {
                IconButton(onClick = { onEvent(HistoryEvent.OnDateRangeSelected(null, null)) }) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear Filter")
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDateRangePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    onEvent(HistoryEvent.OnDateRangeSelected(
                        datePickerState.selectedStartDateMillis,
                        datePickerState.selectedEndDateMillis
                    ))
                    showDatePicker = false
                }) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DateRangePicker(state = datePickerState, modifier = Modifier.height(400.dp))
        }
    }
}

@Composable
fun HistorySummaryHeader(
    summary: HistorySummary,
    tab: HistoryTab,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
        ),
        shape = MaterialTheme.shapes.extraLarge,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = summary.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date()),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SummaryItem(
                    label = summary.label1,
                    amount = summary.value1,
                    color = if (tab == HistoryTab.SALES) Color(0xFF1B5E20) else Color(0xFF0D47A1),
                    icon = if (tab == HistoryTab.SALES) Icons.Default.TrendingUp else Icons.Default.AddShoppingCart,
                    modifier = Modifier.weight(1f)
                )
                VerticalDivider(
                    modifier = Modifier.height(48.dp).align(Alignment.CenterVertically),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
                SummaryItem(
                    label = summary.label2,
                    amount = summary.value2,
                    color = if (tab == HistoryTab.SALES) Color(0xFFE65100) else Color(0xFFB71C1C),
                    icon = if (tab == HistoryTab.SALES) Icons.Default.Analytics else Icons.Default.Inventory2,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SummaryItem(
    label: String,
    amount: Double,
    color: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = "$${String.format(Locale.US, "%,.2f", amount)}",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
    }
}

@Composable
fun DateHeader(date: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background
    ) {
        Text(
            text = date,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = 8.dp)
        )
    }
}

@Composable
fun TransactionCard(
    transaction: TransactionModel,
    dateFormatter: SimpleDateFormat,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (icon, color) = when (transaction.type) {
        TransactionType.SALE -> Icons.Default.PointOfSale to MaterialTheme.colorScheme.primary
        TransactionType.STOCK_IN -> Icons.Default.ShoppingCart to Color(0xFF1B5E20)
        TransactionType.STOCK_OUT -> Icons.Default.Inventory2 to Color(0xFFB71C1C)
    }

    ElevatedCard(
        modifier = modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = MaterialTheme.shapes.medium,
                color = color.copy(alpha = 0.1f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.padding(8.dp).size(24.dp)
                )
            }
            
            Spacer(Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when (transaction.type) {
                        TransactionType.SALE -> "Sale: ${transaction.transactionId.takeLast(6)}"
                        TransactionType.STOCK_IN -> "Stock In / Purchase"
                        TransactionType.STOCK_OUT -> "Stock Out / Removal"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = dateFormatter.format(Date(transaction.timestamp)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = "${if (transaction.type == TransactionType.STOCK_IN) "-" else "+"}$${
                    String.format(Locale.US, "%.2f", transaction.netAmount)
                }",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (transaction.type == TransactionType.SALE) color else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun EmptyHistoryState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.History,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "No transactions found",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun HistoryContentPreview() {
    SMETheme(dynamicColor = false) {
        Surface {
            HistoryContent(
                uiState = HistoryUiState(
                    filteredTransactions = listOf(
                        TransactionModel(
                            "TXN-2024-001",
                            TransactionType.SALE,
                            System.currentTimeMillis(),
                            45.5,
                            discount = 0.4,
                            netAmount = 45.1,
                            "user1"
                        )
                    ),
                    isLoading = false,
                    selectedTab = HistoryTab.SALES
                ),
                onNavigateToDetail = {},
                onOpenDrawer = {},
                onEvent = {}
            )
        }
    }
}
