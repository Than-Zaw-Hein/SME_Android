package com.tzh.sme.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.tzh.sme.data.local.dao.CategoryDao
import com.tzh.sme.data.local.dao.ProductDao
import com.tzh.sme.data.local.dao.TransactionDao
import com.tzh.sme.data.local.entities.CategoryEntity
import com.tzh.sme.data.local.entities.ProductConverters
import com.tzh.sme.data.local.entities.ProductEntity
import com.tzh.sme.data.local.entities.TransactionEntity
import com.tzh.sme.data.local.entities.TransactionItemEntity

@Database(
    entities = [ProductEntity::class, TransactionEntity::class, TransactionItemEntity::class, CategoryEntity::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(ProductConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
}
