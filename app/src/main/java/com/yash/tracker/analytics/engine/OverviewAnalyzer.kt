package com.yash.tracker.analytics.engine

import com.yash.tracker.analytics.model.AnalyticsValue
import com.yash.tracker.analytics.model.DateRangeFilter
import com.yash.tracker.analytics.model.OverviewMetrics
import com.yash.tracker.analytics.utils.DateHelpers
import com.yash.tracker.analytics.utils.Statistics
import com.yash.tracker.data.model.HabitRecord
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

object OverviewAnalyzer {

    fun analyze(
        records: List<HabitRecord>,
        filter: DateRangeFilter
    ): OverviewMetrics {
        val (start, end) = filter.getEffectiveRange()
        val currentRecords = records.filter {
            val date = DateHelpers.parseDate(it.session_date)
            !date.isBefore(start) && !date.isAfter(end)
        }

        val previousRecords = if (filter.compareWithPrevious) {
            val daysDiff = ChronoUnit.DAYS.between(start, end) + 1
            val prevStart = start.minusDays(daysDiff)
            val prevEnd = start.minusDays(1)
            records.filter {
                val date = DateHelpers.parseDate(it.session_date)
                !date.isBefore(prevStart) && !date.isAfter(prevEnd)
            }
        } else emptyList()

        return OverviewMetrics(
            totalEntries = createMetric(currentRecords, previousRecords, filter.compareWithPrevious) { it.size },
            entriesToday = createMetric(currentRecords, previousRecords, filter.compareWithPrevious) { list ->
                val today = LocalDate.now()
                list.count { DateHelpers.parseDate(it.session_date) == today }
            },
            entriesThisWeek = createMetric(currentRecords, previousRecords, filter.compareWithPrevious) { list ->
                val weekStart = LocalDate.now().minusDays(7)
                list.count { !DateHelpers.parseDate(it.session_date).isBefore(weekStart) }
            },
            entriesThisMonth = createMetric(currentRecords, previousRecords, filter.compareWithPrevious) { list ->
                val monthStart = LocalDate.now().withDayOfMonth(1)
                list.count { !DateHelpers.parseDate(it.session_date).isBefore(monthStart) }
            },
            avgEntriesPerWeek = createMetric(currentRecords, previousRecords, filter.compareWithPrevious) { list ->
                val days = ChronoUnit.DAYS.between(start, end) + 1
                Statistics.safeDivide(list.size, days / 7.0)
            },
            avgEntriesPerMonth = createMetric(currentRecords, previousRecords, filter.compareWithPrevious) { list ->
                val days = ChronoUnit.DAYS.between(start, end) + 1
                Statistics.safeDivide(list.size, days / 30.0)
            },
            avgGapMinutes = calculateGapMetric(currentRecords, previousRecords, filter.compareWithPrevious) { gaps ->
                if (gaps.isEmpty()) 0.0 else gaps.average()
            },
            medianGapMinutes = calculateGapMetric(currentRecords, previousRecords, filter.compareWithPrevious) { gaps ->
                Statistics.calculateMedian(gaps)
            },
            longestGapMinutes = calculateGapMetric(currentRecords, previousRecords, filter.compareWithPrevious) { gaps ->
                gaps.maxOrNull()?.toDouble() ?: 0.0
            }.map { it.toLong() },
            shortestGapMinutes = calculateGapMetric(currentRecords, previousRecords, filter.compareWithPrevious) { gaps ->
                gaps.minOrNull()?.toDouble() ?: 0.0
            }.map { it.toLong() },
            avgUrgeIntensity = createMetric(currentRecords, previousRecords, filter.compareWithPrevious) { list ->
                if (list.isEmpty()) 0.0 else list.map { it.urge_intensity }.average()
            },
            avgSatisfaction = createMetric(currentRecords, previousRecords, filter.compareWithPrevious) { list ->
                if (list.isEmpty()) 0.0 else list.map { it.experience_satisfaction }.average()
            },
            spontaneousPercent = createMetric(currentRecords, previousRecords, filter.compareWithPrevious) { list ->
                val count = list.count { it.planned == "Spontaneous" }
                Statistics.safeDivide(count * 100, list.size)
            },
            plannedPercent = createMetric(currentRecords, previousRecords, filter.compareWithPrevious) { list ->
                val count = list.count { it.planned == "Planned" }
                Statistics.safeDivide(count * 100, list.size)
            },
            mediaConsumptionPercent = createMetric(currentRecords, previousRecords, filter.compareWithPrevious) { list ->
                val count = list.count { it.media_consumed == "Yes" }
                Statistics.safeDivide(count * 100, list.size)
            },
            fullControlPercent = createMetric(currentRecords, previousRecords, filter.compareWithPrevious) { list ->
                val count = list.count { it.perceived_control == "Complete control" }
                Statistics.safeDivide(count * 100, list.size)
            },
            activityAffectedPercent = createMetric(currentRecords, previousRecords, filter.compareWithPrevious) { list ->
                val count = list.count { it.activity_impact != "No" }
                Statistics.safeDivide(count * 100, list.size)
            }
        )
    }

    private fun <T : Number> createMetric(
        current: List<HabitRecord>,
        previous: List<HabitRecord>,
        compare: Boolean,
        calc: (List<HabitRecord>) -> T
    ): AnalyticsValue<T> {
        val currVal = calc(current)
        val prevVal = if (compare && previous.isNotEmpty()) calc(previous) else null
        
        return AnalyticsValue(
            value = currVal,
            previousValue = prevVal,
            diff = prevVal?.let { currVal.toDouble() - it.toDouble() },
            percentChange = Statistics.calculatePercentChange(currVal.toDouble(), prevVal?.toDouble()),
            sampleSize = current.size,
            recordIds = current.map { it.id }
        )
    }

    private fun calculateGapMetric(
        current: List<HabitRecord>,
        previous: List<HabitRecord>,
        compare: Boolean,
        calc: (List<Long>) -> Double
    ): AnalyticsValue<Double> {
        val currGaps = getGaps(current)
        val currVal = calc(currGaps)
        
        val prevGaps = if (compare && previous.isNotEmpty()) getGaps(previous) else emptyList()
        val prevVal = if (compare && prevGaps.isNotEmpty()) calc(prevGaps) else null

        return AnalyticsValue(
            value = currVal,
            previousValue = prevVal,
            diff = prevVal?.let { currVal - it },
            percentChange = Statistics.calculatePercentChange(currVal, prevVal),
            sampleSize = current.size,
            recordIds = current.map { it.id }
        )
    }

    private fun getGaps(records: List<HabitRecord>): List<Long> {
        if (records.size < 2) return emptyList()
        val sorted = records.map { 
            DateHelpers.toLocalDateTime(it.session_date, it.session_time) 
        }.sorted()
        
        return sorted.zipWithNext { a, b -> ChronoUnit.MINUTES.between(a, b) }
    }
}
