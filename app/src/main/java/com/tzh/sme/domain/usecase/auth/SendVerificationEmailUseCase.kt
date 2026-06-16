package com.tzh.sme.domain.usecase.auth

import com.tzh.sme.domain.repository.AuthRepository
import javax.inject.Inject

class SendVerificationEmailUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        return repository.sendEmailVerification()
    }
}
