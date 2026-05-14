package com.tzh.sme.ui.auth

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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.tzh.sme.R
import kotlinx.coroutines.launch

@Composable
fun SignupScreen(
    viewModel: AuthViewModel,
    onSignupSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var verificationCode by remember { mutableStateOf("") }
    var showVerificationDialog by remember { mutableStateOf(false) }
    var resendCountdown by remember { mutableIntStateOf(0) }
    
    val uiState by viewModel.effect.collectAsState(AuthUiState.Idle)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Error) {
            snackbarHostState.showSnackbar((uiState as AuthUiState.Error).message)
        } else if (uiState is AuthUiState.Success) {
            val message = (uiState as AuthUiState.Success).message
            snackbarHostState.showSnackbar(message)
            if (message.contains("Verification code sent")) {
                showVerificationDialog = true
                resendCountdown = 60
            }
        }
    }

    LaunchedEffect(resendCountdown) {
        if (resendCountdown > 0) {
            kotlinx.coroutines.delay(1000)
            resendCountdown -= 1
        }
    }

    if (showVerificationDialog) {
        AlertDialog(
            onDismissRequest = { showVerificationDialog = false },
            title = { Text("Verify Email") },
            text = {
                Column {
                    Text("Please enter the verification code sent to $email")
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = verificationCode,
                        onValueChange = { verificationCode = it },
                        label = { Text("Verification Code") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    TextButton(
                        onClick = {
                            if (email.isNotBlank()) {
                                viewModel.sendVerificationCode(email)
                            } else {
                                scope.launch { snackbarHostState.showSnackbar("Email cannot be empty") }
                            }
                        },
                        modifier = Modifier.align(Alignment.End),
                        enabled = uiState !is AuthUiState.Loading && resendCountdown == 0 && email.isNotBlank()
                    ) {
                        Text(
                            if (resendCountdown > 0) "Resend Code ($resendCountdown s)"
                            else "Resend Code"
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.verifyCode(email, verificationCode) {
                            showVerificationDialog = false
                            viewModel.signUp(name, phone, address, email, password, onSignupSuccess)
                        }
                    },
                    enabled = verificationCode.length == 6
                ) {
                    Text("Verify")
                }
            },
            dismissButton = {
                TextButton(onClick = { showVerificationDialog = false }) {
                    Text("Cancel")
                }
            }
        )
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
                value = email,
                onValueChange = { email = it },
                label = stringResource(R.string.email),
                leadingIcon = Icons.Default.Email,
                keyboardType = KeyboardType.Email
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
                isPassword = true
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = { 
                    if (name.isBlank() || phone.isBlank() || address.isBlank() || email.isBlank() || password.isBlank()) {
                        scope.launch { snackbarHostState.showSnackbar("Please fill all mandatory fields") }
                    } else if (password != confirmPassword) {
                        scope.launch { snackbarHostState.showSnackbar("Passwords do not match") }
                    } else if (password.length < 6) {
                        scope.launch { snackbarHostState.showSnackbar("Password must be at least 6 characters") }
                    } else {
                        viewModel.sendVerificationCode(email)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState !is AuthUiState.Loading
            ) {
                if (uiState is AuthUiState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Request Verification Code")
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
    keyboardType: KeyboardType = KeyboardType.Text
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
        visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
}
