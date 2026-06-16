package com.tzh.sme.domain.usecase.auth

import com.tzh.sme.domain.repository.AuthRepository
import javax.inject.Inject

class ChangePasswordUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(oldPass: String, newPass: String): Result<Unit> {
        return repository.changePassword(oldPass, newPass)
    }
}
