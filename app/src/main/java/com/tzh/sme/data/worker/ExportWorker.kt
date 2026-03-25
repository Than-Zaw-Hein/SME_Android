package com.tzh.sme.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.tzh.sme.data.local.dao.TransactionDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream

@HiltWorker
class ExportWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val transactionDao: TransactionDao
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val transactions = transactionDao.getAllTransactions().first()
            
            val workbook = XSSFWorkbook()
            val sheet = workbook.createSheet("Transactions")
            
            // Header
            val headerRow = sheet.createRow(0)
            headerRow.createCell(0).setCellValue("ID")
            headerRow.createCell(1).setCellValue("Type")
            headerRow.createCell(2).setCellValue("Amount")
            headerRow.createCell(3).setCellValue("Timestamp")

            // Data
            transactions.forEachIndexed { index, tx ->
                val row = sheet.createRow(index + 1)
                row.createCell(0).setCellValue(tx.transactionId.toDouble())
                row.createCell(1).setCellValue(tx.type.name)
                row.createCell(2).setCellValue(tx.totalAmount)
                row.createCell(3).setCellValue(tx.timestamp.toDouble())
            }

            val file = File(applicationContext.getExternalFilesDir(null), "transactions_export.xlsx")
            FileOutputStream(file).use { 
                workbook.write(it)
            }
            workbook.close()

            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
