package com.tzh.sme.domain.usecase.auth

import com.tzh.sme.data.model.ShopModel
import com.tzh.sme.domain.repository.UserRepository
import javax.inject.Inject

class UpdateShopUseCase @Inject constructor(
    private val repository: UserRepository
) {
    suspend operator fun invoke(shop: ShopModel): Result<Unit> {
        return repository.updateShop(shop)
    }
}
