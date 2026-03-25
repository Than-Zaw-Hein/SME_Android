package com.tzh.sme.data.repository

import com.tzh.sme.domain.repository.AuthRepository
import com.tzh.sme.domain.repository.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor() : AuthRepository {

    private val _currentUser = MutableStateFlow<User?>(null)
    override val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    override suspend fun signInWithEmail(email: String, password: String): Result<Unit> {
        // Placeholder implementation
        return if (email.isNotEmpty() && password.length >= 6) {
            _currentUser.value = User("1", email, email.substringBefore("@"))
            Result.success(Unit)
        } else {
            Result.failure(Exception("Invalid credentials"))
        }
    }

    override suspend fun signInWithGoogle(): Result<Unit> {
        // Placeholder for Google Sign-In
        _currentUser.value = User("2", "google_user@gmail.com", "Google User")
        return Result.success(Unit)
    }

    override suspend fun signOut() {
        _currentUser.value = null
    }
}
