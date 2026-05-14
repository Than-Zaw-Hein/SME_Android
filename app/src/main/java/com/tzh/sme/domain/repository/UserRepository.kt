package com.tzh.sme.domain.repository

import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun getUserById(userId: String): Flow<User?>
    suspend fun updateProfile(user: User): Result<Unit>
}
