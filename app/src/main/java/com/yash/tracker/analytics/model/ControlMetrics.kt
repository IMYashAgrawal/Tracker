package com.yash.tracker.analytics.model

data class ControlMetrics(
    val plannedVsSpontaneous: Pair<Int, Int>,
    val postponementPercent: Double,
    val urgeIntensityStats: Triple<Double, Int, Int>, // Avg, Max, Min
    val perceivedControlDistribution: Map<String, Int>,
    val avgUrgeByControl: Map<String, Double>,
    val avgSatisfactionByControl: Map<String, Double>,
    val recordIds: List<String>
)
