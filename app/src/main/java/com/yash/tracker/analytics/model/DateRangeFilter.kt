package com.yash.tracker.analytics.model

import java.time.LocalDate

enum class DateRangeType {
    LAST_7_DAYS, LAST_30_DAYS, LAST_90_DAYS, LAST_6_MONTHS, THIS_YEAR, ALL_TIME, CUSTOM
}

data class DateRangeFilter(
    val type: DateRangeType = DateRangeType.LAST_30_DAYS,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val compareWithPrevious: Boolean = false
) {
    fun getEffectiveRange(): Pair<LocalDate, LocalDate> {
        val end = endDate ?: LocalDate.now()
        val start = startDate ?: when (type) {
            DateRangeType.LAST_7_DAYS -> end.minusDays(7)
            DateRangeType.LAST_30_DAYS -> end.minusDays(30)
            DateRangeType.LAST_90_DAYS -> end.minusDays(90)
            DateRangeType.LAST_6_MONTHS -> end.minusMonths(6)
            DateRangeType.THIS_YEAR -> end.withDayOfYear(1)
            DateRangeType.ALL_TIME -> LocalDate.of(2000, 1, 1)
            DateRangeType.CUSTOM -> startDate ?: end.minusDays(30)
        }
        return start to end
    }
}
