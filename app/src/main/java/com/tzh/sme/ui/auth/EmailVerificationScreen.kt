package com.tzh.sme.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun EmailVerificationScreen(
    viewModel: AuthViewModel,
    onVerified: () -> Unit,
    onNavigateBack: () -> Unit
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

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Email,
                contentDescription = null,
                modifier = Modifier.size(100.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.height(32.dp))

            Text(
                text = "Verify your Email",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = "A verification link has been sent to ${uiState.currentUser?.email ?: "your email"}. Please check your inbox and click the link to verify your account.",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = { viewModel.sendIntent(AuthUiIntent.CheckVerificationStatus(onVerified)) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("I've Verified")
                }
            }

            Spacer(Modifier.height(16.dp))

            OutlinedButton(
                onClick = { viewModel.sendIntent(AuthUiIntent.SendVerificationEmail) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading
            ) {
                Text("Resend Email")
            }

            Spacer(Modifier.height(16.dp))

            TextButton(onClick = onNavigateBack) {
                Text("Back to Login")
            }
        }
    }
}
