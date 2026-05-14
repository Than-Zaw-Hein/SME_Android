package com.tzh.sme.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.tzh.sme.domain.repository.User
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tzh.sme.ui.pos.DrawerItem
import com.tzh.sme.ui.theme.SMETheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: AuthViewModel,
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit
) {
    val user by viewModel.currentUser.collectAsState()
    val snackBarHost = remember { SnackbarHostState() }

    LaunchedEffect(viewModel.effect) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is AuthUiState.Error -> {
                    snackBarHost.showSnackbar(effect.message)
                }

                AuthUiState.Idle -> {

                }

                AuthUiState.Loading -> {

                }

                is AuthUiState.Success -> {
                    snackBarHost.showSnackbar(effect.message)
                }
            }
        }
    }

    ProfileContent(
        user = user,
        onNavigateBack = onNavigateBack,
        onLogout = onLogout,
        onSignOut = { onSuccess -> viewModel.signOut(onSuccess) },
        onUpdateProfile = { name, phone, address ->
            viewModel.updateProfile(name, phone, address)
        },
        onChangePassword = { old, new -> viewModel.changePassword(old, new) },
        onResetPassword = { viewModel.resetPassword() },
        snackBarHost = snackBarHost
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileContent(
    user: User?,
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit,
    onSignOut: (onSuccess: () -> Unit) -> Unit,
    onUpdateProfile: (String, String, String) -> Unit,
    onChangePassword: (String, String) -> Unit,
    onResetPassword: () -> Unit,
    snackBarHost: SnackbarHostState = remember { SnackbarHostState() }
) {
    var isEditing by remember { mutableStateOf(false) }
    var editedName by remember { mutableStateOf("") }
    var editedPhone by remember { mutableStateOf("") }
    var editedAddress by remember { mutableStateOf("") }
    var showLogoutAlert by remember { mutableStateOf(false) }
    var showResetPasswordAlert by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }

    LaunchedEffect(user, isEditing) {
        if (isEditing && user != null) {
            editedName = user.displayName ?: ""
            editedPhone = user.phone ?: ""
            editedAddress = user.address ?: ""
        }
    }

    if (showLogoutAlert) {
        AlertDialog(
            onDismissRequest = { showLogoutAlert = false },
            title = { Text("Logout") },
            text = { Text("Are you sure you want to logout?") },
            confirmButton = {
                Button(onClick = {
                    onSignOut {
                        onLogout()
                    }
                }) {
                    Text("Yes")
                }
            },
            dismissButton = {
                Button(onClick = { showLogoutAlert = false }) {
                    Text("No")
                }
            }
        )
    }

    if (showResetPasswordAlert) {
        AlertDialog(
            onDismissRequest = { showResetPasswordAlert = false },
            containerColor = Color(0xFF202124), // Dark surface
            shape = RoundedCornerShape(28.dp),
            title = {
                Text("Reset Password", color = Color.White)
            },
            text = {
                Column {
                    Text(
                        "To enhance the security of your account, kindly verify whether a password reset is necessary. Upon resetting, you will be automatically logged back into the app.",
                        color = Color.LightGray
                    )
                    user?.email?.let {
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
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)) // Blue confirm button from screenshot
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetPasswordAlert = false }) {
                    Text("Cancel", color = Color.Gray)
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
            title = { Text("Change Password") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = oldPassword,
                        onValueChange = { oldPassword = it },
                        label = { Text("Old Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("New Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = confirmNewPassword,
                        onValueChange = { confirmNewPassword = it },
                        label = { Text("Confirm New Password") },
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
                        Text("Forgot Password?")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPassword != confirmNewPassword) {
                            scope.launch { snackBarHost.showSnackbar("Passwords do not match") }
                        } else if (newPassword.length < 6) {
                            scope.launch { snackBarHost.showSnackbar("Password must be at least 6 characters") }
                        } else {
                            onChangePassword(oldPassword, newPassword)
                            showChangePasswordDialog = false
                        }
                    },
                    enabled = oldPassword.isNotEmpty() && newPassword.isNotEmpty() && confirmNewPassword.isNotEmpty()
                ) {
                    Text("Update")
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangePasswordDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackBarHost) },
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isEditing) {
                        IconButton(onClick = { isEditing = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel")
                        }
                        IconButton(onClick = {
                            onUpdateProfile(editedName, editedPhone, editedAddress)
                            isEditing = false
                        }) {
                            Icon(Icons.Default.Check, contentDescription = "Save")
                        }
                    } else {
                        IconButton(onClick = { isEditing = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                        }
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

            user?.let {
                if (isEditing) {
                    OutlinedTextField(
                        value = editedName,
                        onValueChange = { editedName = it },
                        label = { Text("Name") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    )

                    OutlinedTextField(
                        value = editedPhone,
                        onValueChange = { editedPhone = it },
                        label = { Text("Phone") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    )
                    OutlinedTextField(
                        value = editedAddress,
                        onValueChange = { editedAddress = it },
                        label = { Text("Address") },
                        leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    )
                    Card(
                        shape = RectangleShape,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)

                    ) {
                        Column() {
                            Text(
                                "You can modify your password anytime, please note that your new password should meet the minimal password strength requirements.",
                                modifier = Modifier.padding(8.dp)
                            )
                            TextButton(
                                onClick = { showChangePasswordDialog = true },
                                modifier = Modifier.align(Alignment.Start)
                            ) {
                                Text("Change Password")
                            }
                        }
                    }
                } else {
                    ProfileDetailItem(
                        icon = Icons.Default.Person,
                        label = "Name",
                        value = it.displayName ?: "N/A"
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    ProfileDetailItem(
                        icon = Icons.Default.Email,
                        label = "Email",
                        value = it.email
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    ProfileDetailItem(
                        icon = Icons.Default.Phone,
                        label = "Phone",
                        value = it.phone ?: "N/A"
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    ProfileDetailItem(
                        icon = Icons.Default.LocationOn,
                        label = "Address",
                        value = it.address ?: "N/A"
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    Spacer(modifier = Modifier.height(32.dp))


                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            showLogoutAlert = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Logout")
                    }
                }
            } ?: run {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
fun ProfileDetailItem(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ProfilePreview() {
    SMETheme {
        ProfileContent(
            user = User(
                id = "1",
                email = "john.doe@example.com",
                displayName = "John Doe",
                phone = "+1234567890",
                address = "123 Main St, Springfield"
            ),
            onNavigateBack = {},
            onLogout = {},
            onSignOut = {},
            onUpdateProfile = { _, _, _ -> },
            onChangePassword = { _, _ -> },
            onResetPassword = {}
        )
    }
}
