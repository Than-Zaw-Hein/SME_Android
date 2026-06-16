package com.tzh.sme.domain.usecase.auth

import com.tzh.sme.domain.usecase.common.GetAuthStateUseCase
import com.tzh.sme.domain.usecase.common.GetShopByIdUseCase
import javax.inject.Inject

data class AuthUseCases @Inject constructor(
    val signIn: SignInUseCase,
    val signUp: SignUpUseCase,
    val signOut: SignOutUseCase,
    val resetPassword: ResetPasswordUseCase,
    val getAuthState: GetAuthStateUseCase,
    val sendVerificationEmail: SendVerificationEmailUseCase,
    val checkVerificationStatus: CheckVerificationStatusUseCase,
    val signInWithGoogle: SignInWithGoogleUseCase,
    val updateProfile: UpdateProfileUseCase,
    val updateShop: UpdateShopUseCase,
    val changePassword: ChangePasswordUseCase,
    val getShopStaff: GetShopStaffUseCase,
    val createStaffAccount: CreateStaffAccountUseCase,
    val updateStaffAccount: UpdateStaffAccountUseCase,
    val removeStaff: RemoveStaffUseCase,
    val getShopById: GetShopByIdUseCase
)
