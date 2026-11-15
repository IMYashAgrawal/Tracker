package com.yash.tracker.analytics.engine

import com.yash.tracker.analytics.model.DateRangeFilter
import com.yash.tracker.analytics.model.FrequencyMetrics
import com.yash.tracker.analytics.utils.DateHelpers
import com.yash.tracker.analytics.utils.Statistics
import com.yash.tracker.data.model.HabitRecord
import java.time.temporal.ChronoUnit
import java.time.temporal.WeekFields
import java.util.Locale

object FrequencyAnalyzer {

    fun analyze(
        records: List<HabitRecord>,
        filter: DateRangeFilter
    ): FrequencyMetrics {
        val (start, end) = filter.getEffectiveRange()
        val currentRecords = records.filter {
            val date = DateHelpers.parseDate(it.session_date)
            !date.isBefore(start) && !date.isAfter(end)
        }

        val dailyCounts = currentRecords
            .groupBy { DateHelpers.parseDate(it.session_date) }
            .mapValues { it.value.size }

        val weekFields = WeekFields.of(Locale.getDefault())
        val weeklyCounts = currentRecords
            .groupBy { DateHelpers.parseDate(it.session_date).get(weekFields.weekOfYear()) }
            .mapValues { it.value.size }

        val monthlyCounts = currentRecords
            .groupBy { DateHelpers.parseDate(it.session_date).monthValue }
            .mapValues { it.value.size }

        val daysInRange = ChronoUnit.DAYS.between(start, end) + 1
        val peakEntry = dailyCounts.maxByOrNull { it.value }

        return FrequencyMetrics(
            totalEntries = currentRecords.size,
            dailyCounts = dailyCounts,
            weeklyCounts = weeklyCounts,
            monthlyCounts = monthlyCounts,
            avgEntriesPerDay = Statistics.safeDivide(currentRecords.size, daysInRange),
            avgEntriesPerWeek = Statistics.safeDivide(currentRecords.size, daysInRange / 7.0),
            avgEntriesPerMonth = Statistics.safeDivide(currentRecords.size, daysInRange / 30.0),
            peakDay = peakEntry?.key,
            peakDayCount = peakEntry?.value ?: 0,
            recordIds = currentRecords.map { it.id }
        )
    }
}
