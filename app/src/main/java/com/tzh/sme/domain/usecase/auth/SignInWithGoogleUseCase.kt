package com.tzh.sme.domain.usecase.auth

import com.tzh.sme.domain.repository.AuthRepository
import javax.inject.Inject

class SignInWithGoogleUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(idToken: String): Result<Unit> {
        return repository.signInWithGoogle(idToken)
    }
}
