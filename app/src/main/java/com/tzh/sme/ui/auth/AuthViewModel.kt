package com.tzh.sme.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tzh.sme.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState = _uiState.asStateFlow()

    fun signIn(email: String, pass: String, onLoginSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val result = authRepository.signInWithEmail(email, pass)
            if (result.isSuccess) {
                _uiState.value = AuthUiState.Success("Login Successful")
                onLoginSuccess()
            } else {
                _uiState.value = AuthUiState.Error(result.exceptionOrNull()?.message ?: "Login Failed")
            }
        }
    }

    fun signInWithGoogle(onLoginSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val result = authRepository.signInWithGoogle()
            if (result.isSuccess) {
                _uiState.value = AuthUiState.Success("Login Successful")
                onLoginSuccess()
            } else {
                _uiState.value = AuthUiState.Error("Google Sign-In Failed")
            }
        }
    }
}
