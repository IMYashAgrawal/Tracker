package com.yash.tracker.analytics.engine

import com.yash.tracker.analytics.model.DateRangeFilter
import com.yash.tracker.analytics.model.Insight
import com.yash.tracker.analytics.model.InsightConfidence
import com.yash.tracker.analytics.utils.DateHelpers
import com.yash.tracker.data.model.HabitRecord

object InsightGenerator {
    private const val MIN_TOTAL = 5
    private const val MIN_SUBGROUP = 3

    fun generate(
        records: List<HabitRecord>,
        filter: DateRangeFilter
    ): List<Insight> {
        val (start, end) = filter.getEffectiveRange()
        val currentRecords = records.filter {
            val date = DateHelpers.parseDate(it.session_date)
            !date.isBefore(start) && !date.isAfter(end)
        }

        if (currentRecords.size < MIN_TOTAL) return emptyList()

        val insights = mutableListOf<Insight>()

        // Insight 1: Peak Time
        val hourGroups = currentRecords.groupBy { DateHelpers.parseTime(it.session_time).hour }
        val peakHour = hourGroups.maxByOrNull { it.value.size }
        if (peakHour != null && peakHour.value.size >= MIN_SUBGROUP) {
            insights.add(Insight(
                title = "Peak Activity Time",
                description = "You are most active around ${peakHour.key}:00. This accounts for ${((peakHour.value.size.toDouble() / currentRecords.size) * 100).toInt()}% of your sessions.",
                confidence = if (peakHour.value.size > 5) InsightConfidence.HIGH else InsightConfidence.MODERATE,
                n = peakHour.value.size,
                recordIds = peakHour.value.map { it.id }
            ))
        }

        // Insight 2: Top Trigger
        val triggerGroups = currentRecords.groupBy { it.primary_trigger }
        val topTrigger = triggerGroups.maxByOrNull { it.value.size }
        if (topTrigger != null && topTrigger.value.size >= MIN_SUBGROUP) {
            insights.add(Insight(
                title = "Primary Influence",
                description = "'${topTrigger.key}' is your most frequent influence. Understanding what leads to this might be helpful.",
                confidence = if (topTrigger.value.size > 5) InsightConfidence.HIGH else InsightConfidence.MODERATE,
                n = topTrigger.value.size,
                recordIds = topTrigger.value.map { it.id }
            ))
        }

        // Insight 3: Satisfaction vs Planned
        val plannedSat = currentRecords.filter { it.planned == "Planned" }.map { it.experience_satisfaction }.average()
        val spontaneousSat = currentRecords.filter { it.planned == "Spontaneous" }.map { it.experience_satisfaction }.average()
        
        if (!plannedSat.isNaN() && !spontaneousSat.isNaN()) {
            val diff = plannedSat - spontaneousSat
            if (Math.abs(diff) > 1.0) {
                val winner = if (diff > 0) "planned" else "spontaneous"
                insights.add(Insight(
                    title = "Planning Impact",
                    description = "Your $winner sessions tend to be more satisfying than others.",
                    confidence = InsightConfidence.MODERATE,
                    n = currentRecords.size,
                    recordIds = currentRecords.map { it.id }
                ))
            }
        }

        return insights
    }
}
