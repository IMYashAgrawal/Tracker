package com.yash.tracker.ui.vault

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yash.tracker.security.CryptoManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class VaultState {
    object Locked : VaultState()
    object Loading : VaultState()
    object Unlocked : VaultState()
    data class Error(val message: String) : VaultState()
}

/**
 * Simplified vault: password hash is checked entirely in-app.
 * On success, an AES key is derived and held in RAM for the session.
 * No network call happens until the vault is unlocked.
 */
class VaultViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<VaultState>(VaultState.Locked)
    val uiState: StateFlow<VaultState> = _uiState

    fun unlockVault(password: String) {
        viewModelScope.launch(Dispatchers.Default) {
            _uiState.value = VaultState.Loading
            try {
                // Step 1: Compare SHA-256 hash in-app
                if (!CryptoManager.verifyPassword(password)) {
                    CryptoManager.wipeKey()
                    _uiState.value = VaultState.Error("Incorrect password")
                    return@launch
                }

                // Step 2: Derive AES encryption key and hold in RAM
                CryptoManager.deriveAndHoldKey(password)

                _uiState.value = VaultState.Unlocked
            } catch (e: Exception) {
                CryptoManager.wipeKey()
                _uiState.value = VaultState.Error(e.message ?: "Unlock failed")
            }
        }
    }

    fun lock() {
        CryptoManager.wipeKey()
        _uiState.value = VaultState.Locked
    }
}
