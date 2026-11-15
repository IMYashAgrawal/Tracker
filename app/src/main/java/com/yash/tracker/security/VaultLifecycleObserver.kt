package com.yash.tracker.security

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

/**
 * Automatically wipes the encryption key from RAM when the app goes to the background.
 */
class VaultLifecycleObserver(private val onLock: () -> Unit) : DefaultLifecycleObserver {
    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        // Wipe keys when the app is no longer visible
        CryptoManager.wipeKey()
        onLock()
    }
}
