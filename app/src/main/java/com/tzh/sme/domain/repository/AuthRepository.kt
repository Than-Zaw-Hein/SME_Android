package com.tzh.sme.domain.repository

import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val currentUser: StateFlow<User?>
    suspend fun signInWithEmail(email: String, password: String): Result<Unit>
    suspend fun signInWithGoogle(): Result<Unit>
    suspend fun signOut()
}

data class User(
    val id: String,
    val email: String,
    val displayName: String?
)
