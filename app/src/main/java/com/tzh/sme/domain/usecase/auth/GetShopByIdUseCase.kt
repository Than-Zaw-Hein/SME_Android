package com.tzh.sme.domain.usecase.auth

import com.tzh.sme.data.model.ShopModel
import com.tzh.sme.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetShopByIdUseCase @Inject constructor(
    private val repository: UserRepository
) {
    operator fun invoke(shopId: String): Flow<ShopModel?> {
        return repository.getShopById(shopId)
    }
}
