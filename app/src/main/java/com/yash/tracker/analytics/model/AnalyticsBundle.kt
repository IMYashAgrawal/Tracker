package com.yash.tracker.analytics.model

data class AnalyticsValue<T>(
    val value: T,
    val previousValue: T? = null,
    val diff: Double? = null,
    val percentChange: Double? = null,
    val sampleSize: Int,
    val recordIds: List<String>
) {
    fun <R> map(transform: (T) -> R): AnalyticsValue<R> {
        return AnalyticsValue(
            value = transform(value),
            previousValue = previousValue?.let { transform(it) },
            diff = diff,
            percentChange = percentChange,
            sampleSize = sampleSize,
            recordIds = recordIds
        )
    }
}

data class OverviewMetrics(
    val totalEntries: AnalyticsValue<Int>,
    val entriesToday: AnalyticsValue<Int>,
    val entriesThisWeek: AnalyticsValue<Int>,
    val entriesThisMonth: AnalyticsValue<Int>,
    val avgEntriesPerWeek: AnalyticsValue<Double>,
    val avgEntriesPerMonth: AnalyticsValue<Double>,
    val avgGapMinutes: AnalyticsValue<Double>,
    val medianGapMinutes: AnalyticsValue<Double>,
    val longestGapMinutes: AnalyticsValue<Long>,
    val shortestGapMinutes: AnalyticsValue<Long>,
    val avgUrgeIntensity: AnalyticsValue<Double>,
    val avgSatisfaction: AnalyticsValue<Double>,
    val spontaneousPercent: AnalyticsValue<Double>,
    val plannedPercent: AnalyticsValue<Double>,
    val mediaConsumptionPercent: AnalyticsValue<Double>,
    val fullControlPercent: AnalyticsValue<Double>,
    val activityAffectedPercent: AnalyticsValue<Double>
)
