package com.tzh.sme.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.tzh.sme.R
import com.tzh.sme.ui.navigation.NavGraph
import com.tzh.sme.ui.navigation.Screen as NavScreen

enum class MainTab(val labelRes: Int, val icon: @Composable () -> Unit, val route: Any) {
    POS(R.string.nav_pos, { Icon(Icons.Default.PointOfSale, contentDescription = null) }, NavScreen.POS),
    Stock(R.string.nav_stock, { Icon(Icons.Default.Inventory, contentDescription = null) }, NavScreen.Stock),
    History(R.string.nav_history, { Icon(Icons.Default.History, contentDescription = null) }, NavScreen.History)
}

@Composable
fun MainScreen(windowWidthSizeClass: WindowWidthSizeClass) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    
    // Bottom Navigation Bar should show for only root screens (MainTab routes)
    val showNavSuite = MainTab.entries.any { tab -> 
        currentDestination?.hasRoute(tab.route::class) == true 
    }

    AnimatedContent(
        targetState = showNavSuite,
        transitionSpec = {
            (fadeIn(animationSpec = tween(300, delayMillis = 90)) + scaleIn(initialScale = 0.92f, animationSpec = tween(300, delayMillis = 90)))
                .togetherWith(fadeOut(animationSpec = tween(90)))
        },
        label = "NavSuiteTransition"
    ) { isRoot ->
        if (isRoot) {
            NavigationSuiteScaffold(
                navigationSuiteItems = {
                    MainTab.entries.forEach { tab ->
                        item(
                            selected = currentDestination?.hasRoute(tab.route::class) == true,
                            onClick = {
                                if (currentDestination?.hasRoute(tab.route::class) == true) return@item

                                navController.navigate(tab.route) {
                                    popUpTo(MainTab.POS.route) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = tab.icon,
                            label = { Text(stringResource(tab.labelRes)) }
                        )
                    }
                }
            ) {
                NavGraph(navController = navController, windowWidthSizeClass = windowWidthSizeClass)
            }
        } else {
            NavGraph(navController = navController, windowWidthSizeClass = windowWidthSizeClass)
        }
    }
}
