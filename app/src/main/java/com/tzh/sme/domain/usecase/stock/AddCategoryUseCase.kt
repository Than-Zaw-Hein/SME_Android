package com.tzh.sme.domain.usecase.stock

import com.tzh.sme.data.model.CategoryModel
import com.tzh.sme.domain.repository.CategoryRepository
import javax.inject.Inject

class AddCategoryUseCase @Inject constructor(
    private val repository: CategoryRepository
) {
    suspend operator fun invoke(category: CategoryModel): Result<CategoryModel?> {
        return try {
            val category = repository.addCategory(category)
            Result.success(category)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
