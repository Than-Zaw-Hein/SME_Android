package com.tzh.sme.ui.history

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.tzh.sme.data.local.entities.TransactionEntity
import com.tzh.sme.data.local.entities.TransactionType
import com.tzh.sme.ui.theme.SMETheme

@Preview(showBackground = true)
@Composable
fun HistoryContentPreview() {
    SMETheme {
        Surface {
            HistoryContent(
                uiState = HistoryUiState.Success(
                    transactions = listOf(
                        TransactionEntity(1, TransactionType.SALE, System.currentTimeMillis(), 45.5, "user1"),
                        TransactionEntity(2, TransactionType.STOCK_IN, System.currentTimeMillis() - 3600000, 0.0, "user1"),
                        TransactionEntity(3, TransactionType.SALE, System.currentTimeMillis() - 7200000, 120.0, "user1")
                    )
                )
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HistoryLoadingPreview() {
    SMETheme {
        Surface {
            HistoryContent(uiState = HistoryUiState.Loading)
        }
    }
}
