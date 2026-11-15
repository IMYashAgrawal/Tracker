package com.yash.tracker.analytics.model

data class LifestyleMetrics(
    val topLocations: List<Pair<String, Int>>,
    val topActivitiesBeforehand: List<Pair<String, Int>>,
    val alonePercent: Double,
    val mediaConsumptionPercent: Double,
    val topStimulationTypes: List<Pair<String, Int>>,
    val sleepDurationDistribution: Map<String, Int>,
    val topAffectedActivities: List<Pair<String, Int>>,
    val recordIds: List<String>
)
