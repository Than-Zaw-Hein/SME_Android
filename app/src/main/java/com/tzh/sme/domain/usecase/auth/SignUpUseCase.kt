package com.tzh.sme.domain.usecase.auth

import com.tzh.sme.domain.repository.AuthRepository
import javax.inject.Inject

class SignUpUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(
        name: String,
        phone: String,
        address: String,
        email: String,
        pass: String,
        shopName: String,
        latitude: Double? = null,
        longitude: Double? = null
    ): Result<Unit> {
        return repository.signUpWithEmail(
            name, phone, address, email, pass, shopName, latitude, longitude
        )
    }
}
