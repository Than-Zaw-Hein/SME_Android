package com.tzh.sme.domain.usecase.common

import com.tzh.sme.data.DefaultData.defaultCategory
import com.tzh.sme.data.model.CategoryModel
import com.tzh.sme.domain.repository.StockRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetCategoriesUseCase @Inject constructor(
    private val repository: StockRepository
) {
    operator fun invoke(): Flow<List<CategoryModel>> {
        return repository.getAllCategories().map { list ->
            listOf(defaultCategory) + list
        }
    }
}
