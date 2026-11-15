package com.yash.tracker.analytics.model

data class TimeMetrics(
    val bucketCounts: Map<String, Int>,
    val hourlyCounts: Map<Int, Int>,
    val weekdayCounts: Map<Int, Int>, // 1 (Mon) to 7 (Sun)
    val mostCommonTimeBucket: String?,
    val mostCommonHour: Int?,
    val mostCommonWeekday: Int?,
    val weekdayVsWeekend: Pair<Int, Int>, // Count for Weekday, Count for Weekend
    val recordIds: List<String>
)
