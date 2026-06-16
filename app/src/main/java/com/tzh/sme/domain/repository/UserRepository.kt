package com.tzh.sme.domain.repository

import com.tzh.sme.data.model.ShopModel
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun getUserById(userId: String): Flow<User?>
    suspend fun updateProfile(user: User,shopName : String): Result<Unit>
    fun getShopById(shopId: String): Flow<ShopModel?>
    suspend fun updateShop(shop: ShopModel): Result<Unit>
}
