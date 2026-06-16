package com.tzh.sme.domain.usecase.auth

import com.tzh.sme.domain.repository.AuthRepository
import com.tzh.sme.domain.repository.UserRole
import javax.inject.Inject

class UpdateStaffAccountUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(userId: String, name: String, phone: String, role: UserRole): Result<Unit> {
        return repository.updateStaffAccount(userId, name,phone, role)
    }
}
