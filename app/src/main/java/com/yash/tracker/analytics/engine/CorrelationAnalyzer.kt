package com.yash.tracker.analytics.engine

import com.yash.tracker.analytics.model.CorrelationMetrics
import com.yash.tracker.analytics.model.CorrelationResult
import com.yash.tracker.analytics.model.CrossTabStats
import com.yash.tracker.analytics.model.DateRangeFilter
import com.yash.tracker.analytics.utils.DateHelpers
import com.yash.tracker.analytics.utils.Statistics
import com.yash.tracker.data.model.HabitRecord

object CorrelationAnalyzer {

    fun analyze(
        records: List<HabitRecord>,
        filter: DateRangeFilter
    ): CorrelationMetrics {
        val (start, end) = filter.getEffectiveRange()
        val currentRecords = records.filter {
            val date = DateHelpers.parseDate(it.session_date)
            !date.isBefore(start) && !date.isAfter(end)
        }

        val results = mutableListOf<CorrelationResult>()

        // 1. Trigger vs Satisfaction
        results.add(calculateCorrelation(currentRecords, "Trigger", "Satisfaction", 
            { it.primary_trigger }, { it.experience_satisfaction.toString() }))

        // 2. Emotion Before vs After
        results.add(calculateCorrelation(currentRecords, "Emotion Before", "Emotion After",
            { it.emotions_before.firstOrNull() ?: "None" }, { it.emotions_after.firstOrNull() ?: "None" }))

        // 3. Planned vs Satisfaction
        results.add(calculateCorrelation(currentRecords, "Planned", "Satisfaction",
            { it.planned }, { it.experience_satisfaction.toString() }))

        // 4. Urge vs Control
        results.add(calculateCorrelation(currentRecords, "Urge Intensity", "Control",
            { it.urge_intensity.toString() }, { it.perceived_control }))

        // 5. Sleep vs Urge
        results.add(calculateCorrelation(currentRecords, "Sleep", "Urge Intensity",
            { it.sleep_duration ?: "Unknown" }, { it.urge_intensity.toString() }))

        return CorrelationMetrics(
            correlations = results,
            recordIds = currentRecords.map { it.id }
        )
    }

    private fun calculateCorrelation(
        records: List<HabitRecord>,
        varA: String,
        varB: String,
        getA: (HabitRecord) -> String,
        getB: (HabitRecord) -> String
    ): CorrelationResult {
        val groups = records.groupBy { "${getA(it)} | ${getB(it)}" }
        val crossTabs = groups.mapValues { entry ->
            val list = entry.value
            CrossTabStats(
                count = list.size,
                percentage = Statistics.safeDivide(list.size * 100, records.size),
                avgUrge = list.map { it.urge_intensity }.average(),
                avgSatisfaction = list.map { it.experience_satisfaction }.average()
            )
        }
        return CorrelationResult(varA, varB, crossTabs, records.map { it.id })
    }
}
