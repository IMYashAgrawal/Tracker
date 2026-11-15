package com.yash.tracker.analytics.model

enum class InsightConfidence {
    LOW, MODERATE, HIGH
}

data class Insight(
    val title: String,
    val description: String,
    val confidence: InsightConfidence,
    val n: Int,
    val recordIds: List<String>
)
