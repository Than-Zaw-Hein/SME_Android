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
import com.tzh.sme.domain.repository.User
import com.tzh.sme.domain.repository.UserRole
import com.tzh.sme.domain.usecase.auth.AuthUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authUseCases: AuthUseCases
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = combine(
        _uiState,
        authUseCases.getAuthState.currentUser,
        authUseCases.getAuthState.isInitialized,
        authUseCases.getAuthState.currentUser.flatMapLatest { user ->
            user?.shopId?.let { authUseCases.getShopById(it) } ?: flowOf(null)
        }) { state, user, initialized, shop ->
        state.copy(
            currentUser = user, isInitialized = initialized, currentShop = shop
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AuthUiState())

    private val _sideEffect = MutableSharedFlow<AuthUiSideEffect>()
    val sideEffect = _sideEffect.asSharedFlow()

    fun sendIntent(intent: AuthUiIntent) {
        when (intent) {
            is AuthUiIntent.SignIn -> signIn(intent.email, intent.pass, intent.onLoginSuccess)
            is AuthUiIntent.SignUp -> signUp(
                intent.name,
                intent.phone,
                intent.address,
                intent.email,
                intent.pass,
                intent.shopName,
                intent.latitude,
                intent.longitude,
                intent.onSignUpSuccess
            )

            AuthUiIntent.SignOut -> signOut()
            AuthUiIntent.ResetPassword -> resetPassword()
            AuthUiIntent.SendVerificationEmail -> sendVerificationEmail()
            is AuthUiIntent.CheckVerificationStatus -> checkVerificationStatus(intent.onVerified)
            is AuthUiIntent.StartGoogleSignIn -> startGoogleSignIn(
                intent.context, intent.onLoginSuccess
            )

            is AuthUiIntent.UpdateProfile -> updateProfile(intent.name, intent.phone)
            is AuthUiIntent.UpdateShop -> updateShop(
                intent.name, intent.address, intent.latitude, intent.longitude
            )

            is AuthUiIntent.ChangePassword -> changePassword(intent.oldPass, intent.newPass)
            is AuthUiIntent.LoadShopStaff -> loadShopStaff(intent.shopId)
            is AuthUiIntent.CreateStaffAccount -> createStaffAccount(
                intent.name, intent.email, intent.pass, intent.phone, intent.role
            )

            is AuthUiIntent.UpdateStaffAccount -> updateStaffAccount(
                intent.userId, intent.name, intent.phone, intent.role
            )

            is AuthUiIntent.RemoveStaff -> removeStaff(intent.userId)
        }
    }

    private fun signIn(email: String, pass: String, onLoginSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = authUseCases.signIn(email, pass)
            _uiState.update { it.copy(isLoading = false) }
            if (result.isSuccess) {
                val user = authUseCases.getAuthState.currentUser.value
                if (user != null && !user.isEmailVerified) {
                    _sideEffect.emit(AuthUiSideEffect.VerificationPending)
                } else {
                    _sideEffect.emit(AuthUiSideEffect.Success("Login Successful"))
                    onLoginSuccess()
                }
            } else {
                _sideEffect.emit(
                    AuthUiSideEffect.Error(result.exceptionOrNull()?.message ?: "Login Failed")
                )
            }
        }
    }

    private fun signUp(
        name: String,
        phone: String,
        address: String,
        email: String,
        pass: String,
        shopName: String,
        latitude: Double?,
        longitude: Double?,
        onSignUpSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = authUseCases.signUp(
                name, phone, address, email, pass, shopName, latitude, longitude
            )
            _uiState.update { it.copy(isLoading = false) }
            if (result.isSuccess) {
                _sideEffect.emit(AuthUiSideEffect.VerificationPending)
                onSignUpSuccess()
            } else {
                _sideEffect.emit(
                    AuthUiSideEffect.Error(result.exceptionOrNull()?.message ?: "Signup Failed")
                )
            }
        }
    }

    private fun signOut() {
        viewModelScope.launch {
            authUseCases.signOut()
        }
    }

    private fun resetPassword() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val email = uiState.value.currentUser?.email
            val result = authUseCases.resetPassword(email)
            _uiState.update { it.copy(isLoading = false) }
            if (result.isSuccess) {
                _sideEffect.emit(AuthUiSideEffect.Success("Reset link sent to $email"))
            } else {
                _sideEffect.emit(
                    AuthUiSideEffect.Error(
                        result.exceptionOrNull()?.message ?: "Failed to send reset email"
                    )
                )
            }
        }
    }

    private fun sendVerificationEmail() {
        viewModelScope.launch {
            val result = authUseCases.sendVerificationEmail()
            if (result.isSuccess) {
                _sideEffect.emit(AuthUiSideEffect.Success("Verification email sent"))
            } else {
                _sideEffect.emit(
                    AuthUiSideEffect.Error(
                        result.exceptionOrNull()?.message ?: "Failed to send verification"
                    )
                )
            }
        }
    }

    private fun checkVerificationStatus(onVerified: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = authUseCases.checkVerificationStatus()
            if (result.isSuccess && result.getOrNull() == true) {
                _sideEffect.emit(AuthUiSideEffect.Success("Email Verified"))
                onVerified()
            } else {
                _sideEffect.emit(AuthUiSideEffect.Error("Email not yet verified. Please check your inbox."))
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private fun startGoogleSignIn(context: Context, onLoginSuccess: () -> Unit) {
        val credentialManager = CredentialManager.create(context)
        val googleIdOption = GetGoogleIdOption.Builder().setFilterByAuthorizedAccounts(false)
            .setServerClientId("527416293780-l1o2su0di0nt89544i4mvqhood43tkeq.apps.googleusercontent.com")
            .setAutoSelectEnabled(true).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()

        viewModelScope.launch {
            try {
                val result = credentialManager.getCredential(context, request)
                val credential = result.credential
                if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdTokenCredential =
                        GoogleIdTokenCredential.createFrom(credential.data)
                    signInWithGoogle(googleIdTokenCredential.idToken, onLoginSuccess)
                }
            } catch (e: GetCredentialException) {
                Log.e("Auth", "Full Error: ${e.javaClass.simpleName} - ${e.message}")
                _sideEffect.emit(AuthUiSideEffect.Error(e.message ?: "Failed"))
            }
        }
    }

    private fun signInWithGoogle(idToken: String, onLoginSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = authUseCases.signInWithGoogle(idToken)
            _uiState.update { it.copy(isLoading = false) }
            if (result.isSuccess) {
                _sideEffect.emit(AuthUiSideEffect.Success("Login Successful"))
                onLoginSuccess()
            } else {
                _sideEffect.emit(AuthUiSideEffect.Error("Google Sign-In Failed"))
            }
        }
    }

    private fun updateProfile(name: String, phone: String) {
        val user = uiState.value.currentUser ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val updatedUser = user.copy(displayName = name, phone = phone)
            val result =
                authUseCases.updateProfile(updatedUser, uiState.value.currentShop?.name ?: "")
            _uiState.update { it.copy(isLoading = false) }
            if (result.isSuccess) {
                _sideEffect.emit(AuthUiSideEffect.Success("Profile Updated"))
            } else {
                _sideEffect.emit(
                    AuthUiSideEffect.Error(result.exceptionOrNull()?.message ?: "Update Failed")
                )
            }
        }
    }

    private fun updateShop(name: String, address: String, latitude: Double?, longitude: Double?) {
        val shop = uiState.value.currentShop ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val updatedShop = shop.copy(
                name = name, address = address, latitude = latitude, longitude = longitude
            )
            val result = authUseCases.updateShop(updatedShop)
            _uiState.update { it.copy(isLoading = false) }
            if (result.isSuccess) {
                _sideEffect.emit(AuthUiSideEffect.Success("Shop Settings Updated"))
            } else {
                _sideEffect.emit(
                    AuthUiSideEffect.Error(result.exceptionOrNull()?.message ?: "Update Failed")
                )
            }
        }
    }

    private fun changePassword(oldPass: String, newPass: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = authUseCases.changePassword(oldPass, newPass)
            _uiState.update { it.copy(isLoading = false) }
            if (result.isSuccess) {
                _sideEffect.emit(AuthUiSideEffect.Success("Password changed successfully"))
            } else {
                _sideEffect.emit(
                    AuthUiSideEffect.Error(
                        result.exceptionOrNull()?.message ?: "Failed to change password"
                    )
                )
            }
        }
    }

    private fun loadShopStaff(shopId: String) {
        viewModelScope.launch {
            authUseCases.getShopStaff(shopId).collect { staff ->
                val sortedStaff = staff.sortedWith(
                    compareBy<User> { it.role }.thenBy { it.displayName?.lowercase() ?: "" }
                )
                _uiState.update { it.copy(staffList = sortedStaff) }
            }
        }
    }

    private fun createStaffAccount(
        name: String, email: String, pass: String, phone: String, role: UserRole
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = authUseCases.createStaffAccount(name, email, pass, phone, role)
            _uiState.update { it.copy(isLoading = false) }
            if (result.isSuccess) {
                _sideEffect.emit(AuthUiSideEffect.Success("Staff account created successfully"))
            } else {
                _sideEffect.emit(
                    AuthUiSideEffect.Error(
                        result.exceptionOrNull()?.message ?: "Failed to create staff"
                    )
                )
            }
        }
    }

    private fun updateStaffAccount(
        userId: String, name: String, phone: String, role: UserRole
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = authUseCases.updateStaffAccount(userId, name, phone, role)
            _uiState.update { it.copy(isLoading = false) }
            if (result.isSuccess) {
                _sideEffect.emit(AuthUiSideEffect.Success("Staff account updated successfully"))
            } else {
                _sideEffect.emit(
                    AuthUiSideEffect.Error(
                        result.exceptionOrNull()?.message ?: "Failed to update staff"
                    )
                )
            }
        }
    }

    private fun removeStaff(userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = authUseCases.removeStaff(userId)
            _uiState.update { it.copy(isLoading = false) }
            if (result.isSuccess) {
                _sideEffect.emit(AuthUiSideEffect.Success("Staff access removed"))
            } else {
                _sideEffect.emit(
                    AuthUiSideEffect.Error(
                        result.exceptionOrNull()?.message ?: "Failed to remove staff"
                    )
                )
            }
        }
    }
}
