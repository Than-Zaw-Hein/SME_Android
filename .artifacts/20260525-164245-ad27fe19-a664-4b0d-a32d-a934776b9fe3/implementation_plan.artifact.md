# Email Verification Implementation Plan

The goal is to implement an email verification flow during signup to ensure users provide valid email addresses. Currently, the app allows signup with any email string without validation.

## User Review Required

- **Verification Method**: I recommend using **Firebase Email Verification (Link-based)** as it is natively supported and secure.
- **Code vs Link**: Although you asked for "code verification", Firebase Auth standardly uses links. If you strictly require a 6-digit code, a backend service (Cloud Functions) would be needed. For this implementation, I will provide a **Link-based verification flow** but include a UI placeholder for a code if you choose to add a custom OTP service later.

## Proposed Changes

### Domain Layer (Repository Interface)

#### [AuthRepository.kt](file:///C:/Users/thanz/OneDrive/Desktop/Development/TZH/SME_Android/app/src/main/java/com/tzh/sme/domain/repository/AuthRepository.kt)

- Add `sendEmailVerification()` method.
- Add `reloadUser()` to refresh the email verification status.
- Add `isEmailVerified` property to the `User` data class or a separate check.

### Data Layer (Implementation)

#### [AuthRepositoryImpl.kt](file:///C:/Users/thanz/OneDrive/Desktop/Development/TZH/SME_Android/app/src/main/java/com/tzh/sme/data/repository/AuthRepositoryImpl.kt)

- Implement `sendEmailVerification()` using `auth.currentUser?.sendEmailVerification()`.
- Implement `reloadUser()` using `auth.currentUser?.reload()`.
- Update the `AuthStateListener` to include the `isEmailVerified` status in the `User` object.

### UI Layer (ViewModel & Screens)

#### [AuthViewModel.kt](file:///C:/Users/thanz/OneDrive/Desktop/Development/TZH/SME_Android/app/src/main/java/com/tzh/sme/ui/auth/AuthViewModel.kt)

- Add a state to track if the user is in the verification pending state.
- Add `checkVerificationStatus()` to refresh and check if the user has clicked the link.

#### [AuthUiState.kt](file:///C:/Users/thanz/OneDrive/Desktop/Development/TZH/SME_Android/app/src/main/java/com/tzh/sme/ui/auth/AuthUiState.kt)

- Add a `VerificationPending` state.

#### [NEW] [EmailVerificationScreen.kt](file:///C:/Users/thanz/OneDrive/Desktop/Development/TZH/SME_Android/app/src/main/java/com/tzh/sme/ui/auth/EmailVerificationScreen.kt)

- Create a new screen that shows when verification is required.
- Buttons for "I've Verified" and "Resend Email".

#### [NavGraph.kt](file:///C:/Users/thanz/OneDrive/Desktop/Development/TZH/SME_Android/app/src/main/java/com/tzh/sme/ui/navigation/NavGraph.kt)

- Add `EmailVerification` route.

---

## Verification Plan

### Automated Tests
- No new automated tests planned; manual verification is primary for this flow.

### Manual Verification
1. **Signup Flow**:
   - Create a new account with a real email.
   - Verify that the app navigates to the `EmailVerificationScreen`.
   - Verify that an email is received from Firebase.
   - Click the link in the email.
   - Click "I've Verified" in the app and ensure it proceeds to the main screen.
2. **Resend Logic**:
   - Test the "Resend Email" button.
3. **Invalid Email**:
   - Verify that an invalid email format is caught by Firebase.
