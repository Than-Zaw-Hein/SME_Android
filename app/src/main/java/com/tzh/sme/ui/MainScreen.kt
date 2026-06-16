package com.tzh.sme.ui

import android.Manifest
import android.os.Build
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.tzh.sme.BuildConfig
import com.tzh.sme.R
import com.tzh.sme.domain.repository.UserRole
import com.tzh.sme.ui.auth.AuthUiIntent
import com.tzh.sme.ui.auth.AuthViewModel
import com.tzh.sme.ui.navigation.NavGraph
import com.tzh.sme.ui.navigation.Screen as NavScreen
import kotlinx.coroutines.launch

enum class MainTab(val labelRes: Int, val icon: @Composable () -> Unit, val route: Any) {
    POS(R.string.nav_pos, { Icon(Icons.Default.PointOfSale, null) }, NavScreen.POS),
    Stock(R.string.nav_stock, { Icon(Icons.Default.Inventory, null) }, NavScreen.Stock),
    History(R.string.nav_history, { Icon(Icons.Default.History, null) }, NavScreen.History),
    StaffManagement(R.string.nav_staff_management, { Icon(Icons.Default.Group, null) }, NavScreen.StaffManagement),
    Suppliers(R.string.nav_suppliers, { Icon(Icons.Default.Business, null) }, NavScreen.SupplierManagement);

    fun isAvailableFor(role: UserRole): Boolean {
        return when (role) {
            UserRole.ADMIN -> true
            UserRole.SELLER -> this == POS
            UserRole.BUYER -> this == Stock || this == Suppliers
            UserRole.FINANCE -> this == History
        }
    }
}

enum class SettingsType(val labelRes: Int, val icon: ImageVector, val hasBadge: Boolean = false, val hasChevron: Boolean = true) {
    LANGUAGES(R.string.settings_languages, Icons.Default.Public, hasBadge = false, hasChevron = false),
    CONTACT_US(R.string.settings_contact_us, Icons.Default.Email, hasBadge = true),
    FAQ(R.string.settings_faq, Icons.AutoMirrored.Filled.HelpOutline, hasBadge = true),
    PRIVACY_POLICY(R.string.settings_privacy_policy, Icons.Default.Folder, hasChevron = true),
    TERMS_OF_SERVICE(R.string.settings_terms_of_service, Icons.Default.Folder, hasChevron = true),
    VERSIONS(R.string.settings_versions, Icons.Default.Info, hasChevron = false)
}

@Composable
fun MainScreen(
    windowWidthSizeClass: WindowWidthSizeClass,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.currentUser, uiState.isInitialized) {
        if (uiState.isInitialized && uiState.currentUser == null) {
            navController.navigate(NavScreen.Login) { popUpTo(0) { inclusive = true } }
        }
    }

    LaunchedEffect(uiState.currentUser?.role, currentDestination) {
        val role = uiState.currentUser?.role ?: return@LaunchedEffect
        val currentTab = MainTab.entries.find { currentDestination?.hasRoute(it.route::class) == true }
        if (currentTab != null && !currentTab.isAvailableFor(role)) {
            val destination = when (role) {
                UserRole.ADMIN, UserRole.SELLER -> NavScreen.POS
                UserRole.BUYER -> NavScreen.Stock
                UserRole.FINANCE -> NavScreen.History
            }
            navController.navigate(destination) { popUpTo(0) { inclusive = true } }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        permissions.entries.forEach { Log.d("MainScreen", "${it.key} = ${it.value}") }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionLauncher.launch(arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT))
        }
    }

    if (!uiState.isInitialized) {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val startDestination = remember(uiState.currentUser) {
        val role = uiState.currentUser?.role
        if (role == null) NavScreen.Login
        else if( role!=null && uiState.currentUser?.isEmailVerified==false){
            NavScreen.EmailVerification
        }
        else when (role) {
            UserRole.ADMIN, UserRole.SELLER -> NavScreen.POS
            UserRole.BUYER -> NavScreen.Stock
            UserRole.FINANCE -> NavScreen.History
        }
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showLogoutDialog by remember { mutableStateOf(false) }

    BackHandler(drawerState.isOpen) { scope.launch { drawerState.close() } }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text(stringResource(R.string.logout)) },
            text = { Text(stringResource(R.string.logout_confirmation)) },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        viewModel.sendIntent(AuthUiIntent.SignOut)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text(stringResource(R.string.yes)) }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text(stringResource(R.string.no)) }
            }
        )
    }

    val showDrawer = MainTab.entries.any { currentDestination?.hasRoute(it.route::class) == true }

    if (showDrawer) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet {
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(end = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            uiState.currentShop?.name ?: stringResource(R.string.app_name_full),
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.titleLarge
                        )
                        IconButton(onClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate(NavScreen.Profile)
                        }) {
                            Icon(Icons.Default.AccountCircle, "Profile", modifier = Modifier.size(32.dp))
                        }
                    }
                    HorizontalDivider()
                    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                        Text(stringResource(R.string.nav_main_menu), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp))
                        MainTab.entries.forEach { tab ->
                            if (uiState.currentUser?.role?.let { tab.isAvailableFor(it) } == true) {
                                NavigationDrawerItem(
                                    label = { Text(stringResource(tab.labelRes)) },
                                    selected = currentDestination?.hasRoute(tab.route::class) == true,
                                    onClick = {
                                        scope.launch { drawerState.close() }
                                        if (currentDestination?.hasRoute(tab.route::class) != true) {
                                            navController.navigate(tab.route) {
                                                popUpTo(MainTab.POS.route) { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    },
                                    icon = tab.icon,
                                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                                )
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Spacer(Modifier.weight(1f))
                        HorizontalDivider()
                        Text(stringResource(R.string.nav_settings_support), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp))
                        SettingsType.entries.forEach { type ->
                            DrawerItem(
                                icon = type.icon,
                                label = stringResource(type.labelRes),
                                value = when (type) {
                                    SettingsType.LANGUAGES -> "English (en)"
                                    SettingsType.VERSIONS -> "V${BuildConfig.VERSION_NAME}"
                                    else -> null
                                },
                                showChevron = type.hasChevron,
                                onClick = {
                                    scope.launch { drawerState.close() }
                                    when (type) {
                                        SettingsType.CONTACT_US -> navController.navigate(NavScreen.ContactUs)
                                        SettingsType.FAQ -> navController.navigate(NavScreen.FAQ)
                                        SettingsType.PRIVACY_POLICY -> navController.navigate(NavScreen.PrivacyPolicy)
                                        SettingsType.TERMS_OF_SERVICE -> navController.navigate(NavScreen.TermsOfService)
                                        else -> {}
                                    }
                                }
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        HorizontalDivider()
                        NavigationDrawerItem(
                            label = { Text(stringResource(R.string.logout)) },
                            selected = false,
                            onClick = { scope.launch { drawerState.close() }; showLogoutDialog = true },
                            icon = { Icon(Icons.AutoMirrored.Filled.Logout, null) },
                            colors = NavigationDrawerItemDefaults.colors(unselectedIconColor = MaterialTheme.colorScheme.error, unselectedTextColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
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

@Composable
fun DrawerItem(icon: ImageVector, label: String, value: String? = null, showBadge: Boolean = false, showChevron: Boolean = true, onClick: () -> Unit = {}) {
    Row(modifier = Modifier.fillMaxWidth().clickable(enabled = showChevron, onClick = onClick).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Text(text = label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        if (value != null) {
            Text(text = value, color = Color.Gray, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(end = 8.dp))
        }
        if (showBadge || showChevron) {
            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
        }
    }
}
