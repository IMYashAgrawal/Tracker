package com.yash.tracker.analytics.engine

import com.yash.tracker.analytics.model.DateRangeFilter
import com.yash.tracker.analytics.model.TimeMetrics
import com.yash.tracker.analytics.utils.DateHelpers
import com.yash.tracker.data.model.HabitRecord

object TimeAnalyzer {

    fun analyze(
        records: List<HabitRecord>,
        filter: DateRangeFilter
    ): TimeMetrics {
        val (start, end) = filter.getEffectiveRange()
        val currentRecords = records.filter {
            val date = DateHelpers.parseDate(it.session_date)
            !date.isBefore(start) && !date.isAfter(end)
        }

        val bucketCounts = currentRecords
            .groupBy { DateHelpers.getTimeBucket(DateHelpers.parseTime(it.session_time)) }
            .mapValues { it.value.size }

        val hourlyCounts = currentRecords
            .groupBy { DateHelpers.parseTime(it.session_time).hour }
            .mapValues { it.value.size }

        val weekdayCounts = currentRecords
            .groupBy { DateHelpers.parseDate(it.session_date).dayOfWeek.value }
            .mapValues { it.value.size }

        val weekdayCount = weekdayCounts.filter { it.key in 1..5 }.values.sum()
        val weekendCount = weekdayCounts.filter { it.key in 6..7 }.values.sum()

        return TimeMetrics(
            bucketCounts = bucketCounts,
            hourlyCounts = hourlyCounts,
            weekdayCounts = weekdayCounts,
            mostCommonTimeBucket = bucketCounts.maxByOrNull { it.value }?.key,
            mostCommonHour = hourlyCounts.maxByOrNull { it.value }?.key,
            mostCommonWeekday = weekdayCounts.maxByOrNull { it.value }?.key,
            weekdayVsWeekend = weekdayCount to weekendCount,
            recordIds = currentRecords.map { it.id }
        )
    }
}
