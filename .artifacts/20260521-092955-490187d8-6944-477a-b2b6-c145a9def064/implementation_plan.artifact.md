# Admin Web App Implementation Plan (Standalone)

Create a **standalone** web application for administrators using **Compose Multiplatform (Wasm/JS)**. This project will be located in a new root directory `sme-admin-web` and will operate independently of the main mobile project while replicating its Material 3 design and functionality.

## User Review Required

- **Standalone Nature**: Since this is a standalone project, it will have its own `settings.gradle.kts` and `gradle.properties`. It will not share code directly via Gradle module dependencies in this phase (though logic can be copied/adapted).
- **Firebase Configuration**: A separate `firebase-config.js` or similar setup for Web might be needed if using Firebase features.

## Proposed Changes

### Project Structure (Standalone)

I will create a new directory `sme-admin-web` at the same level as the `sme_android` project or as a sibling directory. For this task, I will place it at `C:/Users/thanz/OneDrive/Desktop/Development/TZH/sme-admin-web/`.

#### [NEW] [sme-admin-web/build.gradle.kts](file:///C:/Users/thanz/OneDrive/Desktop/Development/TZH/sme-admin-web/build.gradle.kts)
- Configure Compose Multiplatform for Web (Wasm).
- Add dependencies for Compose, Navigation, and Material 3.

#### [NEW] [sme-admin-web/settings.gradle.kts](file:///C:/Users/thanz/OneDrive/Desktop/Development/TZH/sme-admin-web/settings.gradle.kts)
- Root project definition for the standalone web app.

---

### Core Web Components [NEW]

#### [NEW] [sme-admin-web/src/wasmJsMain/kotlin/Main.kt](file:///C:/Users/thanz/OneDrive/Desktop/Development/TZH/sme-admin-web/src/wasmJsMain/kotlin/Main.kt)
- Entry point for the web application.
- Sets up the root Composable, Navigation Host, and Theme.

#### [NEW] [sme-admin-web/src/wasmJsMain/resources/index.html](file:///C:/Users/thanz/OneDrive/Desktop/Development/TZH/sme-admin-web/src/wasmJsMain/resources/index.html)
- HTML host for the Wasm application.

---

### Web Screens [NEW]

Replicate the mobile screens using web-friendly Compose components.

#### [NEW] [LoginScreenWeb.kt](file:///C:/Users/thanz/OneDrive/Desktop/Development/TZH/sme-admin-web/src/wasmJsMain/kotlin/com/tzh/sme/web/ui/auth/LoginScreenWeb.kt)
- Web-adapted Login screen with email/password fields.

#### [NEW] [StockManagementWeb.kt](file:///C:/Users/thanz/OneDrive/Desktop/Development/TZH/sme-admin-web/src/wasmJsMain/kotlin/com/tzh/sme/web/ui/stock/StockManagementWeb.kt)
- Adapted version of `StockManagementScreen` for the web.

#### [NEW] [StaffManagementWeb.kt](file:///C:/Users/thanz/OneDrive/Desktop/Development/TZH/sme-admin-web/src/wasmJsMain/kotlin/com/tzh/sme/web/ui/staff/StaffManagementWeb.kt)
- Adapted version of `StaffManagementScreen` for the web.

#### [NEW] [TransactionHistoryWeb.kt](file:///C:/Users/thanz/OneDrive/Desktop/Development/TZH/sme-admin-web/src/wasmJsMain/kotlin/com/tzh/sme/web/ui/history/TransactionHistoryWeb.kt)
- Adapted version of `HistoryScreen` for the web.

## Verification Plan

### Manual Verification
- **Build the web app**: Run `./gradlew wasmJsBrowserDevelopmentRun` inside the `sme-admin-web` folder.
- **Login Flow**: Verify the Login screen appears first and navigates to the dashboard.
- **UI Consistency**: Compare web screens with mobile app screenshots/behavior.
- **Navigation**: Verify switching between Stock, Staff, and Transaction tabs.
