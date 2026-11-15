package com.yash.tracker.analytics.model

data class EmotionalMetrics(
    val topEmotionsBefore: List<Pair<String, Int>>,
    val topEmotionsAfter: List<Pair<String, Int>>,
    val motivationRanking: List<Pair<String, Int>>,
    val avgSatisfactionByTrigger: Map<String, Double>,
    val avgUrgeByEmotionBefore: Map<String, Double>,
    val decisionSatisfactionPercents: Map<String, Double>,
    val recordIds: List<String>
)
