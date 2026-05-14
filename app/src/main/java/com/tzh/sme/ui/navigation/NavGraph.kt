package com.tzh.sme.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.tzh.sme.ui.auth.AuthViewModel
import com.tzh.sme.ui.auth.LoginScreen
import com.tzh.sme.ui.auth.ProfileScreen
import com.tzh.sme.ui.auth.SignupScreen
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
import com.tzh.sme.ui.pos.SettingsType
import com.tzh.sme.ui.settings.SettingsDetailScreen
import com.tzh.sme.ui.stock.StockViewModel
import kotlinx.serialization.Serializable

@Serializable
sealed interface Screen {
    @Serializable
    object Login : Screen

    @Serializable
    object Signup : Screen

    @Serializable
    object Profile : Screen

    @Serializable
    object POS : Screen

    @Serializable
    object Stock : Screen

    @Serializable
    object History : Screen

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
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
           ) {
        composable<Screen.Login> {
            val viewModel: AuthViewModel = hiltViewModel()
            LoginScreen(
                viewModel = viewModel,
                onLoginSuccess = {
                    navController.navigate(Screen.POS) {
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
            SignupScreen(
                viewModel = viewModel,
                onSignupSuccess = {
                    navController.navigate(Screen.POS) {
                        popUpTo(Screen.Login) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                }
            )
        }
        composable<Screen.Profile> {
            val viewModel: AuthViewModel = hiltViewModel()
            ProfileScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onLogout = {
                    navController.navigate(Screen.Login) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        composable<Screen.POS> {
            val viewModel: PosViewModel = hiltViewModel()
            PosScreen(
                viewModel = viewModel,
                onNavigateToCheckout = {
                    viewModel.defaultCheckOutUiState()
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
                onNavigateBack = { navController.popBackStack() }
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
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable<Screen.EditProduct> {
            val viewModel: ProductDetailViewModel = hiltViewModel()
            ProductDetailScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
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
        composable<Screen.TransactionDetail> {
            val viewModel: TransactionDetailViewModel = hiltViewModel()
            TransactionDetailScreen(
                viewModel = viewModel,
                onNavigateUp = { navController.popBackStack() }
            )
        }
        composable<Screen.Languages> {
            SettingsDetailScreen(
                type = SettingsType.LANGUAGES,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable<Screen.ContactUs> {
            val contactViewModel: ContactViewModel = hiltViewModel()
            ContactScreen(
                viewModel = contactViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable<Screen.FAQ> {
            SettingsDetailScreen(
                type = SettingsType.FAQ,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable<Screen.PrivacyPolicy> {
            SettingsDetailScreen(
                type = SettingsType.PRIVACY_POLICY,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable<Screen.TermsOfService> {
            SettingsDetailScreen(
                type = SettingsType.TERMS_OF_SERVICE,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
