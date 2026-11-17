package com.yash.tracker.ui.screen1_log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yash.tracker.data.model.HabitRecord
import com.yash.tracker.data.repository.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class LogUiState {
    object Idle : LogUiState()
    object Submitting : LogUiState()
    object Success : LogUiState()
    data class Error(val message: String) : LogUiState()
}

class LogViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<LogUiState>(LogUiState.Idle)
    val uiState: StateFlow<LogUiState> = _uiState

    fun submitRecord(record: HabitRecord) {
        viewModelScope.launch {
            _uiState.value = LogUiState.Submitting
            try {
                SessionRepository.saveRecord(record)
                _uiState.value = LogUiState.Success
            } catch (e: Throwable) {
                _uiState.value = LogUiState.Error(e.message ?: "Failed to save record")
            }
        }
    }

    fun resetState() {
        _uiState.value = LogUiState.Idle
    }
}
