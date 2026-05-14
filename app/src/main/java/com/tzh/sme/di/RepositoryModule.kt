package com.tzh.sme.di

import com.tzh.sme.data.repository.AuthRepositoryImpl
import com.tzh.sme.data.repository.CategoryRepositoryImpl
import com.tzh.sme.data.repository.FirebaseStorageRepositoryImpl
import com.tzh.sme.data.repository.ProductRepositoryImpl
import com.tzh.sme.data.repository.StockRepositoryImpl
import com.tzh.sme.data.repository.SupplierRepositoryImpl
import com.tzh.sme.data.repository.TransactionRepositoryImpl
import com.tzh.sme.data.repository.UserRepositoryImpl
import com.tzh.sme.domain.repository.AuthRepository
import com.tzh.sme.domain.repository.CategoryRepository
import com.tzh.sme.domain.repository.FileRepository
import com.tzh.sme.domain.repository.ProductRepository
import com.tzh.sme.domain.repository.StockRepository
import com.tzh.sme.domain.repository.SupplierRepository
import com.tzh.sme.domain.repository.TransactionRepository
import com.tzh.sme.domain.repository.UserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindCategoryRepository(
        categoryRepository: CategoryRepositoryImpl
    ): CategoryRepository

    @Binds
    @Singleton
    abstract fun bindProductRepository(
        productRepository: ProductRepositoryImpl
    ): ProductRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(
        userRepository: UserRepositoryImpl
    ): UserRepository

    @Binds
    @Singleton
    abstract fun bindTransactionRepository(
        transactionRepository: TransactionRepositoryImpl
    ): TransactionRepository

    @Binds
    @Singleton
    abstract fun bindStockRepository(
        stockRepositoryImpl: StockRepositoryImpl
    ): StockRepository

    @Binds
    @Singleton
    abstract fun bindFileRepository(
        fileRepositoryImpl: FirebaseStorageRepositoryImpl
    ): FileRepository


    @Binds
    @Singleton
    abstract fun bindSupplierRepository(
        repository: SupplierRepositoryImpl
    ): SupplierRepository
}
