package com.yash.tracker.analytics.engine

import com.yash.tracker.analytics.model.DateRangeFilter
import com.yash.tracker.analytics.model.ControlMetrics
import com.yash.tracker.analytics.utils.DateHelpers
import com.yash.tracker.analytics.utils.Statistics
import com.yash.tracker.data.model.HabitRecord

object ControlAnalyzer {

    fun analyze(
        records: List<HabitRecord>,
        filter: DateRangeFilter
    ): ControlMetrics {
        val (start, end) = filter.getEffectiveRange()
        val currentRecords = records.filter {
            val date = DateHelpers.parseDate(it.session_date)
            !date.isBefore(start) && !date.isAfter(end)
        }

        val plannedCount = currentRecords.count { it.planned == "Planned" }
        val spontaneousCount = currentRecords.count { it.planned == "Spontaneous" }

        val triedResistingCount = currentRecords.count { it.tried_resisting == "Yes" }

        val intensities = currentRecords.map { it.urge_intensity }
        val avgUrge = if (intensities.isEmpty()) 0.0 else intensities.average()
        val maxUrge = intensities.maxOrNull() ?: 0
        val minUrge = intensities.minOrNull() ?: 0

        val controlDist = currentRecords.groupBy { it.perceived_control }.mapValues { it.value.size }

        val urgeByControl = currentRecords.groupBy { it.perceived_control }
            .mapValues { it.value.map { r -> r.urge_intensity }.average() }

        val satByControl = currentRecords.groupBy { it.perceived_control }
            .mapValues { it.value.map { r -> r.experience_satisfaction }.average() }

        return ControlMetrics(
            plannedVsSpontaneous = plannedCount to spontaneousCount,
            postponementPercent = Statistics.safeDivide(triedResistingCount * 100, currentRecords.size),
            urgeIntensityStats = Triple(avgUrge, maxUrge, minUrge),
            perceivedControlDistribution = controlDist,
            avgUrgeByControl = urgeByControl,
            avgSatisfactionByControl = satByControl,
            recordIds = currentRecords.map { it.id }
        )
    }
}
