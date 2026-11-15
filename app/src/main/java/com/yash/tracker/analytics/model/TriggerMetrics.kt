package com.yash.tracker.analytics.model

data class TriggerMetrics(
    val primaryTriggerRanking: List<Pair<String, Int>>,
    val secondaryTriggerRanking: List<Pair<String, Int>>,
    val overallTriggerFrequency: Map<String, Double>, // Percentage
    val topCombinations: List<Pair<List<String>, Int>>,
    val avgUrgePerTrigger: Map<String, Double>,
    val avgSatisfactionPerTrigger: Map<String, Double>,
    val recordIds: List<String>
)
