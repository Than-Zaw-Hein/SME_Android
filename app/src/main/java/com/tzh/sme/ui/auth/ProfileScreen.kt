package com.tzh.sme.ui.auth

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tzh.sme.R
import com.tzh.sme.data.model.ShopModel
import com.tzh.sme.domain.repository.User
import com.tzh.sme.domain.repository.UserRole
import com.tzh.sme.ui.theme.SMETheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: AuthViewModel,
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel.sideEffect) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                is AuthUiSideEffect.Error -> snackbarHostState.showSnackbar(effect.message)
                is AuthUiSideEffect.Success -> snackbarHostState.showSnackbar(effect.message)
                AuthUiSideEffect.VerificationPending -> {}
                AuthUiSideEffect.NavigateBack -> onNavigateBack()
            }
        }
    }

    ProfileContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onLogout = onLogout,
        onSignOut = { viewModel.sendIntent(AuthUiIntent.SignOut) },
        onUpdateProfile = { name, phone ->
            viewModel.sendIntent(AuthUiIntent.UpdateProfile(name, phone))
        },
        onUpdateShop = { name, address, lat, lng ->
            viewModel.sendIntent(AuthUiIntent.UpdateShop(name, address, lat, lng))
        },
        onChangePassword = { old, new -> viewModel.sendIntent(AuthUiIntent.ChangePassword(old, new)) },
        onResetPassword = { viewModel.sendIntent(AuthUiIntent.ResetPassword) },
        snackbarHostState = snackbarHostState
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileContent(
    uiState: AuthUiState,
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit,
    onSignOut: () -> Unit,
    onUpdateProfile: (String, String) -> Unit,
    onUpdateShop: (String, String, Double?, Double?) -> Unit,
    onChangePassword: (String, String) -> Unit,
    onResetPassword: () -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    var isEditingProfile by remember { mutableStateOf(false) }
    var isEditingShop by remember { mutableStateOf(false) }
    var editedName by remember { mutableStateOf("") }
    var editedPhone by remember { mutableStateOf("") }
    var editedShopName by remember { mutableStateOf("") }
    var editedShopAddress by remember { mutableStateOf("") }
    var editedLat by remember { mutableStateOf<Double?>(null) }
    var editedLng by remember { mutableStateOf<Double?>(null) }
    var showLogoutAlert by remember { mutableStateOf(false) }
    var showResetPasswordAlert by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(uiState.currentUser, isEditingProfile) {
        if (isEditingProfile && uiState.currentUser != null) {
            editedName = uiState.currentUser.displayName ?: ""
            editedPhone = uiState.currentUser.phone ?: ""
        }
    }

    LaunchedEffect(uiState.currentShop, isEditingShop) {
        if (isEditingShop && uiState.currentShop != null) {
            editedShopName = uiState.currentShop.name
            editedShopAddress = uiState.currentShop.address
            editedLat = uiState.currentShop.latitude
            editedLng = uiState.currentShop.longitude
        }
    }

    if (showLogoutAlert) {
        AlertDialog(
            onDismissRequest = { showLogoutAlert = false },
            title = { Text(stringResource(R.string.logout)) },
            text = { Text(stringResource(R.string.logout_confirmation)) },
            confirmButton = {
                Button(onClick = {
                    onSignOut()
                    onLogout()
                }) {
                    Text(stringResource(R.string.yes))
                }
            },
            dismissButton = {
                Button(onClick = { showLogoutAlert = false }) {
                    Text(stringResource(R.string.no))
                }
            }
        )
    }

    if (showResetPasswordAlert) {
        AlertDialog(
            onDismissRequest = { showResetPasswordAlert = false },
            containerColor = Color(0xFF202124),
            shape = RoundedCornerShape(28.dp),
            title = { Text(stringResource(R.string.reset_password_title), color = Color.White) },
            text = {
                Column {
                    Text(stringResource(R.string.reset_password_message), color = Color.LightGray)
                    uiState.currentUser?.email?.let {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(it, color = Color(0xFFD0BCFF), fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onResetPassword()
                        showResetPasswordAlert = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                ) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetPasswordAlert = false }) {
                    Text(stringResource(R.string.cancel), color = Color.Gray)
                }
            }
        )
    }

    if (showChangePasswordDialog) {
        var oldPassword by remember { mutableStateOf("") }
        var newPassword by remember { mutableStateOf("") }
        var confirmNewPassword by remember { mutableStateOf("") }
        val scope = rememberCoroutineScope()

        AlertDialog(
            onDismissRequest = { showChangePasswordDialog = false },
            title = { Text(stringResource(R.string.change_password)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = oldPassword,
                        onValueChange = { oldPassword = it },
                        label = { Text(stringResource(R.string.old_password)) },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text(stringResource(R.string.new_password)) },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = confirmNewPassword,
                        onValueChange = { confirmNewPassword = it },
                        label = { Text(stringResource(R.string.confirm_new_password)) },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    TextButton(
                        onClick = {
                            showChangePasswordDialog = false
                            showResetPasswordAlert = true
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(stringResource(R.string.forgot_password_q))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPassword != confirmNewPassword) {
                            scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.passwords_do_not_match)) }
                        } else if (newPassword.length < 6) {
                            scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.password_too_short)) }
                        } else {
                            onChangePassword(oldPassword, newPassword)
                            showChangePasswordDialog = false
                        }
                    },
                    enabled = oldPassword.isNotEmpty() && newPassword.isNotEmpty() && confirmNewPassword.isNotEmpty()
                ) {
                    Text(stringResource(R.string.update))
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangePasswordDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.profile_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (uiState.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            uiState.currentUser?.let { user ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Personal Information", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            if (isEditingProfile) {
                                Row {
                                    IconButton(onClick = { isEditingProfile = false }) { Icon(Icons.Default.Close, null) }
                                    IconButton(onClick = {
                                        onUpdateProfile(editedName, editedPhone)
                                        isEditingProfile = false
                                    }) { Icon(Icons.Default.Check, null) }
                                }
                            } else {
                                IconButton(onClick = { isEditingProfile = true }) { Icon(Icons.Default.Edit, null) }
                            }
                        }

                        if (isEditingProfile) {
                            OutlinedTextField(value = editedName, onValueChange = { editedName = it }, label = { Text(stringResource(R.string.name)) }, leadingIcon = { Icon(Icons.Default.Person, null) }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
                            OutlinedTextField(value = editedPhone, onValueChange = { editedPhone = it }, label = { Text(stringResource(R.string.phone_label)) }, leadingIcon = { Icon(Icons.Default.Phone, null) }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
                        } else {
                            ProfileDetailItem(Icons.Default.Person, stringResource(R.string.name), user.displayName ?: "N/A")
                            ProfileDetailItem(Icons.Default.Email, stringResource(R.string.email), user.email)
                            ProfileDetailItem(Icons.Default.Phone, stringResource(R.string.phone_label), user.phone ?: "N/A")
                            ProfileDetailItem(Icons.Default.Person, "Role", user.role.name)
                        }
                    }
                }

                uiState.currentShop?.let { shop ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Shop Information", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                if (user.role == UserRole.ADMIN) {
                                    if (isEditingShop) {
                                        Row {
                                            IconButton(onClick = { isEditingShop = false }) { Icon(Icons.Default.Close, null) }
                                            IconButton(onClick = {
                                                onUpdateShop(editedShopName, editedShopAddress, editedLat, editedLng)
                                                isEditingShop = false
                                            }) { Icon(Icons.Default.Check, null) }
                                        }
                                    } else {
                                        IconButton(onClick = { isEditingShop = true }) { Icon(Icons.Default.Edit, null) }
                                    }
                                }
                            }

                            if (isEditingShop) {
                                OutlinedTextField(value = editedShopName, onValueChange = { editedShopName = it }, label = { Text("Shop Name") }, leadingIcon = { Icon(Icons.Default.Person, null) }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
                                OutlinedTextField(value = editedShopAddress, onValueChange = { editedShopAddress = it }, label = { Text(stringResource(R.string.address_label)) }, leadingIcon = { Icon(Icons.Default.LocationOn, null) }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))

                                val locationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                                    if (result.resultCode == Activity.RESULT_OK) {
                                        editedLat = result.data?.getDoubleExtra("latitude", 0.0)
                                        editedLng = result.data?.getDoubleExtra("longitude", 0.0)
                                    }
                                }

                                OutlinedButton(onClick = {
                                    val intent = Intent(context, LocationPickerActivity::class.java).apply {
                                        editedLat?.let { putExtra("latitude", it) }
                                        editedLng?.let { putExtra("longitude", it) }
                                    }
                                    locationLauncher.launch(intent)
                                }, modifier = Modifier.fillMaxWidth()) {
                                    Icon(Icons.Default.LocationOn, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text(if (editedLat != null) "Update Map Location" else "Pick Location on Map")
                                }
                                if (editedLat != null && editedLng != null) {
                                    Text("GPS: ${String.format(java.util.Locale.US, "%.5f, %.5f", editedLat, editedLng)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                }
                            } else {
                                ProfileDetailItem(Icons.Default.Person, "Shop Name", shop.name)
                                ProfileDetailItem(Icons.Default.LocationOn, "Address", shop.address)
                                if (shop.latitude != null && shop.longitude != null) {
                                    Text("GPS: ${String.format(java.util.Locale.US, "%.5f, %.5f", shop.latitude, shop.longitude)}", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(start = 40.dp))
                                }
                            }
                        }
                    }
                }

                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Security", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        TextButton(onClick = { showChangePasswordDialog = true }) {
                            Icon(Icons.Default.Lock, null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.change_password))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(onClick = { showLogoutAlert = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                    Text(stringResource(R.string.logout))
                }
            } ?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
fun ProfileDetailItem(icon: ImageVector, label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
            Text(text = value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ProfilePreview() {
    SMETheme {
        ProfileContent(
            uiState = AuthUiState(
                currentUser = User(id = "1", email = "john.doe@example.com", displayName = "John Doe", phone = "+1234567890"),
                currentShop = ShopModel(id = "1", name = "My Shop", address = "123 Street")
            ),
            onNavigateBack = {},
            onLogout = {},
            onSignOut = {},
            onUpdateProfile = { _, _ -> },
            onUpdateShop = { _, _, _, _ -> },
            onChangePassword = { _, _ -> },
            onResetPassword = {}
        )
    }
}
