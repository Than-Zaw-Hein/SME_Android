package com.tzh.sme.domain.usecase.stock

import com.tzh.sme.data.model.CategoryModel
import com.tzh.sme.domain.repository.CategoryRepository
import javax.inject.Inject

class UpdateCategoryUseCase @Inject constructor(
    private val repository: CategoryRepository
) {
    suspend operator fun invoke(category: CategoryModel): Result<Unit> {
        return try {
            repository.updateCategory(category)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
