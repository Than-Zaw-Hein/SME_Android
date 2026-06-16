package com.tzh.sme.data.repository

import android.annotation.SuppressLint
import com.dantsu.escposprinter.EscPosPrinter
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections
import com.tzh.sme.data.model.ShopModel
import com.tzh.sme.data.model.TransactionItemModel
import com.tzh.sme.data.model.TransactionModel
import com.tzh.sme.domain.repository.PrinterRepository
import com.tzh.sme.domain.repository.User
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class PrinterRepositoryImpl @Inject constructor() : PrinterRepository {

    @SuppressLint("MissingPermission")
    override suspend fun printReceipt(
        transaction: TransactionModel,
        items: List<TransactionItemModel>,
        shop: ShopModel?,
        user: User?
    ): Result<Unit> {
        return try {
            val printers = BluetoothPrintersConnections.selectFirstPaired()
            if (printers != null) {
                val printer = EscPosPrinter(printers, 203, 48f, 32)
                val date = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(
                    Date(transaction.timestamp)
                )

                var receiptText =
                    "[C]<b>${shop?.name?.uppercase(Locale.US) ?: "SME BUSINESS"}</b>\n" +
                    "[C]${shop?.address ?: ""}\n" +
                    (if (!user?.phone.isNullOrBlank()) "[C]Tel: ${user?.phone}\n" else "") +
                    "[C]--------------------------------\n"

                receiptText +=
                    "[L]Date: $date\n" +
                    "[L]Cashier: ${transaction.userName ?: user?.displayName ?: "Staff"}\n" +
                    "[L]Order ID: ${transaction.transactionId.takeLast(8)}\n" +
                    "[C]--------------------------------\n"

                items.forEach { item ->
                    val itemTotal = item.quantity * item.priceAtTime
                    receiptText += "[L]${item.productName}\n" +
                                   "[L]  ${item.quantity} x $${String.format(Locale.US, "%.2f", item.priceAtTime)} [R]$${String.format(Locale.US, "%.2f", itemTotal)}\n"
                }

                receiptText +=
                    "[C]--------------------------------\n" +
                    "[L]TOTAL [R]$${String.format(Locale.US, "%.2f", transaction.totalAmount)}\n" +
                    "[L]DISCOUNT [R]$${String.format(Locale.US, "%.2f", transaction.discount)}\n" +
                    "[L]<b>NET TOTAL</b> [R]<b>$${String.format(Locale.US, "%.2f", transaction.netAmount)}</b>\n" +
                    "[C]--------------------------------\n"

                receiptText +=
                    "[C]Thank you for your visit!\n" +
                    "[C]<qrcode size='20'>${transaction.transactionId}</qrcode>\n" +
                    "\n\n\n"

                printer.printFormattedText(receiptText)
                Result.success(Unit)
            } else {
                Result.failure(Exception("No paired bluetooth printer found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
