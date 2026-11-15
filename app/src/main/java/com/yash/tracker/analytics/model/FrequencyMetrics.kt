package com.yash.tracker.analytics.model

import java.time.LocalDate

data class FrequencyMetrics(
    val totalEntries: Int,
    val dailyCounts: Map<LocalDate, Int>,
    val weeklyCounts: Map<Int, Int>, // Week of year
    val monthlyCounts: Map<Int, Int>, // Month value
    val avgEntriesPerDay: Double,
    val avgEntriesPerWeek: Double,
    val avgEntriesPerMonth: Double,
    val peakDay: LocalDate?,
    val peakDayCount: Int,
    val recordIds: List<String>
)
