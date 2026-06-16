package com.tzh.sme.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tzh.sme.R
import com.tzh.sme.ui.theme.SMETheme

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onLoginSuccess: () -> Unit,
    onNavigateToSignup: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel.sideEffect) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                is AuthUiSideEffect.Error -> snackbarHostState.showSnackbar(effect.message)
                is AuthUiSideEffect.Success -> snackbarHostState.showSnackbar(effect.message)
                AuthUiSideEffect.VerificationPending -> {} // Handled elsewhere or as needed
                AuthUiSideEffect.NavigateBack -> {}
            }
        }
    }

    LoginScreenContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onLoginClick = { email, password ->
            viewModel.sendIntent(AuthUiIntent.SignIn(email, password, onLoginSuccess))
        },
        onGoogleSignInClick = {
            viewModel.sendIntent(AuthUiIntent.StartGoogleSignIn(context, onLoginSuccess))
        },
        onNavigateToSignup = onNavigateToSignup
    )
}

@Composable
fun LoginScreenContent(
    uiState: AuthUiState,
    snackbarHostState: SnackbarHostState,
    onLoginClick: (String, String) -> Unit,
    onGoogleSignInClick: () -> Unit,
    onNavigateToSignup: () -> Unit
) {
    var email by remember { mutableStateOf("mgmg@email.com") }
    var password by remember { mutableStateOf("password") }
    var showPassword by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(R.drawable.logo),
                contentDescription = "Logo",
                modifier = Modifier.size(100.dp)
            )

            Text(
                text = stringResource(R.string.welcome_to_sme),
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(Modifier.height(32.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text(stringResource(R.string.email)) },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(stringResource(R.string.password)) },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            imageVector = if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "passwordToggle"
                        )
                    }
                }
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = { onLoginClick(email, password) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Text(stringResource(R.string.login))
                }
            }

            Spacer(Modifier.height(16.dp))

            TextButton(onClick = onNavigateToSignup) {
                Text(stringResource(R.string.dont_have_account))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    SMETheme(darkTheme = false) {
        LoginScreenContent(
            uiState = AuthUiState(),
            snackbarHostState = SnackbarHostState(),
            onLoginClick = { _, _ -> },
            onGoogleSignInClick = {},
            onNavigateToSignup = {}
        )
    }
}
