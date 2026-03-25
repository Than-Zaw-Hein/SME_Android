package com.tzh.sme.di

import android.content.Context
import androidx.room.Room
import com.tzh.sme.data.local.AppDatabase
import com.tzh.sme.data.local.dao.CategoryDao
import com.tzh.sme.data.local.dao.ProductDao
import com.tzh.sme.data.local.dao.TransactionDao
import com.tzh.sme.data.repository.StockRepositoryImpl
import com.tzh.sme.domain.repository.StockRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "sme_database"
        )
        .fallbackToDestructiveMigration() // Added for development as version changed from 2 to 3
        .build()
    }

    @Provides
    fun provideProductDao(db: AppDatabase): ProductDao = db.productDao()

    @Provides
    fun provideTransactionDao(db: AppDatabase): TransactionDao = db.transactionDao()

    @Provides
    fun provideCategoryDao(db: AppDatabase): CategoryDao = db.categoryDao()

    @Provides
    @Singleton
    fun provideStockRepository(
        productDao: ProductDao,
        transactionDao: TransactionDao,
        categoryDao: CategoryDao
    ): StockRepository {
        return StockRepositoryImpl(productDao, transactionDao, categoryDao)
    }
}
