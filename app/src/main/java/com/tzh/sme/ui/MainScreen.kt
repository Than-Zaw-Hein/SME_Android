package com.tzh.sme.ui

import android.Manifest
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tzh.sme.BuildConfig
import com.tzh.sme.ui.pos.DrawerItem
import com.tzh.sme.ui.pos.SettingsType
import kotlinx.coroutines.launch
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.tzh.sme.R
import com.tzh.sme.ui.auth.AuthViewModel
import com.tzh.sme.ui.navigation.NavGraph
import com.tzh.sme.ui.navigation.Screen as NavScreen

enum class MainTab(val labelRes: Int, val icon: @Composable () -> Unit, val route: Any) {
    POS(
        R.string.nav_pos,
        { Icon(Icons.Default.PointOfSale, contentDescription = null) },
        NavScreen.POS
    ),
    Stock(
        R.string.nav_stock,
        { Icon(Icons.Default.Inventory, contentDescription = null) },
        NavScreen.Stock
    ),
    History(
        R.string.nav_history,
        { Icon(Icons.Default.History, contentDescription = null) },
        NavScreen.History
    ),
    Profile(
        R.string.nav_profile,
        { Icon(Icons.Default.Person, contentDescription = null) },
        NavScreen.Profile
    )
}

@Composable
fun MainScreen(
    windowWidthSizeClass: WindowWidthSizeClass,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val user by viewModel.currentUser.collectAsState()
    val isInitialized by viewModel.isInitialized.collectAsState(initial = false)

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissions.entries.forEach {
            Log.d("MainScreen", "${it.key} = ${it.value}")
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
                )
            )
        }
    }

    if (!isInitialized) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    val startDestination = if (user != null) NavScreen.POS else NavScreen.Login

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Show drawer for main tabs only
    val showDrawer = MainTab.entries.any { tab ->
        currentDestination?.hasRoute(tab.route::class) == true
    }

    if (showDrawer) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "SME Business",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.titleLarge
                    )
                    HorizontalDivider()
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {


                        Text(
                            "Main Menu",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                        )

                        MainTab.entries.forEach { tab ->
                            NavigationDrawerItem(
                                label = { Text(stringResource(tab.labelRes)) },
                                selected = currentDestination?.hasRoute(tab.route::class) == true,
                                onClick = {
                                    scope.launch { drawerState.close() }
                                    if (currentDestination?.hasRoute(tab.route::class) == true) return@NavigationDrawerItem

                                    navController.navigate(tab.route) {
                                        popUpTo(MainTab.POS.route) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = tab.icon,
                                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                            )
                        }

                        Spacer(Modifier.weight(1f))

                        HorizontalDivider()
                        Text(
                            "Settings & Support",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                        )

                        SettingsType.entries.forEach { type ->
                            DrawerItem(
                                icon = type.icon,
                                label = type.label,
                                value = when (type) {
                                    SettingsType.LANGUAGES -> "English (en)"
                                    SettingsType.VERSIONS -> "V${BuildConfig.VERSION_NAME}"
                                    else -> null
                                },
                                showChevron = type.hasChevron,
                                onClick = {
                                    scope.launch { drawerState.close() }
                                    when (type) {
                                        SettingsType.LANGUAGES -> navController.navigate(NavScreen.Languages)
                                        SettingsType.CONTACT_US -> navController.navigate(NavScreen.ContactUs)
                                        SettingsType.FAQ -> navController.navigate(NavScreen.FAQ)
                                        SettingsType.PRIVACY_POLICY -> navController.navigate(NavScreen.PrivacyPolicy)
                                        SettingsType.TERMS_OF_SERVICE -> navController.navigate(NavScreen.TermsOfService)
                                        SettingsType.VERSIONS -> { /* display-only */ }
                                    }
                                }
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
        ) {
            NavGraph(
                navController = navController,
                windowWidthSizeClass = windowWidthSizeClass,
                startDestination = startDestination,
                onOpenDrawer = { scope.launch { drawerState.open() } }
            )
        }
    } else {
        NavGraph(
            navController = navController,
            windowWidthSizeClass = windowWidthSizeClass,
            startDestination = startDestination,
            onOpenDrawer = { scope.launch { drawerState.open() } }
        )
    }
}
