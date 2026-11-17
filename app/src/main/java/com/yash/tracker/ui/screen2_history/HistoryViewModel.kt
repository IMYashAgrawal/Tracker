package com.yash.tracker.ui.screen2_history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yash.tracker.data.model.HabitRecord
import com.yash.tracker.data.repository.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel : ViewModel() {
    val records: StateFlow<List<HabitRecord>> = SessionRepository.records
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            try {
                SessionRepository.fetchAllRecords()
                _errorMessage.value = null
            } catch (e: Throwable) {
                _errorMessage.value = e.message ?: "Failed to load records"
            }
        }
    }

    fun updateRecord(record: HabitRecord) {
        viewModelScope.launch {
            try {
                SessionRepository.saveRecord(record)
            } catch (e: Throwable) {
                _errorMessage.value = e.message ?: "Failed to update record"
            }
        }
    }

    fun deleteRecord(record: HabitRecord) {
        viewModelScope.launch {
            try {
                SessionRepository.deleteRecord(record.id)
            } catch (e: Throwable) {
                _errorMessage.value = e.message ?: "Failed to delete record"
            }
        }
    }
}
