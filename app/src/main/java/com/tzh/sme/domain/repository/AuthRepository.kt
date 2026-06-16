package com.tzh.sme.domain.repository

import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val currentUser: StateFlow<User?>
    val isInitialized: StateFlow<Boolean>
    suspend fun signInWithEmail(email: String, password: String): Result<Unit>
    suspend fun signUpWithEmail(
        name: String,
        phone: String,
        address: String,
        email: String,
        password: String,
        shopName: String,
        latitude: Double? = null,
        longitude: Double? = null
    ): Result<Unit>
    suspend fun signInWithGoogle(idToken: String): Result<Unit>
    suspend fun signOut()
    suspend fun sendPasswordResetEmail(email: String?): Result<Unit>
    suspend fun changePassword(oldPass: String, newPass: String): Result<Unit>
    suspend fun sendEmailVerification(): Result<Unit>
    suspend fun reloadUser(): Result<Unit>

    // Staff Management
    fun getShopStaff(shopId: String): kotlinx.coroutines.flow.Flow<List<User>>
    suspend fun createStaffAccount(name: String, email: String, pass: String,phone: String, role: UserRole): Result<Unit>
    suspend fun updateStaffAccount(userId: String, name: String, phone: String, role: UserRole): Result<Unit>
    suspend fun removeStaff(userId: String): Result<Unit>
}

enum class UserRole {
    ADMIN, SELLER, BUYER, FINANCE
}

data class User(
    val id: String,
    val email: String,
    val displayName: String?,
    val phone: String? = null,
    val shopId: String? = null,
    val shopName: String? = null,
    val role: UserRole = UserRole.ADMIN,
    val isEmailVerified: Boolean = false
)
