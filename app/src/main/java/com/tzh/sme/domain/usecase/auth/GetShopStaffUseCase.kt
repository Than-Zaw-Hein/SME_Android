package com.tzh.sme.domain.usecase.auth

import com.tzh.sme.domain.repository.AuthRepository
import com.tzh.sme.domain.repository.User
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetShopStaffUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    operator fun invoke(shopId: String): Flow<List<User>> {
        return repository.getShopStaff(shopId)
    }
}
