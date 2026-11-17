package com.yash.tracker.ui.screen3_analysis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yash.tracker.analytics.engine.InsightGenerator
import com.yash.tracker.analytics.engine.OverviewAnalyzer
import com.yash.tracker.analytics.model.DateRangeFilter
import com.yash.tracker.analytics.model.DateRangeType
import com.yash.tracker.analytics.model.Insight
import com.yash.tracker.analytics.model.OverviewMetrics
import com.yash.tracker.data.repository.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AnalysisUiState(
    val isLoading: Boolean = true,
    val overview: OverviewMetrics? = null,
    val insights: List<Insight> = emptyList(),
    val selectedRange: DateRangeType = DateRangeType.LAST_30_DAYS,
    val error: String? = null
)

class AnalysisViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AnalysisUiState())
    val uiState: StateFlow<AnalysisUiState> = _uiState.asStateFlow()

    init {
        analyze()
    }

    fun setDateRange(rangeType: DateRangeType) {
        _uiState.value = _uiState.value.copy(selectedRange = rangeType)
        analyze()
    }

    fun analyze() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                // Make sure records are loaded
                if (SessionRepository.records.value.isEmpty()) {
                    SessionRepository.fetchAllRecords()
                }

                val records = SessionRepository.records.value
                val filter = DateRangeFilter(
                    type = _uiState.value.selectedRange,
                    compareWithPrevious = true
                )

                if (records.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        overview = null,
                        insights = emptyList()
                    )
                    return@launch
                }

                val overview = OverviewAnalyzer.analyze(records, filter)
                val insights = InsightGenerator.generate(records, filter)

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    overview = overview,
                    insights = insights
                )
            } catch (e: Throwable) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Analysis failed"
                )
            }
        }
    }
}
