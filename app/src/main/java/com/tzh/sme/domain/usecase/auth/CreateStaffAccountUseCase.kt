package com.tzh.sme.domain.usecase.auth

import com.tzh.sme.domain.repository.AuthRepository
import com.tzh.sme.domain.repository.UserRole
import javax.inject.Inject

class CreateStaffAccountUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(
        name: String,
        email: String,
        pass: String,
        phone: String,
        role: UserRole
    ): Result<Unit> {
        return repository.createStaffAccount(name, email, pass, phone, role)
    }
}
