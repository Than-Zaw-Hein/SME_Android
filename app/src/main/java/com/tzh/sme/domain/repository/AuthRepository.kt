package com.tzh.sme.domain.repository

import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val currentUser: StateFlow<User?>
    val isInitialized: StateFlow<Boolean>
    suspend fun signInWithEmail(email: String, password: String): Result<Unit>
    suspend fun signUpWithEmail(name: String, phone: String, address: String, email: String, password: String): Result<Unit>
    suspend fun signInWithGoogle(idToken: String): Result<Unit>
    suspend fun signOut()
    suspend fun sendPasswordResetEmail(email: String?): Result<Unit>
    suspend fun sendVerificationCode(email: String): Result<Unit>
    suspend fun verifyCode(email: String, code: String): Result<Unit>
    suspend fun isEmailRegistered(email: String): Result<Boolean>
    suspend fun changePassword(oldPass: String, newPass: String): Result<Unit>
}

data class User(
    val id: String,
    val email: String,
    val displayName: String?,
    val phone: String? = null,
    val address: String? = null
)
