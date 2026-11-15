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
import java.util.UUID

@Serializable
data class EncryptedHabitEntry(
    val id: String,
    val user_id: String,
    val encrypted_payload: String
)

object SessionRepository {
    private val _records = MutableStateFlow<List<HabitRecord>>(emptyList())
    val records: StateFlow<List<HabitRecord>> = _records.asStateFlow()

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun saveRecord(record: HabitRecord) {
        val plaintext = json.encodeToString(record)
        val encryptedPayload = CryptoManager.encrypt(plaintext)
        
        val entry = mapOf(
            "id" to record.id,
            "encrypted_payload" to encryptedPayload
        )

        SupabaseManager.client.from("habit_entries").upsert(entry)
        
        // Update local state
        _records.value = (listOf(record) + _records.value.filter { it.id != record.id })
            .sortedByDescending { it.session_date + it.session_time }
    }

    suspend fun deleteRecord(recordId: String) {
        SupabaseManager.client.from("habit_entries").delete {
            filter {
                eq("id", recordId)
            }
        }
        _records.value = _records.value.filter { it.id != recordId }
    }

    suspend fun fetchAllRecords() {
        val entries = SupabaseManager.client.from("habit_entries").select().decodeList<EncryptedHabitEntry>()
        val decryptedRecords = entries.mapNotNull { entry ->
            try {
                val plaintext = CryptoManager.decrypt(entry.encrypted_payload)
                json.decodeFromString<HabitRecord>(plaintext)
            } catch (e: Exception) {
                null
            }
        }.sortedByDescending { it.session_date + it.session_time }
        
        _records.value = decryptedRecords
    }

    fun clearLocalData() {
        _records.value = emptyList()
    }
}
