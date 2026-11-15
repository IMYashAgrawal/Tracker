package com.yash.tracker.ui.screen2_history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yash.tracker.data.model.HabitRecord
import com.yash.tracker.data.repository.SessionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel : ViewModel() {
    val records: StateFlow<List<HabitRecord>> = SessionRepository.records
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            SessionRepository.fetchAllRecords()
        }
    }

    fun updateRecord(record: HabitRecord) {
        viewModelScope.launch {
            SessionRepository.saveRecord(record)
        }
    }

    fun deleteRecord(record: HabitRecord) {
        viewModelScope.launch {
            SessionRepository.deleteRecord(record.id)
        }
    }
}
