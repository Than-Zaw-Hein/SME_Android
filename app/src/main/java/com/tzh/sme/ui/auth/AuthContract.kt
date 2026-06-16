package com.tzh.sme.ui.auth

import android.content.Context
import com.tzh.sme.data.model.ShopModel
import com.tzh.sme.domain.repository.User
import com.tzh.sme.domain.repository.UserRole

data class AuthUiState(
    val isLoading: Boolean = false,
    val currentUser: User? = null,
    val isInitialized: Boolean = false,
    val currentShop: ShopModel? = null,
    val staffList: List<User> = emptyList()
)

sealed interface AuthUiIntent {
    data class SignIn(val email: String, val pass: String, val onLoginSuccess: () -> Unit) : AuthUiIntent
    data class SignUp(
        val name: String,
        val phone: String,
        val address: String,
        val email: String,
        val pass: String,
        val shopName: String,
        val latitude: Double? = null,
        val longitude: Double? = null,
        val onSignUpSuccess: () -> Unit
    ) : AuthUiIntent
    object SignOut : AuthUiIntent
    object ResetPassword : AuthUiIntent
    object SendVerificationEmail : AuthUiIntent
    data class CheckVerificationStatus(val onVerified: () -> Unit) : AuthUiIntent
    data class StartGoogleSignIn(val context: Context, val onLoginSuccess: () -> Unit) : AuthUiIntent
    data class UpdateProfile(val name: String, val phone: String) : AuthUiIntent
    data class UpdateShop(
        val name: String,
        val address: String,
        val latitude: Double? = null,
        val longitude: Double? = null
    ) : AuthUiIntent
    data class ChangePassword(val oldPass: String, val newPass: String) : AuthUiIntent
    data class LoadShopStaff(val shopId: String) : AuthUiIntent
    data class CreateStaffAccount(val name: String, val email: String, val pass: String,val phone: String, val role: UserRole) : AuthUiIntent
    data class UpdateStaffAccount(val userId: String, val name: String,val phone: String, val role: UserRole) : AuthUiIntent
    data class RemoveStaff(val userId: String) : AuthUiIntent
}

sealed interface AuthUiSideEffect {
    data class Success(val message: String) : AuthUiSideEffect
    data class Error(val message: String) : AuthUiSideEffect
    object VerificationPending : AuthUiSideEffect
    object NavigateBack : AuthUiSideEffect
}
