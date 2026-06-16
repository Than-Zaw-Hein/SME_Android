package com.tzh.sme.domain.usecase.common

import com.tzh.sme.domain.repository.AuthRepository
import com.tzh.sme.domain.repository.User
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class GetAuthStateUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    val currentUser: StateFlow<User?> = repository.currentUser
    val isInitialized: StateFlow<Boolean> = repository.isInitialized
}
