package com.tzh.sme.ui.navigation

import android.util.Log
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.tzh.sme.ui.SettingsType
import com.tzh.sme.ui.auth.AuthUiIntent
import com.tzh.sme.ui.auth.AuthUiSideEffect
import com.tzh.sme.ui.auth.AuthViewModel
import com.tzh.sme.ui.auth.LoginScreen
import com.tzh.sme.ui.auth.ProfileScreen
import com.tzh.sme.ui.auth.StaffManagementScreen
import com.tzh.sme.ui.auth.SignupScreen
import com.tzh.sme.ui.auth.EmailVerificationScreen
import com.tzh.sme.ui.history.HistoryScreen
import com.tzh.sme.ui.history.HistoryViewModel
import com.tzh.sme.ui.history.TransactionDetailScreen
import com.tzh.sme.ui.history.TransactionDetailViewModel
import com.tzh.sme.ui.pos.check_out.CheckOutScreen
import com.tzh.sme.ui.pos.PosScreen
import com.tzh.sme.ui.pos.PosViewModel
import com.tzh.sme.ui.stock.ProductDetailScreen
import com.tzh.sme.ui.stock.ProductDetailViewModel
import com.tzh.sme.ui.stock.StockManagementScreen
import com.tzh.sme.ui.contact.ContactScreen
import com.tzh.sme.ui.contact.ContactViewModel
import com.tzh.sme.ui.settings.SettingsDetailScreen
import com.tzh.sme.ui.stock.StockViewModel
import com.tzh.sme.ui.supplier.SupplierDetailScreen
import com.tzh.sme.ui.supplier.SupplierManagementScreen
import com.tzh.sme.ui.supplier.SupplierViewModel
import kotlinx.serialization.Serializable

import com.tzh.sme.domain.repository.UserRole

@Serializable
sealed interface Screen {
    @Serializable
    object Login : Screen

    @Serializable
    object Signup : Screen

    @Serializable
    object EmailVerification : Screen

    @Serializable
    object Profile : Screen

    @Serializable
    object StaffManagement : Screen

    @Serializable
    object POS : Screen

    @Serializable
    object Stock : Screen

    @Serializable
    object History : Screen

    @Serializable
    object SupplierManagement : Screen

    @Serializable
    object AddSupplier : Screen

    @Serializable
    data class SupplierDetail(val supplierId: String) : Screen

    @Serializable
    object Checkout : Screen

    @Serializable
    object AddProduct : Screen

    @Serializable
    data class EditProduct(val productId: String) : Screen

    @Serializable
    data class TransactionDetail(val transactionId: String) : Screen

    @Serializable
    object Languages : Screen

    @Serializable
    object ContactUs : Screen

    @Serializable
    object FAQ : Screen

    @Serializable
    object PrivacyPolicy : Screen

    @Serializable
    object TermsOfService : Screen
}

@Composable
fun NavGraph(
    navController: NavHostController,
    windowWidthSizeClass: WindowWidthSizeClass,
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier,
    startDestination: Screen = Screen.Login
) {
    // Prevent double-click navigation / white screen issue
    val safePopBackStack: () -> Unit = {
        if (navController.previousBackStackEntry != null) {
            navController.popBackStack()
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        composable<Screen.Login> {
            val viewModel: AuthViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            androidx.compose.runtime.LaunchedEffect(viewModel.sideEffect) {
                viewModel.sideEffect.collect { effect ->
                    if (effect is AuthUiSideEffect.VerificationPending) {
                        navController.navigate(Screen.EmailVerification)
                    }
                }
            }

            LoginScreen(
                viewModel = viewModel,
                onLoginSuccess = {
                    val role = uiState.currentUser?.role
                    val destination = when (role) {
                        UserRole.ADMIN, UserRole.SELLER -> Screen.POS
                        UserRole.BUYER -> Screen.Stock
                        UserRole.FINANCE -> Screen.History
                        null -> Screen.POS
                    }
                    navController.navigate(destination) {
                        popUpTo(Screen.Login) { inclusive = true }
                    }
                },
                onNavigateToSignup = {
                    navController.navigate(Screen.Signup)
                }
            )
        }
        composable<Screen.Signup> {
            val viewModel: AuthViewModel = hiltViewModel()

            androidx.compose.runtime.LaunchedEffect(viewModel.sideEffect) {
                viewModel.sideEffect.collect { effect ->
                    if (effect is AuthUiSideEffect.VerificationPending) {
                        navController.navigate(Screen.EmailVerification) {
                            popUpTo(Screen.Signup) { inclusive = true }
                        }
                    }
                }
            }

            SignupScreen(
                viewModel = viewModel,
                onSignupSuccess = {
                    // This is now handled by the side effect for VerificationPending
                },
                onNavigateToLogin = {
                    safePopBackStack()
                }
            )
        }
        composable<Screen.EmailVerification> {
            val viewModel: AuthViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            EmailVerificationScreen(
                viewModel = viewModel,
                onVerified = {
                    val role = uiState.currentUser?.role
                    val destination = when (role) {
                        UserRole.ADMIN, UserRole.SELLER -> Screen.POS
                        UserRole.BUYER -> Screen.Stock
                        UserRole.FINANCE -> Screen.History
                        null -> Screen.POS
                    }
                    navController.navigate(destination) {
                        popUpTo(Screen.Login) { inclusive = true }
                    }
                },
                onNavigateBack = {
                    viewModel.sendIntent(AuthUiIntent.SignOut)
                    navController.navigate(Screen.Login) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        composable<Screen.Profile> {
            val viewModel: AuthViewModel = hiltViewModel()
            ProfileScreen(
                viewModel = viewModel,
                onNavigateBack = { safePopBackStack() },
                onLogout = {
                    navController.navigate(Screen.Login) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        composable<Screen.StaffManagement> {
            val viewModel: AuthViewModel = hiltViewModel()
            StaffManagementScreen(
                viewModel = viewModel,
                onNavigateBack = { safePopBackStack() }
            )
        }
        composable<Screen.POS> {
            val viewModel: PosViewModel = hiltViewModel()
            PosScreen(
                viewModel = viewModel,
                onNavigateToCheckout = {
                    viewModel.sendIntent(com.tzh.sme.ui.pos.PosUiIntent.ResetCheckoutState)
                    navController.navigate(Screen.Checkout)
                },
                onOpenDrawer = onOpenDrawer
            )
        }
        composable<Screen.Checkout> { backStackEntry ->
            val posBackStackEntry = remember(backStackEntry) {
                navController.getBackStackEntry(Screen.POS)
            }
            val viewModel: PosViewModel = hiltViewModel(posBackStackEntry)
            CheckOutScreen(
                viewModel = viewModel,
                windowWidthSizeClass = windowWidthSizeClass,
                onNavigateBack = { safePopBackStack() }
            )
        }
        composable<Screen.Stock> {
            val viewModel: StockViewModel = hiltViewModel()
            StockManagementScreen(
                viewModel = viewModel,
                onNavigateToAddProduct = {
                    navController.navigate(Screen.AddProduct)
                },
                onNavigateToEditProduct = { productId ->
                    navController.navigate(Screen.EditProduct(productId))
                },
                onOpenDrawer = onOpenDrawer
            )
        }
        composable<Screen.AddProduct> {
            val viewModel: ProductDetailViewModel = hiltViewModel()
            ProductDetailScreen(
                viewModel = viewModel,
                onNavigateBack = { safePopBackStack() }
            )
        }
        composable<Screen.EditProduct> {
            val viewModel: ProductDetailViewModel = hiltViewModel()
            ProductDetailScreen(
                viewModel = viewModel,
                onNavigateBack = { safePopBackStack() }
            )
        }
        composable<Screen.History> {
            val viewModel: HistoryViewModel = hiltViewModel()
            HistoryScreen(
                viewModel = viewModel,
                onNavigateToDetail = { transactionId ->
                    navController.navigate(Screen.TransactionDetail(transactionId))
                },
                onOpenDrawer = onOpenDrawer
            )
        }
        composable<Screen.SupplierManagement> {
            val viewModel: SupplierViewModel = hiltViewModel()
            SupplierManagementScreen(
                viewModel = viewModel,
                onNavigateToAddSupplier = {
                    navController.navigate(Screen.AddSupplier)
                },
                onNavigateToSupplierDetail = { supplierId ->
                    navController.navigate(Screen.SupplierDetail(supplierId))
                },
                onOpenDrawer = onOpenDrawer
            )
        }
        composable<Screen.AddSupplier> {
            val viewModel: SupplierViewModel = hiltViewModel()
            SupplierDetailScreen(
                viewModel = viewModel,
                supplierId = null,
                onNavigateBack = { safePopBackStack() }
            )
        }
        composable<Screen.SupplierDetail> {
            val viewModel: SupplierViewModel = hiltViewModel()
            SupplierDetailScreen(
                viewModel = viewModel,
                supplierId = it.arguments?.getString("supplierId"),
                onNavigateBack = { safePopBackStack() }
            )
        }
        composable<Screen.TransactionDetail> {
            val viewModel: TransactionDetailViewModel = hiltViewModel()
            TransactionDetailScreen(
                viewModel = viewModel,
                onNavigateUp = { safePopBackStack() }
            )
        }
        composable<Screen.Languages> {
            SettingsDetailScreen(
                type = SettingsType.LANGUAGES,
                onNavigateBack = { safePopBackStack() }
            )
        }
        composable<Screen.ContactUs> {
            val contactViewModel: ContactViewModel = hiltViewModel()
            ContactScreen(
                viewModel = contactViewModel,
                onNavigateBack = { safePopBackStack() }
            )
        }
        composable<Screen.FAQ> {
            SettingsDetailScreen(
                type = SettingsType.FAQ,
                onNavigateBack = { safePopBackStack() }
            )
        }
        composable<Screen.PrivacyPolicy> {
            SettingsDetailScreen(
                type = SettingsType.PRIVACY_POLICY,
                onNavigateBack = { safePopBackStack() }
            )
        }
        composable<Screen.TermsOfService> {
            SettingsDetailScreen(
                type = SettingsType.TERMS_OF_SERVICE,
                onNavigateBack = { safePopBackStack() }
            )
        }
    }
}
