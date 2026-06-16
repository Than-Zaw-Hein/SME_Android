package com.tzh.sme.domain.usecase.auth

import com.tzh.sme.domain.repository.AuthRepository
import javax.inject.Inject

class CheckVerificationStatusUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(): Result<Boolean> {
        val reloadResult = repository.reloadUser()
        if (reloadResult.isFailure) return Result.failure(reloadResult.exceptionOrNull()!!)
        val user = repository.currentUser.value
        return Result.success(user?.isEmailVerified == true)
    }
}
