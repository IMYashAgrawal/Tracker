package com.yash.tracker.analytics.engine

import com.yash.tracker.analytics.model.DateRangeFilter
import com.yash.tracker.analytics.model.TriggerMetrics
import com.yash.tracker.analytics.utils.DateHelpers
import com.yash.tracker.analytics.utils.Statistics
import com.yash.tracker.data.model.HabitRecord

object TriggerAnalyzer {

    fun analyze(
        records: List<HabitRecord>,
        filter: DateRangeFilter
    ): TriggerMetrics {
        val (start, end) = filter.getEffectiveRange()
        val currentRecords = records.filter {
            val date = DateHelpers.parseDate(it.session_date)
            !date.isBefore(start) && !date.isAfter(end)
        }

        val primaryRanking = currentRecords
            .groupBy { it.primary_trigger }
            .map { it.key to it.value.size }
            .sortedByDescending { it.second }

        val allTriggers = currentRecords.flatMap { it.triggers }
        val overallFrequency = allTriggers
            .groupBy { it }
            .mapValues { Statistics.safeDivide(it.value.size * 100, currentRecords.size) }

        val secondaryTriggers = currentRecords.flatMap { record ->
            record.triggers.filter { it != record.primary_trigger }
        }
        val secondaryRanking = secondaryTriggers
            .groupBy { it }
            .map { it.key to it.value.size }
            .sortedByDescending { it.second }

        val combinations = currentRecords
            .map { it.triggers.sorted() }
            .filter { it.size > 1 }
            .groupBy { it }
            .map { it.key to it.value.size }
            .sortedByDescending { it.second }
            .take(10)

        val urgeByTrigger = currentRecords
            .groupBy { it.primary_trigger }
            .mapValues { it.value.map { r -> r.urge_intensity }.average() }

        val satByTrigger = currentRecords
            .groupBy { it.primary_trigger }
            .mapValues { it.value.map { r -> r.experience_satisfaction }.average() }

        return TriggerMetrics(
            primaryTriggerRanking = primaryRanking,
            secondaryTriggerRanking = secondaryRanking,
            overallTriggerFrequency = overallFrequency,
            topCombinations = combinations,
            avgUrgePerTrigger = urgeByTrigger,
            avgSatisfactionPerTrigger = satByTrigger,
            recordIds = currentRecords.map { it.id }
        )
    }
}
