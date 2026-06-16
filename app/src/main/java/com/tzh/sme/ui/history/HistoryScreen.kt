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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HistoryContent(
        uiState = uiState,
        onIntent = viewModel::sendIntent,
        onNavigateToDetail = onNavigateToDetail,
        onOpenDrawer = onOpenDrawer
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
    onIntent: (HistoryUiIntent) -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onOpenDrawer: () -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }
    val sectionFormatter = remember { SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault()) }

    val summaryData by remember(
        uiState.filteredTransactions,
        uiState.selectedTab,
        uiState.searchQuery,
        uiState.startDate
    ) {
        derivedStateOf {
            val transactions = uiState.filteredTransactions
            val tab = uiState.selectedTab
            val isTodayRange = if (uiState.startDate != null && uiState.endDate != null) {
                val today = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0); set(
                    Calendar.MINUTE,
                    0
                ); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                uiState.startDate == today
            } else false

            if (tab == HistoryTab.SALES) {
                val title = when {
                    isTodayRange -> "Today's Sales Summary"; uiState.startDate != null -> "Sales Period Summary"; uiState.searchQuery.isNotEmpty() -> "Sales Search Summary"; else -> "All Sales Summary"
                }
                val total = transactions.sumOf { it.netAmount }
                val totalCost = transactions.sumOf { it.totalCost }
                HistorySummary(
                    title = title,
                    label1 = "Total Sales",
                    value1 = total,
                    label2 = "Net Profit",
                    value2 = total - totalCost
                )
            } else {
                val title = when {
                    isTodayRange -> "Today's Stock Summary"; uiState.startDate != null -> "Stock Period Summary"; else -> "All Stock Summary"
                }
                HistorySummary(
                    title = title,
                    label1 = "Stock In",
                    value1 = transactions.filter { it.type == TransactionType.STOCK_IN }
                        .sumOf { it.netAmount },
                    label2 = "Stock Out",
                    value2 = transactions.filter { it.type == TransactionType.STOCK_OUT }
                        .sumOf { it.netAmount })
            }
        }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier.windowInsetsPadding(
                    WindowInsets.safeDrawing.only(
                        WindowInsetsSides.Horizontal
                    )
                )
            ) {
                TopAppBar(
                    title = { Text(stringResource(R.string.history_title)) },
                    navigationIcon = {
                        IconButton(onClick = onOpenDrawer) {
                            Icon(
                                Icons.AutoMirrored.Filled.Notes,
                                "Menu"
                            )
                        }
                    })
                PrimaryTabRow(
                    selectedTabIndex = uiState.selectedTab.ordinal,
                    containerColor = MaterialTheme.colorScheme.background,
                    divider = {}) {
                    HistoryTab.entries.forEach { tab ->
                        Tab(
                            selected = uiState.selectedTab == tab,
                            onClick = { onIntent(HistoryUiIntent.SelectTab(tab)) },
                            text = {
                                Text(
                                    text = if (tab == HistoryTab.SALES) "Sales" else "Stock History",
                                    style = MaterialTheme.typography.titleSmall
                                )
                            })
                    }
                }
                FilterContent(uiState, onIntent)
            }
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
        } else if (uiState.error != null) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) { Text(uiState.error!!, color = MaterialTheme.colorScheme.error) }
        } else {
            if (uiState.filteredTransactions.isEmpty()) {
                EmptyHistoryState(modifier = Modifier.padding(padding))
            } else {
                val groupedTransactions = remember(uiState.filteredTransactions) {
                    uiState.filteredTransactions.groupBy { tx ->
                        val cal = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
                        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(
                        Calendar.SECOND,
                        0
                    ); cal.set(Calendar.MILLISECOND, 0)
                        cal.timeInMillis
                    }
                }
                LazyColumn(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item { HistorySummaryHeader(summary = summaryData, tab = uiState.selectedTab) }
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
fun FilterContent(uiState: HistoryUiState, onIntent: (HistoryUiIntent) -> Unit) {
    var showDatePicker by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (uiState.selectedTab == HistoryTab.SALES) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { onIntent(HistoryUiIntent.UpdateSearchQuery(it)) },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Search by ID", fontSize = 14.sp) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            null,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )
            } else {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FilterChip(
                        selected = uiState.selectedStockFilter == null,
                        onClick = { onIntent(HistoryUiIntent.SelectStockFilter(null)) },
                        label = { Text("All", fontSize = 12.sp) })
                    FilterChip(
                        selected = uiState.selectedStockFilter == TransactionType.STOCK_IN,
                        onClick = { onIntent(HistoryUiIntent.SelectStockFilter(TransactionType.STOCK_IN)) },
                        label = { Text("In", fontSize = 12.sp) })
                    FilterChip(
                        selected = uiState.selectedStockFilter == TransactionType.STOCK_OUT,
                        onClick = { onIntent(HistoryUiIntent.SelectStockFilter(TransactionType.STOCK_OUT)) },
                        label = { Text("Out", fontSize = 12.sp) })
                }
            }
            val dateRangeText = if (uiState.startDate != null && uiState.endDate != null) {
                val df = SimpleDateFormat("dd/MM", Locale.getDefault())
                if (uiState.startDate == uiState.endDate) df.format(Date(uiState.startDate!!)) else "${
                    df.format(
                        Date(uiState.startDate!!)
                    )
                }-${df.format(Date(uiState.endDate!!))}"
            } else "Date"
            FilledTonalButton(
                onClick = { showDatePicker = true },
                shape = MaterialTheme.shapes.medium,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                modifier = Modifier.height(40.dp)
            ) {
                Icon(Icons.Default.DateRange, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(dateRangeText, fontSize = 13.sp)
            }
            if (uiState.startDate != null) {
                IconButton(
                    onClick = { onIntent(HistoryUiIntent.SelectDateRange(null, null)) },
                    modifier = Modifier.size(32.dp)
                ) { Icon(Icons.Default.Clear, "Clear Filter", modifier = Modifier.size(18.dp)) }
            }
        }
    }
    if (showDatePicker) {
        val datePickerState = rememberDateRangePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    onIntent(
                        HistoryUiIntent.SelectDateRange(
                            datePickerState.selectedStartDateMillis,
                            datePickerState.selectedEndDateMillis
                        )
                    ); showDatePicker = false
                }) { Text("Confirm") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) { DateRangePicker(state = datePickerState, modifier = Modifier.height(400.dp)) }
    }
}

@Composable
fun HistorySummaryHeader(summary: HistorySummary, tab: HistoryTab, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
        ),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = summary.title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date()),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryItem(
                    label = summary.label1,
                    amount = summary.value1,
                    color = if (tab == HistoryTab.SALES) Color(0xFF1B5E20) else Color(0xFF0D47A1),
                    icon = if (tab == HistoryTab.SALES) Icons.Default.TrendingUp else Icons.Default.AddShoppingCart,
                    modifier = Modifier.weight(1f)
                )
                VerticalDivider(
                    modifier = Modifier
                        .height(40.dp)
                        .align(Alignment.CenterVertically),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
                SummaryItem(
                    label = summary.label2,
                    amount = summary.value2,
                    color = if (tab == HistoryTab.SALES) (if (summary.value2 >= 0) Color(0xFF1B5E20) else Color(
                        0xFFB71C1C
                    )) else Color(0xFFB71C1C),
                    icon = if (tab == HistoryTab.SALES) Icons.Default.Payments else Icons.Default.Inventory2,
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
    Surface(modifier = modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.background) {
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
        modifier = modifier
            .fillMaxWidth(),
        onClick = onClick
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
                    modifier = Modifier
                        .padding(8.dp)
                        .size(24.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when (transaction.type) {
                        TransactionType.SALE -> "Sale: ${transaction.transactionId.takeLast(6)}"; TransactionType.STOCK_IN -> "Stock In / Purchase"; TransactionType.STOCK_OUT -> "Stock Out / Removal"
                    }, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = dateFormatter.format(Date(transaction.timestamp)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "$${String.format(Locale.US, "%.2f", transaction.netAmount)}",
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
            null,
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
@Composable
fun HistoryContentPreview() {
    SMETheme(dynamicColor = false) {
        Surface {
            HistoryContent(
                uiState = HistoryUiState(
                    filteredTransactions = listOf(
                        TransactionModel(
                            transactionId = "TXN-2024-001",
                            type = TransactionType.SALE,
                            timestamp = System.currentTimeMillis(),
                            totalAmount = 45.5,
                            discount = 0.4,
                            netAmount = 45.1,
                            totalCost = 10.0,
                            userId = "user1"
                        )
                    ), isLoading = false, selectedTab = HistoryTab.SALES
                ),
                onNavigateToDetail = {},
                onOpenDrawer = {},
                onIntent = {}
            )
        }
    }
}
