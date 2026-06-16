package com.tzh.sme.ui.auth

import android.app.Activity
import android.content.Intent
import android.util.Patterns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tzh.sme.R
import kotlinx.coroutines.launch

@Composable
fun SignupScreen(
    viewModel: AuthViewModel,
    onSignupSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var shopName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var latitude by remember { mutableStateOf<Double?>(null) }
    var longitude by remember { mutableStateOf<Double?>(null) }

    LaunchedEffect(viewModel.sideEffect) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                is AuthUiSideEffect.Error -> snackbarHostState.showSnackbar(effect.message)
                is AuthUiSideEffect.Success -> snackbarHostState.showSnackbar(effect.message)
                AuthUiSideEffect.VerificationPending -> {}
                AuthUiSideEffect.NavigateBack -> {}
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(R.drawable.logo),
                contentDescription = "Logo",
                modifier = Modifier.size(100.dp)
            )

            Text(
                text = stringResource(R.string.create_account),
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(Modifier.height(32.dp))

            MandatoryTextField(
                value = name,
                onValueChange = { name = it },
                label = stringResource(R.string.full_name),
                leadingIcon = Icons.Default.Person
            )

            Spacer(Modifier.height(16.dp))

            MandatoryTextField(
                value = phone,
                onValueChange = { phone = it },
                label = stringResource(R.string.phone),
                leadingIcon = Icons.Default.Phone,
                keyboardType = KeyboardType.Phone
            )

            Spacer(Modifier.height(16.dp))

            MandatoryTextField(
                value = address,
                onValueChange = { address = it },
                label = stringResource(R.string.address),
                leadingIcon = Icons.Default.Home
            )

            Spacer(Modifier.height(16.dp))

            MandatoryTextField(
                value = shopName,
                onValueChange = { shopName = it },
                label = "Shop Name",
                leadingIcon = Icons.Default.Business
            )

            Spacer(Modifier.height(8.dp))

            val locationLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    latitude = result.data?.getDoubleExtra("latitude", 0.0)
                    longitude = result.data?.getDoubleExtra("longitude", 0.0)
                }
            }

            OutlinedButton(
                onClick = {
                    val intent = Intent(context, LocationPickerActivity::class.java).apply {
                        latitude?.let { putExtra("latitude", it) }
                        longitude?.let { putExtra("longitude", it) }
                    }
                    locationLauncher.launch(intent)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.LocationOn, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (latitude != null) "Location Picked" else "Pick Location on Map")
            }

            if (latitude != null && longitude != null) {
                Text(
                    "Selected: ${String.format(java.util.Locale.US, "%.5f, %.5f", latitude, longitude)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(16.dp))

            MandatoryTextField(
                value = email,
                onValueChange = { email = it },
                label = stringResource(R.string.email),
                leadingIcon = Icons.Default.Email,
                keyboardType = KeyboardType.Email,
                isError = !email.matches(Patterns.EMAIL_ADDRESS.toRegex()),
                supportingText = if (email.isNotEmpty() && !email.matches(Patterns.EMAIL_ADDRESS.toRegex())) {
                    { Text("Invalid email format", color = MaterialTheme.colorScheme.error) }
                } else null
            )

            Spacer(Modifier.height(16.dp))

            MandatoryTextField(
                value = password,
                onValueChange = { password = it },
                label = stringResource(R.string.password),
                leadingIcon = Icons.Default.Lock,
                isPassword = true
            )

            Spacer(Modifier.height(16.dp))

            MandatoryTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = "Confirm Password",
                leadingIcon = Icons.Default.Lock,
                isPassword = true,
                supportingText = if (confirmPassword.isNotEmpty() && password != confirmPassword) {
                    { Text("Password and confirm password do not match.", color = MaterialTheme.colorScheme.error) }
                } else null,
                isError = confirmPassword.isNotEmpty() && password != confirmPassword
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    if (name.isBlank() || phone.isBlank() || address.isBlank() || shopName.isBlank() || email.isBlank() || password.isBlank()) {
                        scope.launch { snackbarHostState.showSnackbar("Please fill all mandatory fields") }
                    } else if (password != confirmPassword) {
                        scope.launch { snackbarHostState.showSnackbar("Passwords do not match") }
                    } else if (password.length < 6) {
                        scope.launch { snackbarHostState.showSnackbar("Password must be at least 6 characters") }
                    } else {
                        viewModel.sendIntent(
                            AuthUiIntent.SignUp(
                                name, phone, address, email, password, shopName, latitude, longitude, onSignupSuccess
                            )
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Sign Up")
                }
            }

            Spacer(Modifier.height(16.dp))

            TextButton(onClick = onNavigateToLogin) {
                Text(stringResource(R.string.already_have_account))
            }
        }
    }
}

@Composable
fun MandatoryTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    isError: Boolean = false,
    supportingText: @Composable (() -> Unit)? = null
) {
    var passwordVisible by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = {
            Text(buildAnnotatedString {
                append(label)
                withStyle(style = SpanStyle(color = Color.Red)) {
                    append(" *")
                }
            })
        },
        leadingIcon = { Icon(leadingIcon, contentDescription = null) },
        trailingIcon = {
            if (isPassword) {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password"
                    )
                }
            }
        },
        visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        supportingText = supportingText,
        isError = isError
    )
}
