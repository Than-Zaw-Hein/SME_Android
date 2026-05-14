package com.tzh.sme.ui.auth

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.tzh.sme.domain.repository.AuthRepository
import com.tzh.sme.domain.repository.User
import com.tzh.sme.domain.repository.UserRepository
import com.tzh.sme.ui.stock.ProductDetailEffect
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _effect = Channel<AuthUiState>()
    val effect = _effect.receiveAsFlow()

    val currentUser = authRepository.currentUser
    val isInitialized = authRepository.isInitialized

    fun signIn(email: String, pass: String, onLoginSuccess: () -> Unit) {
        viewModelScope.launch {
            _effect.send(AuthUiState.Loading)
            val result = authRepository.signInWithEmail(email, pass)
            if (result.isSuccess) {
                _effect.send(AuthUiState.Success("Login Successful"))
                onLoginSuccess()
            } else {
                _effect.send(AuthUiState.Error(result.exceptionOrNull()?.message ?: "Login Failed"))
            }
        }
    }

    // 1. Add this function to your AuthViewModel
    fun resetPassword() {
        viewModelScope.launch {
            _effect.send(AuthUiState.Loading)
            val result = authRepository.sendPasswordResetEmail(currentUser.value?.email)
            if (result.isSuccess) {
                _effect.send(AuthUiState.Success("Reset link sent to ${currentUser.value?.email}"))
            } else {
                _effect.send(
                    AuthUiState.Error(
                        result.exceptionOrNull()?.message ?: "Failed to send reset email"
                    )
                )
            }
        }
    }

    fun signUp(
        name: String,
        phone: String,
        address: String,
        email: String,
        pass: String,
        onSignUpSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _effect.send(AuthUiState.Loading)
            val result = authRepository.signUpWithEmail(name, phone, address, email, pass)
            if (result.isSuccess) {
                _effect.send(AuthUiState.Success("Account Created Successfully"))
                onSignUpSuccess()
            } else {
                _effect.send(
                    AuthUiState.Error(result.exceptionOrNull()?.message ?: "Signup Failed")
                )
            }
        }
    }

    private fun signInWithGoogle(idToken: String, onLoginSuccess: () -> Unit) {
        viewModelScope.launch {
            _effect.send(AuthUiState.Loading)
            val result = authRepository.signInWithGoogle(idToken)
            if (result.isSuccess) {
                _effect.send(AuthUiState.Success("Login Successful"))
                onLoginSuccess()
            } else {
                _effect.send(AuthUiState.Error("Google Sign-In Failed"))
            }
        }
    }

    fun startGoogleSignIn(context: Context, onLoginSuccess: () -> Unit) {
        val credentialManager = CredentialManager.create(context)

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId("527416293780-l1o2su0di0nt89544i4mvqhood43tkeq.apps.googleusercontent.com")
            .setAutoSelectEnabled(true)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        viewModelScope.launch {
            try {
                val result = credentialManager.getCredential(context, request)
                val credential = result.credential

                if (credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val googleIdTokenCredential =
                        GoogleIdTokenCredential.createFrom(credential.data)
                    signInWithGoogle(googleIdTokenCredential.idToken, onLoginSuccess)
                }
            } catch (e: GetCredentialException) {
                Log.e("Auth", "Full Error: ${e.javaClass.simpleName} - ${e.message}")
                _effect.send(AuthUiState.Error(e.message ?: "Failed"))
            }
        }
    }

    fun signOut(onSignOut: () -> Unit) {
        viewModelScope.launch {
            authRepository.signOut()
            onSignOut()
        }
    }

    fun sendVerificationCode(email: String) {
        viewModelScope.launch {
            _effect.send(AuthUiState.Loading)
            
            // Check if email exists first
            val emailCheck = authRepository.isEmailRegistered(email)
            if (emailCheck.isSuccess && emailCheck.getOrDefault(false)) {
                _effect.send(AuthUiState.Error("An account with this email already exists. Please login instead."))
                return@launch
            }

            val result = authRepository.sendVerificationCode(email)
            if (result.isSuccess) {
                _effect.send(AuthUiState.Success("Verification code sent to $email"))
            } else {
                _effect.send(
                    AuthUiState.Error(
                        result.exceptionOrNull()?.message ?: "Failed to send code"
                    )
                )
            }
        }
    }

    fun verifyCode(email: String, code: String, onVerificationSuccess: () -> Unit) {
        viewModelScope.launch {
            _effect.send(AuthUiState.Loading)
            val result = authRepository.verifyCode(email, code)
            if (result.isSuccess) {
                _effect.send(AuthUiState.Success("Email Verified"))
                onVerificationSuccess()
            } else {
                _effect.send(
                    AuthUiState.Error(
                        result.exceptionOrNull()?.message ?: "Verification Failed"
                    )
                )
            }
        }
    }

    fun updateProfile(name: String, phone: String, address: String) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            _effect.send(AuthUiState.Loading)
            val updatedUser = user.copy(displayName = name, phone = phone, address = address)
            val result = userRepository.updateProfile(updatedUser)
            if (result.isSuccess) {
                _effect.send(AuthUiState.Success("Profile Updated"))
            } else {
                _effect.send(
                    AuthUiState.Error(result.exceptionOrNull()?.message ?: "Update Failed")
                )
            }
        }
    }

    fun changePassword(oldPass: String, newPass: String) {
        viewModelScope.launch {
            _effect.send(AuthUiState.Loading)
            val result = authRepository.changePassword(oldPass, newPass)
            if (result.isSuccess) {
                _effect.send(AuthUiState.Success("Password changed successfully"))
            } else {
                _effect.send(
                    AuthUiState.Error(result.exceptionOrNull()?.message ?: "Failed to change password")
                )
            }
        }
    }
}
