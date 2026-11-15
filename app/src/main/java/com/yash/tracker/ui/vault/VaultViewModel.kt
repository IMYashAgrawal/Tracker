package com.yash.tracker.ui.vault

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yash.tracker.data.remote.SupabaseManager
import com.yash.tracker.security.CryptoManager
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.query.filter.filter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data class UserVaultMeta(
    val salt: String,
    val canary_payload: String
)

sealed class VaultState {
    object Idle : VaultState()
    object Loading : VaultState()
    object NeedSetup : VaultState()
    object Locked : VaultState()
    object Unlocked : VaultState()
    data class Error(val message: String) : VaultState()
}

class VaultViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<VaultState>(VaultState.Idle)
    val uiState: StateFlow<VaultState> = _uiState

    private var vaultMeta: UserVaultMeta? = null

    init {
        checkVaultStatus()
    }

    private fun checkVaultStatus() {
        viewModelScope.launch {
            _uiState.value = VaultState.Loading
            try {
                // In a real app, you'd handle authentication first.
                // For this zero-knowledge tracker, we assume the user is "logged in" to Supabase Auth
                // but the Vault remains locked until the master password is provided.
                val user = SupabaseManager.client.auth.currentUserOrNull()
                if (user == null) {
                    _uiState.value = VaultState.Error("Please log in to your account first.")
                    return@launch
                }

                val meta = SupabaseManager.client.from("user_vault_meta")
                    .select {
                        filter {
                            eq("user_id", user.id)
                        }
                    }
                    .decodeSingleOrNull<UserVaultMeta>()

                if (meta == null) {
                    _uiState.value = VaultState.NeedSetup
                } else {
                    vaultMeta = meta
                    _uiState.value = VaultState.Locked
                }
            } catch (e: Exception) {
                _uiState.value = VaultState.Error(e.message ?: "Connection error")
            }
        }
    }

    fun setupVault(password: String) {
        viewModelScope.launch {
            _uiState.value = VaultState.Loading
            try {
                val salt = CryptoManager.generateSalt()
                val saltBase64 = android.util.Base64.encodeToString(salt, android.util.Base64.NO_WRAP)
                
                CryptoManager.deriveKey(password.toCharArray(), salt)
                val canary = CryptoManager.encrypt("VAULT_VERIFIED")
                
                val user = SupabaseManager.client.auth.currentUserOrNull() ?: throw IllegalStateException("User not authenticated")
                val meta = UserVaultMeta(saltBase64, canary)
                
                SupabaseManager.client.from("user_vault_meta").insert(meta)
                
                _uiState.value = VaultState.Unlocked
            } catch (e: Exception) {
                _uiState.value = VaultState.Error(e.message ?: "Setup failed")
            }
        }
    }

    fun unlockVault(password: String) {
        viewModelScope.launch {
            _uiState.value = VaultState.Loading
            try {
                val meta = vaultMeta ?: throw IllegalStateException("Vault not initialized")
                val salt = android.util.Base64.decode(meta.salt, android.util.Base64.NO_WRAP)
                
                CryptoManager.deriveKey(password.toCharArray(), salt)
                
                val decrypted = CryptoManager.decrypt(meta.canary_payload)
                if (decrypted == "VAULT_VERIFIED") {
                    _uiState.value = VaultState.Unlocked
                } else {
                    CryptoManager.wipeKey()
                    _uiState.value = VaultState.Error("Incorrect master password")
                }
            } catch (e: Exception) {
                CryptoManager.wipeKey()
                _uiState.value = VaultState.Error("Decryption failed. Wrong password?")
            }
        }
    }

    fun lock() {
        CryptoManager.wipeKey()
        _uiState.value = VaultState.Locked
    }
}
