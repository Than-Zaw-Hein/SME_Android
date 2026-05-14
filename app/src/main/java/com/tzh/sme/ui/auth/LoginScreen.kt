package com.tzh.sme.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tzh.sme.R
import com.tzh.sme.ui.theme.SMETheme

@Composable
fun LoginScreen(
    viewModel: AuthViewModel, onLoginSuccess: () -> Unit, onNavigateToSignup: () -> Unit
) {

    val context = LocalContext.current
    val effect by viewModel.effect.collectAsState(initial = AuthUiState.Idle)

    LoginScreenContent(
        effect = effect, onLoginClick = { email, password ->
            viewModel.signIn(email, password, onLoginSuccess)
        }, onGoogleSignInClick = {
            viewModel.startGoogleSignIn(context, onLoginSuccess = onLoginSuccess)
        }, onNavigateToSignup = onNavigateToSignup
    )
}

@Composable
fun LoginScreenContent(
    effect: AuthUiState,
    onLoginClick: (String, String) -> Unit,
    onGoogleSignInClick: () -> Unit,
    onNavigateToSignup: () -> Unit
) {
    var email by remember { mutableStateOf("mgmg@email.com") }
    var password by remember { mutableStateOf("Password") }
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(effect) {
        if (effect is AuthUiState.Error) {
            snackbarHostState.showSnackbar(
                message = effect.message,
            )
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }) { innerPadding ->
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
                modifier = Modifier
                    .size(100.dp)
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
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = { onLoginClick(email, password) },
                modifier = Modifier.fillMaxWidth(),
                enabled = effect !is AuthUiState.Loading
            ) {
                if (effect is AuthUiState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Text(stringResource(R.string.login))
                }
            }

//            Spacer(Modifier.height(16.dp))
//
//            OutlinedButton(
//                onClick = onGoogleSignInClick,
//                modifier = Modifier.fillMaxWidth(),
//                colors = ButtonDefaults.outlinedButtonColors(
//                    contentColor = MaterialTheme.colorScheme.onSurface
//                ),
//                contentPadding = PaddingValues(vertical = 12.dp)
//            ) {
//                if (effect is AuthUiState.Loading) {
//                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
//                } else {
//                    Row(
//                        verticalAlignment = Alignment.CenterVertically,
//                        horizontalArrangement = Arrangement.Center
//                    ) {
//                        Image(
//                            painter = painterResource(id = R.drawable.ic_google),
//                            contentDescription = null,
//                            modifier = Modifier.size(24.dp)
//                        )
//                        Spacer(Modifier.width(12.dp))
//                        Text(
//                            text = stringResource(R.string.sign_in_with_google),
//                            style = MaterialTheme.typography.labelLarge
//                        )
//                    }
//                }
//            }

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
            effect = AuthUiState.Idle,
            onLoginClick = { _, _ -> },
            onGoogleSignInClick = {},
            onNavigateToSignup = {})
    }
}
