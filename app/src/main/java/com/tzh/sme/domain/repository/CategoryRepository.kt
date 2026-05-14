package com.tzh.sme.domain.repository

import com.tzh.sme.data.model.CategoryModel
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun getAllCategories(): Flow<List<CategoryModel>>
    suspend fun addCategory(category: CategoryModel)
    suspend fun updateCategory(category: CategoryModel)
    suspend fun deleteCategory(category: CategoryModel)
}
