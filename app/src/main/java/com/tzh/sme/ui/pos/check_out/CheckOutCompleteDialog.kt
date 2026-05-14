package com.tzh.sme.ui.pos.check_out

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tzh.sme.data.model.TransactionItemModel
import com.tzh.sme.data.model.TransactionModel
import java.util.Locale

@Composable
fun CheckOutCompleteDialog(
    transaction: TransactionModel,
    items: List<TransactionItemModel>,
    isPrinting: Boolean,
    printError: String,
    onPrint: () -> Unit,
    onClose: () -> Unit,
    onGoHome: () -> Unit
) {
    AlertDialog(
        onDismissRequest = if (isPrinting) ({}) else onClose,
        icon = {
            if (isPrinting) {
                CircularProgressIndicator(modifier = Modifier.size(48.dp))
            } else {
                Icon(
                    if (printError.isEmpty()) Icons.Default.CheckCircle else Icons.Default.Error,
                    contentDescription = null,
                    tint = if (printError.isEmpty()) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(48.dp)
                )
            }
        },
        title = {
            Text(
                text = if (isPrinting) "Printing Receipt..." else if (printError.isEmpty()) "Checkout Successful" else "Printing Failed",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (printError.isNotEmpty()) {
                    Text(
                        text = printError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Text(
                    text = "Transaction ID: ${transaction.transactionId.takeLast(8).uppercase(Locale.US)}",
                    style = MaterialTheme.typography.bodyMedium
                )

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total Amount:")
                    Text("$${String.format(Locale.US, "%.2f", transaction.totalAmount)}")
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Discount:")
                    Text("-$${String.format(Locale.US, "%.2f", transaction.discount)}", color = MaterialTheme.colorScheme.error)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Net Total:", fontWeight = FontWeight.Bold)
                    Text(
                        "$${String.format(Locale.US, "%.2f", transaction.netAmount)}",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "What would you like to do next?",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onPrint,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isPrinting
            ) {
                Icon(Icons.Default.Print, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (printError.isEmpty()) "Print Receipt" else "Reprint Receipt")
            }
        },
        dismissButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onGoHome,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isPrinting
                ) {
                    Icon(Icons.Default.Home, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Go to Home")
                }

                TextButton(
                    onClick = onClose,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isPrinting
                ) {
                    Icon(Icons.Default.Close, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Close")
                }
            }
        }
    )
}
