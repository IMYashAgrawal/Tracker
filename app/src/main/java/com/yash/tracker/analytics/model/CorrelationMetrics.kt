package com.yash.tracker.analytics.model

data class CorrelationResult(
    val variableA: String,
    val variableB: String,
    val crossTabs: Map<String, CrossTabStats>,
    val recordIds: List<String>
)

data class CrossTabStats(
    val count: Int,
    val percentage: Double,
    val avgUrge: Double,
    val avgSatisfaction: Double
)

data class CorrelationMetrics(
    val correlations: List<CorrelationResult>,
    val recordIds: List<String>
)
