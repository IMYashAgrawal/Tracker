package com.yash.tracker.analytics.utils

object Statistics {
    fun safeDivide(numerator: Number, denominator: Number): Double {
        if (denominator.toDouble() == 0.0) return 0.0
        return numerator.toDouble() / denominator.toDouble()
    }

    fun calculateMedian(values: List<Long>): Double {
        if (values.isEmpty()) return 0.0
        val sorted = values.sorted()
        return if (sorted.size % 2 == 0) {
            (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]).toDouble() / 2
        } else {
            sorted[sorted.size / 2].toDouble()
        }
    }

    fun calculatePercentChange(current: Double, previous: Double?): Double? {
        if (previous == null || previous == 0.0) return null
        return ((current - previous) / previous) * 100.0
    }
}
