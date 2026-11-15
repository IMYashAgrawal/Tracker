package com.yash.tracker.analytics.engine

import com.yash.tracker.analytics.model.DateRangeFilter
import com.yash.tracker.analytics.model.LifestyleMetrics
import com.yash.tracker.analytics.utils.DateHelpers
import com.yash.tracker.analytics.utils.Statistics
import com.yash.tracker.data.model.HabitRecord

object LifestyleAnalyzer {

    fun analyze(
        records: List<HabitRecord>,
        filter: DateRangeFilter
    ): LifestyleMetrics {
        val (start, end) = filter.getEffectiveRange()
        val currentRecords = records.filter {
            val date = DateHelpers.parseDate(it.session_date)
            !date.isBefore(start) && !date.isAfter(end)
        }

        val locationRanking = currentRecords.groupBy { it.location }
            .map { it.key to it.value.size }.sortedByDescending { it.second }

        val activityRanking = currentRecords.groupBy { it.activity_before }
            .map { it.key to it.value.size }.sortedByDescending { it.second }

        val aloneCount = currentRecords.count { it.alone == "Yes" }
        val mediaCount = currentRecords.count { it.media_consumed == "Yes" }

        val stimulationRanking = currentRecords.mapNotNull { it.stimulation_type }.flatten()
            .groupBy { it }.map { it.key to it.value.size }.sortedByDescending { it.second }

        val sleepDist = currentRecords.mapNotNull { it.sleep_duration }
            .groupBy { it }.mapValues { it.value.size }

        val affectedRanking = currentRecords.mapNotNull { it.affected_activity }
            .groupBy { it }.map { it.key to it.value.size }.sortedByDescending { it.second }

        return LifestyleMetrics(
            topLocations = locationRanking,
            topActivitiesBeforehand = activityRanking,
            alonePercent = Statistics.safeDivide(aloneCount * 100, currentRecords.size),
            mediaConsumptionPercent = Statistics.safeDivide(mediaCount * 100, currentRecords.size),
            topStimulationTypes = stimulationRanking,
            sleepDurationDistribution = sleepDist,
            topAffectedActivities = affectedRanking,
            recordIds = currentRecords.map { it.id }
        )
    }
}
