package com.yash.tracker.data.repository

import com.yash.tracker.data.model.HabitRecord
import com.yash.tracker.data.remote.SupabaseManager
import com.yash.tracker.security.CryptoManager
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * DB model for the `entries` table.
 * Only `id` + `encrypted_payload`. No user_id — everything is encrypted client-side.
 */
@Serializable
data class EncryptedEntry(
    val id: String,
    val encrypted_payload: String,
    val created_at: String? = null
)

object SessionRepository {
    private val _records = MutableStateFlow<List<HabitRecord>>(emptyList())
    val records: StateFlow<List<HabitRecord>> = _records.asStateFlow()

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun saveRecord(record: HabitRecord) {
        val plaintext = json.encodeToString(record)
        val encryptedPayload = CryptoManager.encrypt(plaintext)

        val entry = EncryptedEntry(
            id = record.id,
            encrypted_payload = encryptedPayload
        )

        SupabaseManager.client.from("entries").upsert(entry)

        // Update local in-memory state
        _records.value = (listOf(record) + _records.value.filter { it.id != record.id })
            .sortedByDescending { it.session_date + it.session_time }
    }

    suspend fun deleteRecord(recordId: String) {
        SupabaseManager.client.from("entries").delete {
            filter {
                eq("id", recordId)
            }
        }
        _records.value = _records.value.filter { it.id != recordId }
    }

    suspend fun fetchAllRecords() {
        val entries = SupabaseManager.client.from("entries").select().decodeList<EncryptedEntry>()
        val decryptedRecords = entries.mapNotNull { entry ->
            try {
                val plaintext = CryptoManager.decrypt(entry.encrypted_payload)
                json.decodeFromString<HabitRecord>(plaintext)
            } catch (e: Exception) {
                // Entries that can't be decrypted with the current session key are silently skipped.
                null
            }
        }.sortedByDescending { it.session_date + it.session_time }

        _records.value = decryptedRecords
    }

    fun clearLocalData() {
        _records.value = emptyList()
    }
}
