package com.tzh.sme.domain.usecase.auth

import com.tzh.sme.domain.repository.AuthRepository
import javax.inject.Inject

class SignInUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(email: String, pass: String): Result<Unit> {
        return repository.signInWithEmail(email, pass)
    }
}
