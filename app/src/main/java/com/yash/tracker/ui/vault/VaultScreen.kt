package com.yash.tracker.ui.vault

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun VaultScreen(
    viewModel: VaultViewModel,
    onVaultUnlocked: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var password by remember { mutableStateOf("") }

    LaunchedEffect(uiState) {
        if (uiState is VaultState.Unlocked) {
            onVaultUnlocked()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Personal Vault",
                style = MaterialTheme.typography.headlineMedium
            )

            when (uiState) {
                is VaultState.Loading -> CircularProgressIndicator()
                is VaultState.Error -> {
                    Text(
                        text = (uiState as VaultState.Error).message,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                else -> {}
            }

            if (uiState is VaultState.Locked || uiState is VaultState.Error) {
                Text(
                    text = "Enter your password to unlock.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Button(
                    onClick = {
                        viewModel.unlockVault(password)
                        // Clear password from the text field's memory
                        password = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = password.isNotBlank() && uiState !is VaultState.Loading
                ) {
                    Text("Unlock")
                }
            }
        }
    }
}
