package com.tzh.sme.ui.navigation

import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.tzh.sme.ui.auth.AuthViewModel
import com.tzh.sme.ui.auth.LoginScreen
import com.tzh.sme.ui.history.HistoryScreen
import com.tzh.sme.ui.history.HistoryViewModel
import com.tzh.sme.ui.pos.CheckOutScreen
import com.tzh.sme.ui.pos.PosScreen
import com.tzh.sme.ui.pos.PosViewModel
import com.tzh.sme.ui.stock.ProductDetailScreen
import com.tzh.sme.ui.stock.ProductDetailViewModel
import com.tzh.sme.ui.stock.StockScreen
import com.tzh.sme.ui.stock.StockViewModel
import kotlinx.serialization.Serializable

@Serializable
sealed interface Screen {
    @Serializable object Login : Screen
    @Serializable object POS : Screen
    @Serializable object Stock : Screen
    @Serializable object History : Screen
    @Serializable object Checkout : Screen
    @Serializable object AddProduct : Screen
    @Serializable data class EditProduct(val productId: Long) : Screen
}

@Composable
fun NavGraph(
    navController: NavHostController,
    windowWidthSizeClass: WindowWidthSizeClass,
    modifier: Modifier = Modifier,
    startDestination: Screen = Screen.Login
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable<Screen.Login> {
            val viewModel: AuthViewModel = hiltViewModel()
            LoginScreen(
                viewModel = viewModel,
                onLoginSuccess = {
                    navController.navigate(Screen.POS) {
                        popUpTo(Screen.Login) { inclusive = true }
                    }
                }
            )
        }
        composable<Screen.POS> {
            val viewModel: PosViewModel = hiltViewModel()
            PosScreen(
                viewModel = viewModel,
                windowWidthSizeClass = windowWidthSizeClass,
                onNavigateToCheckout = {
                    navController.navigate(Screen.Checkout)
                }
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
            StockScreen(
                viewModel = viewModel,
                onNavigateToAddProduct = {
                    navController.navigate(Screen.AddProduct)
                },
                onNavigateToEditProduct = { productId ->
                    navController.navigate(Screen.EditProduct(productId))
                }
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
            HistoryScreen(viewModel = viewModel)
        }
    }
}
