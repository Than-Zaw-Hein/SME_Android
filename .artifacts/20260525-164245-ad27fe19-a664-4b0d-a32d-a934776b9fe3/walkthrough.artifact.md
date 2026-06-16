# Email Verification Walkthrough

I have implemented a complete email verification flow using **Firebase Email Verification**. This ensures that users must verify their email address via a link sent to their inbox before they can access the application's core features.

## Changes Overview

### 1. Domain Layer
- **[AuthRepository.kt](file:///C:/Users/thanz/OneDrive/Desktop/Development/TZH/SME_Android/app/src/main/java/com/tzh/sme/domain/repository/AuthRepository.kt)**: Added `sendEmailVerification()` and `reloadUser()` methods.
- **`User` model**: Added `isEmailVerified` property to track the verification status.

### 2. Data Layer
- **[AuthRepositoryImpl.kt](file:///C:/Users/thanz/OneDrive/Desktop/Development/TZH/SME_Android/app/src/main/java/com/tzh/sme/data/repository/AuthRepositoryImpl.kt)**:
    - Implemented Firebase's `sendEmailVerification()`.
    - Implemented `reloadUser()` to fetch the latest verification status from Firebase servers.
    - Updated the `AuthStateListener` to sync the `isEmailVerified` flag.

### 3. UI Layer
- **[AuthViewModel.kt](file:///C:/Users/thanz/OneDrive/Desktop/Development/TZH/SME_Android/app/src/main/java/com/tzh/sme/ui/auth/AuthViewModel.kt)**:
    - Updated `signIn` and `signUp` to check for verification status.
    - Added logic to trigger the `VerificationPending` state if a user logs in but hasn't verified their email.
- **[EmailVerificationScreen.kt](file:///C:/Users/thanz/OneDrive/Desktop/Development/TZH/SME_Android/app/src/main/java/com/tzh/sme/ui/auth/EmailVerificationScreen.kt)**: A new screen that instructs users to check their email and provides buttons to "Resend Email" or check status via "I've Verified".
- **[NavGraph.kt](file:///C:/Users/thanz/OneDrive/Desktop/Development/TZH/SME_Android/app/src/main/java/com/tzh/sme/ui/navigation/NavGraph.kt)**: Integrated the new screen and added side-effects to automatically navigate to verification if needed.

## Verification Summary

### Automated Build
- Verified the project builds successfully with `./gradlew assembleDebug`.

### Manual Flow (for you to test)
1. **Signup**: Create a new account. You should be automatically navigated to the "Verify your Email" screen.
2. **Email Check**: Check the inbox of the email you used. Click the Firebase link.
3. **App Update**: Return to the app and click "I've Verified". It should proceed to the main dashboard.
4. **Login Protection**: Try logging in with an unverified account. The app should block access and show the verification screen.

> [!NOTE]
> Ensure your Firebase project has "Email/Password" sign-in enabled in the Firebase Console (Authentication > Sign-in method).
