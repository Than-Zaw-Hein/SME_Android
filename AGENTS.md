# AGENTS.md

## Build & Toolchain
- `./gradlew assembleDebug` — build debug APK
- `./gradlew test` — unit tests; `./gradlew connectedAndroidTest` — instrumented tests
- Gradle **9.3.1**, JDK **21** (auto-resolved via foojay toolchain in `gradle-daemon-jvm.properties`)
- Kotlin **2.0.21**, AGP **8.7.3**, Compose BOM **2024.11.00**, compileSdk/targetSdk **35**, minSdk **29**
- Java target: **11** (`jvmTarget = "11"` in app/build.gradle.kts)
- **No CI**, **no lint/formatting config** (no ktlint, detekt, spotless, checkstyle, editorconfig)
- ProGuard/minification is **off** (`isMinifyEnabled = false`)

## Architecture
- **Single-module** Android project (`:app` only), package `com.tzh.sme`
- **Clean Architecture**: `data/` (repositories + Firestore/Retrofit), `domain/` (interfaces + usecases), `ui/` (Compose screens + ViewModels), `di/` (Hilt modules)
- **MVVM** with Jetpack Compose + Material3
- **Dagger Hilt** for DI — use `ksp` (not kapt). Annotations required on: `@HiltAndroidApp` (Application), `@AndroidEntryPoint` (Activity), `@HiltViewModel` (ViewModels), `@HiltWorker` (Workers)
- **Type-safe navigation**: all routes defined as `@Serializable sealed interface Screen` in `ui/navigation/NavGraph.kt`. Navigate with `navController.navigate(Screen.POS)` etc., NOT string routes.
- `buildConfig = true` (enabled in app/build.gradle.kts) — `BuildConfig.DEBUG` etc. are available

## Firebase & Backend
- `google-services.json` is **checked into the repo** — no need to generate/download one
- Primary backend: **Firebase** (Firestore, Auth, Storage) + Firebase AppCheck (debug in dev, Play Integrity in release)
- Secondary backend: local file server via **Retrofit** at hardcoded `http://192.168.1.6/LocalFileServer/` (`data/remote/FileApiService.kt:17`) — `usesCleartextTraffic=true` in AndroidManifest is for this
- Debug AppCheck token: `C91B00E1-E341-4E8C-8D04-767E6881211F`

## Key Libraries
- **CameraX + ML Kit** for barcode scanning (ui/inventory)
- **ESC/POS** for Bluetooth receipt printing
- **Apache POI** for Excel export (via `data/worker/ExportWorker.kt`)
- **Coil** for image loading (configured in `SMEApplication` with custom cache sizes)
- Credential Manager API (`androidx.credentials`) for Google Sign-In

## App Behavior
- App requests **Bluetooth permissions** (`BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT`) on launch for Android 12+
- Auth gate in `MainScreen.kt`: shows loader until Firebase auth state initializes, then routes to Login or POS
- Main navigation is a `ModalNavigationDrawer` with tabs: POS, Stock, History, Profile
- `MainActivity.kt` uses `enableEdgeToEdge()` and `WindowSizeClass` adaptive layout

## Testing
- Tests are minimal placeholder stubs (`ExampleUnitTest`, `ExampleInstrumentedTest`)
- Use `hiltViewModel()` in test contexts if testing composables with ViewModels
