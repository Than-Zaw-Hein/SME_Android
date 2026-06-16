package com.tzh.sme.domain.usecase.auth

import com.tzh.sme.domain.repository.AuthRepository
import javax.inject.Inject

class RemoveStaffUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(userId: String): Result<Unit> {
        return repository.removeStaff(userId)
    }
}
