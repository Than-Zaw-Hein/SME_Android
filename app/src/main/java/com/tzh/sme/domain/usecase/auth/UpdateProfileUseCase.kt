package com.tzh.sme.domain.usecase.auth

import com.tzh.sme.domain.repository.User
import com.tzh.sme.domain.repository.UserRepository
import javax.inject.Inject

class UpdateProfileUseCase @Inject constructor(
    private val repository: UserRepository
) {
    suspend operator fun invoke(user: User, shopName: String): Result<Unit> {
        return repository.updateProfile(user, shopName)
    }
}
