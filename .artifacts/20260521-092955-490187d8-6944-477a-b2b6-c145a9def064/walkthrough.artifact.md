# Admin Web App Walkthrough

I have created a standalone **Compose Multiplatform** web application for SME administration. This project is located in `C:/Users/thanz/OneDrive/Desktop/Development/TZH/sme-admin-web/`.

## Key Features Implemented

1.  **Standalone Architecture**: A completely independent Gradle project with its own configuration.
2.  **Material 3 UI**: Replicated the mobile app's theme and design language.
3.  **Core Screens**:
    - **Login**: Entry point with email/password fields.
    - **Stock Management**: Searchable list of products with price and quantity.
    - **Staff Management**: List of staff members with add/delete functionality.
    - **Transaction History**: List of recent sales and stock movements.

## Project Structure

- `settings.gradle.kts`: Project name and repositories.
- `build.gradle.kts`: Compose Multiplatform (Wasm) configuration.
- `src/wasmJsMain/kotlin/`: Core logic and UI components.
- `src/wasmJsMain/resources/`: HTML host file.

## How to Run

To run the web application, navigate to the `sme-admin-web` folder in your terminal and execute:

```bash
./gradlew wasmJsBrowserDevelopmentRun
```

This will start a development server and open the app in your default browser.

## Next Steps

- **Real Data Integration**: Currently, the screens use dummy data. You can integrate your Firebase or Retrofit logic by adding the necessary dependencies and implementing the repositories in the `admin-web` project.
- **Shared Logic**: In the future, you can move common data models and logic to a shared Kotlin Multiplatform module to avoid duplication between Android and Web.
