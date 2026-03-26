package com.tzh.sme.domain.repository

import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val currentUser: StateFlow<User?>
    suspend fun signInWithEmail(email: String, password: String): Result<Unit>
    suspend fun signUpWithEmail(name: String, phone: String, address: String, email: String, password: String): Result<Unit>
    suspend fun signInWithGoogle(idToken: String): Result<Unit>
    suspend fun signOut()
}

data class User(
    val id: String,
    val email: String,
    val displayName: String?,
    val phone: String? = null,
    val address: String? = null
)
